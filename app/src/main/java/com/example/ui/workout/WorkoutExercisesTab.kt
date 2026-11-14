package com.example.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.DudhnoiFloorInventory
import com.example.data.model.ExerciseCatalogEntry
import com.example.data.model.ExerciseZone
import com.example.ui.components.CyberpunkOutlinedButton
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberHudBg
import com.example.ui.theme.CyberHudSurface
import com.example.ui.theme.CyberHudSurfaceElevated
import com.example.ui.theme.CyberNeonGreen

@Composable
fun WorkoutExercisesTab(
    onAddExerciseToActiveWorkout: (ExerciseCatalogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedZone by remember { mutableStateOf(ExerciseZone.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCueId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(selectedZone, searchQuery) {
        val q = searchQuery.trim().lowercase()
        DudhnoiFloorInventory.catalog.filter { item ->
            val matchesZone = selectedZone == ExerciseZone.ALL || item.zone == selectedZone
            val matchesQuery = q.isBlank() ||
                item.name.lowercase().contains(q) ||
                item.equipmentTag.lowercase().contains(q) ||
                item.muscleGroup.lowercase().contains(q)
            matchesZone && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("workout_exercises_tab")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DUDHNOI FLOOR INVENTORY (${filtered.size} STATIONS)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = CyberCyan
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Jerai machines, dumbbells, Aerofit...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
                singleLine = true,
                colors = darkHudTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_library_search_input")
            )
        }

        // Scrollable zone pill filters matching exact Dudhnoi floor zones
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ExerciseZone.entries, key = { it.name }) { zone ->
                val isSelected = selectedZone == zone
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedZone = zone },
                    label = {
                        Text(
                            text = zone.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CyberHudBg else CyberCyan
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        containerColor = CyberHudSurfaceElevated
                    ),
                    modifier = Modifier.testTag("exercise_zone_pill_${zone.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { exercise ->
                val isExpanded = expandedCueId == exercise.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberHudSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_library_card_${exercise.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = CyberCyan.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = exercise.zone.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = CyberCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = CyberNeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = exercise.equipmentTag,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyberNeonGreen
                                    )
                                }
                                Text(
                                    text = "Muscles: ${exercise.muscleGroup}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9FB0C2)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { expandedCueId = if (isExpanded) null else exercise.id },
                                modifier = Modifier.testTag("form_cues_toggle_${exercise.id}")
                            ) {
                                Text("Form & Setup Cues", color = CyberCyan)
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle form cues",
                                    tint = CyberCyan,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            CyberpunkOutlinedButton(
                                text = "+ Active",
                                icon = Icons.Default.AddCircleOutline,
                                accentColor = CyberNeonGreen,
                                onClick = { onAddExerciseToActiveWorkout(exercise) },
                                modifier = Modifier.testTag("add_exercise_active_${exercise.id}")
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider(color = CyberBorder)
                                Text(
                                    text = "JOINT-ALIGNMENT & SETUP CUES",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = CyberCyan
                                )
                                exercise.formCues.take(3).forEachIndexed { idx, cue ->
                                    Text(
                                        text = "${idx + 1}. $cue",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD5E1ED)
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
