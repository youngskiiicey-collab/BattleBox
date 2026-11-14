package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceRejection
import com.example.data.model.GymConfig
import com.example.data.model.UserProfile
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.security.QrTokenSigner
import com.example.ui.components.QrScannerSheet
import com.example.ui.components.RotatingQrCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun PassAndCheckInScreen(
    userProfile: UserProfile,
    gymConfig: GymConfig,
    deviceFix: DeviceFix?,
    myAttendance: List<AttendanceRecord>,
    rejections: List<AttendanceRejection>,
    isBusy: Boolean,
    onRefreshFix: (DeviceFix?) -> Unit,
    onKioskCheckIn: (qrPayload: String, fix: DeviceFix?) -> Unit,
    onCheckOut: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showScanner by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun refreshLocation() {
        scope.launch {
            isFetchingGps = true
            val fix = GeofenceUtils.fetchCurrentLocation(context)
            onRefreshFix(fix)
            isFetchingGps = false
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            refreshLocation()
        }
    }

    val activeSession = myAttendance.firstOrNull { it.status == AttendanceRecord.STATUS_CHECKED_IN }
    val distanceMeters = deviceFix?.let {
        GeofenceUtils.haversineMeters(it.latitude, it.longitude, gymConfig.latitude, gymConfig.longitude)
    }
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
            modifier = Modifier.widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Personal Rotating Member QR (for Front Desk scan)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    RotatingQrCard(
                        title = "${userProfile.displayName}'s Rotating Pass",
                        subtitle = "Show at Battle Box Front Desk. Rotates every 30 seconds so screenshots cannot be reused.",
                        tokenProvider = { nowMs ->
                            QrTokenSigner.generateMemberToken(userProfile.userId, nowMs)
                        },
                        onCopied = onCopyMessage
                    )
                }
            }

            // 2. Self Check-In via Gym Kiosk QR
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Self Check-In (Permanent Wall QR)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Scan the permanent Battle Box Wall QR pasted inside the gym within ${gymConfig.geofenceRadiusM.toInt()} m (GPS <= ${gymConfig.maxGpsAccuracyM.toInt()} m). Checks you in without Admin involvement and notifies Trainers & Admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (deviceFix != null && distanceMeters != null) {
                                Text(
                                    text = "Distance: ${distanceMeters.toInt()} m (Limit: ${gymConfig.geofenceRadiusM.toInt()} m)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (distanceMeters <= gymConfig.geofenceRadiusM) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Text(
                                    text = "GPS Accuracy: ±${deviceFix.accuracyMeters.toInt()} m (Limit: ±${gymConfig.maxGpsAccuracyM.toInt()} m)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = if (hasLocationPermission) {
                                        "No GPS fix acquired yet. Tap Refresh GPS."
                                    } else {
                                        "Location permission needed to verify gym geofence."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (hasLocationPermission) {
                                    refreshLocation()
                                } else {
                                    locationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isFetchingGps,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("refresh_gps_button")
                        ) {
                            if (isFetchingGps) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Refresh GPS", modifier = Modifier.padding(start = 6.dp))
                            }
                        }

                        if (activeSession != null) {
                            Button(
                                onClick = { onCheckOut(activeSession.id) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pass_checkout_button")
                            ) {
                                Text("Check Out Now")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!hasLocationPermission) {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    } else {
                                        if (deviceFix == null) refreshLocation()
                                        showScanner = true
                                    }
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("scan_kiosk_qr_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Scan Wall QR", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }

            // 3. My Attendance History
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "My Attendance History", style = MaterialTheme.typography.titleMedium)
                    }
                    if (myAttendance.isEmpty()) {
                        Text(
                            text = "No check-ins recorded yet. Scan the Wall Kiosk QR or show your pass at Front Desk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        myAttendance.take(8).forEach { record ->
                            val checkInStr = record.checkInAt?.toDate()?.let { fmt.format(it) } ?: "Pending..."
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "$checkInStr IST",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${record.mode} • ${record.distanceMeters.toInt()} m (±${record.gpsAccuracyMeters.toInt()} m)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = record.status.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            // 4. Rejected Attempts Log
            if (rejections.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(text = "Logged Check-In Rejections", style = MaterialTheme.typography.titleMedium)
                        }
                        rejections.take(5).forEach { rej ->
                            Text(
                                text = "• [${rej.mode}] ${rej.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showScanner) {
        QrScannerSheet(
            title = "Scan Battle Box Wall QR",
            subtitle = "Point camera at the permanent Battle Box Wall QR poster pasted in the gym to self check-in.",
            onQrScanned = { scannedPayload ->
                showScanner = false
                scope.launch {
                    val latestFix = GeofenceUtils.fetchCurrentLocation(context) ?: deviceFix
                    onRefreshFix(latestFix)
                    onKioskCheckIn(scannedPayload, latestFix)
                }
            },
            onDismiss = { showScanner = false }
        )
    }
}
