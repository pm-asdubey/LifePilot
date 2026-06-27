package com.lifepilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun homeScreen_isDisplayedOnLaunch() {
        // BottomNav "Home" tab is always present on the home screen
        composeRule.onNodeWithContentDescription("Home")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToLibrary() {
        composeRule.onNodeWithContentDescription("Library")
            .performClick()
        composeRule.onNodeWithText("Library")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToSearch() {
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        // Search placeholder text is always shown in the search field
        composeRule.onNodeWithText("Search objects, documents, tasks...")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToAiChat() {
        composeRule.onNodeWithContentDescription("Ask AI")
            .performClick()
        composeRule.onNodeWithText("AI Assistant")
            .assertIsDisplayed()
    }

    @Test
    fun bottomNav_navigatesToSettings() {
        composeRule.onNodeWithContentDescription("Profile")
            .performClick()
        composeRule.onNodeWithText("Settings")
            .assertIsDisplayed()
    }
}
