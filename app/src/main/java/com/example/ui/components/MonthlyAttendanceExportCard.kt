package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.utils.AttendanceCsvExporter
import com.example.utils.MonthlyAttendanceSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyAttendanceExportCard(
    exportState: UiState<MonthlyAttendanceSummary>?,
    isBusy: Boolean,
    onFetchMonthlyLogs: (year: Int, monthOneBased: Int) -> Unit,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    val initialYearMonth = remember { AttendanceCsvExporter.currentYearMonthIst() }
    var selectedYear by remember { mutableIntStateOf(initialYearMonth.first) }
    var selectedMonth by remember { mutableIntStateOf(initialYearMonth.second) }
    val recentMonths = remember { AttendanceCsvExporter.recentMonthOptions(count = 6) }

    var pendingCsvToSave by remember { mutableStateOf<String?>(null) }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingCsvToSave
        pendingCsvToSave = null
        if (uri != null && content != null) {
            AttendanceCsvExporter.writeCsvToUri(context, uri, content)
                .onSuccess { onFeedback("Saved monthly attendance CSV to device storage.") }
                .onFailure { err -> onFeedback(err.localizedMessage ?: "Failed to save CSV file.") }
        }
    }

    val monthLabel = remember(selectedYear, selectedMonth) {
        AttendanceCsvExporter.formatMonthLabel(selectedYear, selectedMonth)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_attendance_export_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Attendance CSV Export",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Query Firestore logs (Asia/Kolkata IST) and save or share a CSV spreadsheet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Month Stepper
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val (prevY, prevM) = AttendanceCsvExporter.shiftYearMonth(selectedYear, selectedMonth, -1)
                            selectedYear = prevY
                            selectedMonth = prevM
                        },
                        modifier = Modifier.testTag("export_prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("export_selected_month_label")
                    )
                    IconButton(
                        onClick = {
                            val (nextY, nextM) = AttendanceCsvExporter.shiftYearMonth(selectedYear, selectedMonth, +1)
                            selectedYear = nextY
                            selectedMonth = nextM
                        },
                        modifier = Modifier.testTag("export_next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }
            }

            // Recent Quick Month Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                recentMonths.forEach { (y, m) ->
                    val mm = m.toString().padStart(2, '0')
                    FilterChip(
                        selected = y == selectedYear && m == selectedMonth,
                        onClick = {
                            selectedYear = y
                            selectedMonth = m
                        },
                        label = { Text(AttendanceCsvExporter.formatShortMonthLabel(y, m)) },
                        modifier = Modifier.testTag("export_month_chip_${y}_$mm")
                    )
                }
            }

            Button(
                onClick = { onFetchMonthlyLogs(selectedYear, selectedMonth) },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("admin_fetch_monthly_csv_button")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = "Export $monthLabel Logs (CSV)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            when (exportState) {
                is UiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Querying Firestore attendance collection...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is UiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    val summary = exportState.data
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportStatPill("Sessions", "${summary.totalRecords}", Modifier.weight(1f))
                        ExportStatPill("Members", "${summary.uniqueUsers}", Modifier.weight(1f))
                        ExportStatPill("Trainer", "${summary.trainerScanCount}", Modifier.weight(1f))
                        ExportStatPill("Kiosk", "${summary.kioskQrCount}", Modifier.weight(1f))
                    }

                    val previewLines = remember(summary.csvContent) {
                        summary.csvContent.lineSequence().take(5).joinToString("\n")
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_preview")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "FILE: ${summary.fileName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = previewLines,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 6
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                pendingCsvToSave = summary.csvContent
                                saveCsvLauncher.launch(summary.fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_save_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Save CSV", modifier = Modifier.padding(start = 6.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                AttendanceCsvExporter.shareCsvViaIntent(
                                    context = context,
                                    fileName = summary.fileName,
                                    csvContent = summary.csvContent
                                ).onFailure { err ->
                                    onFeedback(err.localizedMessage ?: "Unable to share CSV file.")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("admin_share_csv_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Share", modifier = Modifier.padding(start = 6.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(summary.csvContent))
                                onFeedback("Copied ${summary.totalRecords} CSV row(s) for ${summary.monthLabel}.")
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("admin_copy_csv_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy CSV content", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun ExportStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
