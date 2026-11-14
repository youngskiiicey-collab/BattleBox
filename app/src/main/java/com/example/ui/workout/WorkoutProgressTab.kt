package com.example.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutProfile
import com.example.ui.components.CyberpunkButton
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberHudBg
import com.example.ui.theme.CyberHudSurface
import com.example.ui.theme.CyberHudSurfaceElevated
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen

@Composable
fun WorkoutProgressTab(
    workoutProfile: WorkoutProfile,
    workoutLogs: List<WorkoutLog>,
    isBusy: Boolean,
    onUpdateBodyMetrics: (weightKg: Double, heightCm: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showVolumeChart by remember { mutableStateOf(true) }
    var weightInput by remember(workoutProfile.weightKg) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", workoutProfile.weightKg))
    }
    var heightInput by remember(workoutProfile.heightCm) {
        mutableStateOf(String.format(java.util.Locale.US, "%.0f", workoutProfile.heightCm))
    }

    val volumePoints = remember(workoutLogs) {
        if (workoutLogs.isEmpty()) {
            listOf("S1" to 2850.0, "S2" to 3420.0, "S3" to 4100.0, "S4" to 4680.0)
        } else {
            workoutLogs.take(8).reversed().mapIndexed { idx, log ->
                "W${idx + 1}" to log.totalVolumeKg.coerceAtLeast(100.0)
            }
        }
    }
    val weightPoints = remember(workoutProfile.weightHistory) {
        if (workoutProfile.weightHistory.isEmpty()) {
            listOf("Start" to workoutProfile.weightKg)
        } else {
            workoutProfile.weightHistory.map { it.dateLabel to it.weightKg }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("workout_progress_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Toggleable Canvas Chart: Strength Volume vs Body Weight
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
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
                                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = CyberCyan)
                                    Text(
                                        text = if (showVolumeChart) "STRENGTH VOLUME (KG)" else "BODY WEIGHT TREND (KG)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberCyan
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = showVolumeChart,
                                    onClick = { showVolumeChart = true },
                                    label = {
                                        Text(
                                            "Strength Volume",
                                            color = if (showVolumeChart) CyberHudBg else CyberCyan
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan,
                                        containerColor = CyberHudSurfaceElevated
                                    ),
                                    modifier = Modifier.testTag("progress_toggle_volume")
                                )
                                FilterChip(
                                    selected = !showVolumeChart,
                                    onClick = { showVolumeChart = false },
                                    label = {
                                        Text(
                                            "Body Weight",
                                            color = if (!showVolumeChart) CyberHudBg else CyberNeonGreen
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberNeonGreen,
                                        containerColor = CyberHudSurfaceElevated
                                    ),
                                    modifier = Modifier.testTag("progress_toggle_weight")
                                )
                            }

                            val activeSeries = if (showVolumeChart) volumePoints else weightPoints
                            val lineColor = if (showVolumeChart) CyberCyan else CyberNeonGreen
                            CyberLineChart(
                                points = activeSeries,
                                lineColor = lineColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .testTag("progress_canvas_chart")
                            )
                        }
                    }

                    // Quick-Entry Weight & Height BMI Update Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = CyberNeonGreen)
                                    Text(
                                        text = "LOG BODY WEIGHT & BMI",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberNeonGreen
                                    )
                                }
                                Text(
                                    text = "BMI ${WorkoutProfile.formatBmi(workoutProfile.bmiRatio)} • ${workoutProfile.bmiLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { weightInput = it },
                                    label = { Text("Weight (kg)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = darkHudTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("progress_weight_input")
                                )
                                OutlinedTextField(
                                    value = heightInput,
                                    onValueChange = { heightInput = it },
                                    label = { Text("Height (cm)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = darkHudTextFieldColors(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("progress_height_input")
                                )
                                CyberpunkButton(
                                    text = "SAVE",
                                    accentColor = CyberNeonGreen,
                                    enabled = !isBusy,
                                    onClick = {
                                        val w = weightInput.toDoubleOrNull() ?: workoutProfile.weightKg
                                        val h = heightInput.toDoubleOrNull() ?: workoutProfile.heightCm
                                        onUpdateBodyMetrics(w, h)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .testTag("progress_save_metrics_button")
                                )
                            }
                        }
                    }

                    // Recent Personal Records (PRs)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recent_prs_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CyberAmber)
                                Text(
                                    text = "RECENT PERSONAL RECORDS (PRs)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberAmber
                                )
                            }
                            workoutProfile.personalRecords.forEachIndexed { idx, pr ->
                                if (idx > 0) HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pr.exerciseName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = pr.equipmentTag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF9FB0C2)
                                        )
                                    }
                                    Surface(
                                        color = CyberAmber.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.65f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${pr.bestWeightKg.toInt()}kg × ${pr.bestReps}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = CyberAmber,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Completed Session Logs
                    if (workoutLogs.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "COMPLETED WORKOUT LOGS (${workoutLogs.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberMagenta
                                )
                                workoutLogs.take(5).forEach { log ->
                                    Surface(
                                        color = CyberHudSurfaceElevated,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = log.workoutTitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${log.completedSets} sets • ${log.caloriesBurned} kcal • ${(log.durationSeconds / 60L).coerceAtLeast(1)}m",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF9EB0C2)
                                                )
                                            }
                                            Text(
                                                text = "${log.totalVolumeKg.toInt()}kg",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = CyberNeonGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberLineChart(
    points: List<Pair<String, Double>>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val values = points.map { it.second }
        val minVal = (values.minOrNull() ?: 0.0)
        val maxVal = (values.maxOrNull() ?: 100.0)
        val span = (maxVal - minVal).takeIf { it > 0.5 } ?: 10.0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${minVal.toInt()} kg",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8FA2B5)
            )
            Text(
                text = "Peak: ${maxVal.toInt()} kg",
                style = MaterialTheme.typography.labelSmall,
                color = lineColor
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            val padX = 14.dp.toPx()
            val padY = 12.dp.toPx()
            val plotW = (w - padX * 2).coerceAtLeast(10f)
            val plotH = (h - padY * 2).coerceAtLeast(10f)

            // Horizontal HUD gridlines
            for (g in 0..3) {
                val gy = padY + (plotH * g / 3f)
                drawLine(
                    color = CyberBorder.copy(alpha = 0.55f),
                    start = Offset(padX, gy),
                    end = Offset(w - padX, gy),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.size == 1) {
                val cx = w / 2f
                val cy = h / 2f
                drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                return@Canvas
            }

            val coords = points.mapIndexed { idx, (_, value) ->
                val x = padX + (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * plotW
                val normY = ((value - minVal) / span).toFloat().coerceIn(0f, 1f)
                val y = padY + (1f - normY) * plotH
                Offset(x, y)
            }

            val path = Path().apply {
                coords.forEachIndexed { idx, pt ->
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            coords.forEach { pt ->
                drawCircle(color = CyberHudBg, radius = 5.5.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = pt)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9EB0C2)
                )
            }
        }
    }
}
