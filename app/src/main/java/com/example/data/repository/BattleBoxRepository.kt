package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.legal.LegalDrafts
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceRejection
import com.example.data.model.AuditLogEntry
import com.example.data.model.ConsentRecord
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.UserProfile
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.security.QrTokenSigner
import com.example.utils.AttendanceCsvExporter
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

sealed interface CheckInOutcome {
    data class CheckedIn(
        val attendanceId: String,
        val memberName: String,
        val distanceMeters: Double,
        val accuracyMeters: Double
    ) : CheckInOutcome

    data class Rejected(val reason: String) : CheckInOutcome
}

class BattleBoxRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Battle Box data.")

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.GET, "users/$uid")
                throw error
            }

    fun observeGymConfig(): Flow<GymConfig> =
        db.collection("gym_config").document(GYM_MAIN_DOC)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(GymConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: GymConfig(id = GYM_MAIN_DOC)
                } else {
                    GymConfig(id = GYM_MAIN_DOC)
                }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.GET, "gym_config/$GYM_MAIN_DOC")
                throw error
            }

    fun observeGymPublicStatus(): Flow<GymPublicStatus> =
        db.collection("gym_public").document(GYM_STATUS_DOC)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(GymPublicStatus::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: GymPublicStatus(id = GYM_STATUS_DOC)
                } else {
                    GymPublicStatus(id = GYM_STATUS_DOC)
                }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.GET, "gym_public/$GYM_STATUS_DOC")
                throw error
            }

    fun observeMyAttendance(uid: String): Flow<List<AttendanceRecord>> =
        db.collection("attendance")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.checkInAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "attendance")
                throw error
            }

    fun observeLiveAttendanceRoster(): Flow<List<AttendanceRecord>> =
        db.collection("attendance")
            .whereEqualTo("status", AttendanceRecord.STATUS_CHECKED_IN)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.checkInAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "attendance")
                throw error
            }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayName.lowercase() }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "users")
                throw error
            }

    fun observeAssignedMembers(trainerUid: String): Flow<List<UserProfile>> =
        db.collection("users")
            .whereEqualTo("assignedTrainerId", trainerUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayName.lowercase() }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "users")
                throw error
            }

    fun observeRecentRejections(uid: String, isStaff: Boolean): Flow<List<AttendanceRejection>> {
        val query = if (isStaff) {
            db.collection("attendance_rejections")
        } else {
            db.collection("attendance_rejections").whereEqualTo("userId", uid)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(AttendanceRejection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .take(25)
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "attendance_rejections")
                throw error
            }
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> =
        db.collection("audit_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AuditLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .take(60)
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "audit_logs")
                throw error
            }

    fun observeMyConsents(uid: String): Flow<List<ConsentRecord>> =
        db.collection("consents")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ConsentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.acceptedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                handleFirestoreError(error, OperationType.LIST, "consents")
                throw error
            }

    suspend fun completeOnboarding(
        displayName: String,
        email: String,
        phone: String?,
        emergencyContact: String?,
        photoUrl: String?,
        isBootstrappedOwner: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val existing = userRef.get().await()

        val cleanedPhone = phone?.trim()?.takeIf { it.isNotEmpty() }
        val cleanedEmergency = emergencyContact?.trim()?.takeIf { it.isNotEmpty() }
        val cleanedPhoto = photoUrl?.trim()?.takeIf { it.isNotEmpty() }

        if (!existing.exists()) {
            val createPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName.trim().take(80),
                "email" to email.trim().take(160),
                "role" to UserProfile.ROLE_MEMBER,
                "planTier" to UserProfile.PLAN_BASIC,
                "membershipStatus" to UserProfile.STATUS_ACTIVE,
                "onboardingCompleted" to true,
                "termsVersion" to LegalDrafts.TERMS_VERSION,
                "dpdpConsentAccepted" to true,
                "healthWaiverAccepted" to true,
                "consentAcceptedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            cleanedPhone?.let { createPayload["phone"] = it }
            cleanedEmergency?.let { createPayload["emergencyContact"] = it }
            cleanedPhoto?.let { createPayload["photoUrl"] = it }

            try {
                userRef.set(createPayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, userRef.path)
                throw e
            }

            if (isBootstrappedOwner) {
                promoteBootstrappedOwner(uid, email)
            }
        } else {
            val updatePayload = mutableMapOf<String, Any>(
                "displayName" to displayName.trim().take(80),
                "onboardingCompleted" to true,
                "termsVersion" to LegalDrafts.TERMS_VERSION,
                "dpdpConsentAccepted" to true,
                "healthWaiverAccepted" to true,
                "consentAcceptedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            cleanedPhone?.let { updatePayload["phone"] = it }
            cleanedEmergency?.let { updatePayload["emergencyContact"] = it }
            cleanedPhoto?.let { updatePayload["photoUrl"] = it }

            try {
                userRef.update(updatePayload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
                throw e
            }

            if (isBootstrappedOwner && existing.getString("role") != UserProfile.ROLE_ADMIN) {
                promoteBootstrappedOwner(uid, email)
            }
        }

        // Write immutable consent log
        val consentId = "${uid}_${UUID.randomUUID().toString().take(8)}"
        val consentRef = db.collection("consents").document(consentId)
        val consentPayload = mapOf(
            "userId" to uid,
            "termsVersion" to LegalDrafts.TERMS_VERSION,
            "dpdpConsentAccepted" to true,
            "healthWaiverAccepted" to true,
            "acceptedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            consentRef.set(consentPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, consentRef.path)
            throw e
        }

        ensureGymDefaultsIfAdmin(isBootstrappedOwner)
    }

    private suspend fun promoteBootstrappedOwner(uid: String, email: String) {
        val adminDoc = db.collection("admins").document(uid)
        try {
            adminDoc.set(
                mapOf(
                    "userId" to uid,
                    "email" to email,
                    "grantedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).update(
                mapOf(
                    "role" to UserProfile.ROLE_ADMIN,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                action = "OWNER_ADMIN_BOOTSTRAPPED",
                targetUid = uid,
                details = "Bootstrapped owner admin access for $email"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, adminDoc.path)
        }
    }

    suspend fun ensureGymDefaultsIfAdmin(isAdmin: Boolean) {
        if (!isAdmin) return
        val configRef = db.collection("gym_config").document(GYM_MAIN_DOC)
        val statusRef = db.collection("gym_public").document(GYM_STATUS_DOC)
        try {
            if (!configRef.get().await().exists()) {
                val defaults = GymConfig(id = GYM_MAIN_DOC)
                configRef.set(
                    mapOf(
                        "gymName" to defaults.gymName,
                        "mapsUrl" to defaults.mapsUrl,
                        "latitude" to defaults.latitude,
                        "longitude" to defaults.longitude,
                        "geofenceRadiusM" to defaults.geofenceRadiusM,
                        "maxGpsAccuracyM" to defaults.maxGpsAccuracyM,
                        "autoCheckoutHours" to defaults.autoCheckoutHours,
                        "morningOpen" to defaults.morningOpen,
                        "morningClose" to defaults.morningClose,
                        "eveningOpen" to defaults.eveningOpen,
                        "eveningClose" to defaults.eveningClose,
                        "timezone" to defaults.timezone,
                        "currency" to defaults.currency,
                        "dataRegion" to defaults.dataRegion,
                        "manualStatusOverride" to defaults.manualStatusOverride,
                        "maxCapacity" to defaults.maxCapacity,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            if (!statusRef.get().await().exists()) {
                statusRef.set(
                    mapOf(
                        "inGymCount" to 0L,
                        "isOpen" to true,
                        "statusLabel" to "OPEN • IST",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, configRef.path)
        }
    }

    suspend fun performKioskSelfCheckIn(
        scannedQrPayload: String,
        currentUser: UserProfile,
        gymConfig: GymConfig,
        deviceFix: DeviceFix?
    ): CheckInOutcome {
        val uid = requireUserId()
        val distance = if (deviceFix != null) {
            GeofenceUtils.haversineMeters(
                deviceFix.latitude,
                deviceFix.longitude,
                gymConfig.latitude,
                gymConfig.longitude
            )
        } else {
            999_999.0
        }
        val accuracy = deviceFix?.accuracyMeters ?: 999_999.0

        suspend fun reject(reason: String): CheckInOutcome {
            logRejection(uid, AttendanceRecord.MODE_KIOSK_QR, reason, distance, accuracy)
            return CheckInOutcome.Rejected(reason)
        }

        if (currentUser.membershipStatus != UserProfile.STATUS_ACTIVE) {
            return reject("Membership is ${currentUser.membershipStatus}. Contact Front Desk.")
        }

        val qrResult = QrTokenSigner.verifyToken(scannedQrPayload, QrTokenSigner.KIND_KIOSK)
        if (qrResult is QrTokenSigner.VerificationResult.Invalid) {
            return reject(qrResult.reason)
        }
        val parsedToken = (qrResult as QrTokenSigner.VerificationResult.Valid).token

        if (deviceFix == null) {
            return reject("GPS fix unavailable. Enable high-accuracy location inside Battle Box.")
        }
        if (accuracy > gymConfig.maxGpsAccuracyM) {
            return reject(
                "GPS accuracy ${accuracy.toInt()} m exceeds ${gymConfig.maxGpsAccuracyM.toInt()} m limit."
            )
        }
        if (distance > gymConfig.geofenceRadiusM) {
            return reject(
                "Outside gym geofence: ${distance.toInt()} m away (limit ${gymConfig.geofenceRadiusM.toInt()} m)."
            )
        }

        // Prevent duplicate active check-in
        val existingActive = db.collection("attendance")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", AttendanceRecord.STATUS_CHECKED_IN)
            .get()
            .await()
        if (!existingActive.isEmpty) {
            val activeDoc = existingActive.documents.firstOrNull()
            val expiresAt = activeDoc?.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
            if (expiresAt > System.currentTimeMillis()) {
                return reject("Duplicate check-in blocked: you are already checked in.")
            }
        }

        return commitAttendanceRecord(
            targetUser = currentUser,
            mode = AttendanceRecord.MODE_KIOSK_QR,
            verifiedByUid = uid,
            epochSlot = parsedToken.epochSlot,
            deviceFix = deviceFix,
            distanceMeters = distance,
            gymConfig = gymConfig
        )
    }

    suspend fun performFrontDeskScanCheckIn(
        scannedMemberQr: String,
        scannerFix: DeviceFix?,
        gymConfig: GymConfig
    ): CheckInOutcome = performStaffQrScanCheckIn(
        scannedMemberQr = scannedMemberQr,
        scannerFix = scannerFix,
        gymConfig = gymConfig,
        mode = AttendanceRecord.MODE_FRONT_DESK,
        writeAdminAuditLog = true
    )

    suspend fun performTrainerQrScanCheckIn(
        scannedMemberQr: String,
        scannerFix: DeviceFix?,
        gymConfig: GymConfig
    ): CheckInOutcome = performStaffQrScanCheckIn(
        scannedMemberQr = scannedMemberQr,
        scannerFix = scannerFix,
        gymConfig = gymConfig,
        mode = AttendanceRecord.MODE_TRAINER_SCAN,
        writeAdminAuditLog = false
    )

    private suspend fun performStaffQrScanCheckIn(
        scannedMemberQr: String,
        scannerFix: DeviceFix?,
        gymConfig: GymConfig,
        mode: String,
        writeAdminAuditLog: Boolean
    ): CheckInOutcome {
        val staffUid = requireUserId()
        val distance = if (scannerFix != null) {
            GeofenceUtils.haversineMeters(
                scannerFix.latitude,
                scannerFix.longitude,
                gymConfig.latitude,
                gymConfig.longitude
            )
        } else {
            999_999.0
        }
        val accuracy = scannerFix?.accuracyMeters ?: 999_999.0

        val qrResult = QrTokenSigner.verifyToken(scannedMemberQr, QrTokenSigner.KIND_MEMBER)
        if (qrResult is QrTokenSigner.VerificationResult.Invalid) {
            logRejection(staffUid, mode, qrResult.reason, distance, accuracy)
            return CheckInOutcome.Rejected(qrResult.reason)
        }
        val parsedToken = (qrResult as QrTokenSigner.VerificationResult.Valid).token
        val memberUid = parsedToken.subjectId

        if (scannerFix == null) {
            val reason = "Scanner GPS unavailable. Enable location services inside Battle Box."
            logRejection(memberUid, mode, reason, distance, accuracy)
            return CheckInOutcome.Rejected(reason)
        }
        if (distance > gymConfig.geofenceRadiusM) {
            val reason = "Scanner is ${distance.toInt()} m outside gym geofence (${gymConfig.geofenceRadiusM.toInt()} m)."
            logRejection(memberUid, mode, reason, distance, accuracy)
            return CheckInOutcome.Rejected(reason)
        }

        val memberSnap = try {
            db.collection("users").document(memberUid).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$memberUid")
            return CheckInOutcome.Rejected(e.localizedMessage ?: "Permission denied reading member profile.")
        }
        val member = memberSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return CheckInOutcome.Rejected("Member profile not found for UID $memberUid.")

        if (member.membershipStatus != UserProfile.STATUS_ACTIVE) {
            val reason = "${member.displayName} membership is ${member.membershipStatus}."
            logRejection(memberUid, mode, reason, distance, accuracy)
            return CheckInOutcome.Rejected(reason)
        }

        val activeCheck = db.collection("attendance")
            .whereEqualTo("userId", memberUid)
            .whereEqualTo("status", AttendanceRecord.STATUS_CHECKED_IN)
            .get()
            .await()
        if (!activeCheck.isEmpty) {
            val activeDoc = activeCheck.documents.firstOrNull()
            val expiresAt = activeDoc?.getTimestamp("expiresAt")?.toDate()?.time ?: Long.MAX_VALUE
            if (expiresAt > System.currentTimeMillis()) {
                val reason = "${member.displayName} is already checked in."
                logRejection(memberUid, mode, reason, distance, accuracy)
                return CheckInOutcome.Rejected(reason)
            }
        }

        val outcome = commitAttendanceRecord(
            targetUser = member,
            mode = mode,
            verifiedByUid = staffUid,
            epochSlot = parsedToken.epochSlot,
            deviceFix = scannerFix,
            distanceMeters = distance,
            gymConfig = gymConfig
        )
        if (outcome is CheckInOutcome.CheckedIn && writeAdminAuditLog) {
            writeAuditLog(
                action = "FRONT_DESK_CHECK_IN",
                targetUid = member.userId,
                details = "Checked in ${member.displayName} via Front Desk QR (${distance.toInt()}m)"
            )
        }
        return outcome
    }

    private suspend fun commitAttendanceRecord(
        targetUser: UserProfile,
        mode: String,
        verifiedByUid: String,
        epochSlot: Long,
        deviceFix: DeviceFix,
        distanceMeters: Double,
        gymConfig: GymConfig
    ): CheckInOutcome {
        val attId = "att_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val attRef = db.collection("attendance").document(attId)
        val statusRef = db.collection("gym_public").document(GYM_STATUS_DOC)
        val expiresDate = Date(System.currentTimeMillis() + (gymConfig.autoCheckoutHours * 3_600_000.0).toLong())
        val (isOpen, statusLabel) = GeofenceUtils.evaluateOpenStatusIst(gymConfig)

        val payload = mapOf(
            "userId" to targetUser.userId,
            "memberName" to targetUser.displayName,
            "memberRole" to targetUser.role,
            "mode" to mode,
            "status" to AttendanceRecord.STATUS_CHECKED_IN,
            "distanceMeters" to distanceMeters,
            "gpsAccuracyMeters" to deviceFix.accuracyMeters,
            "latitude" to deviceFix.latitude,
            "longitude" to deviceFix.longitude,
            "verifiedByUid" to verifiedByUid,
            "qrEpochSlot" to epochSlot,
            "checkInAt" to FieldValue.serverTimestamp(),
            "expiresAt" to Timestamp(expiresDate),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.runTransaction { tx ->
                val statusSnap = tx.get(statusRef)
                val currentCount = statusSnap.getLong("inGymCount") ?: 0L
                tx.set(attRef, payload)
                if (statusSnap.exists()) {
                    tx.update(
                        statusRef,
                        mapOf(
                            "inGymCount" to (currentCount + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        statusRef,
                        mapOf(
                            "inGymCount" to 1L,
                            "isOpen" to isOpen,
                            "statusLabel" to statusLabel,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            CheckInOutcome.CheckedIn(attId, targetUser.displayName, distanceMeters, deviceFix.accuracyMeters)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, attRef.path)
            CheckInOutcome.Rejected(e.localizedMessage ?: "Failed to record check-in.")
        }
    }

    suspend fun checkOutSession(attendanceId: String, isAdminAction: Boolean = false, targetUserName: String = ""): Result<Unit> =
        runCatching {
            val attRef = db.collection("attendance").document(attendanceId)
            val statusRef = db.collection("gym_public").document(GYM_STATUS_DOC)
            try {
                db.runTransaction { tx ->
                    val attSnap = tx.get(attRef)
                    val statusSnap = tx.get(statusRef)
                    val currentStatus = attSnap.getString("status")
                    if (currentStatus == AttendanceRecord.STATUS_CHECKED_IN) {
                        tx.update(
                            attRef,
                            mapOf(
                                "status" to AttendanceRecord.STATUS_CHECKED_OUT,
                                "checkOutAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        val count = statusSnap.getLong("inGymCount") ?: 0L
                        if (statusSnap.exists()) {
                            tx.update(
                                statusRef,
                                mapOf(
                                    "inGymCount" to (count - 1L).coerceAtLeast(0L),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                }.await()
                if (isAdminAction) {
                    writeAuditLog(
                        action = "ADMIN_CHECK_OUT",
                        targetUid = null,
                        details = "Checked out active session $attendanceId ($targetUserName)"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, attRef.path)
                throw e
            }
        }

    suspend fun autoCheckOutIfOutsideGeofence(
        activeSession: AttendanceRecord,
        deviceFix: DeviceFix,
        gymConfig: GymConfig
    ): Result<com.example.security.GeofenceCheckResult> = runCatching {
        val check = GeofenceUtils.evaluateGeofenceExit(deviceFix, gymConfig)
        if (check.isInsideGeofence) {
            return@runCatching check
        }
        val attRef = db.collection("attendance").document(activeSession.id)
        val statusRef = db.collection("gym_public").document(GYM_STATUS_DOC)
        try {
            db.runTransaction { tx ->
                val attSnap = tx.get(attRef)
                val statusSnap = tx.get(statusRef)
                if (attSnap.getString("status") == AttendanceRecord.STATUS_CHECKED_IN) {
                    tx.update(
                        attRef,
                        mapOf(
                            "status" to AttendanceRecord.STATUS_AUTO_CHECKED_OUT,
                            "checkOutAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (statusSnap.exists()) {
                        val count = statusSnap.getLong("inGymCount") ?: 0L
                        tx.update(
                            statusRef,
                            mapOf(
                                "inGymCount" to (count - 1L).coerceAtLeast(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, attRef.path)
            throw e
        }
        check
    }

    suspend fun sweepExpiredSessions(records: List<AttendanceRecord>, isAdmin: Boolean) {
        val nowMs = System.currentTimeMillis()
        val expired = records.filter {
            it.status == AttendanceRecord.STATUS_CHECKED_IN &&
                (it.expiresAt?.toDate()?.time ?: Long.MAX_VALUE) <= nowMs
        }
        if (expired.isEmpty()) return

        for (record in expired) {
            val attRef = db.collection("attendance").document(record.id)
            val statusRef = db.collection("gym_public").document(GYM_STATUS_DOC)
            try {
                db.runTransaction { tx ->
                    val statusSnap = tx.get(statusRef)
                    val count = statusSnap.getLong("inGymCount") ?: 0L
                    tx.update(
                        attRef,
                        mapOf(
                            "status" to AttendanceRecord.STATUS_AUTO_CHECKED_OUT,
                            "checkOutAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (statusSnap.exists()) {
                        tx.update(
                            statusRef,
                            mapOf(
                                "inGymCount" to (count - 1L).coerceAtLeast(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }.await()
                if (isAdmin) {
                    writeAuditLog(
                        action = "AUTO_CHECK_OUT",
                        targetUid = record.userId,
                        details = "Auto checked out ${record.memberName} after 3h session expiry"
                    )
                }
            } catch (_: Exception) {
                // Non-fatal background sweep
            }
        }
    }

    suspend fun adminUpdateUser(
        targetUser: UserProfile,
        newRole: String,
        newPlanTier: String,
        newMembershipStatus: String,
        newAssignedTrainerId: String?,
        newDisplayName: String = targetUser.displayName,
        newPhone: String? = targetUser.phone,
        newEmergencyContact: String? = targetUser.emergencyContact
    ): Result<Unit> = runCatching {
        val actorUid = requireUserId()
        val userRef = db.collection("users").document(targetUser.userId)
        val cleanedName = newDisplayName.trim().ifBlank { targetUser.displayName }.take(80)
        val cleanedPhone = newPhone?.trim()?.takeIf { it.isNotEmpty() }?.take(25)
        val cleanedEmergency = newEmergencyContact?.trim()?.takeIf { it.isNotEmpty() }?.take(120)

        val payload = mutableMapOf<String, Any>(
            "displayName" to cleanedName,
            "role" to newRole,
            "planTier" to newPlanTier,
            "membershipStatus" to newMembershipStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (cleanedPhone != null) {
            payload["phone"] = cleanedPhone
        } else if (targetUser.phone != null) {
            payload["phone"] = FieldValue.delete()
        }
        if (cleanedEmergency != null) {
            payload["emergencyContact"] = cleanedEmergency
        } else if (targetUser.emergencyContact != null) {
            payload["emergencyContact"] = FieldValue.delete()
        }
        if (!newAssignedTrainerId.isNullOrBlank()) {
            payload["assignedTrainerId"] = newAssignedTrainerId.trim()
        } else if (targetUser.assignedTrainerId != null) {
            payload["assignedTrainerId"] = FieldValue.delete()
        }

        try {
            userRef.update(payload).await()
            val adminRef = db.collection("admins").document(targetUser.userId)
            if (newRole == UserProfile.ROLE_ADMIN) {
                adminRef.set(
                    mapOf(
                        "userId" to targetUser.userId,
                        "email" to targetUser.email,
                        "grantedBy" to actorUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (targetUser.role == UserProfile.ROLE_ADMIN && newRole != UserProfile.ROLE_ADMIN) {
                adminRef.delete().await()
            }
            writeAuditLog(
                action = "USER_ACCESS_UPDATED",
                targetUid = targetUser.userId,
                details = "$cleanedName: role=$newRole, plan=$newPlanTier, status=$newMembershipStatus, trainer=${newAssignedTrainerId ?: "none"}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun updateGymConfig(updated: GymConfig): Result<Unit> = runCatching {
        val configRef = db.collection("gym_config").document(GYM_MAIN_DOC)
        val statusRef = db.collection("gym_public").document(GYM_STATUS_DOC)
        val (isOpen, statusLabel) = GeofenceUtils.evaluateOpenStatusIst(updated)
        val payload = mapOf(
            "gymName" to updated.gymName.trim(),
            "mapsUrl" to updated.mapsUrl.trim(),
            "latitude" to updated.latitude,
            "longitude" to updated.longitude,
            "geofenceRadiusM" to updated.geofenceRadiusM,
            "maxGpsAccuracyM" to updated.maxGpsAccuracyM,
            "autoCheckoutHours" to updated.autoCheckoutHours,
            "morningOpen" to updated.morningOpen.trim(),
            "morningClose" to updated.morningClose.trim(),
            "eveningOpen" to updated.eveningOpen.trim(),
            "eveningClose" to updated.eveningClose.trim(),
            "timezone" to updated.timezone,
            "currency" to updated.currency,
            "dataRegion" to updated.dataRegion,
            "manualStatusOverride" to updated.manualStatusOverride,
            "maxCapacity" to updated.maxCapacity.coerceIn(5L, 1000L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            configRef.set(payload, SetOptions.merge()).await()
            statusRef.set(
                mapOf(
                    "isOpen" to isOpen,
                    "statusLabel" to statusLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            writeAuditLog(
                action = "GYM_SETTINGS_UPDATED",
                targetUid = null,
                details = "Updated geofence (${updated.latitude}, ${updated.longitude}, ${updated.geofenceRadiusM}m) & status=${updated.manualStatusOverride}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, configRef.path)
            throw e
        }
    }

    private suspend fun logRejection(
        userId: String,
        mode: String,
        reason: String,
        distanceMeters: Double,
        gpsAccuracyMeters: Double
    ) {
        val rejId = "rej_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = db.collection("attendance_rejections").document(rejId)
        try {
            ref.set(
                mapOf(
                    "userId" to userId,
                    "mode" to mode,
                    "reason" to reason.take(300),
                    "distanceMeters" to distanceMeters,
                    "gpsAccuracyMeters" to gpsAccuracyMeters,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }

    suspend fun writeAuditLog(action: String, targetUid: String?, details: String) {
        val user = Firebase.auth.currentUser ?: return
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val ref = db.collection("audit_logs").document(logId)
        val payload = mutableMapOf<String, Any>(
            "actorUid" to user.uid,
            "actorEmail" to (user.email ?: "unknown"),
            "action" to action.take(80),
            "details" to details.take(500),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!targetUid.isNullOrBlank()) {
            payload["targetUid"] = targetUid
        }
        try {
            ref.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }

    suspend fun fetchMonthlyAttendanceRecords(
        year: Int,
        monthOneBased: Int
    ): Result<List<AttendanceRecord>> = runCatching {
        requireUserId()
        val (startDate, endDate) = AttendanceCsvExporter.monthDateRangeIst(year, monthOneBased)
        val snapshot = try {
            db.collection("attendance")
                .whereGreaterThanOrEqualTo("checkInAt", Timestamp(startDate))
                .whereLessThan("checkInAt", Timestamp(endDate))
                .get()
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "attendance")
            throw e
        }
        val records = snapshot.toObjects(
            AttendanceRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedBy { it.checkInAt?.toDate()?.time ?: 0L }

        val label = AttendanceCsvExporter.formatMonthLabel(year, monthOneBased)
        writeAuditLog(
            action = "ATTENDANCE_CSV_EXPORTED",
            targetUid = null,
            details = "Queried ${records.size} attendance log(s) for $label (IST) for CSV export"
        )
        records
    }

    companion object {
        const val GYM_MAIN_DOC = "main"
        const val GYM_STATUS_DOC = "status"
    }
}
