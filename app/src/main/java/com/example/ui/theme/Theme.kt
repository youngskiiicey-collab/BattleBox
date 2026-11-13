package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BattleBoxDarkColorScheme = darkColorScheme(
    primary = BattleGreenDark,
    onPrimary = OnBattleGreenDark,
    primaryContainer = BattleGreenContainerDark,
    onPrimaryContainer = OnBattleGreenContainerDark,
    secondary = BattleGoldDark,
    onSecondary = OnBattleGoldDark,
    secondaryContainer = BattleGoldContainerDark,
    onSecondaryContainer = Color(0xFFFFE5A3),
    tertiary = BrickTerracottaDark,
    onTertiary = OnBrickDark,
    tertiaryContainer = BrickContainerDark,
    onTertiaryContainer = OnBrickContainerDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkOutline,
    error = StatusClosedRed,
    onError = Color(0xFFFFFFFF)
)

private val BattleBoxLightColorScheme = lightColorScheme(
    primary = BattleGreenLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BattleGreenContainerLight,
    onPrimaryContainer = OnBattleGreenContainerLight,
    secondary = BattleGoldLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BattleGoldContainerLight,
    onSecondaryContainer = Color(0xFF2B1900),
    tertiary = BrickTerracottaLight,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = BrickContainerLight,
    onTertiaryContainer = OnBrickContainerLight,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = Color(0xFFEEF3EE),
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF)
)

val BattleBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BattleBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BattleBoxDarkColorScheme else BattleBoxLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BattleBoxTypography,
        shapes = BattleBoxShapes,
        content = content
    )
}
