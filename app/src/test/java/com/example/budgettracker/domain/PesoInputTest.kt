package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PesoInputTest {

    @Test
    fun testParseBudgetPesoInput_explicitRequiredCases() {
        // 1. "1,5000" -> Invalid comma placement
        val result1 = PesoInput.parseBudgetPesoInput("1,5000")
        assertTrue("Expected Error for '1,5000'", result1 is BudgetInputResult.Error)
        assertEquals("Invalid comma placement", (result1 as BudgetInputResult.Error).message)

        // 2. "1.234" -> Maximum 2 decimal places allowed
        val result2 = PesoInput.parseBudgetPesoInput("1.234")
        assertTrue("Expected Error for '1.234'", result2 is BudgetInputResult.Error)
        assertEquals("Maximum 2 decimal places allowed", (result2 as BudgetInputResult.Error).message)

        // 3. ".5" -> Valid 50 centavos (50L)
        val result3 = PesoInput.parseBudgetPesoInput(".5")
        assertTrue("Expected Valid for '.5'", result3 is BudgetInputResult.Valid)
        assertEquals(50L, (result3 as BudgetInputResult.Valid).centavos)

        // 4. "5." -> Valid 500 centavos (500L)
        val result4 = PesoInput.parseBudgetPesoInput("5.")
        assertTrue("Expected Valid for '5.'", result4 is BudgetInputResult.Valid)
        assertEquals(500L, (result4 as BudgetInputResult.Valid).centavos)

        // 5. "0" -> Budget amount must be greater than ₱0
        val result5 = PesoInput.parseBudgetPesoInput("0")
        assertTrue("Expected Error for '0'", result5 is BudgetInputResult.Error)
        assertEquals("Budget amount must be greater than ₱0", (result5 as BudgetInputResult.Error).message)

        // 6. "0.00" -> Budget amount must be greater than ₱0
        val result6 = PesoInput.parseBudgetPesoInput("0.00")
        assertTrue("Expected Error for '0.00'", result6 is BudgetInputResult.Error)
        assertEquals("Budget amount must be greater than ₱0", (result6 as BudgetInputResult.Error).message)

        // 7. "  250  " -> Valid 25,000 centavos (25000L)
        val result7 = PesoInput.parseBudgetPesoInput("  250  ")
        assertTrue("Expected Valid for '  250  '", result7 is BudgetInputResult.Valid)
        assertEquals(25000L, (result7 as BudgetInputResult.Valid).centavos)

        // 8. "1,500.50" -> Valid 150,050 centavos (150050L)
        val result8 = PesoInput.parseBudgetPesoInput("1,500.50")
        assertTrue("Expected Valid for '1,500.50'", result8 is BudgetInputResult.Valid)
        assertEquals(150050L, (result8 as BudgetInputResult.Valid).centavos)

        // 9. Value above max ("100,000,000.01") -> Amount exceeds maximum allowed budget
        val result9 = PesoInput.parseBudgetPesoInput("100,000,000.01")
        assertTrue("Expected Error for value above max", result9 is BudgetInputResult.Error)
        assertEquals("Amount exceeds maximum allowed budget (₱100,000,000)", (result9 as BudgetInputResult.Error).message)

        val result9b = PesoInput.parseBudgetPesoInput("100,000,001")
        assertTrue("Expected Error for value above max", result9b is BudgetInputResult.Error)
        assertEquals("Amount exceeds maximum allowed budget (₱100,000,000)", (result9b as BudgetInputResult.Error).message)
    }

    @Test
    fun testParseBudgetPesoInput_boundaryAndFormatCases() {
        // Exact max allowed: 100,000,000.00 -> 10,000,000,000L
        val maxResult = PesoInput.parseBudgetPesoInput("100,000,000.00")
        assertTrue(maxResult is BudgetInputResult.Valid)
        assertEquals(100_000_000_00L, (maxResult as BudgetInputResult.Valid).centavos)

        // Single digit centavo fraction
        val singleDigitCents = PesoInput.parseBudgetPesoInput("10.5")
        assertTrue(singleDigitCents is BudgetInputResult.Valid)
        assertEquals(1050L, (singleDigitCents as BudgetInputResult.Valid).centavos)

        // Empty / Whitespace
        val emptyResult = PesoInput.parseBudgetPesoInput("")
        assertTrue(emptyResult is BudgetInputResult.Error)
        assertEquals("Amount cannot be empty", (emptyResult as BudgetInputResult.Error).message)

        // Negative values
        val negativeResult = PesoInput.parseBudgetPesoInput("-50")
        assertTrue(negativeResult is BudgetInputResult.Error)
        assertEquals("Amount cannot be negative", (negativeResult as BudgetInputResult.Error).message)

        // Invalid characters
        val invalidChars = PesoInput.parseBudgetPesoInput("₱500")
        assertTrue(invalidChars is BudgetInputResult.Error)
        assertEquals("Amount contains invalid characters", (invalidChars as BudgetInputResult.Error).message)

        // Multiple dots
        val multiDot = PesoInput.parseBudgetPesoInput("12.34.56")
        assertTrue(multiDot is BudgetInputResult.Error)
        assertEquals("Amount cannot have multiple decimal points", (multiDot as BudgetInputResult.Error).message)

        // Comma in decimal part
        val commaInDecimal = PesoInput.parseBudgetPesoInput("100.5,0")
        assertTrue(commaInDecimal is BudgetInputResult.Error)
        assertEquals("Invalid comma placement", (commaInDecimal as BudgetInputResult.Error).message)
    }

    @Test
    fun testFormatPesoInput_preservesTypingFlow() {
        assertEquals("1,500", PesoInput.formatPesoInput("1500"))
        assertEquals("1,500.", PesoInput.formatPesoInput("1500."))
        assertEquals("1,500.5", PesoInput.formatPesoInput("1500.5"))
        assertEquals("1,500.50", PesoInput.formatPesoInput("1500.50"))
        assertEquals("1,500.50", PesoInput.formatPesoInput("1500.509"))
        assertEquals("0", PesoInput.formatPesoInput("0"))
        assertEquals("100,000,000", PesoInput.formatPesoInput("100000000"))
    }
}
