package com.example.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutProfile
import com.example.ui.components.BattleBoxCrest
import com.example.ui.components.CyberpunkButton
import com.example.ui.components.MechatronicStatusChip
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberHudSurface
import com.example.ui.theme.CyberHudSurfaceElevated
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen

@Composable
fun WorkoutHomeTab(
    user: UserProfile,
    workoutProfile: WorkoutProfile,
    recentLogs: List<WorkoutLog>,
    activeSession: AttendanceRecord?,
    isBusy: Boolean,
    onQuickLogMacros: (kcal: Int, proteinG: Int, activeMin: Int) -> Unit,
    onUpdateBmiMetrics: (weightKg: Double, heightCm: Double) -> Unit,
    onStartWorkoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var weightInput by remember(workoutProfile.weightKg) {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", workoutProfile.weightKg))
    }
    var heightInput by remember(workoutProfile.heightCm) {
        mutableStateOf(String.format(java.util.Locale.US, "%.0f", workoutProfile.heightCm))
    }
    var customKcalInput by remember { mutableStateOf("") }
    var customProteinInput by remember { mutableStateOf("") }

    val bmiRatio = workoutProfile.bmiRatio
    val bmiFormatted = WorkoutProfile.formatBmi(bmiRatio)
    val bmiLabel = workoutProfile.bmiLabel

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("workout_home_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.widthIn(max = 680.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header & Personal Workout Continuation Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp),
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    BattleBoxCrest(size = 46.dp)
                                    Column {
                                        Text(
                                            text = "READINESS HUB • ${user.role.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = user.displayName.ifBlank { "Fighter" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Surface(
                                    color = CyberAmber.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, CyberAmber),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = CyberAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${workoutProfile.streakDays}D STREAK",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = CyberAmber
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MechatronicStatusChip(
                                    label = if (activeSession != null) "FLOOR SESSION ACTIVE • NO RE-SCAN NEEDED" else "PERSONAL SUMMARY READY • NO QR RE-SCAN NEEDED",
                                    accentColor = if (activeSession != null) CyberNeonGreen else CyberCyan
                                )
                            }
                            if (recentLogs.isNotEmpty()) {
                                val lastLog = recentLogs.first()
                                Text(
                                    text = "Last Session: ${lastLog.workoutTitle} • ${lastLog.totalVolumeKg.toInt()} kg lifted • ${lastLog.completedSets} sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB4C3D4)
                                )
                            }
                        }
                    }

                    // Hero Workout Card: "Start Today's Workout: Full Gym Push & Pull"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurfaceElevated),
                        border = BorderStroke(1.5.dp, CyberNeonGreen.copy(alpha = 0.75f)),
                        shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_hero_start_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = CyberNeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "DUDHNOI FLOOR ROUTINE • JERAI & AEROFIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberNeonGreen
                                )
                            }
                            Text(
                                text = "Start Today's Workout: Full Gym Push & Pull",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "5 stations mapped to Battle Box : FFF Dudhnoi — Jerai Smith Rack, Dual Cable Tower, Rubber Dumbbells (2.5–40kg), 45° Leg Press & Selectorized Leg Machine.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFC2CFD9)
                            )
                            CyberpunkButton(
                                text = "LAUNCH ACTIVE WORKOUT",
                                icon = Icons.Default.PlayArrow,
                                accentColor = CyberNeonGreen,
                                onClick = onStartWorkoutClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_todays_workout_button")
                            )
                        }
                    }

                    // 3 SVG/Canvas Circular Progress Gauges
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "DAILY READINESS & TELEMETRY",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CyberCyan
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReadinessCircularGauge(
                                    label = "Daily Calories",
                                    current = workoutProfile.caloriesToday,
                                    target = workoutProfile.calorieTarget,
                                    unit = "kcal",
                                    color = CyberMagenta,
                                    modifier = Modifier.weight(1f)
                                )
                                ReadinessCircularGauge(
                                    label = "Active Minutes",
                                    current = workoutProfile.activeMinutesToday,
                                    target = workoutProfile.activeMinutesTarget,
                                    unit = "min",
                                    color = CyberNeonGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                ReadinessCircularGauge(
                                    label = "Protein Intake",
                                    current = workoutProfile.proteinTodayG,
                                    target = workoutProfile.proteinTargetG,
                                    unit = "g",
                                    color = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Quick Macro Log Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_macro_log_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "QUICK MACRO LOG",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = CyberAmber
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = { onQuickLogMacros(300, 28, 0) },
                                    label = { Text("+Whey 28g", color = CyberCyan) },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = CyberHudSurfaceElevated),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("macro_chip_whey")
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { onQuickLogMacros(580, 42, 0) },
                                    label = { Text("+Meal 580", color = CyberNeonGreen) },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = CyberHudSurfaceElevated),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("macro_chip_meal")
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = { onQuickLogMacros(140, 0, 15) },
                                    label = { Text("+15m Turf", color = CyberMagenta) },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = CyberHudSurfaceElevated),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("macro_chip_cardio")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customKcalInput,
                                    onValueChange = { customKcalInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("kcal") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = darkHudTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customProteinInput,
                                    onValueChange = { customProteinInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Protein (g)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = darkHudTextFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                CyberpunkButton(
                                    text = "ADD",
                                    icon = Icons.Default.Add,
                                    accentColor = CyberCyan,
                                    enabled = !isBusy && ((customKcalInput.toIntOrNull() ?: 0) > 0 || (customProteinInput.toIntOrNull() ?: 0) > 0),
                                    onClick = {
                                        val k = customKcalInput.toIntOrNull() ?: 0
                                        val p = customProteinInput.toIntOrNull() ?: 0
                                        onQuickLogMacros(k, p, 0)
                                        customKcalInput = ""
                                        customProteinInput = ""
                                    },
                                    modifier = Modifier.testTag("quick_macro_add_button")
                                )
                            }
                        }
                    }

                    // BMI Ratio Calculator Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bmi_calculator_card")
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
                                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = CyberCyan)
                                    Text(
                                        text = "BMI RATIO & BODY METRICS",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberCyan
                                    )
                                }
                                Surface(
                                    color = CyberNeonGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, CyberNeonGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "BMI $bmiFormatted • $bmiLabel",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CyberNeonGreen,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
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
                                        .testTag("bmi_weight_input")
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
                                        .testTag("bmi_height_input")
                                )
                                CyberpunkButton(
                                    text = "CALC",
                                    accentColor = CyberNeonGreen,
                                    enabled = !isBusy,
                                    onClick = {
                                        val w = weightInput.toDoubleOrNull() ?: workoutProfile.weightKg
                                        val h = heightInput.toDoubleOrNull() ?: workoutProfile.heightCm
                                        onUpdateBmiMetrics(w, h)
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .testTag("bmi_calculate_button")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ReadinessCircularGauge(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    Surface(
        color = CyberHudSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(74.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    drawArc(
                        color = color.copy(alpha = 0.16f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$current",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "/$target$unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9EB0C2)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun darkHudTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberCyan,
    unfocusedBorderColor = CyberBorder,
    focusedLabelColor = CyberCyan,
    unfocusedLabelColor = Color(0xFF99AABB),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = CyberCyan
)
