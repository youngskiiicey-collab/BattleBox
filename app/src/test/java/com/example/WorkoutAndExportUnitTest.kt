package com.example

import com.example.data.model.AttendanceRecord
import com.example.data.model.DudhnoiFloorInventory
import com.example.data.model.GymConfig
import com.example.data.model.WorkoutProfile
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.utils.AttendanceCsvExporter
import com.example.utils.MonthlyAttendancePdfGenerator
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class WorkoutAndExportUnitTest {

    @Test
    fun workoutProfile_computesBmiRatioAndCategoryCorrectly() {
        val bmiOptimal = WorkoutProfile.calculateBmi(weightKg = 72.0, heightCm = 172.0)
        assertEquals(24.3, bmiOptimal, 0.1)
        assertEquals("Optimal Fit", WorkoutProfile.bmiCategory(bmiOptimal))

        val bmiUnder = WorkoutProfile.calculateBmi(weightKg = 50.0, heightCm = 175.0)
        assertEquals("Underweight", WorkoutProfile.bmiCategory(bmiUnder))

        val bmiOver = WorkoutProfile.calculateBmi(weightKg = 82.0, heightCm = 170.0)
        assertEquals("Overweight", WorkoutProfile.bmiCategory(bmiOver))

        val bmiObese = WorkoutProfile.calculateBmi(weightKg = 105.0, heightCm = 170.0)
        assertEquals("Obese", WorkoutProfile.bmiCategory(bmiObese))
    }

    @Test
    fun dudhnoiFloorInventory_buildsFiveStationsAndFloorOnlySwaps() {
        val session = DudhnoiFloorInventory.buildDefaultPushPullSession()
        assertEquals(5, session.size)
        assertEquals("smith_flat_bench", session[0].catalogId)
        assertEquals("cable_crossover_fly", session[1].catalogId)
        assertEquals("db_incline_press", session[2].catalogId)
        assertEquals("jerai_45_leg_press", session[3].catalogId)
        assertEquals("jerai_leg_extension", session[4].catalogId)

        val swaps = DudhnoiFloorInventory.swapAlternativesFor("smith_flat_bench")
        assertTrue(swaps.isNotEmpty())
        assertTrue(swaps.all { DudhnoiFloorInventory.byId(it.id) != null })
        assertTrue(swaps.any { it.equipmentTag.contains("Jerai", ignoreCase = true) })
    }

    @Test
    fun geofenceUtils_enforcesRadiusAndMaxGpsForAutoOut() {
        val config = GymConfig(
            gymName = "Battle Box : FFF",
            latitude = 25.9859145,
            longitude = 90.7858894,
            geofenceRadiusM = 100.0,
            maxGpsAccuracyM = 50.0
        )

        val insideFix = DeviceFix(latitude = 25.9859145, longitude = 90.7858894, accuracyMeters = 12.0)
        val insideEval = GeofenceUtils.evaluateGeofenceExit(insideFix, config)
        assertTrue(insideEval.isInsideGeofence)

        val outsideRadiusFix = DeviceFix(latitude = 25.9900000, longitude = 90.7858894, accuracyMeters = 12.0)
        val outsideEval = GeofenceUtils.evaluateGeofenceExit(outsideRadiusFix, config)
        assertFalse(outsideEval.isInsideGeofence)
        assertNotNull(outsideEval.violationReason)
        assertTrue(outsideEval.violationReason!!.contains("geofence", ignoreCase = true))

        val poorGpsFix = DeviceFix(latitude = 25.9859145, longitude = 90.7858894, accuracyMeters = 120.0)
        val poorGpsEval = GeofenceUtils.evaluateGeofenceExit(poorGpsFix, config)
        assertFalse(poorGpsEval.isInsideGeofence)
        assertNotNull(poorGpsEval.violationReason)
        assertTrue(poorGpsEval.violationReason!!.contains("GPS", ignoreCase = true))
    }

    @Test
    fun monthlyAttendancePdfAndCsv_generatesValidReportAndHeader() {
        val sampleRecords = listOf(
            AttendanceRecord(
                id = "att_test_1",
                userId = "user_1",
                memberName = "Rohan Das",
                memberRole = "member",
                mode = AttendanceRecord.MODE_KIOSK_QR,
                status = AttendanceRecord.STATUS_CHECKED_OUT,
                distanceMeters = 18.0,
                gpsAccuracyMeters = 9.0,
                checkInAt = Timestamp(Date(1794640000000L)),
                checkOutAt = Timestamp(Date(1794643600000L))
            ),
            AttendanceRecord(
                id = "att_test_2",
                userId = "user_2",
                memberName = "Priya Sangma",
                memberRole = "member",
                mode = AttendanceRecord.MODE_TRAINER_SCAN,
                status = AttendanceRecord.STATUS_AUTO_CHECKED_OUT,
                distanceMeters = 140.0,
                gpsAccuracyMeters = 15.0,
                checkInAt = Timestamp(Date(1794645000000L)),
                checkOutAt = Timestamp(Date(1794648000000L))
            )
        )
        val summary = AttendanceCsvExporter.buildSummaryAndCsv(2026, 11, sampleRecords)
        assertEquals(2, summary.totalRecords)
        assertEquals(1, summary.autoCheckedOutCount)
        assertTrue(summary.csvContent.contains("Rohan Das"))
        assertTrue(summary.csvContent.contains("Priya Sangma"))

        val pages = MonthlyAttendancePdfGenerator.paginateRecords(sampleRecords)
        assertEquals(1, pages.size)
        assertEquals(2, pages[0].size)

        val multiPageRecords = List(55) { idx ->
            sampleRecords[0].copy(id = "att_$idx", memberName = "Member $idx")
        }
        val multiPages = MonthlyAttendancePdfGenerator.paginateRecords(multiPageRecords)
        assertEquals(3, multiPages.size)
        assertEquals(23, multiPages[0].size)
        assertEquals(31, multiPages[1].size)
        assertEquals(1, multiPages[2].size)

        val line1 = MonthlyAttendancePdfGenerator.formatSummaryLine1(summary)
        val line2 = MonthlyAttendancePdfGenerator.formatSummaryLine2(summary)
        assertTrue(line1.contains("Sessions: 2"))
        assertTrue(line1.contains("Trainer Scans: 1"))
        assertTrue(line2.contains("Geofence Auto-Out: 1"))

        val row1 = MonthlyAttendancePdfGenerator.formatRowCells(sampleRecords[0])
        assertEquals("Rohan Das", row1.name)
        assertEquals("60m", row1.durStr)
        assertEquals("18m / ±9m", row1.geo)

        assertEquals(
            "battlebox_fff_attendance_2026_11.pdf",
            MonthlyAttendancePdfGenerator.suggestedPdfFileName(2026, 11)
        )
    }
}
