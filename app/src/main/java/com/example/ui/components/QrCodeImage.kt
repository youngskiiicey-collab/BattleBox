package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.security.QrTokenSigner
import kotlinx.coroutines.delay

@Composable
fun RotatingQrCard(
    title: String,
    subtitle: String,
    tokenProvider: (nowMillis: Long) -> String,
    onCopied: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
    }

    val epochSlot = QrTokenSigner.currentEpochSlot(nowMillis)
    val secondsLeft = QrTokenSigner.secondsRemainingInSlot(nowMillis)
    val payload = remember(epochSlot) { tokenProvider(nowMillis) }
    val qrBitmap = remember(payload) { QrTokenSigner.renderQrBitmap(payload, 560) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "$title QR code",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(196.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROTATES IN ${secondsLeft}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (secondsLeft <= 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Surface(
                    onClick = {
                        clipboard.setText(AnnotatedString(payload))
                        onCopied("Signed rotating token copied (expires in ${secondsLeft}s).")
                    },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "Copy Token",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { secondsLeft.toFloat() / QrTokenSigner.ROTATION_INTERVAL_SECONDS.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = if (secondsLeft <= 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PermanentWallQrCard(
    geofenceRadiusMeters: Int,
    maxGpsAccuracyMeters: Int,
    onPrintPoster: () -> Unit,
    onCopied: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val payload = remember { QrTokenSigner.generatePermanentWallToken("main") }
    val qrBitmap = remember(payload) { QrTokenSigner.renderQrBitmap(payload, 640) }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permanent_wall_qr_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "PERMANENT • NON-ROTATING • NO EXPIRY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "Battle Box Printable Wall QR",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Print and paste this permanent QR inside the gym. Members self check-in using GPS geofence (<= ${geofenceRadiusMeters}m, ±${maxGpsAccuracyMeters}m) without Admin involvement, and Trainers + Admin receive instant alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Permanent printable Battle Box Wall QR code",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(202.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = onPrintPoster,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("print_wall_qr_button")
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("Print Wall Poster PDF", modifier = Modifier.padding(start = 6.dp))
            }
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(payload))
                    onCopied("Permanent Wall QR token copied.")
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("copy_wall_qr_button")
            ) {
                Text("Copy Token")
            }
        }
    }
}
