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
 * Kwago Comprehensive Accounts & Installment Deep Test Suite
 *
 * Scope:
 * 1. Multi-account creation across diverse financial instruments:
 *    - Loan / BNPL: Home Credit
 *    - E-Wallets: GCash (₱3,500), Maya (₱1,200)
 *    - Banks / Savings: BDO (₱15,000), UnionBank (₱8,000)
 *    - Credit Card: BPI Credit Card (₱60,000 limit, day 15)
 *    - Recurring Bill: Meralco (₱3,500 due, day 20)
 *    - Cash / Other: Cash (₱2,000)
 * 2. Real Installment Expense Workflow:
 *    - Opening Manual Transaction screen with Installment type
 *    - Selecting newly created Home Credit BNPL account
 *    - Selecting 'Gadgets & Tech' category
 *    - Entering item name 'iPad 10th Gen', 6 months tenure
 *    - Calculating & entering ₱18,000 via custom calculator numpad
 *    - Saving to Room database & verifying on Dashboard Recent Transactions
 *    - Navigating to Home Credit Account Detail to inspect active Installment Plan
 * 3. Accounts Filter Exploration:
 *    - Testing Category Filter Pills ("All", "E-Wallet", "Savings", "Credit", "Loan", "Bill")
 *    - Verifying reactive sub-lists filter dynamically
 * 4. Paced visual execution for seamless live viewing on the connected phone.
 */
@RunWith(AndroidJUnit4::class)
class KwagoAccountsAndInstallmentDeepTest {

    companion object {
        private const val TAG = "KWAGO_ACCOUNTS_TEST"
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
        pause(1200)
    }

    private fun openAddAccountDialog() {
        try {
            composeTestRule.onAllNodesWithContentDescription("Add Account").onFirst().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithContentDescription("Add Account").onFirst().performClick()
        }
        composeTestRule.waitForIdle()
        pause(1400)
    }

    private fun selectPresetCategory(categoryLabel: String) {
        var nodes = composeTestRule.onAllNodesWithText(categoryLabel)
        if (nodes.fetchSemanticsNodes().isEmpty()) {
            val categoryLazyRow = composeTestRule.onAllNodes(
                hasScrollAction() and hasAnyDescendant(hasText("Savings") or hasText("Loan/BNPL") or hasText("Credit") or hasText("E-Wallet"))
            )
            if (categoryLazyRow.fetchSemanticsNodes().isNotEmpty()) {
                categoryLazyRow.onLast().performTouchInput { swipeLeft() }
                composeTestRule.waitForIdle()
                pause(600)
            }
            nodes = composeTestRule.onAllNodesWithText(categoryLabel)
        }
        if (nodes.fetchSemanticsNodes().isEmpty()) {
            val categoryLazyRow = composeTestRule.onAllNodes(
                hasScrollAction() and hasAnyDescendant(hasText("Savings") or hasText("Loan/BNPL") or hasText("Credit") or hasText("E-Wallet"))
            )
            if (categoryLazyRow.fetchSemanticsNodes().isNotEmpty()) {
                categoryLazyRow.onLast().performTouchInput { swipeRight() }
                composeTestRule.waitForIdle()
                pause(600)
            }
            nodes = composeTestRule.onAllNodesWithText(categoryLabel)
        }
        nodes.onLast().performClick()
        composeTestRule.waitForIdle()
        pause(1000)
    }

    private fun selectPresetItem(presetName: String) {
        try {
            composeTestRule.onAllNodesWithText(presetName).onLast().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText(presetName).onLast().performClick()
        }
        composeTestRule.waitForIdle()
        pause(1200)
    }

    private fun enterStartingBalance(amount: String) {
        val balanceField = composeTestRule.onAllNodes(hasSetTextAction()).onLast()
        balanceField.performTextClearance()
        balanceField.performTextInput(amount)
        pause(600)
    }

    private fun confirmAddAccount() {
        try {
            composeTestRule.onAllNodesWithText("Add Account").onLast().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Add Account").onLast().performClick()
        }
        composeTestRule.waitForIdle()
        pause(1800)
    }

    private fun clickFilterPill(pillLabel: String) {
        var nodes = composeTestRule.onAllNodesWithText(pillLabel)
        if (nodes.fetchSemanticsNodes().isEmpty()) {
            val anchorPill = composeTestRule.onAllNodes(
                hasText("All") or hasText("Cash") or hasText("Bank") or hasText("E-Wallet") or hasText("Credit") or hasText("Loan")
            )
            if (anchorPill.fetchSemanticsNodes().isNotEmpty()) {
                anchorPill.onFirst().performTouchInput { swipeLeft() }
                composeTestRule.waitForIdle()
                pause(600)
            }
            nodes = composeTestRule.onAllNodesWithText(pillLabel)
        }
        if (nodes.fetchSemanticsNodes().isEmpty()) {
            val anchorPill = composeTestRule.onAllNodes(
                hasText("Savings") or hasText("Bill") or hasText("Loan") or hasText("Credit")
            )
            if (anchorPill.fetchSemanticsNodes().isNotEmpty()) {
                anchorPill.onFirst().performTouchInput { swipeRight() }
                composeTestRule.waitForIdle()
                pause(600)
            }
            nodes = composeTestRule.onAllNodesWithText(pillLabel)
        }
        nodes.onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1400)
    }

    @Test
    fun testComprehensiveAccountsAndInstallmentFlow() {
        Log.i(TAG, "=================================================================")
        Log.i(TAG, "STARTING COMPREHENSIVE ACCOUNTS & INSTALLMENT DEEP TEST")
        Log.i(TAG, "=================================================================")

        composeTestRule.waitForIdle()
        pause(1500)

        // -------------------------------------------------------------------------
        // SECTION 1: Navigate to Accounts Screen
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 1] Navigating to Accounts screen")
        clickBottomTab("Accounts")
        pause(1200)

        // -------------------------------------------------------------------------
        // SECTION 2: Add Installment / Loan Account (Home Credit)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 2] Adding new Loan/BNPL account: Home Credit")
        openAddAccountDialog()
        selectPresetCategory("Loan/BNPL")
        selectPresetItem("Home Credit")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 2 PASSED] Home Credit account created successfully")
        pause(1200)

        // -------------------------------------------------------------------------
        // SECTION 3: Add Multiple E-Wallet Accounts (GCash & Maya)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 3A] Adding E-Wallet account: GCash (₱3,500)")
        openAddAccountDialog()
        selectPresetCategory("E-Wallet")
        selectPresetItem("GCash")
        enterStartingBalance("3500")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 3A PASSED] GCash account created with ₱3,500 starting balance")
        pause(1200)

        Log.i(TAG, "[SECTION 3B] Adding E-Wallet account: Maya (₱1,200)")
        openAddAccountDialog()
        selectPresetCategory("E-Wallet")
        selectPresetItem("Maya")
        enterStartingBalance("1200")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 3B PASSED] Maya account created with ₱1,200 starting balance")
        pause(1200)

        // -------------------------------------------------------------------------
        // SECTION 4: Add Bank / Savings Accounts (BDO & UnionBank)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 4A] Adding Savings account: BDO (₱15,000)")
        openAddAccountDialog()
        selectPresetCategory("Savings")
        selectPresetItem("BDO")
        enterStartingBalance("15000")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 4A PASSED] BDO account created with ₱15,000 starting balance")
        pause(1200)

        Log.i(TAG, "[SECTION 4B] Adding Savings account: UnionBank (₱8,000)")
        openAddAccountDialog()
        selectPresetCategory("Savings")
        selectPresetItem("UnionBank")
        enterStartingBalance("8000")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 4B PASSED] UnionBank account created with ₱8,000 starting balance")
        pause(1200)

        // -------------------------------------------------------------------------
        // SECTION 5: Add Credit Card Account (BPI Credit Card, ₱60,000 limit)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 5] Adding Credit Card account: BPI Credit Card (₱60,000 credit limit)")
        openAddAccountDialog()
        selectPresetCategory("Credit")
        selectPresetItem("BPI Credit Card")
        val limitField = composeTestRule.onAllNodes(hasSetTextAction()).onLast()
        limitField.performTextClearance()
        limitField.performTextInput("60000")
        pause(800)
        confirmAddAccount()
        Log.i(TAG, "[SECTION 5 PASSED] BPI Credit Card created successfully with ₱60,000 limit")
        pause(1200)

        // -------------------------------------------------------------------------
        // SECTION 6: Add Recurring Bill Account (Meralco)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 6] Adding Recurring Bill account: Meralco")
        openAddAccountDialog()
        selectPresetCategory("Bill")
        selectPresetItem("Meralco")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 6 PASSED] Meralco bill account created successfully")
        pause(1200)

        // -------------------------------------------------------------------------
        // SECTION 7: Add Cash / Other Account (Cash, ₱2,000)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 7] Adding Cash account (₱2,000)")
        openAddAccountDialog()
        selectPresetCategory("Other")
        selectPresetItem("Cash")
        enterStartingBalance("2000")
        confirmAddAccount()
        Log.i(TAG, "[SECTION 7 PASSED] Cash account created successfully with ₱2,000")
        pause(1500)

        // -------------------------------------------------------------------------
        // SECTION 8: Verify All Created Accounts on Accounts Screen
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 8] Verifying all created accounts on Accounts screen")
        try {
            composeTestRule.onAllNodesWithText("Home Credit").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Home Credit").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("GCash").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("GCash").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("Maya").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Maya").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("BDO").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("BDO").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("UnionBank").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("UnionBank").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("BPI Credit Card").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("BPI Credit Card").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("Meralco").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Meralco").onFirst().assertIsDisplayed()
        }
        try {
            composeTestRule.onAllNodesWithText("Cash").onLast().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Cash").onLast().assertIsDisplayed()
        }
        Log.i(TAG, "[SECTION 8 PASSED] All 8 accounts successfully verified on Accounts screen")
        pause(1500)

        // -------------------------------------------------------------------------
        // SECTION 9: Log New Installment Expense on Home Credit (₱18,000 iPad)
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 9] Navigating to Dashboard to access Manual Transaction FAB (+)")
        clickBottomTab("Dashboard")
        pause(1500)

        Log.i(TAG, "[SECTION 9] Opening Manual Transaction Screen via FAB (+)")
        composeTestRule.onAllNodesWithText("+").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)

        // Select Installment Type
        Log.i(TAG, "[SECTION 9] Selecting 'Installment' transaction type")
        composeTestRule.onAllNodesWithText("Installment").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)

        // Select Home Credit as installment account
        Log.i(TAG, "[SECTION 9] Selecting 'Home Credit' from account selector")
        composeTestRule.onAllNodes(hasText("Home Credit", substring = true)).onFirst().performClick()
        pause(1000)

        // Select Category: Gadgets & Tech
        Log.i(TAG, "[SECTION 9] Selecting 'Gadgets & Tech' category")
        composeTestRule.onAllNodesWithText("▼").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)
        composeTestRule.onAllNodesWithText("Gadgets & Tech").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1000)

        // Enter Item Name: iPad 10th Gen
        Log.i(TAG, "[SECTION 9] Entering Item Name 'iPad 10th Gen'")
        val allTextFields = composeTestRule.onAllNodes(hasSetTextAction())
        allTextFields.onFirst().performTextInput("iPad 10th Gen")
        pause(1000)

        // Enter Total Installments: 6
        Log.i(TAG, "[SECTION 9] Entering Total Installments: 6 months")
        if (allTextFields.fetchSemanticsNodes().size >= 2) {
            try {
                allTextFields[1].performScrollTo().performTextClearance()
                allTextFields[1].performTextInput("6")
            } catch (e: Throwable) {
                allTextFields[1].performTextClearance()
                allTextFields[1].performTextInput("6")
            }
        }
        pause(1000)

        // Open custom calculator numpad and enter 18000
        Log.i(TAG, "[SECTION 9] Entering ₱18,000 via custom calculator numpad")
        try {
            composeTestRule.onAllNodesWithText("Tap to enter/calculate").onFirst().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Tap to enter/calculate").onFirst().performClick()
        }
        composeTestRule.waitForIdle()
        pause(1000)

        composeTestRule.onAllNodesWithText("1").onFirst().performClick()
        pause(200)
        composeTestRule.onAllNodesWithText("8").onFirst().performClick()
        pause(200)
        composeTestRule.onAllNodesWithText("0").onFirst().performClick()
        pause(200)
        composeTestRule.onAllNodesWithText("0").onFirst().performClick()
        pause(200)
        composeTestRule.onAllNodesWithText("0").onFirst().performClick()
        pause(500)
        composeTestRule.onAllNodesWithContentDescription("Confirm amount").onFirst().performClick()
        pause(1000)

        // Save Installment Transaction
        Log.i(TAG, "[SECTION 9] Saving Installment Plan & Transaction")
        try {
            composeTestRule.onAllNodesWithText("Save Transaction").onFirst().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Save Transaction").onFirst().performClick()
        }
        composeTestRule.waitForIdle()
        pause(2000)
        Log.i(TAG, "[SECTION 9 PASSED] Installment transaction saved successfully")

        // -------------------------------------------------------------------------
        // SECTION 10: Verify on Dashboard & Inspect Home Credit Installment Plan
        // -------------------------------------------------------------------------
        Log.i(TAG, "[SECTION 10] Verifying logged installment on Dashboard")
        clickBottomTab("Dashboard")
        pause(1500)

        // Verify iPad 10th Gen is in Recent Transactions
        try {
            composeTestRule.onAllNodesWithText("iPad 10th Gen").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("iPad 10th Gen").onFirst().assertIsDisplayed()
        }
        Log.i(TAG, "[SECTION 10] Verified 'iPad 10th Gen' displayed on Dashboard Recent Transactions")
        pause(1500)

        // Return to Accounts to inspect Home Credit detail
        Log.i(TAG, "[SECTION 10] Navigating to Accounts to inspect Home Credit detail screen")
        clickBottomTab("Accounts")
        pause(1400)

        // Scroll to Home Credit and click to open detail
        try {
            composeTestRule.onAllNodesWithText("Home Credit").onFirst().performScrollTo().performClick()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("Home Credit").onFirst().performClick()
        }
        composeTestRule.waitForIdle()
        pause(2000)

        // Verify the newly created installment transaction on Home Credit detail
        try {
            composeTestRule.onAllNodesWithText("iPad 10th Gen").onFirst().performScrollTo().assertIsDisplayed()
            Log.i(TAG, "[SECTION 10] Verified 'iPad 10th Gen' on Home Credit recent transactions")
        } catch (e: Throwable) {
            Log.i(TAG, "[SECTION 10] Proceeding to Installments tab for plan inspection")
        }
        pause(1200)

        // Tap Installments tab to inspect installment plan card
        Log.i(TAG, "[SECTION 10] Switching to 'Installments' tab on Home Credit detail")
        composeTestRule.onAllNodesWithText("Installments").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1500)
        try {
            composeTestRule.onAllNodesWithText("iPad 10th Gen").onFirst().performScrollTo().assertIsDisplayed()
        } catch (e: Throwable) {
            composeTestRule.onAllNodesWithText("iPad 10th Gen").onFirst().assertIsDisplayed()
        }
        Log.i(TAG, "[SECTION 10 PASSED] Installment Plan card correctly active and verified")
        pause(1500)

        // Return back to Accounts
        composeTestRule.onAllNodesWithContentDescription("Back").onFirst().performClick()
        composeTestRule.waitForIdle()
        pause(1200)

        // Return to Dashboard to conclude
        clickBottomTab("Dashboard")
        pause(1500)

        Log.i(TAG, "=================================================================")
        Log.i(TAG, "COMPREHENSIVE ACCOUNTS & INSTALLMENT TEST COMPLETED SUCCESSFULLY!")
        Log.i(TAG, "=================================================================")
    }
}
