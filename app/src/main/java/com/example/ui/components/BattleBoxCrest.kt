package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberNeonGreen

/**
 * Brand logo emblem rendered from the official Battle Box : FFF winged green bodybuilder shield asset,
 * framed in a crisp Cyberpunk neon-ringed badge. Used on the TopAppBar, SignIn screen, and profile badges.
 */
@Composable
fun BattleBoxCrest(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shieldColor: Color = MaterialTheme.colorScheme.primary,
    wingColor: Color = CyberCyan,
    accentColor: Color = CyberNeonGreen,
    roundedSquare: Boolean = false
) {
    val shape = if (roundedSquare) RoundedCornerShape((size.value * 0.24f).dp) else CircleShape
    val borderWidth = if (size >= 48.dp) 2.dp else 1.5.dp

    Surface(
        shape = shape,
        color = Color(0xFF070A0E),
        border = BorderStroke(
            width = borderWidth,
            brush = Brush.linearGradient(listOf(accentColor, wingColor, shieldColor))
        ),
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (size >= 48.dp) 2.dp else 1.5.dp)
                .clip(shape)
                .background(Color(0xFF0A0E13))
                .border(0.5.dp, Color(0x4400F0FF), shape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.battlebox_logo_icon_1794561839972),
                contentDescription = "Battle Box : FFF Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        }
    }
}
