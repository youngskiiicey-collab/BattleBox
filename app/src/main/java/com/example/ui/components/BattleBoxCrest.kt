package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vector emblem modeled after Battle Box's winged shield logo.
 */
@Composable
fun BattleBoxCrest(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shieldColor: Color = MaterialTheme.colorScheme.primary,
    wingColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f

        // Left & Right Wings (3 stylized feathers each)
        val featherAngles = listOf(-0.18f, -0.04f, 0.10f)
        featherAngles.forEachIndexed { idx, yShift ->
            val lenFactor = 0.45f - idx * 0.055f
            val yStart = h * (0.38f + idx * 0.075f)
            val yEnd = yStart + h * yShift

            // Left wing feather
            drawLine(
                color = wingColor,
                start = Offset(cx - w * 0.18f, yStart),
                end = Offset(cx - w * lenFactor, yEnd),
                strokeWidth = h * 0.065f,
                cap = StrokeCap.Round
            )
            // Right wing feather
            drawLine(
                color = wingColor,
                start = Offset(cx + w * 0.18f, yStart),
                end = Offset(cx + w * lenFactor, yEnd),
                strokeWidth = h * 0.065f,
                cap = StrokeCap.Round
            )
        }

        // Central Shield Silhouette
        val shieldPath = Path().apply {
            moveTo(cx, h * 0.14f)
            lineTo(cx + w * 0.24f, h * 0.22f)
            cubicTo(
                cx + w * 0.25f, h * 0.56f,
                cx + w * 0.16f, h * 0.78f,
                cx, h * 0.89f
            )
            cubicTo(
                cx - w * 0.16f, h * 0.78f,
                cx - w * 0.25f, h * 0.56f,
                cx - w * 0.24f, h * 0.22f
            )
            close()
        }

        drawPath(
            path = shieldPath,
            color = shieldColor
        )
        drawPath(
            path = shieldPath,
            color = wingColor,
            style = Stroke(width = h * 0.045f, join = StrokeJoin.Round)
        )

        // Small Gold Star inside bottom apex of shield
        val starCy = h * 0.69f
        val outerR = h * 0.075f
        val innerR = outerR * 0.42f
        val starPath = Path()
        for (i in 0 until 10) {
            val angle = (-PI / 2.0) + i * (PI / 5.0)
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + (cos(angle) * r).toFloat()
            val py = starCy + (sin(angle) * r).toFloat()
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        drawPath(path = starPath, color = accentColor)
    }
}
