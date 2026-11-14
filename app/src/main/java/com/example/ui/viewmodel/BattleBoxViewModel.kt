package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BattleBoxConciergeRepository
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceRejection
import com.example.data.model.AuditLogEntry
import com.example.data.model.ConsentRecord
import com.example.data.model.GymAnnouncement
import com.example.data.model.GymConfig
import com.example.data.model.GymPlan
import com.example.data.model.GymPublicStatus
import com.example.data.model.MemberSubscription
import com.example.data.model.NotificationDeliveryLog
import com.example.data.model.NotificationPreference
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentRequest
import com.example.data.model.PersonalRecordEntry
import com.example.data.model.Phase3BillingConfig
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutProfile
import com.example.data.repository.BattleBoxRepository
import com.example.data.repository.CheckInOutcome
import com.example.data.repository.Phase2Repository
import com.example.data.repository.WorkoutRepository
import com.example.security.DeviceFix
import com.example.ui.components.CyberHudAlert
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen
import com.example.utils.AttendanceCsvExporter
import com.example.utils.BattleBoxNotificationHelper
import com.example.utils.BattleBoxSoundEngine
import com.example.utils.MonthlyAttendanceSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConciergeMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class BattleBoxViewModel(
    private val repository: BattleBoxRepository,
    private val phase2Repository: Phase2Repository,
    private val conciergeRepository: BattleBoxConciergeRepository,
    private val workoutRepository: WorkoutRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    val isEmailVerified: Boolean
) : ViewModel() {

    val isBootstrappedOwner: Boolean =
        isEmailVerified && currentUserEmail.equals(OWNER_ADMIN_EMAIL, ignoreCase = true)

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestore(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", root)
                emit(UiState.Error(root.message ?: "Failed to load Battle Box profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val gymConfig: StateFlow<GymConfig> =
        repository.observeGymConfig()
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing gym config", error)
                emit(GymConfig())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = GymConfig()
            )

    val gymPublicStatus: StateFlow<GymPublicStatus> =
        repository.observeGymPublicStatus()
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing gym public status", error)
                emit(GymPublicStatus())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = GymPublicStatus()
            )

    val myAttendance: StateFlow<List<AttendanceRecord>> =
        repository.observeMyAttendance(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing my attendance", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val myConsents: StateFlow<List<ConsentRecord>> =
        repository.observeMyConsents(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing consent receipts", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val rejections: StateFlow<List<AttendanceRejection>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile == null || !profile.onboardingCompleted) {
                flowOf(emptyList())
            } else {
                val isStaff = profile.role == UserProfile.ROLE_ADMIN ||
                    profile.role == UserProfile.ROLE_TRAINER ||
                    isBootstrappedOwner
                repository.observeRecentRejections(currentUserId, isStaff)
                    .catch { error ->
                        if (unwrapFirestore(error) == null && error is CancellationException) throw error
                        emit(emptyList())
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Admin-only stream: full user directory
    val allUsers: StateFlow<List<UserProfile>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && (profile.role == UserProfile.ROLE_ADMIN || isBootstrappedOwner)) {
                repository.observeAllUsers().catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Staff live occupancy monitoring stream: enabled for Admin and Trainer
    val liveRoster: StateFlow<List<AttendanceRecord>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && (
                    profile.role == UserProfile.ROLE_ADMIN ||
                        profile.role == UserProfile.ROLE_TRAINER ||
                        isBootstrappedOwner
                    )
            ) {
                repository.observeLiveAttendanceRoster().catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val auditLogs: StateFlow<List<AuditLogEntry>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && (profile.role == UserProfile.ROLE_ADMIN || isBootstrappedOwner)) {
                repository.observeAuditLogs().catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Trainer-only stream: strictly assigned members
    val assignedMembers: StateFlow<List<UserProfile>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && profile.role == UserProfile.ROLE_TRAINER) {
                repository.observeAssignedMembers(currentUserId).catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Phase 2: Plans, Payments, Subscriptions & Notification Opt-Ins
    val plans: StateFlow<List<GymPlan>> =
        phase2Repository.observePlans()
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(GymPlan.defaultSeedPlans())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GymPlan.defaultSeedPlans())

    val adminPlans: StateFlow<List<GymPlan>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && (profile.role == UserProfile.ROLE_ADMIN || isBootstrappedOwner)) {
                phase2Repository.observeAllPlansForAdmin().catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(GymPlan.defaultSeedPlans())
                }
            } else {
                flowOf(GymPlan.defaultSeedPlans())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GymPlan.defaultSeedPlans())

    val allSubscriptions: StateFlow<List<MemberSubscription>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && (profile.role == UserProfile.ROLE_ADMIN || isBootstrappedOwner)) {
                phase2Repository.observeAllSubscriptions().catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val billingConfig: StateFlow<Phase3BillingConfig> =
        phase2Repository.observeBillingConfig()
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(Phase3BillingConfig())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), Phase3BillingConfig())

    val myPaymentRequests: StateFlow<List<PaymentRequest>> =
        phase2Repository.observeMyPaymentRequests(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allPaymentRequests: StateFlow<List<PaymentRequest>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile != null && (profile.role == UserProfile.ROLE_ADMIN || isBootstrappedOwner)) {
                phase2Repository.observeAllPaymentRequests().catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val mySubscriptions: StateFlow<List<MemberSubscription>> =
        phase2Repository.observeMySubscriptions(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myPayments: StateFlow<List<PaymentRecord>> =
        phase2Repository.observeMyPayments(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notificationPref: StateFlow<NotificationPreference> =
        phase2Repository.observeNotificationPreference(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(NotificationPreference(userId = currentUserId))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                NotificationPreference(userId = currentUserId)
            )

    val announcements: StateFlow<List<GymAnnouncement>> =
        phase2Repository.observeAnnouncements()
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val deliveryLogs: StateFlow<List<NotificationDeliveryLog>> =
        userProfileState.flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            if (profile == null || !profile.onboardingCompleted) {
                flowOf(emptyList())
            } else {
                val isAdmin = profile.role == UserProfile.ROLE_ADMIN || isBootstrappedOwner
                phase2Repository.observeDeliveryLogs(currentUserId, isAdmin).catch { error ->
                    if (unwrapFirestore(error) == null && error is CancellationException) throw error
                    emit(emptyList())
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val workoutProfile: StateFlow<WorkoutProfile> =
        workoutRepository.observeWorkoutProfile(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing workout profile", error)
                emit(WorkoutProfile(id = currentUserId, userId = currentUserId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = WorkoutProfile(id = currentUserId, userId = currentUserId)
            )

    val workoutLogs: StateFlow<List<WorkoutLog>> =
        workoutRepository.observeMyWorkoutLogs(currentUserId)
            .catch { error ->
                if (unwrapFirestore(error) == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing workout logs", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _isWorkoutMode = MutableStateFlow(false)
    val isWorkoutMode: StateFlow<Boolean> = _isWorkoutMode.asStateFlow()

    private val _hudAlert = MutableStateFlow<CyberHudAlert?>(null)
    val hudAlert: StateFlow<CyberHudAlert?> = _hudAlert.asStateFlow()

    val isSoundMuted: StateFlow<Boolean> = BattleBoxSoundEngine.isMuted

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _lastScanOutcome = MutableStateFlow<CheckInOutcome?>(null)
    val lastScanOutcome: StateFlow<CheckInOutcome?> = _lastScanOutcome.asStateFlow()

    private val _monthlyExportState = MutableStateFlow<UiState<MonthlyAttendanceSummary>?>(null)
    val monthlyExportState: StateFlow<UiState<MonthlyAttendanceSummary>?> = _monthlyExportState.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ConciergeMessage(
                id = "welcome",
                isUser = false,
                text = "Welcome to Battle Box : FFF (Fight For Fitness). Ask me about IST gym timings, directions, 30s rotating QR check-in, geofencing, Jerai/Aerofit stations, or our draft DPDP Act 2023 privacy terms."
            )
        )
    )
    val chatMessages: StateFlow<List<ConciergeMessage>> = _chatMessages.asStateFlow()

    private val _isConciergeBusy = MutableStateFlow(false)
    val isConciergeBusy: StateFlow<Boolean> = _isConciergeBusy.asStateFlow()

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleWorkoutMode(enabled: Boolean? = null) {
        val next = enabled ?: !_isWorkoutMode.value
        _isWorkoutMode.value = next
        if (next) {
            BattleBoxSoundEngine.play(BattleBoxSoundEngine.SoundType.QR_SCAN_LOCK)
        }
    }

    fun toggleSoundMute() {
        val muted = BattleBoxSoundEngine.toggleMute()
        _actionBanner.value = if (muted) "Cyberpunk HUD sounds muted." else "Cyberpunk HUD sounds active."
    }

    fun emitHudAlert(
        badge: String,
        title: String,
        message: String,
        accentColor: Color = CyberCyan,
        sound: BattleBoxSoundEngine.SoundType? = null
    ) {
        _hudAlert.value = CyberHudAlert(
            id = "hud_${System.currentTimeMillis()}",
            title = title,
            message = message,
            badge = badge,
            accentColor = accentColor
        )
        if (sound != null) {
            BattleBoxSoundEngine.play(sound)
        }
    }

    fun dismissHudAlert() {
        _hudAlert.value = null
    }

    fun dismissBanner() {
        _actionBanner.value = null
    }

    fun clearLastScanOutcome() {
        _lastScanOutcome.value = null
    }

    fun showBanner(message: String) {
        _actionBanner.value = message
    }

    fun completeOnboarding(
        displayName: String,
        phone: String?,
        emergencyContact: String?,
        photoUrl: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.completeOnboarding(
                displayName = displayName,
                email = currentUserEmail,
                phone = phone,
                emergencyContact = emergencyContact,
                photoUrl = photoUrl,
                isBootstrappedOwner = isBootstrappedOwner
            )
            _isBusy.value = false
            result.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Failed to save onboarding consent."
            }.onSuccess {
                _actionBanner.value = "Onboarding & DPDP consent saved."
                emitHudAlert(
                    badge = "ONLINE",
                    title = "Battle Box : FFF Activated",
                    message = "Athlete profile synced. Ready for Workout Mode & QR pass.",
                    accentColor = CyberNeonGreen,
                    sound = BattleBoxSoundEngine.SoundType.PASS_CHECK_IN
                )
            }
        }
    }

    fun performKioskCheckIn(
        scannedQrPayload: String,
        currentUser: UserProfile,
        deviceFix: DeviceFix?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val outcome = repository.performKioskSelfCheckIn(
                scannedQrPayload = scannedQrPayload,
                currentUser = currentUser,
                gymConfig = gymConfig.value,
                deviceFix = deviceFix
            )
            _isBusy.value = false
            when (outcome) {
                is CheckInOutcome.CheckedIn -> {
                    val msg = "Checked in (${outcome.distanceMeters.toInt()} m from Battle Box : FFF, ±${outcome.accuracyMeters.toInt()} m GPS). Auto-checkout in ${gymConfig.value.autoCheckoutHours}h."
                    _actionBanner.value = msg
                    emitHudAlert(
                        badge = "PASS IN",
                        title = "Wall QR Verified",
                        message = msg,
                        accentColor = CyberNeonGreen,
                        sound = BattleBoxSoundEngine.SoundType.PASS_CHECK_IN
                    )
                }
                is CheckInOutcome.Rejected -> {
                    _actionBanner.value = "Check-in rejected: ${outcome.reason}"
                    emitHudAlert(
                        badge = "DENIED",
                        title = "Check-In Blocked",
                        message = outcome.reason,
                        accentColor = CyberMagenta
                    )
                }
            }
        }
    }

    fun performFrontDeskScan(
        scannedMemberQr: String,
        scannerFix: DeviceFix?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val outcome = repository.performFrontDeskScanCheckIn(
                scannedMemberQr = scannedMemberQr,
                scannerFix = scannerFix,
                gymConfig = gymConfig.value
            )
            _isBusy.value = false
            _lastScanOutcome.value = outcome
            when (outcome) {
                is CheckInOutcome.CheckedIn -> {
                    val msg = "Front Desk verified ${outcome.memberName} (${outcome.distanceMeters.toInt()} m)."
                    _actionBanner.value = msg
                    emitHudAlert(
                        badge = "DESK PASS",
                        title = "${outcome.memberName} Checked In",
                        message = msg,
                        accentColor = CyberNeonGreen,
                        sound = BattleBoxSoundEngine.SoundType.PASS_CHECK_IN
                    )
                }
                is CheckInOutcome.Rejected -> {
                    _actionBanner.value = "Front Desk rejected scan: ${outcome.reason}"
                    emitHudAlert(
                        badge = "SCAN REJECT",
                        title = "QR Verification Failed",
                        message = outcome.reason,
                        accentColor = CyberMagenta
                    )
                }
            }
        }
    }

    fun performTrainerQrScan(
        scannedMemberQr: String,
        scannerFix: DeviceFix?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val outcome = repository.performTrainerQrScanCheckIn(
                scannedMemberQr = scannedMemberQr,
                scannerFix = scannerFix,
                gymConfig = gymConfig.value
            )
            _isBusy.value = false
            _lastScanOutcome.value = outcome
            when (outcome) {
                is CheckInOutcome.CheckedIn -> {
                    val msg = "Attendance marked for ${outcome.memberName} (${outcome.distanceMeters.toInt()} m)."
                    _actionBanner.value = msg
                    emitHudAlert(
                        badge = "QR PASS",
                        title = "${outcome.memberName} Verified",
                        message = msg,
                        accentColor = CyberNeonGreen,
                        sound = BattleBoxSoundEngine.SoundType.PASS_CHECK_IN
                    )
                }
                is CheckInOutcome.Rejected -> {
                    _actionBanner.value = "QR scan rejected: ${outcome.reason}"
                    emitHudAlert(
                        badge = "SCAN REJECT",
                        title = "QR Scan Rejected",
                        message = outcome.reason,
                        accentColor = CyberMagenta
                    )
                }
            }
        }
    }

    fun checkOut(attendanceId: String, isAdminAction: Boolean = false, memberName: String = "") {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.checkOutSession(attendanceId, isAdminAction, memberName)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Check-out failed."
            }.onSuccess {
                val msg = if (isAdminAction) "Checked out $memberName." else "Checked out. Great Battle Box : FFF session!"
                _actionBanner.value = msg
                emitHudAlert(
                    badge = "CHECK-OUT",
                    title = if (isAdminAction) "Staff Checkout" else "Session Ended",
                    message = msg,
                    accentColor = CyberAmber,
                    sound = BattleBoxSoundEngine.SoundType.CHECK_OUT
                )
            }
        }
    }

    fun checkGeofenceAutoOut(
        context: Context,
        activeSession: AttendanceRecord,
        deviceFix: DeviceFix
    ) {
        viewModelScope.launch {
            val res = repository.autoCheckOutIfOutsideGeofence(activeSession, deviceFix, gymConfig.value)
            res.onSuccess { check ->
                if (!check.isInsideGeofence) {
                    val reason = check.violationReason
                        ?: "Auto checked-out: outside ${gymConfig.value.geofenceRadiusM.toInt()}m geofence."
                    BattleBoxNotificationHelper.showGeofenceAutoOutNotification(
                        context = context,
                        attendanceId = activeSession.id,
                        reason = reason
                    )
                    emitHudAlert(
                        badge = "AUTO-OUT",
                        title = "Geofence Auto Check-Out",
                        message = reason,
                        accentColor = CyberMagenta,
                        sound = BattleBoxSoundEngine.SoundType.CHECK_OUT
                    )
                    _actionBanner.value = reason
                }
            }
        }
    }

    fun logQuickMacros(addKcal: Int, addProteinG: Int, addActiveMin: Int) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = workoutRepository.recordQuickMacroEntry(
                current = workoutProfile.value,
                addCalories = addKcal,
                addProteinG = addProteinG,
                addActiveMinutes = addActiveMin
            )
            _isBusy.value = false
            res.onSuccess {
                _actionBanner.value = "Logged +$addKcal kcal / +${addProteinG}g protein / +${addActiveMin}m active."
                emitHudAlert(
                    badge = "MACRO SYNC",
                    title = "Readiness Updated",
                    message = "+$addKcal kcal • +${addProteinG}g protein synced to Firestore.",
                    accentColor = CyberCyan,
                    sound = BattleBoxSoundEngine.SoundType.QR_SCAN_LOCK
                )
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Failed to save quick macro log."
            }
        }
    }

    fun updateBodyMetrics(weightKg: Double, heightCm: Double) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = workoutRepository.updateBodyMetrics(
                current = workoutProfile.value,
                newWeightKg = weightKg,
                newHeightCm = heightCm
            )
            _isBusy.value = false
            res.onSuccess { updated ->
                val bmiText = "BMI ${WorkoutProfile.formatBmi(updated.bmiRatio)} (${updated.bmiLabel})"
                _actionBanner.value = "Body metrics saved: $bmiText"
                emitHudAlert(
                    badge = "BMI TELEMETRY",
                    title = bmiText,
                    message = "${updated.weightKg.toInt()} kg • ${updated.heightCm.toInt()} cm recorded.",
                    accentColor = CyberNeonGreen,
                    sound = BattleBoxSoundEngine.SoundType.QR_SCAN_LOCK
                )
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Failed to save BMI body metrics."
            }
        }
    }

    fun completeWorkoutSession(
        workoutTitle: String,
        durationSeconds: Long,
        totalVolumeKg: Double,
        completedSets: Int,
        exercisesCompleted: Int,
        updatedPrs: List<PersonalRecordEntry>,
        prLabel: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = workoutRepository.completeAndSaveWorkoutSession(
                currentProfile = workoutProfile.value,
                workoutTitle = workoutTitle,
                durationSeconds = durationSeconds,
                totalVolumeKg = totalVolumeKg,
                completedSets = completedSets,
                exercisesCompleted = exercisesCompleted,
                updatedPrs = updatedPrs,
                prAchievedLabel = prLabel
            )
            _isBusy.value = false
            res.onSuccess {
                val msg = "Completed $workoutTitle • ${totalVolumeKg.toInt()} kg volume • $completedSets sets."
                _actionBanner.value = msg
                emitHudAlert(
                    badge = "SESSION LOGGED",
                    title = "Streak & Volume Saved",
                    message = msg,
                    accentColor = CyberNeonGreen,
                    sound = BattleBoxSoundEngine.SoundType.PASS_CHECK_IN
                )
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "Failed to save workout session."
            }
        }
    }

    fun sweepExpired(records: List<AttendanceRecord>, isAdmin: Boolean) {
        viewModelScope.launch {
            repository.sweepExpiredSessions(records, isAdmin)
        }
    }

    fun adminUpdateUser(
        targetUser: UserProfile,
        newRole: String,
        newPlanTier: String,
        newMembershipStatus: String,
        newAssignedTrainerId: String?,
        newDisplayName: String = targetUser.displayName,
        newPhone: String? = targetUser.phone,
        newEmergencyContact: String? = targetUser.emergencyContact
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.adminUpdateUser(
                targetUser = targetUser,
                newRole = newRole,
                newPlanTier = newPlanTier,
                newMembershipStatus = newMembershipStatus,
                newAssignedTrainerId = newAssignedTrainerId,
                newDisplayName = newDisplayName,
                newPhone = newPhone,
                newEmergencyContact = newEmergencyContact
            )
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to update user."
            }.onSuccess {
                _actionBanner.value = "Updated ${newDisplayName.ifBlank { targetUser.displayName }} (audit logged)."
            }
        }
    }

    fun updateGymConfig(updated: GymConfig) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateGymConfig(updated)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to update gym settings."
            }.onSuccess {
                _actionBanner.value = "Battle Box settings saved & audit logged."
            }
        }
    }

    fun loadMonthlyAttendanceForExport(year: Int, monthOneBased: Int) {
        viewModelScope.launch {
            _isBusy.value = true
            _monthlyExportState.value = UiState.Loading
            val res = repository.fetchMonthlyAttendanceRecords(year, monthOneBased)
            _isBusy.value = false
            res.onSuccess { records ->
                val summary = AttendanceCsvExporter.buildSummaryAndCsv(year, monthOneBased, records)
                _monthlyExportState.value = UiState.Success(summary)
                _actionBanner.value = "Prepared ${summary.totalRecords} log(s) for ${summary.monthLabel} CSV."
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to load monthly attendance logs."
                _monthlyExportState.value = UiState.Error(msg)
                _actionBanner.value = msg
            }
        }
    }

    // Phase 2 Actions
    fun createCheckoutIntent(
        user: UserProfile,
        plan: GymPlan,
        paymentMode: String,
        preferredUpiApp: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.createCheckoutIntent(user, plan, paymentMode, preferredUpiApp)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to create checkout intent."
            }.onSuccess { reqId ->
                val label = if (paymentMode == PaymentRequest.MODE_UPI_AUTOPAY) "UPI AutoPay mandate" else "Razorpay Order"
                _actionBanner.value = "$label intent logged ($reqId). Awaiting Cloud Functions webhook settlement."
            }
        }
    }

    fun requestAutopayCancellation(subscriptionId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.requestAutopayCancellation(subscriptionId)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to request AutoPay cancellation."
            }.onSuccess {
                _actionBanner.value = "AutoPay cancellation requested. No further renewals will be drafted."
            }
        }
    }

    fun saveNotificationPreference(pref: NotificationPreference) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.saveNotificationPreference(pref)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to save notification settings."
            }.onSuccess {
                _actionBanner.value = "WhatsApp & notification preferences saved."
            }
        }
    }

    fun adminSeedDefaultPlans() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminSeedDefaultPlans()
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to seed membership plans."
            }.onSuccess {
                _actionBanner.value = "6 Battle Box INR plans seeded to Firestore & audit logged."
            }
        }
    }

    fun adminPostAnnouncement(
        statusType: String,
        title: String,
        message: String,
        effectiveHours: String,
        sendPush: Boolean,
        sendWhatsappClosure: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminPostAnnouncement(
                statusType = statusType,
                title = title,
                message = message,
                effectiveHours = effectiveHours,
                sendPush = sendPush,
                sendWhatsappClosure = sendWhatsappClosure
            )
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to post announcement."
            }.onSuccess {
                _actionBanner.value = "Announcement banner live for all members & trainers."
            }
        }
    }

    fun adminDeactivateAnnouncement(announcementId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminDeactivateAnnouncement(announcementId)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to clear announcement."
            }.onSuccess {
                _actionBanner.value = "Announcement cleared."
            }
        }
    }

    fun adminReconcileSandboxPayment(request: PaymentRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminReconcileSandboxPayment(request)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to reconcile sandbox webhook."
            }.onSuccess {
                _actionBanner.value = "Admin sandbox invoice & plan tier reconciled (audit logged)."
            }
        }
    }

    fun adminSavePlan(plan: GymPlan) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminSavePlan(plan)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to save membership plan."
            }.onSuccess {
                _actionBanner.value = "Saved plan '${plan.name}' (₹${plan.totalAmountInr} INR)."
            }
        }
    }

    fun adminSaveSubscription(subscription: MemberSubscription) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminSaveSubscription(subscription)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to update membership subscription."
            }.onSuccess {
                _actionBanner.value = "Updated membership '${subscription.planName}' & synced access."
            }
        }
    }

    fun adminUpdatePaymentRequest(request: PaymentRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminUpdatePaymentRequest(request)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to update pending payment."
            }.onSuccess {
                _actionBanner.value = "Updated pending pay for ${request.memberName} (₹${request.amountInr})."
            }
        }
    }

    fun adminDeletePaymentRequest(requestId: String, memberName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminDeletePaymentRequest(requestId, memberName)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to remove pending payment."
            }.onSuccess {
                _actionBanner.value = "Removed pending pay for $memberName."
            }
        }
    }

    fun adminSaveBillingConfig(config: Phase3BillingConfig) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = phase2Repository.adminSaveBillingConfig(config)
            _isBusy.value = false
            res.onFailure {
                _actionBanner.value = it.localizedMessage ?: "Failed to save Phase 3 billing config."
            }.onSuccess {
                _actionBanner.value = "Saved Phase 3 ${config.gatewayProvider} & AutoPay config."
            }
        }
    }

    fun sendConciergeMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isConciergeBusy.value) return

        val userMsgId = "u_${System.currentTimeMillis()}"
        val aiMsgId = "a_${System.currentTimeMillis() + 1}"
        _chatMessages.value = _chatMessages.value +
            ConciergeMessage(userMsgId, isUser = true, text = trimmed) +
            ConciergeMessage(aiMsgId, isUser = false, text = "", isStreaming = true)

        viewModelScope.launch {
            _isConciergeBusy.value = true
            try {
                var lastText = ""
                conciergeRepository.sendMessageStream(trimmed, gymConfig.value).collect { partial ->
                    lastText = partial
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == aiMsgId) msg.copy(text = partial, isStreaming = true) else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(
                            text = lastText.ifBlank { "No response received. Please try again." },
                            isStreaming = false
                        )
                    } else msg
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Concierge timed out", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(
                            text = "Request timed out after 60 seconds. Please check your connection and try again.",
                            isStreaming = false
                        )
                    } else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Concierge failed", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == aiMsgId) {
                        msg.copy(
                            text = "Concierge error: ${e.localizedMessage ?: "Unable to reach Firebase AI Logic."}",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isConciergeBusy.value = false
            }
        }
    }

    private fun unwrapFirestore(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "BattleBoxVM"
        const val OWNER_ADMIN_EMAIL = "youngskiiicey@gmail.com"
    }
}
