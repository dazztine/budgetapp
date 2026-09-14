package com.example.budgettracker

import com.example.budgettracker.data.local.entity.AccountEntity
import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.data.model.TransactionType
import com.example.budgettracker.parser.SingleTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SingleTransactionParserTest {

    private val knownAccounts = listOf(
        AccountEntity(id = 1, name = "GCash", type = AccountType.E_WALLET),
        AccountEntity(id = 2, name = "BDO", type = AccountType.BANK),
        AccountEntity(id = 3, name = "BPI", type = AccountType.BANK),
        AccountEntity(id = 4, name = "Cash", type = AccountType.CASH),
        AccountEntity(id = 5, name = "Maya", type = AccountType.E_WALLET)
    )

    @Test
    fun testParseSimpleExpense() {
        val sentence = "150 gcash lunch"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(15_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
        assertEquals("Food & Dining", result.category)
        assertEquals("Lunch", result.title)
    }

    @Test
    fun testParseTagalogIncome() {
        val sentence = "sahod 25000 bpi"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(2_500_000L, result.amountCentavos)
        assertEquals("BPI", result.accountName)
        assertEquals("Salary", result.category)
    }

    @Test
    fun testParseTagalogTransportationExpense() {
        val sentence = "pamasahe 45 cash"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(4_500L, result.amountCentavos)
        assertEquals("Cash", result.accountName)
        assertEquals("Transportation", result.category)
    }

    @Test
    fun testParseFundTransfer() {
        val sentence = "transfer 2000 from bpi to maya"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.TRANSFER, result.type)
        assertEquals(200_000L, result.amountCentavos)
        assertEquals("BPI", result.accountName)
        assertEquals("Maya", result.toAccountName)
        assertEquals("Transfer", result.category)
    }

    @Test
    fun testUserBugFixGCashThat() {
        // Must lock to canonical "GCash" and NEVER create/return "GCash that"
        val sentence = "GCash that 500"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(50_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
    }

    @Test
    fun testPillar1AffixStripping() {
        val sentence = "nag-Grab 320 via gcash"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(32_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
        assertEquals("Transportation", result.category)
        assertEquals("Grab", result.title)
    }

    @Test
    fun testPillar2MultiplierNormalization() {
        val sentence = "sahod 35k bpi"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.INCOME, result.type)
        assertEquals(3_500_000L, result.amountCentavos)
        assertEquals("BPI", result.accountName)
        assertEquals("Salary", result.category)
    }

    @Test
    fun testPillar3MarkerGrammarTransfer() {
        val sentence = "lipat 500 gcash to maya"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.TRANSFER, result.type)
        assertEquals(50_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
        assertEquals("Maya", result.toAccountName)
    }

    @Test
    fun testPillar4ConversationalTagalog() {
        val sentence = "nagbayad ako 1500 sa meralco gamit maya"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(150_000L, result.amountCentavos)
        assertEquals("Maya", result.accountName)
        assertEquals("Bills & Utilities", result.category)
        assertEquals("Meralco", result.title)
    }

    @Test
    fun testPillar5VoiceTypoFuzzyTolerance() {
        val sentence = "jolibee 250 gecash"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(25_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
        assertEquals("Food & Dining", result.category)
    }

    @Test
    fun testPillar6ResidualTitleRetention() {
        val sentence = "Dinner 450 with Sarah GCash"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(45_000L, result.amountCentavos)
        assertEquals("GCash", result.accountName)
        assertEquals("Food & Dining", result.category)
        assertEquals("Dinner With Sarah", result.title)
    }

    @Test
    fun testPillarInstallmentBNPL() {
        val sentence = "bumili ako s24 3500/mo spaylater 6 months"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.INSTALLMENT, result.type)
        assertEquals(2_100_000L, result.amountCentavos)
        assertEquals("SPayLater", result.accountName)
        assertEquals(6, result.totalInstallments)
        assertEquals("S24", result.title)
    }

    @Test
    fun testUnmatchedAccountDoesNotDefaultToExisting() {
        val sentence = "meron akong 200 sa maribank"
        val result = SingleTransactionParser.parse(sentence, knownAccounts)

        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(20_000L, result.amountCentavos)
        org.junit.Assert.assertNull("Account should be null when unmatched", result.accountName)
        assertEquals("Maribank", result.title)
    }
}
