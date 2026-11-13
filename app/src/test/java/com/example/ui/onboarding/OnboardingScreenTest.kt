package com.example.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.ui.theme.BattleBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_requiresAllExplicitConsentsBeforeProceeding_andSubmitsWhenAccepted() {
        var submitted = false
        var submittedName = ""

        composeRule.setContent {
            BattleBoxTheme(darkTheme = true) {
                OnboardingScreen(
                    initialName = "Aarav Verma",
                    email = "aarav@battlebox.in",
                    isBootstrappedOwner = false,
                    isBusy = false,
                    onSubmit = { name, _, _ ->
                        submitted = true
                        submittedName = name
                    },
                    onSignOut = {}
                )
            }
        }

        // Verify DPDP Act 2023 + Privacy Policy sections and disabled CTA initially
        composeRule.onNodeWithTag("legal_filter_privacy_dpdp").performScrollTo().performClick()
        composeRule.onNodeWithTag("legal_document_scroll_container").performScrollTo()
        composeRule.onNodeWithText("9. Privacy Policy — Personal Data Collected & Purpose Limitation")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("11. Digital Personal Data Protection (DPDP) Act, 2023 Notice & Rights")
            .performScrollTo()
            .assertIsDisplayed()

        val submitBtn = composeRule.onNodeWithTag("onboarding_submit_button")
        submitBtn.performScrollTo()
        submitBtn.assertIsNotEnabled()

        // Checking only 3 of 4 checkboxes keeps CTA disabled
        composeRule.onNodeWithTag("consent_terms_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("consent_privacy_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("consent_health_checkbox").performScrollTo().performClick()
        submitBtn.performScrollTo()
        submitBtn.assertIsNotEnabled()

        // Blank name keeps CTA disabled even after all 4 checkboxes are checked
        composeRule.onNodeWithTag("consent_dpdp_checkbox").performScrollTo().performClick()
        composeRule.onNodeWithTag("onboarding_name_input").performScrollTo().performTextReplacement("   ")
        submitBtn.performScrollTo()
        submitBtn.assertIsNotEnabled()

        // Valid name + all 4 explicit acceptances enables CTA and triggers submit
        composeRule.onNodeWithTag("onboarding_name_input").performScrollTo().performTextReplacement("Aarav Verma")
        submitBtn.performScrollTo()
        submitBtn.assertIsEnabled()
        submitBtn.performClick()

        assertTrue(submitted)
        assertEquals("Aarav Verma", submittedName)
    }
}
