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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.legal.LegalDrafts
import com.example.data.model.AttendanceRecord
import com.example.data.model.ConsentRecord
import com.example.data.model.GymConfig
import com.example.data.model.MemberSubscription
import com.example.data.model.UserProfile
import com.example.ui.components.MemberAttendanceHistoryCard
import com.example.ui.components.MemberMembershipStatusCard
import com.example.ui.components.PersonalWorkoutSummaryCard
import com.example.utils.MemberWorkoutSummaryCalculator
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun ProfileAndLegalScreen(
    userProfile: UserProfile,
    gymConfig: GymConfig,
    myAttendance: List<AttendanceRecord> = emptyList(),
    mySubscriptions: List<MemberSubscription> = emptyList(),
    consentReceipts: List<ConsentRecord> = emptyList(),
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenPlansTab: () -> Unit = {},
    onOpenPassTab: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandLegal by remember { mutableStateOf(false) }
    val fmt = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
    }
    val latestReceipt = consentReceipts.firstOrNull()
    val consentDate = (latestReceipt?.acceptedAt ?: userProfile.consentAcceptedAt)
        ?.toDate()
        ?.let { fmt.format(it) }
        ?: "Recorded"

    val workoutSummary = remember(myAttendance) {
        MemberWorkoutSummaryCalculator.summarize(myAttendance)
    }
    val activeSession = remember(myAttendance) {
        myAttendance.firstOrNull { it.status == AttendanceRecord.STATUS_CHECKED_IN }
    }
    val activeSubscription = remember(mySubscriptions) {
        mySubscriptions.firstOrNull {
            it.status == MemberSubscription.STATUS_ACTIVE ||
                it.status == MemberSubscription.STATUS_CANCEL_REQUESTED
        } ?: mySubscriptions.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("member_profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Membership Status & Plan Details (from Firestore users + subscriptions)
            MemberMembershipStatusCard(
                userProfile = userProfile,
                activeSubscription = activeSubscription,
                onOpenPlansTab = onOpenPlansTab
            )

            // 2. Personal Workout Summary (computed from Firestore attendance history)
            PersonalWorkoutSummaryCard(
                summary = workoutSummary,
                activeSession = activeSession
            )

            // 3. Attendance History (retrieved live from Firestore attendance collection)
            MemberAttendanceHistoryCard(
                attendanceRecords = myAttendance,
                onOpenPassTab = onOpenPassTab
            )

            // 4. Google Sign-In Verification Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Google Account Sign-In Active", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Authenticated via Credential Manager Google ID (${userProfile.email}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "UID: ${userProfile.userId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 5. Theme Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(text = "Battle Box Dark Theme", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Dudhnoi brick, matte charcoal, battle green & gold palette",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_switch")
                    )
                }
            }

            // 6. Legal & DPDP Act 2023 Consent
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
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(text = "Legal & DPDP Act 2023 Consent", style = MaterialTheme.typography.titleMedium)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Consent Verified • v${latestReceipt?.termsVersion ?: userProfile.termsVersion}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "Server Timestamp (IST): $consentDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (latestReceipt != null) {
                                Text(
                                    text = "Immutable Receipt ID: ${latestReceipt.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = LegalDrafts.DRAFT_NOTICE,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Data Residency: ${gymConfig.dataRegion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedButton(onClick = { expandLegal = !expandLegal }) {
                        Text(if (expandLegal) "Hide Full T&C & DPDP Policy" else "View Full T&C & DPDP Policy")
                    }

                    if (expandLegal) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        LegalDrafts.sections.forEach { sec ->
                            Text(text = sec.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = sec.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = "Sign Out", modifier = Modifier.padding(start = 8.dp))
            }

            Text(
                text = "App created by DocCoder.exe",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, bottom = 12.dp)
                    .testTag("profile_creator_attribution")
            )
        }
    }
}
