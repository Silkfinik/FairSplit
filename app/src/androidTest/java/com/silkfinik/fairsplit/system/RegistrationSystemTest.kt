package com.silkfinik.fairsplit.system

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.app.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistrationSystemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun getString(resId: Int): String = composeTestRule.activity.getString(resId)

    @Test
    fun testRegistrationFormAllFieldsValidationAndNavigation() {
        composeTestRule.onNodeWithText(getString(R.string.welcome_btn_email)).performClick()

        val loginTitle = getString(R.string.login_title)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(loginTitle).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(getString(R.string.login_btn_to_register)).performClick()

        val registerTitle = getString(R.string.register_title)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(registerTitle).fetchSemanticsNodes().isNotEmpty()
        }

        val nameLabel = getString(R.string.label_name)
        val emailLabel = getString(R.string.label_email)
        val passwordLabel = getString(R.string.label_password)
        val confirmPasswordLabel = getString(R.string.register_label_confirm_password)
        val signUpBtn = getString(R.string.register_btn_sign_up)

        composeTestRule.onNodeWithText(registerTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(nameLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(emailLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(passwordLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(confirmPasswordLabel).assertIsDisplayed()

        val signUpButton = composeTestRule.onNode(hasText(signUpBtn) and hasClickAction())
        signUpButton.assertIsNotEnabled()

        composeTestRule.onNodeWithText(nameLabel).performTextInput("Алексей")
        signUpButton.assertIsNotEnabled()

        composeTestRule.onNodeWithText(emailLabel).performTextInput("alex@example.com")
        signUpButton.assertIsNotEnabled()

        composeTestRule.onNodeWithText(passwordLabel).performTextInput("Secret1234!")
        signUpButton.assertIsNotEnabled()

        composeTestRule.onNodeWithText(confirmPasswordLabel).performTextInput("Secret1234!")

        signUpButton.assertIsEnabled()

        val backDesc = getString(R.string.action_back)
        composeTestRule.onNodeWithContentDescription(backDesc).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(loginTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText(loginTitle).onFirst().assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(backDesc).performClick()

        val welcomeSubtitle = getString(R.string.welcome_subtitle)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(welcomeSubtitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(welcomeSubtitle).assertIsDisplayed()
    }
}
