package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.GymAnnouncement
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.UserProfile
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.ui.components.EmbeddedMapCard
import com.example.ui.components.LiveOccupancyMonitorCard
import com.example.ui.components.MapsPhotoShowcaseCard
import com.example.ui.theme.StatusClosedRed
import com.example.ui.theme.StatusOpenGreen
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun HomeDashboardScreen(
    userProfile: UserProfile,
    gymConfig: GymConfig,
    gymPublicStatus: GymPublicStatus,
    activeSession: AttendanceRecord?,
    liveRoster: List<AttendanceRecord>,
    announcements: List<GymAnnouncement> = emptyList(),
    deviceFix: DeviceFix?,
    onOpenPassTab: () -> Unit,
    onCheckOutActive: (String) -> Unit,
    onOpenConcierge: () -> Unit,
    onOpenAdminRoster: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (isOpenIst, openLabel) = GeofenceUtils.evaluateOpenStatusIst(gymConfig)
    val isAdmin = userProfile.role == UserProfile.ROLE_ADMIN

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
            // Live In-App Announcement Banner (Open / Closed / Special Hours / Holiday)
            announcements.firstOrNull()?.let { ann ->
                val isClosed = ann.statusType == GymAnnouncement.TYPE_CLOSED || ann.statusType == GymAnnouncement.TYPE_HOLIDAY
                Surface(
                    color = if (isClosed) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_announcement_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = if (isClosed) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${ann.statusType.replace("_", " ").uppercase()} • ${ann.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isClosed) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            Text(
                                text = ann.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isClosed) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            if (ann.effectiveHours.isNotBlank()) {
                                Text(
                                    text = "Effective: ${ann.effectiveHours}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isClosed) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Hero Gym Banner Card featuring Battle Box Dudhnoi Google Maps mural
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_maps_battlebox_wall),
                    contentDescription = "Battle Box Fight Club Dudhnoi emblem wall",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x330E1114), Color(0xE80E1114))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isOpenIst) StatusOpenGreen.copy(alpha = 0.24f) else StatusClosedRed.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = openLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isOpenIst) StatusOpenGreen else StatusClosedRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RoleChip(text = userProfile.role.uppercase())
                            RoleChip(text = "${userProfile.planTier.uppercase()} PLAN")
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "WELCOME, ${userProfile.displayName.uppercase()}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Dudhnoi, Assam • AM ${gymConfig.morningOpen}–${gymConfig.morningClose} • PM ${gymConfig.eveningOpen}–${gymConfig.eveningClose} IST",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Active Session Card (if checked in)
            if (activeSession != null) {
                val fmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                }
                val expiresText = activeSession.expiresAt?.toDate()?.let { fmt.format(it) } ?: "3h"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
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
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column {
                                Text(
                                    text = "CHECKED IN • ON THE FLOOR",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Mode: ${activeSession.mode} • Auto check-out at $expiresText IST",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Button(
                            onClick = { onCheckOutActive(activeSession.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("home_checkout_button")
                        ) {
                            Text("Check Out")
                        }
                    }
                }
            }

            // Live Occupancy Monitor (aggregate count for Members; full live floor view for Staff)
            LiveOccupancyMonitorCard(
                gymConfig = gymConfig,
                gymPublicStatus = gymPublicStatus,
                liveRoster = liveRoster,
                showRosterDetails = isAdmin || userProfile.role == UserProfile.ROLE_TRAINER,
                onViewFullRoster = if (isAdmin) onOpenAdminRoster else null
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenPassTab,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_open_pass_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "QR Pass & Check-In",
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                OutlinedButton(
                    onClick = onOpenConcierge,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_open_concierge_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "AI Concierge",
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // Google Maps Photo Showcase (Battle Box Fight Club Dudhnoi)
            MapsPhotoShowcaseCard(
                mapsUrl = gymConfig.mapsUrl
            )

            // Embedded Google Map + Get Directions
            EmbeddedMapCard(
                gymConfig = gymConfig,
                deviceFix = deviceFix
            )

            // Phase 1 Security & Region Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = "Membership: ${userProfile.membershipStatus.uppercase()}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Data Region: ${gymConfig.dataRegion} • Currency: ${gymConfig.currency} • Consent v${userProfile.termsVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RoleChip(text: String) {
    Surface(
        color = Color(0xAA1F262D),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
