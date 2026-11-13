package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceRejection
import com.example.data.model.AuditLogEntry
import com.example.data.model.GymAnnouncement
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.NotificationDeliveryLog
import com.example.data.model.PaymentRequest
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.security.QrTokenSigner
import com.example.ui.components.FirebaseConsoleCard
import com.example.ui.components.FirebaseConsoleLinks
import com.example.ui.components.LiveOccupancyMonitorCard
import com.example.ui.components.MapsPhotoShowcaseCard
import com.example.ui.components.MonthlyAttendanceExportCard
import com.example.ui.components.QrScannerSheet
import com.example.ui.components.RotatingQrCard
import com.example.utils.MonthlyAttendanceSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminConsoleScreen(
    gymConfig: GymConfig,
    gymPublicStatus: GymPublicStatus = GymPublicStatus(),
    liveRoster: List<AttendanceRecord>,
    allUsers: List<UserProfile>,
    auditLogs: List<AuditLogEntry>,
    rejections: List<AttendanceRejection>,
    announcements: List<GymAnnouncement> = emptyList(),
    paymentRequests: List<PaymentRequest> = emptyList(),
    deliveryLogs: List<NotificationDeliveryLog> = emptyList(),
    deviceFix: DeviceFix?,
    monthlyExportState: UiState<MonthlyAttendanceSummary>? = null,
    isBusy: Boolean,
    onOpenScannerTab: () -> Unit = {},
    onFrontDeskScan: (qrPayload: String, fix: DeviceFix?) -> Unit,
    onAdminCheckOut: (attendanceId: String, memberName: String) -> Unit,
    onSweepExpired: (List<AttendanceRecord>) -> Unit,
    onUpdateUser: (UserProfile, String, String, String, String?) -> Unit,
    onUpdateGymConfig: (GymConfig) -> Unit,
    onFetchMonthlyCsv: (Int, Int) -> Unit = { _, _ -> },
    onPostAnnouncement: (String, String, String, String, Boolean, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onClearAnnouncement: (String) -> Unit = {},
    onSeedDefaultPlans: () -> Unit = {},
    onReconcileSandboxPayment: (PaymentRequest) -> Unit = {},
    onCopyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableIntStateOf(0) }
    var showFrontDeskScanner by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserProfile?>(null) }

    val fmt = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 800.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("admin_home_dashboard")
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text(text = "ADMIN HOME DASHBOARD", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${gymConfig.gymName} • Dudhnoi, Assam • ${gymConfig.dataRegion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Admin Quick KPI Overview Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminKpiPill(
                    label = "In Gym",
                    value = "${liveRoster.size}",
                    modifier = Modifier.weight(1f)
                )
                AdminKpiPill(
                    label = "Users",
                    value = "${allUsers.size}",
                    modifier = Modifier.weight(1f)
                )
                AdminKpiPill(
                    label = "Pending Pay",
                    value = "${paymentRequests.count { it.status == PaymentRequest.STATUS_CREATED }}",
                    modifier = Modifier.weight(1f)
                )
                AdminKpiPill(
                    label = "Banners",
                    value = "${announcements.size}",
                    modifier = Modifier.weight(1f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Live & Desk", "Users & Roles", "Comms & Billing", "Settings", "Audit Log").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("admin_tab_$idx")
                    )
                }
            }

            when (selectedSection) {
                0 -> {
                    // Front Desk Scanner + Live Roster + Wall Kiosk QR
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "Front Desk Check-In", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Scan a member's 30s rotating QR pass. Verifies signature, active membership, duplicate check-in, and scanner geofence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onOpenScannerTab,
                                    enabled = !isBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_front_desk_scan_button")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("CameraX Scanner", modifier = Modifier.padding(start = 6.dp))
                                }
                                OutlinedButton(
                                    onClick = { showFrontDeskScanner = true },
                                    enabled = !isBusy
                                ) {
                                    Text("Quick Sheet")
                                }
                            }
                        }
                    }

                    LiveOccupancyMonitorCard(
                        gymConfig = gymConfig,
                        gymPublicStatus = gymPublicStatus,
                        liveRoster = liveRoster,
                        showRosterDetails = true,
                        isBusy = isBusy,
                        onCheckOutSession = onAdminCheckOut,
                        onSweepExpired = onSweepExpired
                    )

                    MonthlyAttendanceExportCard(
                        exportState = monthlyExportState,
                        isBusy = isBusy,
                        onFetchMonthlyLogs = onFetchMonthlyCsv,
                        onFeedback = onCopyMessage
                    )

                    // Wall Kiosk QR Display
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            RotatingQrCard(
                                title = "Battle Box Wall / Kiosk QR",
                                subtitle = "Members and trainers scan this rotating QR code for geofenced self check-in.",
                                tokenProvider = { nowMs -> QrTokenSigner.generateKioskToken("main", nowMs) },
                                onCopied = onCopyMessage
                            )
                        }
                    }

                    // Google Maps Photos Showcase
                    MapsPhotoShowcaseCard(mapsUrl = gymConfig.mapsUrl, compactHeader = true)
                }

                1 -> {
                    // Users, Role Assignment, Plan Tier & Trainer Mapping
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Directory & Role Control (${allUsers.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Roles (member, trainer, admin) and Premium plan tiers are managed exclusively by Admin and logged to the audit trail.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    FirebaseConsoleLinks.openUrlOrCopy(
                                        context = context,
                                        clipboard = clipboard,
                                        url = FirebaseConsoleLinks.authUsersUrl(),
                                        label = "Auth Users (Login ID & Password)",
                                        onFeedback = onCopyMessage
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_users_firebase_console_button")
                            ) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Manage Auth Users & Passwords in Firebase Console", modifier = Modifier.padding(start = 8.dp))
                            }
                            allUsers.forEach { u ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = u.displayName, style = MaterialTheme.typography.titleSmall)
                                        Text(text = u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "role=${u.role} • plan=${u.planTier} • status=${u.membershipStatus}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    OutlinedButton(onClick = { editingUser = u }) {
                                        Text("Edit")
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }
                        }
                    }
                }

                2 -> {
                    AdminPhase2Section(
                        announcements = announcements,
                        paymentRequests = paymentRequests,
                        deliveryLogs = deliveryLogs,
                        isBusy = isBusy,
                        onPostAnnouncement = onPostAnnouncement,
                        onClearAnnouncement = onClearAnnouncement,
                        onSeedDefaultPlans = onSeedDefaultPlans,
                        onReconcileSandboxPayment = onReconcileSandboxPayment,
                        onCopyMessage = onCopyMessage
                    )
                }

                3 -> {
                    // Firebase Console + Facility Settings
                    FirebaseConsoleCard(onFeedback = onCopyMessage)

                    GymSettingsEditor(
                        gymConfig = gymConfig,
                        deviceFix = deviceFix,
                        isBusy = isBusy,
                        onSave = onUpdateGymConfig,
                        onUseCurrentGps = { onApply ->
                            val hasLoc = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasLoc) {
                                scope.launch {
                                    val fix = GeofenceUtils.fetchCurrentLocation(context)
                                    if (fix != null) onApply(fix)
                                }
                            }
                        }
                    )
                }

                4 -> {
                    // Immutable Audit Log + Rejections
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "Immutable Admin Audit Log", style = MaterialTheme.typography.titleMedium)
                            if (auditLogs.isEmpty()) {
                                Text(
                                    text = "No audit events recorded yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                auditLogs.forEach { entry ->
                                    val timeStr = entry.createdAt?.toDate()?.let { fmt.format(it) } ?: "Pending"
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "${entry.action} • $timeStr IST",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(text = entry.details, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "Actor: ${entry.actorEmail}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                    if (rejections.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "Recent Check-In Rejections", style = MaterialTheme.typography.titleMedium)
                                rejections.take(15).forEach { rej ->
                                    Text(
                                        text = "• [${rej.mode}] ${rej.reason} (${rej.distanceMeters.toInt()}m, ±${rej.gpsAccuracyMeters.toInt()}m)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showFrontDeskScanner) {
        QrScannerSheet(
            title = "Front Desk: Scan Member QR",
            subtitle = "Verify member's 30s rotating QR pass within Battle Box geofence.",
            onQrScanned = { payload ->
                showFrontDeskScanner = false
                scope.launch {
                    val latestFix = GeofenceUtils.fetchCurrentLocation(context) ?: deviceFix
                    onFrontDeskScan(payload, latestFix)
                }
            },
            onDismiss = { showFrontDeskScanner = false }
        )
    }

    val target = editingUser
    if (target != null) {
        val trainers = allUsers.filter { it.role == UserProfile.ROLE_TRAINER || it.role == UserProfile.ROLE_ADMIN }
        EditUserAccessDialog(
            user = target,
            trainers = trainers,
            onDismiss = { editingUser = null },
            onSave = { role, plan, status, trainerId ->
                editingUser = null
                onUpdateUser(target, role, plan, status, trainerId)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GymSettingsEditor(
    gymConfig: GymConfig,
    deviceFix: DeviceFix?,
    isBusy: Boolean,
    onSave: (GymConfig) -> Unit,
    onUseCurrentGps: ((DeviceFix) -> Unit) -> Unit
) {
    var gymName by remember(gymConfig.gymName) { mutableStateOf(gymConfig.gymName) }
    var mapsUrl by remember(gymConfig.mapsUrl) { mutableStateOf(gymConfig.mapsUrl) }
    var latStr by remember(gymConfig.latitude) { mutableStateOf(gymConfig.latitude.toString()) }
    var lngStr by remember(gymConfig.longitude) { mutableStateOf(gymConfig.longitude.toString()) }
    var radiusStr by remember(gymConfig.geofenceRadiusM) { mutableStateOf(gymConfig.geofenceRadiusM.toInt().toString()) }
    var accuracyStr by remember(gymConfig.maxGpsAccuracyM) { mutableStateOf(gymConfig.maxGpsAccuracyM.toInt().toString()) }
    var autoCheckoutStr by remember(gymConfig.autoCheckoutHours) { mutableStateOf(gymConfig.autoCheckoutHours.toString()) }
    var maxCapacityStr by remember(gymConfig.maxCapacity) { mutableStateOf(gymConfig.maxCapacity.toString()) }
    var morningOpen by remember(gymConfig.morningOpen) { mutableStateOf(gymConfig.morningOpen) }
    var morningClose by remember(gymConfig.morningClose) { mutableStateOf(gymConfig.morningClose) }
    var eveningOpen by remember(gymConfig.eveningOpen) { mutableStateOf(gymConfig.eveningOpen) }
    var eveningClose by remember(gymConfig.eveningClose) { mutableStateOf(gymConfig.eveningClose) }
    var overrideStatus by remember(gymConfig.manualStatusOverride) { mutableStateOf(gymConfig.manualStatusOverride) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Security & Region Disclosures
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Geofencing Security Notice",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Geofencing is a deterrent, not tamper-proof. Device GPS coordinates can be spoofed; pair self check-in with rotating wall QR and Front Desk supervision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Data Region: ${gymConfig.dataRegion} • Timezone: ${gymConfig.timezone} • Currency: ${gymConfig.currency}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Facility & Geofence Settings", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(value = gymName, onValueChange = { gymName = it }, label = { Text("Gym Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mapsUrl, onValueChange = { mapsUrl = it }, label = { Text("Google Maps Link") }, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = latStr, onValueChange = { latStr = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lngStr, onValueChange = { lngStr = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = {
                        if (deviceFix != null) {
                            latStr = deviceFix.latitude.toString()
                            lngStr = deviceFix.longitude.toString()
                        } else {
                            onUseCurrentGps { fix ->
                                latStr = fix.latitude.toString()
                                lngStr = fix.longitude.toString()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Set Gym Coordinates to Current Device GPS", modifier = Modifier.padding(start = 6.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = radiusStr, onValueChange = { radiusStr = it }, label = { Text("Radius (m)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = accuracyStr, onValueChange = { accuracyStr = it }, label = { Text("Max GPS (m)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = autoCheckoutStr, onValueChange = { autoCheckoutStr = it }, label = { Text("Auto-Out (h)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = maxCapacityStr,
                    onValueChange = { maxCapacityStr = it },
                    label = { Text("Floor Max Capacity (athletes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = morningOpen, onValueChange = { morningOpen = it }, label = { Text("AM Open") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = morningClose, onValueChange = { morningClose = it }, label = { Text("AM Close") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = eveningOpen, onValueChange = { eveningOpen = it }, label = { Text("PM Open") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = eveningClose, onValueChange = { eveningClose = it }, label = { Text("PM Close") }, modifier = Modifier.weight(1f))
                }

                Text(text = "Open / Closed Override", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto", "open", "closed").forEach { mode ->
                        FilterChip(
                            selected = overrideStatus == mode,
                            onClick = { overrideStatus = mode },
                            label = { Text(mode.uppercase()) }
                        )
                    }
                }

                Button(
                    onClick = {
                        onSave(
                            gymConfig.copy(
                                gymName = gymName.ifBlank { "Battle Box" },
                                mapsUrl = mapsUrl.ifBlank { gymConfig.mapsUrl },
                                latitude = latStr.toDoubleOrNull() ?: gymConfig.latitude,
                                longitude = lngStr.toDoubleOrNull() ?: gymConfig.longitude,
                                geofenceRadiusM = (radiusStr.toDoubleOrNull() ?: 100.0).coerceIn(10.0, 5000.0),
                                maxGpsAccuracyM = (accuracyStr.toDoubleOrNull() ?: 50.0).coerceIn(5.0, 500.0),
                                autoCheckoutHours = (autoCheckoutStr.toDoubleOrNull() ?: 3.0).coerceIn(0.5, 24.0),
                                morningOpen = morningOpen,
                                morningClose = morningClose,
                                eveningOpen = eveningOpen,
                                eveningClose = eveningClose,
                                manualStatusOverride = overrideStatus,
                                maxCapacity = (maxCapacityStr.toLongOrNull() ?: 40L).coerceIn(5L, 1000L)
                            )
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_save_settings_button")
                ) {
                    Text("Save Battle Box Settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditUserAccessDialog(
    user: UserProfile,
    trainers: List<UserProfile>,
    onDismiss: () -> Unit,
    onSave: (role: String, planTier: String, status: String, assignedTrainerId: String?) -> Unit
) {
    var role by remember { mutableStateOf(user.role) }
    var planTier by remember { mutableStateOf(user.planTier) }
    var status by remember { mutableStateOf(user.membershipStatus) }
    var assignedTrainerId by remember { mutableStateOf(user.assignedTrainerId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage ${user.displayName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Role (RBAC)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(UserProfile.ROLE_MEMBER, UserProfile.ROLE_TRAINER, UserProfile.ROLE_ADMIN).forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.uppercase()) })
                    }
                }
                Text("Plan Tier (feature tier, not a role)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(UserProfile.PLAN_BASIC, UserProfile.PLAN_PREMIUM).forEach { p ->
                        FilterChip(selected = planTier == p, onClick = { planTier = p }, label = { Text(p.uppercase()) })
                    }
                }
                Text("Membership Status", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(UserProfile.STATUS_ACTIVE, UserProfile.STATUS_FROZEN, UserProfile.STATUS_EXPIRED).forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.uppercase()) })
                    }
                }
                if (trainers.isNotEmpty()) {
                    Text("Assigned Trainer", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = assignedTrainerId == null,
                            onClick = { assignedTrainerId = null },
                            label = { Text("None") }
                        )
                        trainers.forEach { tr ->
                            FilterChip(
                                selected = assignedTrainerId == tr.userId,
                                onClick = { assignedTrainerId = tr.userId },
                                label = { Text(tr.displayName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(role, planTier, status, assignedTrainerId) }) {
                Text("Save & Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminKpiPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

