package com.example.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.AttendanceRecord
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.MonthlyAttendanceExportCard
import com.example.ui.theme.BattleBoxTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class AttendanceCsvExporterTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun toCsv_formatsHeaderRowsDurationEscapingAndFormulaProtection() {
        val checkIn = Timestamp(Date(1_794_570_000_000L))
        val checkOut = Timestamp(Date(1_794_573_600_000L)) // +60 minutes
        val records = listOf(
            AttendanceRecord(
                id = "att_001",
                userId = "uid_1",
                memberName = "Dhrubajit, \"Coach\"",
                memberRole = UserProfile.ROLE_MEMBER,
                mode = AttendanceRecord.MODE_TRAINER_SCAN,
                status = AttendanceRecord.STATUS_CHECKED_OUT,
                distanceMeters = 12.345,
                gpsAccuracyMeters = 6.5,
                latitude = 25.983300,
                longitude = 90.783300,
                verifiedByUid = "trainer_9",
                qrEpochSlot = 42L,
                checkInAt = checkIn,
                checkOutAt = checkOut
            ),
            AttendanceRecord(
                id = "att_002",
                userId = "uid_2",
                memberName = "=cmd|'/C calc'!A0",
                memberRole = UserProfile.ROLE_TRAINER,
                mode = AttendanceRecord.MODE_KIOSK_QR,
                status = AttendanceRecord.STATUS_CHECKED_IN,
                distanceMeters = 5.0,
                gpsAccuracyMeters = 8.0,
                latitude = -25.983300,
                longitude = 90.783300,
                verifiedByUid = "uid_2",
                qrEpochSlot = 43L,
                checkInAt = checkIn,
                checkOutAt = null
            )
        )

        val summary = AttendanceCsvExporter.buildSummaryAndCsv(2026, 11, records)
        assertEquals("battlebox_attendance_2026_11.csv", summary.fileName)
        assertEquals(2, summary.totalRecords)
        assertEquals(2, summary.uniqueUsers)
        assertEquals(1, summary.trainerScanCount)
        assertEquals(1, summary.kioskQrCount)

        val lines = summary.csvContent.trim().lines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("attendance_id,member_name,user_id,member_role"))
        assertTrue(lines[1].contains("\"Dhrubajit, \"\"Coach\"\"\""))
        assertTrue(lines[1].contains(",60,12.35,"))
        assertTrue(lines[2].contains("'=cmd|'/C calc'!A0"))
        assertTrue(lines[2].contains(",active,"))
        assertTrue(lines[2].contains(",-25.983300,"))
    }

    @Test
    fun monthlyAttendanceExportCard_selectsMonthFetchesLogsAndShowsSaveShareCopy() {
        var fetchedYear = 0
        var fetchedMonth = 0
        var feedbackMsg = ""

        val sampleRecords = listOf(
            AttendanceRecord(
                id = "att_99",
                userId = "u_99",
                memberName = "Ananya Das",
                memberRole = UserProfile.ROLE_MEMBER,
                mode = AttendanceRecord.MODE_TRAINER_SCAN,
                status = AttendanceRecord.STATUS_CHECKED_OUT,
                checkInAt = Timestamp(Date(1_794_570_000_000L)),
                checkOutAt = Timestamp(Date(1_794_571_800_000L))
            )
        )
        val readySummary = AttendanceCsvExporter.buildSummaryAndCsv(2026, 10, sampleRecords)

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                MonthlyAttendanceExportCard(
                    exportState = UiState.Success(readySummary),
                    isBusy = false,
                    onFetchMonthlyLogs = { y, m ->
                        fetchedYear = y
                        fetchedMonth = m
                    },
                    onFeedback = { feedbackMsg = it }
                )
            }
        }

        composeRule.onNodeWithTag("monthly_attendance_export_card").assertIsDisplayed()
        composeRule.onNodeWithTag("export_prev_month_button").performClick()
        composeRule.onNodeWithTag("admin_fetch_monthly_pdf_button").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_fetch_monthly_csv_button").performClick()
        assertTrue(fetchedYear > 0 && fetchedMonth in 1..12)

        composeRule.onNodeWithTag("export_csv_preview").assertIsDisplayed()
        composeRule.onNodeWithText(
            "READY (CSV): battlebox_fff_attendance_2026_10.pdf / battlebox_attendance_2026_10.csv"
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("admin_save_pdf_button").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_share_pdf_button").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_save_csv_button").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_share_csv_button").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_copy_csv_button").performClick()
        assertTrue(feedbackMsg.contains("Copied 1 CSV row(s)"))
    }
}
