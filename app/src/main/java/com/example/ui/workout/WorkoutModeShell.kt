package com.example.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveStationExercise
import com.example.data.model.AttendanceRecord
import com.example.data.model.DudhnoiFloorInventory
import com.example.data.model.ExerciseCatalogEntry
import com.example.data.model.PersonalRecordEntry
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutProfile
import com.example.data.model.WorkoutSetEntry
import com.example.ui.components.WorkoutModeToggleBar
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberHudBg
import com.example.ui.theme.CyberHudSurface
import com.example.ui.theme.CyberNeonGreen

enum class WorkoutTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    WORKOUT("Workout", Icons.Default.FitnessCenter),
    EXERCISES("Exercises", Icons.Default.MenuBook),
    PROGRESS("Progress", Icons.Default.ShowChart)
}

@Composable
fun WorkoutModeShell(
    user: UserProfile,
    workoutProfile: WorkoutProfile,
    workoutLogs: List<WorkoutLog>,
    activeSession: AttendanceRecord?,
    isBusy: Boolean,
    onExitWorkoutMode: () -> Unit,
    onQuickLogMacros: (kcal: Int, proteinG: Int, activeMin: Int) -> Unit,
    onUpdateBmiMetrics: (weightKg: Double, heightCm: Double) -> Unit,
    onRestTimerFinished: () -> Unit,
    onCompleteWorkout: (
        workoutTitle: String,
        durationSeconds: Long,
        totalVolumeKg: Double,
        completedSets: Int,
        exercisesCompleted: Int,
        updatedPrs: List<PersonalRecordEntry>,
        prLabel: String
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by rememberSaveable { mutableStateOf(WorkoutTab.HOME) }
    var activeStations by remember {
        mutableStateOf(DudhnoiFloorInventory.buildDefaultPushPullSession())
    }

    BackHandler {
        if (activeTab != WorkoutTab.HOME) {
            activeTab = WorkoutTab.HOME
        } else {
            onExitWorkoutMode()
        }
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = CyberHudBg,
        selectedTextColor = CyberNeonGreen,
        indicatorColor = CyberNeonGreen,
        unselectedIconColor = Color(0xFF9CB0C3),
        unselectedTextColor = Color(0xFF9CB0C3)
    )

    val railItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = CyberHudBg,
        selectedTextColor = CyberNeonGreen,
        indicatorColor = CyberNeonGreen,
        unselectedIconColor = Color(0xFF9CB0C3),
        unselectedTextColor = Color(0xFF9CB0C3)
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = CyberHudBg,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = CyberHudSurface,
                        modifier = Modifier.testTag("workout_bottom_nav_bar")
                    ) {
                        WorkoutTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("workout_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = CyberHudSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("workout_nav_rail")
                    ) {
                        WorkoutTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = railItemColors,
                                modifier = Modifier.testTag("workout_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Shared Workout Mode Toggle directly above the 4 tabs so Member/Trainer/Admin can toggle off cleanly
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        WorkoutModeToggleBar(
                            isWorkoutMode = true,
                            streakDays = workoutProfile.streakDays,
                            bmiText = WorkoutProfile.formatBmi(workoutProfile.bmiRatio),
                            onToggleWorkoutMode = { enabled ->
                                if (!enabled) onExitWorkoutMode()
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (activeTab) {
                            WorkoutTab.HOME -> WorkoutHomeTab(
                                user = user,
                                workoutProfile = workoutProfile,
                                recentLogs = workoutLogs,
                                activeSession = activeSession,
                                isBusy = isBusy,
                                onQuickLogMacros = onQuickLogMacros,
                                onUpdateBmiMetrics = onUpdateBmiMetrics,
                                onStartWorkoutClick = { activeTab = WorkoutTab.WORKOUT }
                            )

                            WorkoutTab.WORKOUT -> WorkoutActiveTab(
                                stations = activeStations,
                                isSaving = isBusy,
                                onUpdateSet = { stIdx, setIdx, weight, reps, done ->
                                    activeStations = activeStations.mapIndexed { idx, station ->
                                        if (idx != stIdx) station
                                        else station.copy(
                                            sets = station.sets.mapIndexed { sIdx, set ->
                                                if (sIdx != setIdx) set
                                                else set.copy(weightKg = weight, reps = reps, completed = done)
                                            }
                                        )
                                    }
                                },
                                onSwapStation = { stIdx, replacement ->
                                    activeStations = activeStations.mapIndexed { idx, station ->
                                        if (idx != stIdx) station
                                        else ActiveStationExercise(
                                            catalogId = replacement.id,
                                            name = replacement.name,
                                            equipmentTag = replacement.equipmentTag,
                                            zone = replacement.zone,
                                            previousLiftSummary = "Swapped • Target ${replacement.defaultWeightKg.toInt()}kg × ${replacement.defaultReps}",
                                            sets = (1..3).map { setNum ->
                                                WorkoutSetEntry(
                                                    setNumber = setNum,
                                                    weightKg = replacement.defaultWeightKg,
                                                    reps = replacement.defaultReps,
                                                    completed = false,
                                                    previousLiftText = "${replacement.defaultWeightKg.toInt()}kg × ${replacement.defaultReps}"
                                                )
                                            }
                                        )
                                    }
                                    onShowMessage("Swapped station to ${replacement.name}.")
                                },
                                onRestTimerFinished = onRestTimerFinished,
                                onCompleteWorkout = { elapsedSec, volKg, setsDone, exercisesDone ->
                                    val updatedPrs = computeUpdatedPrs(workoutProfile.personalRecords, activeStations)
                                    val bestStation = activeStations
                                        .flatMap { st -> st.sets.filter { it.completed }.map { st to it } }
                                        .maxByOrNull { (_, s) -> s.weightKg * s.reps }
                                    val prLabel = bestStation?.let { (st, s) ->
                                        "${st.name}: ${s.weightKg.toInt()}kg × ${s.reps}"
                                    } ?: "Full Gym Push & Pull"

                                    onCompleteWorkout(
                                        "Full Gym Push & Pull",
                                        elapsedSec,
                                        volKg,
                                        setsDone,
                                        exercisesDone,
                                        updatedPrs,
                                        prLabel
                                    )
                                    activeTab = WorkoutTab.PROGRESS
                                }
                            )

                            WorkoutTab.EXERCISES -> WorkoutExercisesTab(
                                onAddExerciseToActiveWorkout = { entry ->
                                    if (activeStations.any { it.catalogId == entry.id }) {
                                        onShowMessage("${entry.name} is already loaded in your active workout.")
                                    } else {
                                        activeStations = activeStations + entryToActiveStation(entry)
                                        onShowMessage("Added ${entry.name} to Active Workout.")
                                    }
                                    activeTab = WorkoutTab.WORKOUT
                                }
                            )

                            WorkoutTab.PROGRESS -> WorkoutProgressTab(
                                workoutProfile = workoutProfile,
                                workoutLogs = workoutLogs,
                                isBusy = isBusy,
                                onUpdateBodyMetrics = onUpdateBmiMetrics
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun entryToActiveStation(entry: ExerciseCatalogEntry): ActiveStationExercise =
    ActiveStationExercise(
        catalogId = entry.id,
        name = entry.name,
        equipmentTag = entry.equipmentTag,
        zone = entry.zone,
        previousLiftSummary = "Target: ${entry.defaultWeightKg.toInt()}kg × ${entry.defaultReps}",
        sets = (1..3).map { num ->
            WorkoutSetEntry(
                setNumber = num,
                weightKg = entry.defaultWeightKg,
                reps = entry.defaultReps,
                completed = false,
                previousLiftText = "${entry.defaultWeightKg.toInt()}kg × ${entry.defaultReps}"
            )
        }
    )

private fun computeUpdatedPrs(
    existingPrs: List<PersonalRecordEntry>,
    stations: List<ActiveStationExercise>
): List<PersonalRecordEntry> {
    val map = existingPrs.associateBy { it.exerciseId }.toMutableMap()
    stations.forEach { station ->
        val topSet = station.sets.filter { it.completed && it.weightKg > 0.0 }
            .maxByOrNull { it.weightKg * 100.0 + it.reps }
        if (topSet != null) {
            val prev = map[station.catalogId]
            if (prev == null || topSet.weightKg > prev.bestWeightKg ||
                (topSet.weightKg == prev.bestWeightKg && topSet.reps > prev.bestReps)
            ) {
                map[station.catalogId] = PersonalRecordEntry(
                    exerciseId = station.catalogId,
                    exerciseName = station.name,
                    equipmentTag = station.equipmentTag,
                    bestWeightKg = topSet.weightKg,
                    bestReps = topSet.reps,
                    achievedDateLabel = "New PR"
                )
            }
        }
    }
    return map.values.toList().take(12)
}
