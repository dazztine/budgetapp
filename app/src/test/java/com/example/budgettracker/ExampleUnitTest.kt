package com.example.budgettracker

import com.example.budgettracker.util.CurrencyUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testFormatAmountInput_emptyAndZeros() {
        assertEquals("", CurrencyUtils.formatAmountInput(""))
        assertEquals("", CurrencyUtils.formatAmountInput("   "))
        assertEquals("0", CurrencyUtils.formatAmountInput("0"))
        assertEquals("0", CurrencyUtils.formatAmountInput("00"))
        assertEquals("0", CurrencyUtils.formatAmountInput("000"))
    }

    @Test
    fun testFormatAmountInput_integerFormatting() {
        assertEquals("1", CurrencyUtils.formatAmountInput("1"))
        assertEquals("12", CurrencyUtils.formatAmountInput("12"))
        assertEquals("123", CurrencyUtils.formatAmountInput("123"))
        assertEquals("1,234", CurrencyUtils.formatAmountInput("1234"))
        assertEquals("12,345", CurrencyUtils.formatAmountInput("12345"))
        assertEquals("123,456", CurrencyUtils.formatAmountInput("123456"))
        assertEquals("1,234,567", CurrencyUtils.formatAmountInput("1234567"))
    }

    @Test
    fun testFormatAmountInput_decimalSupport() {
        assertEquals("0.", CurrencyUtils.formatAmountInput("."))
        assertEquals("1,500.", CurrencyUtils.formatAmountInput("1500."))
        assertEquals("1,500.5", CurrencyUtils.formatAmountInput("1500.5"))
        assertEquals("1,500.50", CurrencyUtils.formatAmountInput("1500.50"))
        // Discards beyond 2 decimal places
        assertEquals("1,500.50", CurrencyUtils.formatAmountInput("1500.509"))
        // Discards second dot
        assertEquals("1,500.50", CurrencyUtils.formatAmountInput("1500.5.0"))
    }

    @Test
    fun testFormatAmountInput_cleanInvalidCharactersAndCommas() {
        assertEquals("1,500.50", CurrencyUtils.formatAmountInput("₱1,500.50"))
        assertEquals("2,500", CurrencyUtils.formatAmountInput("2,500"))
        assertEquals("1,000,000", CurrencyUtils.formatAmountInput("1,000,000"))
    }

    @Test
    fun testParseInputToCentavos_consistencyWithFormattedInput() {
        val input = "1,500.50"
        assertEquals(150050L, CurrencyUtils.parseInputToCentavos(input))

        val inputZero = ""
        assertEquals(0L, CurrencyUtils.parseInputToCentavos(inputZero))

        val inputDot = "0."
        assertEquals(0L, CurrencyUtils.parseInputToCentavos(inputDot))
    }
}