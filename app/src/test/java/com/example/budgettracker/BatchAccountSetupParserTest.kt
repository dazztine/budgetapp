package com.example.budgettracker

import com.example.budgettracker.data.model.AccountType
import com.example.budgettracker.parser.BatchAccountSetupParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchAccountSetupParserTest {

    @Test
    fun testParseMultipleAccountsParagraph() {
        val text = "GCash 3000, BDO savings 15000, SPayLater 2000 due on the 15th"
        val result = BatchAccountSetupParser.parse(text)

        assertFalse(result.hasErrors)
        assertEquals(3, result.accounts.size)

        assertEquals("GCash", result.accounts[0].name)
        assertEquals(AccountType.E_WALLET, result.accounts[0].type)
        assertEquals(300_000L, result.accounts[0].initialBalanceCentavos)

        assertEquals("BDO Savings", result.accounts[1].name)
        assertEquals(AccountType.BANK, result.accounts[1].type)
        assertEquals(1_500_000L, result.accounts[1].initialBalanceCentavos)

        assertEquals("SPayLater", result.accounts[2].name)
        assertEquals(AccountType.BNPL, result.accounts[2].type)
        assertEquals(200_000L, result.accounts[2].initialBalanceCentavos)
        assertEquals(15, result.accounts[2].cycleDay1)
    }

    @Test
    fun testSetupSentenceExtraction() {
        val text = "my current gcash is at 120 pesos"
        val result = BatchAccountSetupParser.parse(text)

        assertEquals(1, result.accounts.size)
        assertEquals("GCash", result.accounts[0].name)
        assertEquals(12_000L, result.accounts[0].initialBalanceCentavos)
        assertEquals(AccountType.E_WALLET, result.accounts[0].type)
    }

    @Test
    fun testCentavoConversionVariants() {
        val text = "GCash 3000, BDO 15k, Maya 250.50"
        val result = BatchAccountSetupParser.parse(text)

        assertEquals(3, result.accounts.size)
        assertEquals(300_000L, result.accounts[0].initialBalanceCentavos)
        assertEquals(1_500_000L, result.accounts[1].initialBalanceCentavos)
        assertEquals(25_050L, result.accounts[2].initialBalanceCentavos)
        assertEquals(1_825_050L, result.totalBalanceCentavos)
    }

    @Test
    fun testEmptyInputHandling() {
        val emptyResult = BatchAccountSetupParser.parse("")
        assertTrue(emptyResult.accounts.isEmpty())
        assertEquals(0L, emptyResult.totalBalanceCentavos)

        val wsResult = BatchAccountSetupParser.parse("   \n\t   ")
        assertTrue(wsResult.accounts.isEmpty())
    }

    @Test
    fun testNoiseAndPunctuationStripping() {
        val text = "₱1,500.50! Maya???"
        val result = BatchAccountSetupParser.parse(text)

        assertEquals(1, result.accounts.size)
        assertEquals(150_050L, result.accounts[0].initialBalanceCentavos)
        assertEquals("Maya", result.accounts[0].name)
        assertEquals(AccountType.E_WALLET, result.accounts[0].type)
    }
}
