package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.AttendanceRecord
import com.example.data.model.GymConfig
import com.example.data.model.GymPlan
import com.example.data.model.MemberSubscription
import com.example.data.model.PaymentRequest
import com.example.data.model.UserProfile
import com.example.ui.theme.BattleBoxTheme
import com.example.utils.MemberWorkoutSummaryCalculator
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ProfileAndLegalScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun memberWorkoutSummaryCalculator_computesAccurateStreakDurationAndModes() {
        val nowMs = 1_794_576_000_000L // reference timestamp
        val dayMs = TimeUnit.DAYS.toMillis(1)
        val records = listOf(
            AttendanceRecord(
                id = "att_today",
                userId = "u1",
                mode = AttendanceRecord.MODE_TRAINER_SCAN,
                status = AttendanceRecord.STATUS_CHECKED_OUT,
                checkInAt = Timestamp(Date(nowMs - 3_600_000L)),
                checkOutAt = Timestamp(Date(nowMs))
            ),
            AttendanceRecord(
                id = "att_yday",
                userId = "u1",
                mode = AttendanceRecord.MODE_KIOSK_QR,
                status = AttendanceRecord.STATUS_CHECKED_OUT,
                checkInAt = Timestamp(Date(nowMs - dayMs - 1_800_000L)),
                checkOutAt = Timestamp(Date(nowMs - dayMs))
            ),
            AttendanceRecord(
                id = "att_active",
                userId = "u1",
                mode = AttendanceRecord.MODE_FRONT_DESK,
                status = AttendanceRecord.STATUS_CHECKED_IN,
                checkInAt = Timestamp(Date(nowMs))
            )
        )

        val summary = MemberWorkoutSummaryCalculator.summarize(records, referenceTimeMs = nowMs)
        assertEquals(3, summary.totalSessions)
        assertEquals(2, summary.completedSessions)
        assertEquals(90L, summary.totalWorkoutMinutes)
        assertEquals(45L, summary.avgSessionMinutes)
        assertEquals("1h 30m", summary.formattedTotalDuration)
        assertEquals(2, summary.currentStreakDays)
        assertEquals(1, summary.trainerScanCount)
        assertEquals(1, summary.kioskQrCount)
        assertEquals(1, summary.frontDeskCount)
    }

    @Test
    fun profileScreen_displaysMembershipStatusWorkoutSummaryAndFirestoreAttendanceHistory() {
        var plansClicked = false
        val nowMs = System.currentTimeMillis()
        val user = UserProfile(
            id = "u_member",
            userId = "u_member",
            displayName = "Arjun Gogoi",
            email = "arjun@battlebox.in",
            role = UserProfile.ROLE_MEMBER,
            planTier = UserProfile.PLAN_PREMIUM,
            membershipStatus = UserProfile.STATUS_ACTIVE,
            phone = "+91 9876543210",
            onboardingCompleted = true
        )
        val sub = MemberSubscription(
            id = "sub_1",
            userId = "u_member",
            planId = "premium_monthly",
            planName = "Premium Monthly",
            tier = UserProfile.PLAN_PREMIUM,
            billingCycle = GymPlan.CYCLE_MONTHLY,
            paymentMode = PaymentRequest.MODE_UPI_AUTOPAY,
            amountInr = 2999L,
            status = MemberSubscription.STATUS_ACTIVE,
            autoRenew = true,
            currentPeriodEnd = Timestamp(Date(nowMs + TimeUnit.DAYS.toMillis(20)))
        )
        val attendance = listOf(
            AttendanceRecord(
                id = "att_1",
                userId = "u_member",
                memberName = "Arjun Gogoi",
                mode = AttendanceRecord.MODE_TRAINER_SCAN,
                status = AttendanceRecord.STATUS_CHECKED_IN,
                distanceMeters = 9.0,
                checkInAt = Timestamp(Date(nowMs - 1_200_000L))
            ),
            AttendanceRecord(
                id = "att_2",
                userId = "u_member",
                memberName = "Arjun Gogoi",
                mode = AttendanceRecord.MODE_KIOSK_QR,
                status = AttendanceRecord.STATUS_CHECKED_OUT,
                distanceMeters = 14.0,
                checkInAt = Timestamp(Date(nowMs - TimeUnit.DAYS.toMillis(1) - 3_000_000L)),
                checkOutAt = Timestamp(Date(nowMs - TimeUnit.DAYS.toMillis(1)))
            )
        )

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                ProfileAndLegalScreen(
                    userProfile = user,
                    gymConfig = GymConfig(),
                    myAttendance = attendance,
                    mySubscriptions = listOf(sub),
                    isDarkTheme = true,
                    onToggleTheme = {},
                    onOpenPlansTab = { plansClicked = true },
                    onSignOut = {}
                )
            }
        }

        // Membership Status Card
        composeRule.onNodeWithTag("member_membership_status_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("member_status_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Premium Monthly • ₹2999 (MONTHLY)").assertIsDisplayed()

        // Personal Workout Summary Card
        composeRule.onNodeWithTag("personal_workout_summary_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Personal Workout Summary").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("50m")[0].performScrollTo().assertIsDisplayed()

        // Attendance History Card from Firestore
        composeRule.onNodeWithTag("member_attendance_history_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("attendance_history_item_att_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("attendance_history_item_att_2").performScrollTo().assertIsDisplayed()

        // Filter to Completed sessions only
        composeRule.onNodeWithTag("history_filter_completed").performScrollTo().performClick()
        composeRule.onNodeWithTag("attendance_history_item_att_2").performScrollTo().assertIsDisplayed()

        // Manage Plans button callback
        composeRule.onNodeWithTag("profile_manage_plan_button").performScrollTo().performClick()
        assertTrue(plansClicked)
    }
}
