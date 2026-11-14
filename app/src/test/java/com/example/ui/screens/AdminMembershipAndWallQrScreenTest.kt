package com.example.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AttendanceRecord
import com.example.data.model.GymConfig
import com.example.data.model.GymPlan
import com.example.data.model.MemberSubscription
import com.example.data.model.PaymentRequest
import com.example.data.model.Phase3BillingConfig
import com.example.data.model.UserProfile
import com.example.ui.theme.BattleBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class AdminMembershipAndWallQrScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun adminConsole_displaysPermanentWallQr_andSelfCheckInAlerts() {
        val wallCheckIn = AttendanceRecord(
            id = "att_wall_1",
            userId = "u_1",
            memberName = "Arup Kalita",
            memberRole = UserProfile.ROLE_MEMBER,
            mode = AttendanceRecord.MODE_KIOSK_QR,
            status = AttendanceRecord.STATUS_CHECKED_IN,
            distanceMeters = 14.0,
            gpsAccuracyMeters = 8.0
        )

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                AdminConsoleScreen(
                    gymConfig = GymConfig(),
                    liveRoster = listOf(wallCheckIn),
                    allUsers = emptyList(),
                    auditLogs = emptyList(),
                    rejections = emptyList(),
                    deviceFix = null,
                    isBusy = false,
                    onFrontDeskScan = { _, _ -> },
                    onAdminCheckOut = { _, _ -> },
                    onSweepExpired = {},
                    onUpdateUser = { _, _, _, _, _ -> },
                    onUpdateGymConfig = {},
                    onCopyMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("self_checkin_notifications_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Arup Kalita (MEMBER)").assertIsDisplayed()
        composeRule.onNodeWithTag("permanent_wall_qr_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PERMANENT • NON-ROTATING • NO EXPIRY").assertIsDisplayed()
    }

    @Test
    fun adminPhase2Section_allowsEditingPlansMembershipsPendingPayAndPhase3Config() {
        var savedPlan: GymPlan? = null
        var updatedReq: PaymentRequest? = null
        var savedBillingConfig: Phase3BillingConfig? = null

        val testPlan = GymPlan(
            id = "basic_monthly",
            name = "Basic Monthly",
            tier = UserProfile.PLAN_BASIC,
            billingCycle = GymPlan.CYCLE_MONTHLY,
            baseAmountInr = 1270L,
            gstPercent = 18L,
            totalAmountInr = 1499L,
            durationDays = 30L
        )
        val pendingReq = PaymentRequest(
            id = "req_test_1",
            userId = "u_1",
            memberName = "Arup Kalita",
            memberEmail = "arup@battlebox.in",
            planId = "basic_monthly",
            planName = "Basic Monthly",
            tier = UserProfile.PLAN_BASIC,
            billingCycle = GymPlan.CYCLE_MONTHLY,
            paymentMode = PaymentRequest.MODE_UPI_AUTOPAY,
            amountInr = 1499L,
            preferredUpiApp = "Google Pay",
            status = PaymentRequest.STATUS_CREATED
        )

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    AdminPhase2Section(
                        announcements = emptyList(),
                        paymentRequests = listOf(pendingReq),
                        deliveryLogs = emptyList(),
                        plans = listOf(testPlan),
                        subscriptions = listOf(
                            MemberSubscription(
                                id = "sub_1",
                                userId = "u_1",
                                planId = "basic_monthly",
                                planName = "Basic Monthly",
                                amountInr = 1499L
                            )
                        ),
                        allUsers = listOf(
                            UserProfile(
                                userId = "u_1",
                                displayName = "Arup Kalita",
                                email = "arup@battlebox.in",
                                role = UserProfile.ROLE_MEMBER
                            )
                        ),
                        billingConfig = Phase3BillingConfig(),
                        isBusy = false,
                        onPostAnnouncement = { _, _, _, _, _, _ -> },
                        onClearAnnouncement = {},
                        onSeedDefaultPlans = {},
                        onSavePlan = { savedPlan = it },
                        onUpdatePaymentRequest = { updatedReq = it },
                        onReconcileSandboxPayment = {},
                        onSaveBillingConfig = { savedBillingConfig = it },
                        onCopyMessage = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("admin_editable_plans_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_editable_memberships_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_pending_pay_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_phase3_integration_card").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("edit_plan_basic_monthly").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_plan_total_input").performTextReplacement("1699")
        composeRule.onNodeWithTag("save_plan_dialog_button").performClick()
        assertNotNull(savedPlan)
        assertEquals(1699L, savedPlan!!.totalAmountInr)

        composeRule.onNodeWithTag("edit_pending_pay_req_test_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_pending_pay_amount_input").performTextReplacement("1599")
        composeRule.onNodeWithTag("save_pending_pay_button").performClick()
        assertNotNull(updatedReq)
        assertEquals(1599L, updatedReq!!.amountInr)

        composeRule.onNodeWithTag("phase3_key_id_input").performScrollTo().performTextReplacement("rzp_live_dudhnoi")
        composeRule.onNodeWithTag("admin_save_phase3_billing_button").performScrollTo().performClick()
        assertNotNull(savedBillingConfig)
        assertEquals("rzp_live_dudhnoi", savedBillingConfig!!.razorpayKeyId)
    }

    @Test
    fun trainerRosterScreen_displaysRealTimeWallQrSelfCheckInAlerts() {
        val trainer = UserProfile(
            userId = "tr_1",
            displayName = "Coach Vikram",
            email = "vikram@battlebox.in",
            role = UserProfile.ROLE_TRAINER
        )
        val wallCheckIn = AttendanceRecord(
            id = "att_wall_trainer_view",
            userId = "m_1",
            memberName = "Neha Sangma",
            memberRole = UserProfile.ROLE_MEMBER,
            mode = AttendanceRecord.MODE_KIOSK_QR,
            status = AttendanceRecord.STATUS_CHECKED_IN,
            distanceMeters = 11.0,
            gpsAccuracyMeters = 6.0
        )

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                TrainerRosterScreen(
                    currentUser = trainer,
                    assignedMembers = emptyList(),
                    liveRoster = listOf(wallCheckIn)
                )
            }
        }

        composeRule.onNodeWithTag("self_checkin_notifications_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Neha Sangma (MEMBER)").assertIsDisplayed()
    }
}
