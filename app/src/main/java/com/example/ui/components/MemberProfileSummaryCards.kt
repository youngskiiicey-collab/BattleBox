package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.MemberSubscription
import com.example.data.model.UserProfile
import com.example.utils.MemberWorkoutSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Composable
fun MemberMembershipStatusCard(
    userProfile: UserProfile,
    activeSubscription: MemberSubscription?,
    onOpenPlansTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }
    val (statusBg, statusFg) = when (userProfile.membershipStatus.lowercase()) {
        UserProfile.STATUS_ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        UserProfile.STATUS_FROZEN -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f) to MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_membership_status_card")
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BattleBoxCrest(size = 44.dp)
                    Column {
                        Text(
                            text = userProfile.displayName.ifBlank { "Battle Box Athlete" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = statusBg,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("member_status_badge")
                ) {
                    Text(
                        text = userProfile.membershipStatus.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CardMembership,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Membership & Plan Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetricTile(
                    label = "Plan Tier",
                    value = userProfile.planTier.uppercase(),
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "Role",
                    value = userProfile.role.uppercase(),
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "AutoPay",
                    value = when {
                        activeSubscription?.autoRenew == true -> "ON"
                        activeSubscription != null -> "OFF"
                        else -> "N/A"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (activeSubscription != null) {
                val periodEndStr = activeSubscription.currentPeriodEnd?.toDate()?.let { dateFmt.format(it) } ?: "Active Cycle"
                val nextDebitStr = activeSubscription.nextDebitAt?.toDate()?.let { dateFmt.format(it) }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${activeSubscription.planName} • ₹${activeSubscription.amountInr} (${activeSubscription.billingCycle.uppercase()})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Valid Through: $periodEndStr IST • Payment: ${activeSubscription.upiHandle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (nextDebitStr != null) {
                            Text(
                                text = "Next Scheduled Debit: $nextDebitStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Plan Tier: ${userProfile.planTier.uppercase()} • No recurring UPI AutoPay mandate active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!userProfile.phone.isNullOrBlank()) {
                Text(text = "Phone: ${userProfile.phone}", style = MaterialTheme.typography.bodySmall)
            }
            if (!userProfile.emergencyContact.isNullOrBlank()) {
                Text(text = "Emergency Contact: ${userProfile.emergencyContact}", style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = onOpenPlansTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("profile_manage_plan_button")
            ) {
                Text("View Plans, Invoices & AutoPay")
            }
        }
    }
}

@Composable
fun PersonalWorkoutSummaryCard(
    summary: MemberWorkoutSummary,
    activeSession: AttendanceRecord?,
    modifier: Modifier = Modifier
) {
    val weeklyTarget = 4
    val weeklyProgress = (summary.last7DaysSessions.toFloat() / weeklyTarget.toFloat()).coerceIn(0f, 1f)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("personal_workout_summary_card")
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Personal Workout Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${summary.currentStreakDays}d Streak",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (activeSession != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "In Gym Now • Session active via ${formatModeLabel(activeSession.mode)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetricTile(
                    label = "Total Sessions",
                    value = "${summary.totalSessions}",
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "This Month",
                    value = "${summary.thisMonthSessions}",
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "Floor Time",
                    value = summary.formattedTotalDuration,
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "Avg / Session",
                    value = if (summary.avgSessionMinutes > 0L) "${summary.avgSessionMinutes}m" else "--",
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last 7 Days Target (${summary.last7DaysSessions}/$weeklyTarget sessions)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(weeklyProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { weeklyProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Preferred Batch: ${summary.preferredWindow}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Trainer Scans: ${summary.trainerScanCount + summary.frontDeskCount} • Kiosk: ${summary.kioskQrCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberAttendanceHistoryCard(
    attendanceRecords: List<AttendanceRecord>,
    onOpenPassTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val istZone = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val dateFmt = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istZone }
    }
    val timeOnlyFmt = remember {
        SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = istZone }
    }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0=All, 1=This Month, 2=Completed
    var showAllRecords by remember { mutableStateOf(false) }

    val nowCal = remember { Calendar.getInstance(istZone) }
    val filteredRecords = remember(attendanceRecords, selectedFilter) {
        when (selectedFilter) {
            1 -> attendanceRecords.filter { rec ->
                val ms = rec.checkInAt?.toDate()?.time ?: return@filter false
                val c = Calendar.getInstance(istZone).apply { timeInMillis = ms }
                c.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    c.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
            }
            2 -> attendanceRecords.filter { it.status != AttendanceRecord.STATUS_CHECKED_IN }
            else -> attendanceRecords
        }
    }

    val displayedRecords = if (showAllRecords) filteredRecords else filteredRecords.take(5)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("member_attendance_history_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance History (${attendanceRecords.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Synced live from Firestore • Asia/Kolkata IST",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("All (${attendanceRecords.size})") },
                    modifier = Modifier.testTag("history_filter_all")
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("This Month") },
                    modifier = Modifier.testTag("history_filter_month")
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("Completed") },
                    modifier = Modifier.testTag("history_filter_completed")
                )
            }

            if (filteredRecords.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (attendanceRecords.isEmpty()) {
                                "No gym check-ins recorded yet. Scan the Battle Box wall QR or present your rotating member pass to a Trainer."
                            } else {
                                "No sessions match this filter."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (attendanceRecords.isEmpty()) {
                            Button(
                                onClick = onOpenPassTab,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("history_open_pass_button")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Open Member QR Pass", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            } else {
                displayedRecords.forEachIndexed { idx, rec ->
                    val checkInDate = rec.checkInAt?.toDate()
                    val checkOutDate = rec.checkOutAt?.toDate()
                    val checkInLabel = checkInDate?.let { "${dateFmt.format(it)} IST" } ?: "Pending Timestamp"
                    val durationText = when {
                        rec.status == AttendanceRecord.STATUS_CHECKED_IN -> "In Gym Now"
                        checkInDate != null && checkOutDate != null -> {
                            val mins = TimeUnit.MILLISECONDS.toMinutes(
                                (checkOutDate.time - checkInDate.time).coerceAtLeast(0L)
                            )
                            "Out ${timeOnlyFmt.format(checkOutDate)} • ${mins}m"
                        }
                        else -> rec.status.replace('_', ' ').uppercase()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendance_history_item_${rec.id}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = checkInLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${formatModeLabel(rec.mode)} • ${rec.distanceMeters.toInt()}m geofence",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = if (rec.status == AttendanceRecord.STATUS_CHECKED_IN) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rec.status == AttendanceRecord.STATUS_CHECKED_IN) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (idx < displayedRecords.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                    }
                }

                if (filteredRecords.size > 5) {
                    OutlinedButton(
                        onClick = { showAllRecords = !showAllRecords },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("history_toggle_expand_button")
                    ) {
                        Text(
                            if (showAllRecords) {
                                "Show Latest 5 Sessions"
                            } else {
                                "Show All ${filteredRecords.size} Sessions"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatModeLabel(mode: String): String = when (mode) {
    AttendanceRecord.MODE_TRAINER_SCAN -> "Trainer QR Scan"
    AttendanceRecord.MODE_FRONT_DESK -> "Front Desk QR"
    AttendanceRecord.MODE_KIOSK_QR -> "Kiosk Wall QR"
    else -> mode.replace('_', ' ')
}
