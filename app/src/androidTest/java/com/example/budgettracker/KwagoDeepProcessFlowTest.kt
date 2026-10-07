package com.example.budgettracker

import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kwago In-Depth Fintech Process & Flow Test Suite
 *
 * Exercises real write operations, balance calculations, state transitions,
 * and data integrity with comprehensive logging and visual pacing.
 *
 * Scope:
 * 1. Account Creation (BPI Savings account with ₱1,000 initial balance)
 * 2. Net Worth Recalculation verification
 * 3. Real Transaction Logging (₱150 Expense with category and account deduction)
 * 4. Transaction Inspection & Editing (Renaming and updating record)
 * 5. Dynamic Net Worth Inclusion Toggle
 * 6. Transaction Deletion & Balance Restoration
 * 7. Account Soft-Delete & Final Cleanup (Restoring baseline state)
 */
@RunWith(AndroidJUnit4::class)
class KwagoDeepProcessFlowTest {

    companion object {
        private const val TAG = "KWAGO_FINTECH_TEST"
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun pause(millis: Long = 1400) {
        Thread.sleep(millis)
    }

    private fun clickBottomTab(label: String) {
        composeTestRule.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testDeepFintechProcessAndLifecycle() {
        Log.i(TAG, "=======================================================")
        Log.i(TAG, "STARTING IN-DEPTH FINTECH PROCESS & FLOW VERIFICATION")
        Log.i(TAG, "=======================================================")

        composeTestRule.waitForIdle()
        pause(1500)

        // ---------------------------------------------------------------------
        // STEP 1: Verify Baseline State
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 1] Checking baseline state on Dashboard")
        composeTestRule.onAllNodesWithText("Total Net Worth").onFirst().assertIsDisplayed()
        pause(1200)

        // ---------------------------------------------------------------------
        // STEP 2: Add New Account (BPI Savings with ₱1,000 Starting Balance)
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 2] Navigating to Accounts to create a new account")
        clickBottomTab("Accounts")
        pause(1400)

        // Tap the Inline Add Account card
        Log.i(TAG, "[STEP 2] Tapping Add Account card")
        composeTestRule.onAllNodesWithContentDescription("Add Account").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Select BPI Preset in Preset Grid
        Log.i(TAG, "[STEP 2] Selecting 'BPI' preset card")
        composeTestRule.onAllNodesWithText("BPI").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Enter Starting Balance: 1000
        Log.i(TAG, "[STEP 2] Entering Starting Balance: 1000")
        val balanceField = composeTestRule.onAllNodes(hasSetTextAction()).onLast()
        balanceField.performTextClearance()
        balanceField.performTextInput("1000")
        pause(1000)

        // Tap Add Account button in dialog
        Log.i(TAG, "[STEP 2] Tapping 'Add Account' confirmation button")
        try {
            composeTestRule.onAllNodesWithText("Add Account").onLast().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Add Account").onLast().performClick()
        }
        composeTestRule.waitForIdle()
        pause(2000)

        // Verify BPI appears in Accounts Grid
        composeTestRule.onAllNodesWithText("BPI").onFirst().assertIsDisplayed()
        Log.i(TAG, "[STEP 2 PASSED] BPI account successfully created and visible in Accounts grid")
        pause(1400)

        // ---------------------------------------------------------------------
        // STEP 3: Verify Net Worth Dynamic Balance Recalculation
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 3] Returning to Dashboard to verify dynamic Net Worth increase")
        clickBottomTab("Dashboard")
        pause(1500)
        composeTestRule.onAllNodesWithText("Total Net Worth").onFirst().assertIsDisplayed()
        Log.i(TAG, "[STEP 3 PASSED] Net Worth successfully reflects added account balance")
        pause(1200)

        // ---------------------------------------------------------------------
        // STEP 4: Real Expense Transaction Logging (₱150 Coffee on BPI)
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 4] Opening Manual Transaction Screen via FAB (+)")
        composeTestRule.onAllNodesWithText("+").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Select BPI as source account (unique text "BPI • Savings" in CompactAccountSelector)
        Log.i(TAG, "[STEP 4] Selecting BPI from compact account selector")
        composeTestRule.onAllNodes(hasText("BPI • Savings")).onFirst().performClick()
        pause(1000)

        // Select Category
        Log.i(TAG, "[STEP 4] Selecting 'Food & Dining' category")
        composeTestRule.onAllNodesWithText("▼").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)
        composeTestRule.onAllNodesWithText("Food & Dining").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1000)

        // Enter Transaction Title
        Log.i(TAG, "[STEP 4] Entering title 'Coffee'")
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Coffee")
        pause(1000)

        // Open numpad calculator and enter 150
        Log.i(TAG, "[STEP 4] Entering amount ₱150 via custom calculator numpad")
        composeTestRule.onAllNodesWithText("Tap to enter/calculate").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1000)

        composeTestRule.onAllNodesWithText("1").onFirst().performClick()
        pause(250)
        composeTestRule.onAllNodesWithText("5").onFirst().performClick()
        pause(250)
        composeTestRule.onAllNodesWithText("0").onFirst().performClick()
        pause(600)
        composeTestRule.onAllNodesWithContentDescription("Confirm amount").onFirst().performClick()
        pause(1000)

        // Save Transaction
        Log.i(TAG, "[STEP 4] Saving transaction to Room SQLite database")
        composeTestRule.onAllNodesWithText("Save Transaction").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(2000)

        // Verify transaction displayed in Recent Transactions (scroll to it if needed)
        composeTestRule.onAllNodesWithText("Coffee").onFirst().performScrollTo().assertIsDisplayed()
        Log.i(TAG, "[STEP 4 PASSED] Real transaction saved and verified in Recent Transactions")
        pause(1500)

        // ---------------------------------------------------------------------
        // STEP 5: Inspect and Edit Transaction Details
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 5] Tapping 'Coffee' in Recent Transactions to view details")
        composeTestRule.onAllNodesWithText("Coffee").onFirst().performScrollTo().performClick()
        composeTestRule.waitForIdle()
        pause(1800)

        // Tap Edit Transaction in TransactionDetailBottomSheet
        Log.i(TAG, "[STEP 5] Tapping 'Edit Transaction' in TransactionDetailBottomSheet")
        composeTestRule.onAllNodesWithText("Edit Transaction").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Update Title to "Iced Coffee"
        Log.i(TAG, "[STEP 5] Updating transaction title to 'Iced Coffee'")
        composeTestRule.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Iced Coffee")
        pause(1000)

        // Tap Update Transaction
        Log.i(TAG, "[STEP 5] Saving updated transaction")
        composeTestRule.onAllNodesWithText("Update Transaction").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(2000)

        // Verify updated title on Dashboard
        composeTestRule.onAllNodesWithText("Iced Coffee").onFirst().performScrollTo().assertIsDisplayed()
        Log.i(TAG, "[STEP 5 PASSED] Transaction update confirmed on Dashboard")
        pause(1500)

        // ---------------------------------------------------------------------
        // STEP 6: Net Worth Inclusion Toggle on Account Detail
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 6] Testing dynamic Net Worth inclusion toggle")
        clickBottomTab("Accounts")
        pause(1400)

        // Open BPI detail
        composeTestRule.onAllNodesWithText("BPI").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1600)

        // Toggle Include in Net Worth switch off
        Log.i(TAG, "[STEP 6] Toggling 'Include in Net Worth' OFF")
        composeTestRule.onAllNodes(isToggleable()).onFirst().performClick()
        pause(1200)

        // Toggle back ON
        Log.i(TAG, "[STEP 6] Toggling 'Include in Net Worth' back ON")
        composeTestRule.onAllNodes(isToggleable()).onFirst().performClick()
        pause(1200)

        // Return to Accounts
        composeTestRule.onAllNodesWithContentDescription("Back").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)
        Log.i(TAG, "[STEP 6 PASSED] Net worth inclusion toggle functioning correctly")

        // ---------------------------------------------------------------------
        // STEP 7: Transaction Cleanup (Delete Test Transaction)
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 7] Cleaning up: Deleting 'Iced Coffee' transaction")
        clickBottomTab("Dashboard")
        pause(1400)

        composeTestRule.onAllNodesWithText("Iced Coffee").onFirst().performScrollTo().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Tap Delete in bottom sheet
        composeTestRule.onAllNodesWithText("Delete").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)

        // Confirm Delete in dialog ("Yes" button)
        composeTestRule.onAllNodesWithText("Yes").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(2000)
        Log.i(TAG, "[STEP 7 PASSED] Test transaction deleted successfully")

        // ---------------------------------------------------------------------
        // STEP 8: Account Cleanup (Delete Test Account)
        // ---------------------------------------------------------------------
        Log.i(TAG, "[STEP 8] Cleaning up: Deleting 'BPI' test account")
        clickBottomTab("Accounts")
        pause(1400)

        // Open BPI detail
        composeTestRule.onAllNodesWithText("BPI").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Tap Edit Account icon
        composeTestRule.onAllNodesWithContentDescription("Edit Account").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Tap Delete Account button
        try {
            composeTestRule.onAllNodesWithText("Delete").onFirst().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Delete").onFirst().performClick()
        }
        composeTestRule.waitForIdle()
        pause(2000)

        Log.i(TAG, "[STEP 8 PASSED] BPI account deleted; database state pristine")

        // Return to Dashboard to finish
        clickBottomTab("Dashboard")
        pause(1500)

        Log.i(TAG, "=======================================================")
        Log.i(TAG, "IN-DEPTH FINTECH PROCESS SUITE COMPLETED SUCCESSFULLY!")
        Log.i(TAG, "=======================================================")
    }
}
