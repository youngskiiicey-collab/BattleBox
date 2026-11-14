package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BattleBoxCrest
import com.example.ui.components.FirebaseConsoleCard
import com.example.ui.components.FirebaseConsoleLinks
import com.example.ui.components.MapsPhotoShowcaseCard

enum class AuthPortalTab {
    USER_TRAINER,
    ADMIN
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignInScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var activePortal by remember {
        mutableStateOf(AdminLoginPrefs.getPreferredPortal(context))
    }
    var showFirebaseConsole by remember { mutableStateOf(activePortal == AuthPortalTab.ADMIN) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Google Maps Battle Box Dudhnoi Brick & Winged Emblem Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_maps_battlebox_wall),
            contentDescription = "Battle Box Fight Club Dudhnoi crest mural",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x440E1114),
                            Color(0xCC0E1114),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(104.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "DUDHNOI, ASSAM • XQPM+99H • ASIA-SOUTH1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BattleBoxCrest(size = 62.dp)
                    Column {
                        Text(
                            text = "BATTLE BOX : FFF",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "FIGHT FOR FITNESS • DUDHNOI",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = "Sign in with one tap using your Google account. Your Firestore role (Member, Trainer, or Admin) automatically routes you to your dedicated dashboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Portal Login Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "SELECT LOGIN PORTAL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = activePortal == AuthPortalTab.USER_TRAINER,
                                onClick = {
                                    activePortal = AuthPortalTab.USER_TRAINER
                                    AdminLoginPrefs.savePreferredPortal(context, AuthPortalTab.USER_TRAINER)
                                    showFirebaseConsole = false
                                    errorMessage = null
                                    statusMessage = null
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("User / Trainer Login") },
                                modifier = Modifier.testTag("portal_tab_user_trainer")
                            )
                            FilterChip(
                                selected = activePortal == AuthPortalTab.ADMIN,
                                onClick = {
                                    activePortal = AuthPortalTab.ADMIN
                                    AdminLoginPrefs.savePreferredPortal(context, AuthPortalTab.ADMIN)
                                    showFirebaseConsole = true
                                    errorMessage = null
                                    statusMessage = null
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Admin Login") },
                                modifier = Modifier.testTag("portal_tab_admin")
                            )
                        }

                        if (activePortal == AuthPortalTab.ADMIN) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "1-TAP OWNER & ADMIN GOOGLE SIGN-IN",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    Text(
                                        text = "Verified owner account (${FirebaseConsoleLinks.OWNER_ADMIN_EMAIL}) and Firestore-assigned admins route straight to the Admin Console with zero password errors.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    GoogleSignInButton(
                                        buttonText = "Sign In to Admin Console with Google",
                                        outlined = false,
                                        useSecondaryContainer = true,
                                        testTagName = "admin_google_sign_in_button",
                                        onAuthSuccess = {
                                            errorMessage = null
                                            AdminLoginPrefs.savePreferredPortal(context, AuthPortalTab.ADMIN)
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg -> errorMessage = msg }
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "MEMBER & TRAINER ACCESS",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = "Sign in with Google to access your rotating 30s QR pass, geofenced check-in, Trainer Scanner roster, or INR plans.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    GoogleSignInButton(
                                        buttonText = "Continue with Google",
                                        outlined = false,
                                        testTagName = "google_sign_in_button",
                                        onAuthSuccess = {
                                            errorMessage = null
                                            AdminLoginPrefs.savePreferredPortal(context, AuthPortalTab.USER_TRAINER)
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg -> errorMessage = msg }
                                    )
                                }
                            }
                        }

                        if (statusMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = statusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (activePortal == AuthPortalTab.ADMIN) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                            OutlinedButton(
                                onClick = { showFirebaseConsole = !showFirebaseConsole },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("toggle_firebase_console_card")
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(
                                    text = if (showFirebaseConsole) {
                                        "Hide Firebase OAuth & Console Banner"
                                    } else {
                                        "Show Firebase OAuth & Console Banner"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                )
                                Icon(
                                    imageVector = if (showFirebaseConsole) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showFirebaseConsole) "Collapse Firebase Console options" else "Expand Firebase Console options"
                                )
                            }

                            if (showFirebaseConsole) {
                                FirebaseConsoleCard(
                                    onFeedback = { msg ->
                                        errorMessage = null
                                        statusMessage = msg
                                    }
                                )
                            }

                            FeatureRow(
                                icon = Icons.Default.VerifiedUser,
                                title = "Firebase OAuth & Credential Manager",
                                subtitle = "Provisioned Google Auth + RBAC custom claim routing for Owner/Admin."
                            )
                        }

                        FeatureRow(
                            icon = Icons.Default.Schedule,
                            title = "IST Timings • 05:30–10:00 & 16:30–21:30",
                            subtitle = "Automatic role-based redirect on login + 100m geofence."
                        )
                        FeatureRow(
                            icon = Icons.Default.LocationOn,
                            title = "Battle Box Fight Club • Dudhnoi, Assam",
                            subtitle = "QR attendance, live occupancy tracking & monthly CSV export."
                        )
                    }
                }

                // Showcase Google Maps photos on Sign-In as well
                MapsPhotoShowcaseCard(
                    mapsUrl = "https://maps.app.goo.gl/4ji9NFtZwpXZSiTN9",
                    compactHeader = true
                )

                Spacer(modifier = Modifier.height(52.dp))
            }
        }

        // Fixed bottom-left attribution badge
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 12.dp)
                .testTag("creator_attribution_badge")
        ) {
            Text(
                text = "App created by DocCoder.exe",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
