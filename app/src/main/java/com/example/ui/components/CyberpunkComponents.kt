package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberHudBg
import com.example.ui.theme.CyberHudSurface
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen

data class CyberHudAlert(
    val id: String,
    val title: String,
    val message: String,
    val badge: String = "SYSTEM HUD",
    val accentColor: Color = CyberCyan
)

/**
 * Chamfered Cyberpunk & Mechatronic primary CTA button with neon edge glow.
 */
@Composable
fun CyberpunkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    accentColor: Color = CyberCyan
) {
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 3.dp, bottomStart = 3.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CyberHudSurface,
            contentColor = accentColor,
            disabledContainerColor = Color(0xFF14191F),
            disabledContentColor = Color(0xFF5D6A78)
        ),
        border = BorderStroke(
            width = 1.4.dp,
            color = if (enabled) accentColor else Color(0xFF2B3540)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Chamfered Cyberpunk & Mechatronic secondary outlined action button.
 */
@Composable
fun CyberpunkOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    accentColor: Color = CyberNeonGreen
) {
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 3.dp, bottomStart = 3.dp)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            width = 1.2.dp,
            color = if (enabled) accentColor.copy(alpha = 0.85f) else Color(0xFF2C3440)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF0A0F14),
            contentColor = accentColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Compact mechatronic HUD telemetry chip.
 */
@Composable
fun MechatronicStatusChip(
    label: String,
    accentColor: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    val shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
    Surface(
        shape = shape,
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Floating Cyberpunk & Mechatronic HUD notification banner for check-in/out, admin join/leave,
 * announcements, geofence auto-out, and membership renewal triggers.
 */
@Composable
fun CyberpunkNotificationBanner(
    alert: CyberHudAlert?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (alert != null) {
            val shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
            Surface(
                shape = shape,
                color = CyberHudBg.copy(alpha = 0.96f),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(alert.accentColor, CyberMagenta))
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("cyberpunk_hud_notification_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(alert.accentColor.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = alert.accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, alert.accentColor.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "HUD Alert",
                            tint = alert.accentColor,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[${alert.badge}]",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = alert.accentColor
                            )
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD5E1ED),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cyberpunk_hud_banner_dismiss")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = Color(0xFFAEB8C4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared Mechatronic Workout Mode switcher placed above Home across Member, Trainer, and Admin consoles
 * so users can jump straight into their 4-tab Jerai/Aerofit workout tracker without re-scanning QR.
 */
@Composable
fun WorkoutModeToggleBar(
    isWorkoutMode: Boolean,
    streakDays: Int,
    bmiText: String?,
    onToggleWorkoutMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
    Surface(
        shape = shape,
        color = CyberHudSurface,
        border = BorderStroke(
            width = 1.4.dp,
            brush = Brush.horizontalGradient(
                if (isWorkoutMode) listOf(CyberNeonGreen, CyberCyan)
                else listOf(CyberCyan, CyberBorder)
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onToggleWorkoutMode(!isWorkoutMode) }
            .testTag("workout_mode_toggle_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            (if (isWorkoutMode) CyberNeonGreen else CyberCyan).copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            (if (isWorkoutMode) CyberNeonGreen else CyberCyan).copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            (if (isWorkoutMode) CyberNeonGreen else CyberCyan).copy(alpha = 0.65f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWorkoutMode) Icons.Default.Bolt else Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isWorkoutMode) CyberNeonGreen else CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isWorkoutMode) "WORKOUT MODE ACTIVE" else "WORKOUT MODE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isWorkoutMode) CyberNeonGreen else CyberCyan
                        )
                        if (streakDays > 0) {
                            Surface(
                                color = CyberAmber.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${streakDays}D STREAK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = buildString {
                            append(
                                if (isWorkoutMode) "Tap to return to Club Console"
                                else "Continue Jerai/Aerofit workout without re-scanning QR"
                            )
                            if (!bmiText.isNullOrBlank()) {
                                append(" • BMI ")
                                append(bmiText)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBAC6D4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(
                checked = isWorkoutMode,
                onCheckedChange = onToggleWorkoutMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberHudBg,
                    checkedTrackColor = CyberNeonGreen,
                    uncheckedThumbColor = CyberCyan,
                    uncheckedTrackColor = CyberHudBg,
                    uncheckedBorderColor = CyberCyan
                ),
                modifier = Modifier.testTag("workout_mode_switch")
            )
        }
    }
}
