package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.ui.theme.StatusOpenGreen
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun SelfCheckInNotificationFeedCard(
    liveRoster: List<AttendanceRecord>,
    modifier: Modifier = Modifier
) {
    val selfCheckIns = remember(liveRoster) {
        liveRoster.filter { it.mode == AttendanceRecord.MODE_KIOSK_QR }
    }
    val fmt = remember {
        SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("self_checkin_notifications_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = StatusOpenGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Wall QR Self Check-In Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time geofenced self log-ins pushed to Trainer & Admin UI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = if (selfCheckIns.isNotEmpty()) StatusOpenGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${selfCheckIns.size} ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selfCheckIns.isNotEmpty()) StatusOpenGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (selfCheckIns.isEmpty()) {
                Text(
                    text = "No active self check-ins via the printed Wall QR right now. When an athlete scans the wall poster inside the 100m geofence, an instant alert appears here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selfCheckIns.take(6).forEachIndexed { idx, rec ->
                            val timeStr = rec.checkInAt?.toDate()?.let { fmt.format(it) } ?: "Just now"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("self_checkin_alert_${rec.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusOpenGreen)
                                    )
                                    Column {
                                        Text(
                                            text = "${rec.memberName} (${rec.memberRole.uppercase()})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Self Check-In • $timeStr IST • ${rec.distanceMeters.toInt()}m (±${rec.gpsAccuracyMeters.toInt()}m GPS)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (idx < selfCheckIns.take(6).lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }
        }
    }
}
