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
}
