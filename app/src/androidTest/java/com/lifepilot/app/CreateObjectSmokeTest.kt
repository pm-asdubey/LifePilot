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
class CreateObjectSmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun fabClick_opensCreateObjectSheet() {
        composeRule.onNodeWithContentDescription("Add Object")
            .performClick()
        // CreateObjectSheet should show a domain selection or title input
        composeRule.onNodeWithText("Add Object")
            .assertIsDisplayed()
    }

    @Test
    fun libraryFab_opensCreateObjectSheet() {
        composeRule.onNodeWithContentDescription("Library")
            .performClick()
        composeRule.onNodeWithContentDescription("Add Object")
            .performClick()
        composeRule.onNodeWithText("Add Object")
            .assertIsDisplayed()
    }
}
