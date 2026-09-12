package com.example.budgettracker

import com.example.budgettracker.util.LoanDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoanDateUtilsTest {

    @Test
    fun testSameMonthUpcomingDueDate() {
        val today = LocalDate.of(2026, 3, 10)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2026, 3, 15), nextDue)
    }

    @Test
    fun testDueDateIsToday() {
        val today = LocalDate.of(2026, 3, 15)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2026, 3, 15), nextDue)
    }

    @Test
    fun testSecondCycleDaySameMonth() {
        val today = LocalDate.of(2026, 3, 16)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2026, 3, 30), nextDue)
    }

    @Test
    fun testNextMonthRollover() {
        val today = LocalDate.of(2026, 3, 31)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2026, 4, 15), nextDue)
    }

    @Test
    fun testFebruaryClampingNonLeapYear() {
        // 2023 is not a leap year -> February has 28 days
        val today = LocalDate.of(2023, 2, 16)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2023, 2, 28), nextDue)
    }

    @Test
    fun testFebruaryClampingLeapYear() {
        // 2024 is a leap year -> February has 29 days
        val today = LocalDate.of(2024, 2, 16)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 30)
        assertEquals(LocalDate.of(2024, 2, 29), nextDue)
    }

    @Test
    fun test30DayMonthClampingFor31st() {
        // April has 30 days
        val today = LocalDate.of(2026, 4, 1)
        val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 31, cycleDay2 = null)
        assertEquals(LocalDate.of(2026, 4, 30), nextDue)
    }

    @Test
    fun testIsDueSoon() {
        val today = LocalDate.of(2026, 3, 12)
        val dueDate = LocalDate.of(2026, 3, 15)

        assertTrue(LoanDateUtils.isDueSoon(dueDate, today, daysBefore = 3))
        assertTrue(LoanDateUtils.isDueSoon(dueDate, today, daysBefore = 5))
        assertFalse(LoanDateUtils.isDueSoon(dueDate, today, daysBefore = 2))
    }

    @Test
    fun testIsDueSoonDefaultSevenDays() {
        val today = LocalDate.of(2026, 3, 15)
        val dueDate7Days = LocalDate.of(2026, 3, 22)
        val dueDate8Days = LocalDate.of(2026, 3, 23)
        val dueDateOverdue = LocalDate.of(2026, 3, 14)

        // Default daysBefore is 7
        assertTrue(LoanDateUtils.isDueSoon(dueDate7Days, today))
        assertFalse(LoanDateUtils.isDueSoon(dueDate8Days, today))
        assertTrue(LoanDateUtils.isDueSoon(dueDateOverdue, today))
    }

    @Test
    fun testDueDateStatusTransitions() {
        val today = LocalDate.of(2026, 3, 15)

        // Overdue: past due date
        val overdueDate = LocalDate.of(2026, 3, 14)
        assertEquals(com.example.budgettracker.util.DueDateStatus.OVERDUE, LoanDateUtils.getDueDateStatus(overdueDate, today))
        assertEquals(com.example.budgettracker.util.DueDateStatus.OVERDUE, com.example.budgettracker.util.getDueDateStatus(overdueDate, today))

        // Due soon: today (0 days) through 7 days
        val dueToday = LocalDate.of(2026, 3, 15)
        assertEquals(com.example.budgettracker.util.DueDateStatus.DUE_SOON, LoanDateUtils.getDueDateStatus(dueToday, today))

        val dueIn7Days = LocalDate.of(2026, 3, 22)
        assertEquals(com.example.budgettracker.util.DueDateStatus.DUE_SOON, LoanDateUtils.getDueDateStatus(dueIn7Days, today))

        // Upcoming: 8 or more days
        val dueIn8Days = LocalDate.of(2026, 3, 23)
        assertEquals(com.example.budgettracker.util.DueDateStatus.UPCOMING, LoanDateUtils.getDueDateStatus(dueIn8Days, today))

        val dueIn30Days = LocalDate.of(2026, 4, 14)
        assertEquals(com.example.budgettracker.util.DueDateStatus.UPCOMING, LoanDateUtils.getDueDateStatus(dueIn30Days, today))
    }

    @Test
    fun testLeapYearsFebruaryClamping2024And2028() {
        val leapYears = listOf(2024, 2028)
        for (year in leapYears) {
            val today = LocalDate.of(year, 2, 1)
            for (cycleDay in listOf(29, 30, 31)) {
                val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = cycleDay)
                assertEquals("Year $year cycleDay $cycleDay should clamp to Feb 29", LocalDate.of(year, 2, 29), nextDue)
            }
        }
    }

    @Test
    fun testNonLeapYearsFebruaryClamping2023_2025_2026_2100() {
        val nonLeapYears = listOf(2023, 2025, 2026, 2100)
        for (year in nonLeapYears) {
            val today = LocalDate.of(year, 2, 1)
            for (cycleDay in listOf(29, 30, 31)) {
                val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = cycleDay)
                assertEquals("Year $year cycleDay $cycleDay should clamp to Feb 28", LocalDate.of(year, 2, 28), nextDue)
            }
        }
    }

    @Test
    fun testAllThirtyDayMonthsClampingFor31st() {
        val thirtyDayMonths = listOf(4, 6, 9, 11) // April, June, September, November
        for (month in thirtyDayMonths) {
            val today = LocalDate.of(2026, month, 1)
            val nextDue = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 31)
            assertEquals("Month $month with cycleDay 31 should clamp to 30th", LocalDate.of(2026, month, 30), nextDue)
        }
    }

    @Test
    fun testYearBoundaryRolloverDecToJan() {
        val today = LocalDate.of(2025, 12, 31)

        // Single cycle day that passed in December
        val nextDueSingle = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 5)
        assertEquals(LocalDate.of(2026, 1, 5), nextDueSingle)

        // Dual cycle day with both passed in December
        val nextDueDual = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 20, cycleDay2 = 5)
        assertEquals(LocalDate.of(2026, 1, 5), nextDueDual)

        // Cycle day is today (31st) -> returns today
        val nextDueToday = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 31)
        assertEquals(LocalDate.of(2025, 12, 31), nextDueToday)

        // Cycle day 1 is passed (15th), cycle day 2 is today (31st)
        val nextDueDualToday = LoanDateUtils.calculateNextDueDate(today, cycleDay1 = 15, cycleDay2 = 31)
        assertEquals(LocalDate.of(2025, 12, 31), nextDueDualToday)
    }

    @Test
    fun testDualCycleDaysBoundaryScenarios() {
        // cycleDay1 passed, cycleDay2 is today
        val today1 = LocalDate.of(2026, 5, 15)
        val nextDueToday = LoanDateUtils.calculateNextDueDate(today1, cycleDay1 = 5, cycleDay2 = 15)
        assertEquals(LocalDate.of(2026, 5, 15), nextDueToday)

        // cycleDay1 passed, cycleDay2 is upcoming
        val today2 = LocalDate.of(2026, 5, 10)
        val nextDueUpcoming = LoanDateUtils.calculateNextDueDate(today2, cycleDay1 = 5, cycleDay2 = 20)
        assertEquals(LocalDate.of(2026, 5, 20), nextDueUpcoming)

        // Inverted cycleDay order: cycleDay1 = 25, cycleDay2 = 5
        val nextDueInvertedToday5 = LoanDateUtils.calculateNextDueDate(LocalDate.of(2026, 5, 5), cycleDay1 = 25, cycleDay2 = 5)
        assertEquals(LocalDate.of(2026, 5, 5), nextDueInvertedToday5)

        val nextDueInvertedUpcoming25 = LoanDateUtils.calculateNextDueDate(LocalDate.of(2026, 5, 10), cycleDay1 = 25, cycleDay2 = 5)
        assertEquals(LocalDate.of(2026, 5, 25), nextDueInvertedUpcoming25)

        val nextDueInvertedBothPassed = LoanDateUtils.calculateNextDueDate(LocalDate.of(2026, 5, 26), cycleDay1 = 25, cycleDay2 = 5)
        assertEquals(LocalDate.of(2026, 6, 5), nextDueInvertedBothPassed)
    }

    @Test
    fun testDueDateStatusExhaustiveWindow() {
        val today = LocalDate.of(2026, 6, 15)

        val testCases = listOf(
            -10L to com.example.budgettracker.util.DueDateStatus.OVERDUE,
            -1L to com.example.budgettracker.util.DueDateStatus.OVERDUE,
            0L to com.example.budgettracker.util.DueDateStatus.DUE_SOON,
            1L to com.example.budgettracker.util.DueDateStatus.DUE_SOON,
            6L to com.example.budgettracker.util.DueDateStatus.DUE_SOON,
            7L to com.example.budgettracker.util.DueDateStatus.DUE_SOON,
            8L to com.example.budgettracker.util.DueDateStatus.UPCOMING,
            9L to com.example.budgettracker.util.DueDateStatus.UPCOMING,
            30L to com.example.budgettracker.util.DueDateStatus.UPCOMING
        )

        for ((dayOffset, expectedStatus) in testCases) {
            val targetDate = today.plusDays(dayOffset)
            assertEquals("Offset $dayOffset should produce $expectedStatus", expectedStatus, LoanDateUtils.getDueDateStatus(targetDate, today))
            val expectedDueSoon = (expectedStatus == com.example.budgettracker.util.DueDateStatus.OVERDUE || expectedStatus == com.example.budgettracker.util.DueDateStatus.DUE_SOON)
            assertEquals("Offset $dayOffset isDueSoon mismatch", expectedDueSoon, LoanDateUtils.isDueSoon(targetDate, today))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCycleDay1ZeroThrows() {
        LoanDateUtils.calculateNextDueDate(LocalDate.now(), cycleDay1 = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCycleDay1ThirtyTwoThrows() {
        LoanDateUtils.calculateNextDueDate(LocalDate.now(), cycleDay1 = 32)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCycleDay2ZeroThrows() {
        LoanDateUtils.calculateNextDueDate(LocalDate.now(), cycleDay1 = 15, cycleDay2 = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCycleDay2ThirtyTwoThrows() {
        LoanDateUtils.calculateNextDueDate(LocalDate.now(), cycleDay1 = 15, cycleDay2 = 32)
    }
}

