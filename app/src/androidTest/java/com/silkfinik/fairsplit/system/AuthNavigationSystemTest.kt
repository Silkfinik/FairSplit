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
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silkfinik.fairsplit.R
import com.silkfinik.fairsplit.app.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthNavigationSystemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun getString(resId: Int): String = composeTestRule.activity.getString(resId)

    @Test
    fun testWelcomeScreenDynamicValidation() {
        composeTestRule.onNodeWithText(getString(R.string.app_name)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.welcome_subtitle)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.welcome_card_title)).assertIsDisplayed()

        val continueBtn = getString(R.string.welcome_btn_continue)
        composeTestRule.onNodeWithText(continueBtn).assertIsNotEnabled()

        val nameLabel = getString(R.string.welcome_label_name)
        composeTestRule.onNodeWithText(nameLabel).performTextInput("Алексей")

        composeTestRule.onNodeWithText(continueBtn).assertIsEnabled()

        composeTestRule.onNodeWithText("Алексей").performTextClearance()
        composeTestRule.onNodeWithText(continueBtn).assertIsNotEnabled()
    }

    @Test
    fun testLoginScreenValidationAndNavigationBack() {
        val emailBtn = getString(R.string.welcome_btn_email)
        composeTestRule.onNodeWithText(emailBtn).performClick()

        val loginTitle = getString(R.string.login_title)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(loginTitle).fetchSemanticsNodes().isNotEmpty()
        }

        val emailLabel = getString(R.string.label_email)
        val passwordLabel = getString(R.string.label_password)
        val signInBtn = getString(R.string.login_btn_sign_in)

        composeTestRule.onAllNodesWithText(loginTitle).onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText(emailLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(passwordLabel).assertIsDisplayed()

        val signInButton = composeTestRule.onNode(hasText(signInBtn) and hasClickAction())
        signInButton.assertIsNotEnabled()

        composeTestRule.onNodeWithText(emailLabel).performTextInput("alex@example.com")
        signInButton.assertIsNotEnabled()

        composeTestRule.onNodeWithText(passwordLabel).performTextInput("SecretPass123!")
        signInButton.assertIsEnabled()

        val backDesc = getString(R.string.action_back)
        composeTestRule.onNodeWithContentDescription(backDesc).performClick()

        val welcomeSubtitle = getString(R.string.welcome_subtitle)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(welcomeSubtitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(welcomeSubtitle).assertIsDisplayed()
    }
}
