package com.example.budgettracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kwago Live On-Device Automated Test Suite
 *
 * Runs all 6 core fintech modules sequentially on the connected physical device
 * with visual pacing (delays) so the user can watch the entire flow execute live.
 */
@RunWith(AndroidJUnit4::class)
class KwagoLiveFullSuiteTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun pause(millis: Long = 1400) {
        Thread.sleep(millis)
    }

    private fun clickBottomTab(label: String) {
        // Bottom nav tabs are anchored at the bottom of the hierarchy
        composeTestRule.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testLiveFullKwagoFintechSuite() {
        composeTestRule.waitForIdle()
        pause(1500)

        // =========================================================================
        // MODULE 1: Dashboard Overview & Privacy Eye Toggle
        // =========================================================================
        // 1. Verify Dashboard presence
        composeTestRule.onAllNodesWithText("Total Net Worth").onFirst().assertIsDisplayed()
        pause(1200)

        // 2. Hide Balance
        composeTestRule.onAllNodesWithContentDescription("Hide Balance").onFirst().performClick()
        pause(1400)

        // 3. Show Balance again
        composeTestRule.onAllNodesWithContentDescription("Show Balance").onFirst().performClick()
        pause(1400)

        // =========================================================================
        // MODULE 2: Accounts Navigation, Filter Pills, and Account Details
        // =========================================================================
        // 1. Switch to Accounts Tab
        clickBottomTab("Accounts")
        pause(1500)

        // 2. Test Category Filters
        composeTestRule.onAllNodes(hasText("Bank") and hasClickAction()).onFirst().performClick()
        pause(1000)
        composeTestRule.onAllNodes(hasText("Credit") and hasClickAction()).onFirst().performClick()
        pause(1000)
        composeTestRule.onAllNodes(hasText("Loan") and hasClickAction()).onFirst().performClick()
        pause(1000)
        composeTestRule.onAllNodes(hasText("All") and hasClickAction()).onFirst().performClick()
        pause(1200)

        // 3. Open Account Detail for SPayLater
        composeTestRule.onAllNodes(hasText("SPayLater") and hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1800)

        // 4. Navigate back from Account Detail
        composeTestRule.onAllNodesWithContentDescription("Back").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1400)

        // =========================================================================
        // MODULE 3: Smart Parser (Taglish NLP) & Manual Keypad Transaction
        // =========================================================================
        // Part A: Smart Parser
        clickBottomTab("Smart Parser")
        pause(1500)

        // Select a sample prompt
        composeTestRule.onAllNodes(hasText("nag-Grab 320 via gcash") and hasClickAction()).onFirst().performClick()
        pause(1200)

        // Tap Parse Text
        composeTestRule.onAllNodes(hasText("Parse Text") and hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(2000)

        // Clear input text
        composeTestRule.onAllNodesWithContentDescription("Clear Text").onFirst().performClick()
        pause(1000)

        // Part B: Manual Transaction Entry & Keypad
        clickBottomTab("Dashboard")
        pause(1200)

        // Tap FAB to open Manual Transaction Screen
        composeTestRule.onAllNodes(hasText("+") and hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Toggle Transaction Types
        composeTestRule.onAllNodes(hasText("Income") and hasClickAction()).onFirst().performClick()
        pause(1000)
        composeTestRule.onAllNodes(hasText("Transfer") and hasClickAction()).onFirst().performClick()
        pause(1000)
        composeTestRule.onAllNodes(hasText("Expense") and hasClickAction()).onFirst().performClick()
        pause(1000)

        // Tap amount field to reveal custom numpad
        composeTestRule.onAllNodes(hasText("Tap to enter/calculate") and hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)

        // Enter 250 on numpad
        composeTestRule.onAllNodes(hasText("2") and hasClickAction()).onFirst().performClick()
        pause(300)
        composeTestRule.onAllNodes(hasText("5") and hasClickAction()).onFirst().performClick()
        pause(300)
        composeTestRule.onAllNodes(hasText("0") and hasClickAction()).onFirst().performClick()
        pause(800)

        // Confirm amount
        composeTestRule.onAllNodesWithContentDescription("Confirm amount").onFirst().performClick()
        pause(1200)

        // Dismiss / navigate back from transaction screen
        composeTestRule.onAllNodesWithContentDescription("Back").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1400)

        // =========================================================================
        // MODULE 4: Billing Cycles & Obligations Verification
        // =========================================================================
        // Verify Upcoming Bills section on Dashboard
        composeTestRule.onAllNodesWithText("Upcoming Bills").onFirst().assertIsDisplayed()
        pause(1500)

        // =========================================================================
        // MODULE 5: Reports & Analytics
        // =========================================================================
        clickBottomTab("Reports")
        pause(1600)

        // Toggle period filters
        composeTestRule.onAllNodes(hasText("Last Month") and hasClickAction()).onFirst().performClick()
        pause(1200)
        composeTestRule.onAllNodes(hasText("Last 3 Months") and hasClickAction()).onFirst().performClick()
        pause(1200)
        composeTestRule.onAllNodes(hasText("Last 6 Months") and hasClickAction()).onFirst().performClick()
        pause(1200)
        composeTestRule.onAllNodes(hasText("This Month") and hasClickAction()).onFirst().performClick()
        pause(1200)

        // Toggle balance privacy in Reports
        composeTestRule.onAllNodesWithContentDescription("Hide Balance").onFirst().performClick()
        pause(1200)
        composeTestRule.onAllNodesWithContentDescription("Show Balance").onFirst().performClick()
        pause(1200)

        // =========================================================================
        // MODULE 6: Settings, CSV Export & Financial Glossary
        // =========================================================================
        clickBottomTab("Settings")
        pause(1500)

        // Test CSV Export copy
        composeTestRule.onAllNodes(hasText("CSV") and hasClickAction()).onFirst().performClick()
        pause(1200)

        // Open Financial Glossary
        composeTestRule.onAllNodes(hasText("Financial Glossary") and hasClickAction()).onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(2000)

        // Close Financial Glossary
        composeTestRule.onAllNodesWithContentDescription("Close").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1400)

        // Click About row
        composeTestRule.onAllNodes(hasText("About") and hasClickAction()).onFirst().performClick()
        pause(1400)

        // Return to Dashboard cleanly
        clickBottomTab("Dashboard")
        pause(2000)
    }
}
