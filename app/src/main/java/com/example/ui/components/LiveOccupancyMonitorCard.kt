package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.UserProfile
import com.example.security.GeofenceUtils
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import com.example.ui.theme.StatusWarningAmber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveOccupancyMonitorCard(
    gymConfig: GymConfig,
    gymPublicStatus: GymPublicStatus,
    liveRoster: List<AttendanceRecord>,
    showRosterDetails: Boolean,
    isBusy: Boolean = false,
    onCheckOutSession: ((attendanceId: String, memberName: String) -> Unit)? = null,
    onSweepExpired: ((List<AttendanceRecord>) -> Unit)? = null,
    onViewFullRoster: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (isOpenIst, openLabel) = GeofenceUtils.evaluateOpenStatusIst(gymConfig)
    val capacity = gymConfig.maxCapacity.coerceAtLeast(1L)
    val liveCount = if (showRosterDetails) {
        liveRoster.size.toLong()
    } else {
        gymPublicStatus.inGymCount.coerceAtLeast(0L)
    }
    val occupancyRatio = (liveCount.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
    val animatedRatio by animateFloatAsState(targetValue = occupancyRatio, label = "occupancy_progress")
    val occupancyPercent = ((liveCount.toFloat() / capacity.toFloat()) * 100f).toInt().coerceIn(0, 100)
    val spotsFree = max(0L, capacity - liveCount)

    val (crowdLabel, crowdColor) = when {
        occupancyPercent >= 80 -> "PEAK CAPACITY" to StatusClosedRed
        occupancyPercent >= 40 -> "ACTIVE FLOOR" to StatusWarningAmber
        else -> "QUIET FLOOR" to StatusOpenGreen
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
            .testTag("live_occupancy_monitor_card")
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
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOpenIst) StatusOpenGreen else StatusClosedRed)
                            )
                            Text(
                                text = "LIVE OCCUPANCY MONITOR",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = if (showRosterDetails) {
                                "$openLabel • Real-time floor roster"
                            } else {
                                "$openLabel • Aggregate headcount only"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$liveCount / $capacity",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("in_gym_count_text")
                    )
                    Surface(
                        color = crowdColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$crowdLabel • $occupancyPercent%",
                            style = MaterialTheme.typography.labelSmall,
                            color = crowdColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { animatedRatio },
                color = crowdColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .testTag("occupancy_progress_bar")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$spotsFree spots open on floor",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Auto check-out: ${gymConfig.autoCheckoutHours}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showRosterDetails) {
                val memberCount = liveRoster.count { it.memberRole == UserProfile.ROLE_MEMBER }
                val staffCount = liveRoster.count { it.memberRole != UserProfile.ROLE_MEMBER }
                val trainerScannedCount = liveRoster.count { it.mode == AttendanceRecord.MODE_TRAINER_SCAN }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OccupancyStatChip(label = "Athletes: $memberCount")
                    OccupancyStatChip(label = "Coaches/Staff: $staffCount")
                    OccupancyStatChip(label = "Trainer QR: $trainerScannedCount")
                }

                if (onSweepExpired != null && liveRoster.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onSweepExpired(liveRoster) },
                            enabled = !isBusy,
                            modifier = Modifier.testTag("occupancy_sweep_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Sweep 3h Expired", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                if (liveRoster.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Floor is currently clear. Scan a member's rotating QR pass to check them in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
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
                            liveRoster.forEachIndexed { idx, rec ->
                                val checkInTime = rec.checkInAt?.toDate()?.let { fmt.format(it) } ?: "Now"
                                val modeLabel = when (rec.mode) {
                                    AttendanceRecord.MODE_TRAINER_SCAN -> "Trainer Scan"
                                    AttendanceRecord.MODE_FRONT_DESK -> "Front Desk"
                                    else -> "Kiosk QR"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("occupancy_roster_item_${rec.userId}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.memberName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${rec.memberRole.uppercase()} • $modeLabel • $checkInTime IST",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (onCheckOutSession != null) {
                                        OutlinedButton(
                                            onClick = { onCheckOutSession(rec.id, rec.memberName) },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.testTag("live_checkout_${rec.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Out", modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                }
                                if (idx < liveRoster.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                }
                            }
                            if (onViewFullRoster != null) {
                                TextButton(
                                    onClick = onViewFullRoster,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Open Staff Console")
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DPDP privacy active: members see live headcount & crowd level only.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OccupancyStatChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
