package com.example.ui.workout

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ActiveStationExercise
import com.example.data.model.DudhnoiFloorInventory
import com.example.data.model.ExerciseCatalogEntry
import com.example.data.model.WorkoutSetEntry
import com.example.ui.components.CyberpunkButton
import com.example.ui.components.CyberpunkOutlinedButton
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberHudBg
import com.example.ui.theme.CyberHudSurface
import com.example.ui.theme.CyberHudSurfaceElevated
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberNeonGreen
import kotlinx.coroutines.delay

@Composable
fun WorkoutActiveTab(
    stations: List<ActiveStationExercise>,
    isSaving: Boolean,
    onUpdateSet: (stationIndex: Int, setIndex: Int, weightKg: Double, reps: Int, completed: Boolean) -> Unit,
    onSwapStation: (stationIndex: Int, replacement: ExerciseCatalogEntry) -> Unit,
    onRestTimerFinished: () -> Unit,
    onCompleteWorkout: (elapsedSeconds: Long, totalVolumeKg: Double, completedSets: Int, exercisesCompleted: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var elapsedRunning by remember { mutableStateOf(true) }

    var restSeconds by remember { mutableIntStateOf(90) }
    var restRunning by remember { mutableStateOf(false) }

    var swapStationIdx by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(elapsedRunning) {
        while (elapsedRunning) {
            delay(1000L)
            elapsedSeconds += 1L
        }
    }

    LaunchedEffect(restRunning) {
        while (restRunning && restSeconds > 0) {
            delay(1000L)
            restSeconds -= 1
            if (restSeconds == 0) {
                restRunning = false
                onRestTimerFinished()
            }
        }
    }

    val totalVolumeKg = remember(stations) {
        stations.sumOf { st ->
            st.sets.filter { it.completed }.sumOf { set -> set.weightKg * set.reps }
        }
    }
    val completedSetsCount = remember(stations) {
        stations.sumOf { st -> st.sets.count { it.completed } }
    }
    val exercisesDoneCount = remember(stations) {
        stations.count { st -> st.sets.any { it.completed } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("workout_active_tab")
    ) {
        // Sticky Workout Elapsed Timer + Live Telemetry Bar
        Surface(
            color = CyberHudSurfaceElevated,
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ACTIVE SESSION • FULL GYM PUSH & PULL",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CyberCyan
                    )
                    Text(
                        text = formatElapsedHms(elapsedSeconds),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        modifier = Modifier.testTag("sticky_workout_timer_text")
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${totalVolumeKg.toInt()} kg VOL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CyberNeonGreen
                        )
                        Text(
                            text = "$completedSetsCount sets checked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA8B8C8)
                        )
                    }
                    Surface(
                        color = CyberHudBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberCyan)
                    ) {
                        IconButton(
                            onClick = { elapsedRunning = !elapsedRunning },
                            modifier = Modifier.testTag("workout_elapsed_pause_button")
                        ) {
                            Icon(
                                imageVector = if (elapsedRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (elapsedRunning) "Pause workout timer" else "Resume workout timer",
                                tint = CyberCyan
                            )
                        }
                    }
                }
            }
        }

        // Main Scrollable Station Logger
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 126.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(stations, key = { idx, item -> "${idx}_${item.catalogId}" }) { stationIdx, station ->
                    StationLoggerCard(
                        index = stationIdx + 1,
                        station = station,
                        onUpdateSet = { setIdx, weight, reps, done ->
                            onUpdateSet(stationIdx, setIdx, weight, reps, done)
                            if (done) {
                                restSeconds = 90
                                restRunning = true
                            }
                        },
                        onRequestSwap = { swapStationIdx = stationIdx }
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CyberpunkButton(
                            text = if (isSaving) "SAVING WORKOUT LOG..." else "FINISH & SAVE WORKOUT ($completedSetsCount SETS)",
                            icon = Icons.Default.CheckCircle,
                            accentColor = CyberNeonGreen,
                            enabled = !isSaving,
                            onClick = {
                                elapsedRunning = false
                                restRunning = false
                                onCompleteWorkout(
                                    elapsedSeconds.coerceAtLeast(60L),
                                    totalVolumeKg,
                                    completedSetsCount,
                                    exercisesDoneCount
                                )
                            },
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .testTag("finish_workout_button")
                        )
                    }
                }
            }

            // Interactive Floating Rest Timer Bar anchored at bottom
            Surface(
                color = Color(0xF50B1118),
                border = BorderStroke(
                    1.5.dp,
                    if (restRunning) CyberMagenta else CyberCyan
                ),
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("floating_rest_timer_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Rest Timer",
                            tint = if (restRunning) CyberMagenta else CyberCyan
                        )
                        Column {
                            Text(
                                text = "REST TIMER",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF9FB0C2)
                            )
                            Text(
                                text = formatRestMmSs(restSeconds),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (restRunning) CyberMagenta else Color.White,
                                modifier = Modifier.testTag("rest_timer_countdown_text")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { restSeconds = (restSeconds - 15).coerceAtLeast(0) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("rest_minus_15_button")
                        ) {
                            Text("-15s", color = CyberCyan)
                        }
                        OutlinedButton(
                            onClick = { restSeconds += 15 },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("rest_plus_15_button")
                        ) {
                            Text("+15s", color = CyberCyan)
                        }
                        Surface(
                            color = (if (restRunning) CyberMagenta else CyberNeonGreen).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (restRunning) CyberMagenta else CyberNeonGreen)
                        ) {
                            IconButton(
                                onClick = {
                                    if (restSeconds == 0) restSeconds = 90
                                    restRunning = !restRunning
                                },
                                modifier = Modifier.testTag("rest_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (restRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (restRunning) "Pause rest" else "Start rest",
                                    tint = if (restRunning) CyberMagenta else CyberNeonGreen
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                restRunning = false
                                restSeconds = 90
                            },
                            modifier = Modifier.testTag("rest_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset rest timer",
                                tint = CyberAmber
                            )
                        }
                    }
                }
            }
        }
    }

    // Swap Exercise Dialog (restricted strictly to Battle Box Dudhnoi floor alternatives)
    val activeSwapIdx = swapStationIdx
    if (activeSwapIdx != null && activeSwapIdx in stations.indices) {
        val currentStation = stations[activeSwapIdx]
        val alternatives = remember(currentStation.catalogId) {
            DudhnoiFloorInventory.swapAlternativesFor(currentStation.catalogId)
        }
        Dialog(
            onDismissRequest = { swapStationIdx = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = CyberHudSurface,
                border = BorderStroke(1.4.dp, CyberCyan),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("swap_exercise_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SWAP FLOOR STATION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CyberCyan
                    )
                    Text(
                        text = "Replacing: ${currentStation.name} — choose another station available on the Battle Box Dudhnoi floor:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB5C4D2)
                    )
                    if (alternatives.isEmpty()) {
                        Text(
                            text = "No floor swap alternatives configured for this station.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9EB0C2)
                        )
                    } else {
                        alternatives.forEach { alt ->
                            Card(
                                onClick = {
                                    onSwapStation(activeSwapIdx, alt)
                                    swapStationIdx = null
                                },
                                colors = CardDefaults.cardColors(containerColor = CyberHudSurfaceElevated),
                                border = BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("swap_option_${alt.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = alt.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = alt.equipmentTag,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "Target: ${alt.defaultWeightKg.toInt()}kg × ${alt.defaultReps} • ${alt.zone.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberNeonGreen
                                    )
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { swapStationIdx = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Keep Current Station", color = CyberAmber)
                    }
                }
            }
        }
    }
}

@Composable
private fun StationLoggerCard(
    index: Int,
    station: ActiveStationExercise,
    onUpdateSet: (setIdx: Int, weightKg: Double, reps: Int, completed: Boolean) -> Unit,
    onRequestSwap: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
        border = BorderStroke(1.dp, CyberBorder),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("station_card_${station.catalogId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "STATION 0$index • ${station.zone.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = CyberCyan
                    )
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = station.equipmentTag,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAB8C7)
                    )
                    Text(
                        text = station.previousLiftSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7B8C9E)
                    )
                }
                CyberpunkOutlinedButton(
                    text = "Swap",
                    icon = Icons.Default.SwapHoriz,
                    accentColor = CyberAmber,
                    onClick = onRequestSwap,
                    modifier = Modifier.testTag("swap_button_${station.catalogId}")
                )
            }

            HorizontalDivider(color = CyberBorder.copy(alpha = 0.6f))

            station.sets.forEachIndexed { setIdx, setItem ->
                SetRowLogger(
                    stationId = station.catalogId,
                    setItem = setItem,
                    onWeightChange = { newW -> onUpdateSet(setIdx, newW, setItem.reps, setItem.completed) },
                    onRepsChange = { newR -> onUpdateSet(setIdx, setItem.weightKg, newR, setItem.completed) },
                    onToggleComplete = {
                        onUpdateSet(setIdx, setItem.weightKg, setItem.reps, !setItem.completed)
                    }
                )
            }
        }
    }
}

@Composable
private fun SetRowLogger(
    stationId: String,
    setItem: WorkoutSetEntry,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onToggleComplete: () -> Unit
) {
    Surface(
        color = if (setItem.completed) Color(0xFF102A18) else CyberHudSurfaceElevated,
        border = BorderStroke(
            1.dp,
            if (setItem.completed) CyberNeonGreen else CyberBorder
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET ${setItem.setNumber}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (setItem.completed) CyberNeonGreen else CyberCyan
                )
                Text(
                    text = "Previous Lift: ${setItem.previousLiftText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7B8C9E)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Oversized Weight Stepper (- / + 2.5kg)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = CyberHudBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        IconButton(
                            onClick = { onWeightChange((setItem.weightKg - 2.5).coerceAtLeast(0.0)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease weight", tint = CyberCyan)
                        }
                    }
                    Text(
                        text = "${setItem.weightKg}kg",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Surface(
                        color = CyberHudBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        IconButton(
                            onClick = { onWeightChange((setItem.weightKg + 2.5).coerceAtMost(400.0)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase weight", tint = CyberCyan)
                        }
                    }
                }

                // Oversized Reps Stepper (- / + 1 rep)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = CyberHudBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        IconButton(
                            onClick = { onRepsChange((setItem.reps - 1).coerceAtLeast(1)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease reps", tint = CyberAmber)
                        }
                    }
                    Text(
                        text = "${setItem.reps}r",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Surface(
                        color = CyberHudBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        IconButton(
                            onClick = { onRepsChange((setItem.reps + 1).coerceAtMost(100)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase reps", tint = CyberAmber)
                        }
                    }
                }

                // Green Completion Checkmark Button
                Surface(
                    color = if (setItem.completed) CyberNeonGreen else Color(0xFF141D26),
                    border = BorderStroke(1.4.dp, if (setItem.completed) CyberNeonGreen else Color(0xFF3A4C5E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("set_complete_${stationId}_${setItem.setNumber}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (setItem.completed) "Set completed" else "Mark set complete",
                            tint = if (setItem.completed) CyberHudBg else CyberNeonGreen
                        )
                    }
                }
            }
        }
    }
}

private fun formatElapsedHms(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600L
    val mins = (totalSeconds % 3600L) / 60L
    val secs = totalSeconds % 60L
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
}

private fun formatRestMmSs(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}
