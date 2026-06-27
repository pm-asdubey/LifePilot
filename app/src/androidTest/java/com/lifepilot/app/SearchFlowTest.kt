package com.lifepilot.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SearchFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun searchScreen_showsSearchBar() {
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        composeRule.onNodeWithText("Search objects, documents, tasks...")
            .assertIsDisplayed()
    }

    @Test
    fun searchScreen_emptyQuery_showsNoResults() {
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        // With no data and no query, expect empty/hint state
        composeRule.onNodeWithText("Search your life")
            .assertIsDisplayed()
    }

    @Test
    fun searchBar_acceptsTextInput() {
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        composeRule.onNodeWithText("Search objects, documents, tasks...")
            .performTextInput("passport")
        // After input, the clear icon should appear (ContentDescription "Clear")
        composeRule.onNodeWithContentDescription("Clear")
            .assertIsDisplayed()
    }

    @Test
    fun searchBar_clearButton_resetsQuery() {
        composeRule.onNodeWithContentDescription("Search")
            .performClick()
        composeRule.onNodeWithText("Search objects, documents, tasks...")
            .performTextInput("passport")
        composeRule.onNodeWithContentDescription("Clear")
            .performClick()
        // Placeholder should reappear after clearing
        composeRule.onNodeWithText("Search objects, documents, tasks...")
            .assertIsDisplayed()
    }
}
