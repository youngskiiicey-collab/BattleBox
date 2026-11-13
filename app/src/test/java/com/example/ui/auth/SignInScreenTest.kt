package com.example.ui.auth

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.FirebaseConsoleLinks
import com.example.ui.theme.BattleBoxTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class SignInScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetPrefs() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        AdminLoginPrefs.savePreferredPortal(ctx, AuthPortalTab.USER_TRAINER)
    }

    @Test
    fun signInScreen_hidesFirebaseBannerOnUserTab_showsInAdminTab_andShowsCreatorBadge() {
        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                SignInScreen(onAuthSuccess = {})
            }
        }

        // Bottom-left attribution badge is visible
        composeRule.onNodeWithTag("creator_attribution_badge").assertIsDisplayed()
        composeRule.onNodeWithText("App created by DocCoder.exe").assertIsDisplayed()

        // Default User/Trainer login portal uses provisioned 1-tap Google Sign-In
        // and does NOT render Firebase OAuth/Console banner
        composeRule.onNodeWithTag("portal_tab_user_trainer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_console_card").assertDoesNotExist()
        composeRule.onNodeWithTag("toggle_firebase_console_card").assertDoesNotExist()

        // Switch to Admin Login portal & verify 1-tap Owner/Admin Google Sign-In + Firebase OAuth/Console card
        composeRule.onNodeWithTag("portal_tab_admin").performScrollTo().performClick()
        composeRule.onNodeWithTag("admin_google_sign_in_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("firebase_console_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_console_users_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_console_providers_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("firebase_console_firestore_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("copy_owner_email_button").performScrollTo().performClick()

        // Switch back to User/Trainer tab and confirm Firebase banner stays hidden
        composeRule.onNodeWithTag("portal_tab_user_trainer").performScrollTo().performClick()
        composeRule.onNodeWithTag("firebase_console_card").assertDoesNotExist()

        // Google Maps Dudhnoi Photos Showcase is visible below login card
        composeRule.onNodeWithTag("maps_photo_showcase_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun firebaseConsoleLinks_generateValidConsoleUrls() {
        val projectId = FirebaseConsoleLinks.DEFAULT_PROJECT_ID
        assertTrue(FirebaseConsoleLinks.authUsersUrl(projectId).endsWith("/project/$projectId/authentication/users"))
        assertTrue(FirebaseConsoleLinks.authProvidersUrl(projectId).endsWith("/project/$projectId/authentication/providers"))
        assertTrue(FirebaseConsoleLinks.firestoreDataUrl(projectId, "test-db").endsWith("/firestore/databases/test-db/data"))
    }
}
