package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceRejection
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.UserProfile
import com.example.data.repository.CheckInOutcome
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.ui.components.LiveOccupancyMonitorCard
import com.example.ui.theme.BattleGoldDark
import com.example.ui.theme.BattleGreenDark
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private const val SCAN_DEBOUNCE_MS = 2_500L

@Composable
fun QrAttendanceScannerScreen(
    currentUser: UserProfile,
    gymConfig: GymConfig,
    gymPublicStatus: GymPublicStatus,
    liveRoster: List<AttendanceRecord>,
    recentRejections: List<AttendanceRejection>,
    deviceFix: DeviceFix?,
    lastScanOutcome: CheckInOutcome?,
    isBusy: Boolean,
    onRefreshFix: (DeviceFix?) -> Unit,
    onMemberQrScanned: (qrPayload: String, fix: DeviceFix?) -> Unit,
    onCheckOutSession: (attendanceId: String, memberName: String) -> Unit,
    onSweepExpired: (List<AttendanceRecord>) -> Unit,
    onDismissOutcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    var isFetchingGps by remember { mutableStateOf(false) }
    var isScanningPaused by remember { mutableStateOf(false) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var manualToken by remember { mutableStateOf("") }

    var lastDecodedPayload by remember { mutableStateOf("") }
    var lastDecodedAtMs by remember { mutableLongStateOf(0L) }

    fun refreshLocation(onFixReady: ((DeviceFix?) -> Unit)? = null) {
        scope.launch {
            isFetchingGps = true
            val fix = GeofenceUtils.fetchCurrentLocation(context)
            onRefreshFix(fix)
            isFetchingGps = false
            onFixReady?.invoke(fix)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }
        if (hasLocationPermission && deviceFix == null) {
            refreshLocation()
        }
    }

    LaunchedEffect(isTorchEnabled, boundCamera) {
        if (hasFlashUnit) {
            boundCamera?.cameraControl?.enableTorch(isTorchEnabled)
        }
    }

    val distanceMeters = deviceFix?.let {
        GeofenceUtils.haversineMeters(it.latitude, it.longitude, gymConfig.latitude, gymConfig.longitude)
    }
    val insideGeofence = distanceMeters != null && distanceMeters <= gymConfig.geofenceRadiusM

    fun submitScannedToken(rawPayload: String) {
        val cleaned = rawPayload.trim()
        if (cleaned.isEmpty() || isBusy) return
        if (deviceFix != null) {
            onMemberQrScanned(cleaned, deviceFix)
        } else if (hasLocationPermission) {
            refreshLocation { fetched ->
                onMemberQrScanned(cleaned, fetched)
            }
        } else {
            onMemberQrScanned(cleaned, null)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWideLayout = maxWidth >= 840.dp

        if (isWideLayout) {
            Row(
                modifier = Modifier.widthIn(max = 1120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ScannerCameraSection(
                        currentUser = currentUser,
                        gymConfig = gymConfig,
                        deviceFix = deviceFix,
                        distanceMeters = distanceMeters,
                        insideGeofence = insideGeofence,
                        hasCameraPermission = hasCameraPermission,
                        hasLocationPermission = hasLocationPermission,
                        isFetchingGps = isFetchingGps,
                        isScanningPaused = isScanningPaused,
                        isTorchEnabled = isTorchEnabled,
                        hasFlashUnit = hasFlashUnit,
                        isBusy = isBusy,
                        manualToken = manualToken,
                        lastScanOutcome = lastScanOutcome,
                        onManualTokenChanged = { manualToken = it },
                        onRequestCameraPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                        onRequestOrRefreshGps = {
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
                        onTogglePause = { isScanningPaused = !isScanningPaused },
                        onToggleTorch = { isTorchEnabled = !isTorchEnabled },
                        onCameraBound = { cam ->
                            boundCamera = cam
                            hasFlashUnit = cam?.cameraInfo?.hasFlashUnit() == true
                        },
                        onCameraQrDecoded = { payload ->
                            val now = System.currentTimeMillis()
                            val isDuplicateFrame = payload == lastDecodedPayload && (now - lastDecodedAtMs) < SCAN_DEBOUNCE_MS
                            if (!isScanningPaused && !isBusy && !isDuplicateFrame) {
                                lastDecodedPayload = payload
                                lastDecodedAtMs = now
                                submitScannedToken(payload)
                            }
                        },
                        onSubmitManualToken = { submitScannedToken(manualToken) },
                        onDismissOutcome = onDismissOutcome
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OccupancyAndRejectionsSection(
                        gymConfig = gymConfig,
                        gymPublicStatus = gymPublicStatus,
                        liveRoster = liveRoster,
                        recentRejections = recentRejections,
                        isBusy = isBusy,
                        onCheckOutSession = onCheckOutSession,
                        onSweepExpired = onSweepExpired
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScannerCameraSection(
                    currentUser = currentUser,
                    gymConfig = gymConfig,
                    deviceFix = deviceFix,
                    distanceMeters = distanceMeters,
                    insideGeofence = insideGeofence,
                    hasCameraPermission = hasCameraPermission,
                    hasLocationPermission = hasLocationPermission,
                    isFetchingGps = isFetchingGps,
                    isScanningPaused = isScanningPaused,
                    isTorchEnabled = isTorchEnabled,
                    hasFlashUnit = hasFlashUnit,
                    isBusy = isBusy,
                    manualToken = manualToken,
                    lastScanOutcome = lastScanOutcome,
                    onManualTokenChanged = { manualToken = it },
                    onRequestCameraPermission = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    onRequestOrRefreshGps = {
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
                    onTogglePause = { isScanningPaused = !isScanningPaused },
                    onToggleTorch = { isTorchEnabled = !isTorchEnabled },
                    onCameraBound = { cam ->
                        boundCamera = cam
                        hasFlashUnit = cam?.cameraInfo?.hasFlashUnit() == true
                    },
                    onCameraQrDecoded = { payload ->
                        val now = System.currentTimeMillis()
                        val isDuplicateFrame = payload == lastDecodedPayload && (now - lastDecodedAtMs) < SCAN_DEBOUNCE_MS
                        if (!isScanningPaused && !isBusy && !isDuplicateFrame) {
                            lastDecodedPayload = payload
                            lastDecodedAtMs = now
                            submitScannedToken(payload)
                        }
                    },
                    onSubmitManualToken = { submitScannedToken(manualToken) },
                    onDismissOutcome = onDismissOutcome
                )

                OccupancyAndRejectionsSection(
                    gymConfig = gymConfig,
                    gymPublicStatus = gymPublicStatus,
                    liveRoster = liveRoster,
                    recentRejections = recentRejections,
                    isBusy = isBusy,
                    onCheckOutSession = onCheckOutSession,
                    onSweepExpired = onSweepExpired
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ScannerCameraSection(
    currentUser: UserProfile,
    gymConfig: GymConfig,
    deviceFix: DeviceFix?,
    distanceMeters: Double?,
    insideGeofence: Boolean,
    hasCameraPermission: Boolean,
    hasLocationPermission: Boolean,
    isFetchingGps: Boolean,
    isScanningPaused: Boolean,
    isTorchEnabled: Boolean,
    hasFlashUnit: Boolean,
    isBusy: Boolean,
    manualToken: String,
    lastScanOutcome: CheckInOutcome?,
    onManualTokenChanged: (String) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestOrRefreshGps: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleTorch: () -> Unit,
    onCameraBound: (Camera?) -> Unit,
    onCameraQrDecoded: (String) -> Unit,
    onSubmitManualToken: () -> Unit,
    onDismissOutcome: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("camerax_scanner_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "CAMERAX ATTENDANCE SCANNER",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Staff: ${currentUser.displayName} (${currentUser.role.uppercase()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRequestOrRefreshGps,
                    enabled = !isFetchingGps,
                    modifier = Modifier.testTag("scanner_refresh_gps_button")
                ) {
                    if (isFetchingGps) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (hasLocationPermission) "Refresh GPS" else "Grant GPS",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Geofence status chip
            Surface(
                color = if (insideGeofence) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when {
                        deviceFix != null && distanceMeters != null ->
                            "Scanner Geofence: ${distanceMeters.toInt()} m from ${gymConfig.gymName} " +
                                "(limit ${gymConfig.geofenceRadiusM.toInt()} m, ±${deviceFix.accuracyMeters.toInt()} m)"
                        hasLocationPermission ->
                            "Acquiring scanner GPS coordinates... Tap Refresh GPS if needed."
                        else ->
                            "Location permission required to verify scanner is inside Battle Box Dudhnoi."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (insideGeofence) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // CameraX Live Viewfinder + Reticle
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0D10))
                        .testTag("camerax_preview_container")
                ) {
                    CameraXQrViewfinder(
                        isPaused = isScanningPaused || isBusy,
                        onQrDecoded = onCameraQrDecoded,
                        onCameraBound = onCameraBound,
                        modifier = Modifier.fillMaxSize()
                    )

                    ScannerReticleOverlay(
                        isPaused = isScanningPaused || isBusy,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top controls inside viewfinder (Torch + Pause/Resume)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (hasFlashUnit) {
                            IconButton(
                                onClick = onToggleTorch,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0x99161A1F),
                                    contentColor = if (isTorchEnabled) BattleGoldDark else Color.White
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("scanner_torch_button")
                            ) {
                                Icon(
                                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = if (isTorchEnabled) "Turn flash off" else "Turn flash on"
                                )
                            }
                        }

                        IconButton(
                            onClick = onTogglePause,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0x99161A1F),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("scanner_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isScanningPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isScanningPaused) "Resume QR scanner" else "Pause QR scanner"
                            )
                        }
                    }

                    Surface(
                        color = Color(0xB30E1114),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = when {
                                isBusy -> "Verifying token & storing in Firestore..."
                                isScanningPaused -> "Scanner paused • Tap play to resume"
                                else -> "Align member's 30s rotating QR inside frame"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .testTag("camerax_permission_prompt")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera permission is required for CameraX QR attendance scanning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestCameraPermission,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("scanner_grant_camera_button")
                        ) {
                            Text("Grant Camera Permission")
                        }
                    }
                }
            }

            // Instant Verification Result Banner
            AnimatedVisibility(visible = lastScanOutcome != null) {
                lastScanOutcome?.let { outcome ->
                    val isSuccess = outcome is CheckInOutcome.CheckedIn
                    Surface(
                        color = if (isSuccess) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_outcome_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isSuccess) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Column {
                                    Text(
                                        text = when (outcome) {
                                            is CheckInOutcome.CheckedIn ->
                                                "ATTENDANCE STORED: ${outcome.memberName.uppercase()}"
                                            is CheckInOutcome.Rejected -> "SCAN REJECTED"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSuccess) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                    Text(
                                        text = when (outcome) {
                                            is CheckInOutcome.CheckedIn ->
                                                "Verified ${outcome.distanceMeters.toInt()} m from Battle Box (±${outcome.accuracyMeters.toInt()} m). Live occupancy updated."
                                            is CheckInOutcome.Rejected -> outcome.reason
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSuccess) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissOutcome,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("scanner_dismiss_outcome_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss scan result"
                                )
                            }
                        }
                    }
                }
            }

            // Fallback / Emulator Manual Token Input
            OutlinedTextField(
                value = manualToken,
                onValueChange = onManualTokenChanged,
                label = { Text("Paste Member Rotating QR Token (Emulator / Fallback)") },
                placeholder = { Text("BB1|MEMBER|<uid>|<slot>|<sig>") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_qr_token_input")
            )

            Button(
                onClick = onSubmitManualToken,
                enabled = manualToken.isNotBlank() && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_manual_qr_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Verify Member QR & Mark Attendance", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun OccupancyAndRejectionsSection(
    gymConfig: GymConfig,
    gymPublicStatus: GymPublicStatus,
    liveRoster: List<AttendanceRecord>,
    recentRejections: List<AttendanceRejection>,
    isBusy: Boolean,
    onCheckOutSession: (attendanceId: String, memberName: String) -> Unit,
    onSweepExpired: (List<AttendanceRecord>) -> Unit
) {
    LiveOccupancyMonitorCard(
        gymConfig = gymConfig,
        gymPublicStatus = gymPublicStatus,
        liveRoster = liveRoster,
        showRosterDetails = true,
        isBusy = isBusy,
        onCheckOutSession = onCheckOutSession,
        onSweepExpired = onSweepExpired
    )

    if (recentRejections.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scanner_rejections_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Recent Rejected Scans (${recentRejections.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                recentRejections.take(5).forEach { rej ->
                    Text(
                        text = "• [${rej.mode}] ${rej.reason} (${rej.distanceMeters.toInt()} m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraXQrViewfinder(
    isPaused: Boolean,
    onQrDecoded: (String) -> Unit,
    onCameraBound: (Camera?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val reader = remember { QRCodeReader() }

    val latestPaused by rememberUpdatedState(isPaused)
    val latestOnDecoded by rememberUpdatedState(onQrDecoded)
    val latestOnCameraBound by rememberUpdatedState(onCameraBound)

    DisposableEffect(Unit) {
        onDispose {
            latestOnCameraBound(null)
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        if (latestPaused) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        try {
                            val plane = imageProxy.planes.firstOrNull()
                            if (plane != null) {
                                val buffer = plane.buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val source = PlanarYUVLuminanceSource(
                                    bytes,
                                    imageProxy.width,
                                    imageProxy.height,
                                    0,
                                    0,
                                    imageProxy.width,
                                    imageProxy.height,
                                    false
                                )
                                val bitmap = BinaryBitmap(HybridBinarizer(source))
                                val result = reader.decode(bitmap)
                                val decodedText = result.text
                                if (!decodedText.isNullOrBlank()) {
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        latestOnDecoded(decodedText)
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Frame had no readable QR
                        } finally {
                            reader.reset()
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    latestOnCameraBound(camera)
                } catch (_: Exception) {
                    // Camera unavailable on headless/virtual device without hardware lens
                    latestOnCameraBound(null)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ScannerReticleOverlay(
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "qr_scan_beam")
    val sweepFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qr_beam_y"
    )

    Canvas(modifier = modifier) {
        val boxSide = minOf(size.width, size.height) * 0.62f
        val left = (size.width - boxSide) / 2f
        val top = (size.height - boxSide) / 2f
        val right = left + boxSide
        val bottom = top + boxSide
        val cornerLen = boxSide * 0.16f
        val strokePx = 4.dp.toPx()
        val frameColor = if (isPaused) Color(0xFF94A3B8) else BattleGreenDark

        // Top-Left corner
        drawLine(frameColor, Offset(left, top), Offset(left + cornerLen, top), strokePx, StrokeCap.Round)
        drawLine(frameColor, Offset(left, top), Offset(left, top + cornerLen), strokePx, StrokeCap.Round)

        // Top-Right corner
        drawLine(frameColor, Offset(right - cornerLen, top), Offset(right, top), strokePx, StrokeCap.Round)
        drawLine(frameColor, Offset(right, top), Offset(right, top + cornerLen), strokePx, StrokeCap.Round)

        // Bottom-Left corner
        drawLine(frameColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokePx, StrokeCap.Round)
        drawLine(frameColor, Offset(left, bottom - cornerLen), Offset(left, bottom), strokePx, StrokeCap.Round)

        // Bottom-Right corner
        drawLine(frameColor, Offset(right - cornerLen, bottom), Offset(right, bottom), strokePx, StrokeCap.Round)
        drawLine(frameColor, Offset(right, bottom - cornerLen), Offset(right, bottom), strokePx, StrokeCap.Round)

        if (!isPaused) {
            val beamY = top + (boxSide * sweepFraction)
            drawLine(
                color = BattleGoldDark.copy(alpha = 0.9f),
                start = Offset(left + 8.dp.toPx(), beamY),
                end = Offset(right - 8.dp.toPx(), beamY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
