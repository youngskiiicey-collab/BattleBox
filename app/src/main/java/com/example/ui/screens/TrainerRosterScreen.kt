package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.GymAnnouncement
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.UserProfile
import com.example.security.GeofenceUtils
import com.example.ui.components.BattleBoxCrest
import com.example.ui.components.CyberpunkButton
import com.example.ui.components.CyberpunkOutlinedButton
import com.example.ui.components.LiveOccupancyMonitorCard
import com.example.ui.components.MapsPhotoShowcaseCard
import com.example.ui.components.SelfCheckInNotificationFeedCard
import com.example.ui.components.WorkoutModeToggleBar
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNeonGreen

@Composable
fun TrainerRosterScreen(
    currentUser: UserProfile,
    assignedMembers: List<UserProfile>,
    liveRoster: List<AttendanceRecord> = emptyList(),
    gymConfig: GymConfig = GymConfig(),
    gymPublicStatus: GymPublicStatus = GymPublicStatus(),
    announcements: List<GymAnnouncement> = emptyList(),
    isBusy: Boolean = false,
    streakDays: Int = 0,
    bmiText: String? = null,
    onToggleWorkoutMode: (Boolean) -> Unit = {},
    onOpenScannerTab: () -> Unit = {},
    onOpenPassTab: () -> Unit = {},
    onCheckOutSession: (attendanceId: String, memberName: String) -> Unit = { _, _ -> },
    onSweepExpired: (List<AttendanceRecord>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (_, openLabel) = GeofenceUtils.evaluateOpenStatusIst(gymConfig)

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
            WorkoutModeToggleBar(
                isWorkoutMode = false,
                streakDays = streakDays,
                bmiText = bmiText,
                onToggleWorkoutMode = onToggleWorkoutMode
            )

            // Trainer Home Dashboard Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trainer_home_dashboard")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            BattleBoxCrest(size = 46.dp)
                            Column {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "TRAINER COMMAND DASHBOARD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Coach ${currentUser.displayName}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "${gymConfig.gymName} • $openLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${liveRoster.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "In Gym",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    CyberpunkButton(
                        text = "OPEN CAMERAX QR SCANNER",
                        icon = Icons.Default.QrCodeScanner,
                        accentColor = CyberNeonGreen,
                        onClick = onOpenScannerTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trainer_open_scanner_button")
                    )

                    CyberpunkOutlinedButton(
                        text = "MY COACH QR PASS / SELF CHECK-IN",
                        icon = Icons.Default.QrCode2,
                        accentColor = CyberCyan,
                        onClick = onOpenPassTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trainer_checkin_pass_button")
                    )
                }
            }

            // Real-time Wall QR Self Check-In Notifications for Trainers
            SelfCheckInNotificationFeedCard(liveRoster = liveRoster)

            // Real-time Live Occupancy Monitoring for Coaches
            LiveOccupancyMonitorCard(
                gymConfig = gymConfig,
                gymPublicStatus = gymPublicStatus,
                liveRoster = liveRoster,
                showRosterDetails = true,
                isBusy = isBusy,
                onCheckOutSession = onCheckOutSession,
                onSweepExpired = onSweepExpired
            )

            // Latest Gym Announcement Banner for Trainers
            announcements.firstOrNull()?.let { ann ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column {
                            Text(
                                text = "${ann.statusType.replace("_", " ").uppercase()} • ${ann.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = ann.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Assigned Athletes Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            text = "Assigned Athletes (${assignedMembers.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Firestore rules enforce that trainers view ONLY athletes assigned to UID ${currentUser.userId.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (assignedMembers.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No Assigned Members Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ask an Admin to assign members to your trainer profile in Admin > Users & Roles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                assignedMembers.forEach { member ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Plan: ${member.planTier.uppercase()} • Consent v${member.termsVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = member.membershipStatus.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Battle Box Dudhnoi Google Maps Photo Showcase
            MapsPhotoShowcaseCard(mapsUrl = gymConfig.mapsUrl, compactHeader = true)

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
