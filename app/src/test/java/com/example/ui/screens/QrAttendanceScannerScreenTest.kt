package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.AttendanceRecord
import com.example.data.model.GymConfig
import com.example.data.model.GymPublicStatus
import com.example.data.model.UserProfile
import com.example.data.repository.CheckInOutcome
import com.example.security.DeviceFix
import com.example.security.QrTokenSigner
import com.example.ui.components.LiveOccupancyMonitorCard
import com.example.ui.theme.BattleBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class QrAttendanceScannerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun qrAttendanceScannerScreen_displaysCameraScannerAndLiveOccupancy_andVerifiesManualMemberToken() {
        var scannedPayload: String? = null
        var checkedOutId: String? = null

        val trainerProfile = UserProfile(
            userId = "trainer_1",
            displayName = "Coach Vikram",
            email = "vikram@battlebox.in",
            role = UserProfile.ROLE_TRAINER
        )
        val gymConfig = GymConfig(maxCapacity = 40L)
        val roster = listOf(
            AttendanceRecord(
                id = "att_1",
                userId = "member_1",
                memberName = "Riya Basumatary",
                memberRole = UserProfile.ROLE_MEMBER,
                mode = AttendanceRecord.MODE_TRAINER_SCAN,
                status = AttendanceRecord.STATUS_CHECKED_IN
            ),
            AttendanceRecord(
                id = "att_2",
                userId = "trainer_1",
                memberName = "Coach Vikram",
                memberRole = UserProfile.ROLE_TRAINER,
                mode = AttendanceRecord.MODE_KIOSK_QR,
                status = AttendanceRecord.STATUS_CHECKED_IN
            )
        )

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                QrAttendanceScannerScreen(
                    currentUser = trainerProfile,
                    gymConfig = gymConfig,
                    gymPublicStatus = GymPublicStatus(inGymCount = 2L, isOpen = true),
                    liveRoster = roster,
                    recentRejections = emptyList(),
                    deviceFix = DeviceFix(gymConfig.latitude, gymConfig.longitude, accuracyMeters = 10.0),
                    lastScanOutcome = CheckInOutcome.CheckedIn(
                        attendanceId = "att_1",
                        memberName = "Riya Basumatary",
                        distanceMeters = 12.0,
                        accuracyMeters = 10.0
                    ),
                    isBusy = false,
                    onRefreshFix = {},
                    onMemberQrScanned = { payload, _ -> scannedPayload = payload },
                    onCheckOutSession = { id, _ -> checkedOutId = id },
                    onSweepExpired = {},
                    onDismissOutcome = {}
                )
            }
        }

        // CameraX Scanner Card & verification feedback card
        composeRule.onNodeWithTag("camerax_scanner_card").assertIsDisplayed()
        composeRule.onNodeWithTag("scanner_outcome_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ATTENDANCE STORED: RIYA BASUMATARY").assertIsDisplayed()

        // Submit member QR token via manual/emulator fallback input
        val memberToken = QrTokenSigner.generateMemberToken("member_99")
        composeRule.onNodeWithTag("manual_qr_token_input").performScrollTo().performTextInput(memberToken)
        composeRule.onNodeWithTag("verify_manual_qr_button").performScrollTo().performClick()
        assertEquals(memberToken, scannedPayload)

        // Live Occupancy Monitoring Card & Roster Checkout
        composeRule.onNodeWithTag("live_occupancy_monitor_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("in_gym_count_text").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2 / 40").assertIsDisplayed()
        composeRule.onNodeWithText("QUIET FLOOR • 5%").assertIsDisplayed()
        composeRule.onNodeWithText("Riya Basumatary").assertIsDisplayed()

        composeRule.onNodeWithTag("live_checkout_att_1").performScrollTo().performClick()
        assertEquals("att_1", checkedOutId)
    }

    @Test
    fun liveOccupancyMonitorCard_memberView_showsAggregateCountWithoutMemberNames() {
        val roster = listOf(
            AttendanceRecord(
                id = "att_hidden",
                userId = "member_hidden",
                memberName = "Secret Athlete Name",
                memberRole = UserProfile.ROLE_MEMBER,
                status = AttendanceRecord.STATUS_CHECKED_IN
            )
        )

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                LiveOccupancyMonitorCard(
                    gymConfig = GymConfig(maxCapacity = 40L),
                    gymPublicStatus = GymPublicStatus(inGymCount = 18L, isOpen = true),
                    liveRoster = roster,
                    showRosterDetails = false
                )
            }
        }

        composeRule.onNodeWithTag("live_occupancy_monitor_card").assertIsDisplayed()
        composeRule.onNodeWithText("18 / 40").assertIsDisplayed()
        composeRule.onNodeWithText("ACTIVE FLOOR • 45%").assertIsDisplayed()
        composeRule.onNodeWithText("22 spots open on floor").assertIsDisplayed()
        composeRule.onNodeWithTag("occupancy_roster_item_member_hidden").assertDoesNotExist()
    }
}
