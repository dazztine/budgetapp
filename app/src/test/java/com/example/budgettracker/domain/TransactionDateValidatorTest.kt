package com.example.budgettracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class TransactionDateValidatorTest {

    private val manilaZone = ZoneId.of("Asia/Manila")

    private fun fixedClock(dateTimeStr: String, zoneId: ZoneId = manilaZone): Clock {
        val zonedDateTime = java.time.ZonedDateTime.parse(dateTimeStr)
        return Clock.fixed(zonedDateTime.toInstant(), zoneId)
    }

    @Test
    fun testTodayIsAllowed() {
        val clock = fixedClock("2026-10-07T12:00:00+08:00")
        val timestamp = clock.millis()

        val result = TransactionDateValidator.validate(timestamp, clock)
        assertEquals(TransactionDateValidationResult.Valid, result)
    }

    @Test
    fun testYesterdayIsAllowed() {
        val clock = fixedClock("2026-10-07T12:00:00+08:00")
        val yesterdayMillis = Instant.parse("2026-10-06T12:00:00Z").toEpochMilli()

        val result = TransactionDateValidator.validate(yesterdayMillis, clock)
        assertEquals(TransactionDateValidationResult.Valid, result)
    }

    @Test
    fun testFutureDateIsRejected() {
        val clock = fixedClock("2026-10-07T12:00:00+08:00")
        // Tomorrow in Asia/Manila: 2026-10-08
        val tomorrowMillis = Instant.parse("2026-10-08T00:00:00Z").toEpochMilli()

        val result = TransactionDateValidator.validate(tomorrowMillis, clock)
        assertTrue(result is TransactionDateValidationResult.FutureDateRejected)
        val rejected = result as TransactionDateValidationResult.FutureDateRejected
        assertEquals("Future dates are not allowed for transactions", rejected.errorMessage)
    }

    @Test
    fun testPast365DaysProducesNoWarning() {
        val clock = fixedClock("2026-10-07T12:00:00+08:00")
        val date365DaysAgo = LocalDate.of(2025, 10, 7) // exactly 365 days
        val millis = date365DaysAgo.atStartOfDay(manilaZone).toInstant().toEpochMilli()

        val result = TransactionDateValidator.validate(millis, clock)
        assertEquals(TransactionDateValidationResult.Valid, result)
    }

    @Test
    fun testPastMoreThan365DaysProducesWarning() {
        val clock = fixedClock("2026-10-07T12:00:00+08:00")
        val date366DaysAgo = LocalDate.of(2025, 10, 6) // 366 days
        val millis = date366DaysAgo.atStartOfDay(manilaZone).toInstant().toEpochMilli()

        val result = TransactionDateValidator.validate(millis, clock)
        assertTrue(result is TransactionDateValidationResult.ValidWithWarning)
        val warning = result as TransactionDateValidationResult.ValidWithWarning
        assertEquals("Transaction date is more than 1 year in the past", warning.warningMessage)
    }

    @Test
    fun testLeapDayHandling() {
        // Leap year 2024: Feb 29
        val clock = fixedClock("2024-03-01T10:00:00+08:00")
        val leapDay = LocalDate.of(2024, 2, 29)
        val millis = leapDay.atStartOfDay(manilaZone).toInstant().toEpochMilli()

        val result = TransactionDateValidator.validate(millis, clock)
        assertEquals(TransactionDateValidationResult.Valid, result)
    }

    @Test
    fun testAsiaManilaBoundaryAtMidnight() {
        // At 2026-10-07 00:01:00 in Manila (+08:00), UTC is 2026-10-06 16:01:00
        val clock = fixedClock("2026-10-07T00:01:00+08:00")
        val timestamp = clock.millis()

        val result = TransactionDateValidator.validate(timestamp, clock)
        assertEquals(TransactionDateValidationResult.Valid, result)

        // 23:59:59 yesterday in Manila is allowed
        val yesterdayEnd = LocalDate.of(2026, 10, 6).atTime(23, 59, 59).atZone(manilaZone).toInstant().toEpochMilli()
        assertEquals(TransactionDateValidationResult.Valid, TransactionDateValidator.validate(yesterdayEnd, clock))
    }

    @Test
    fun testCombineDateWithOriginalTimePreservesTime() {
        val originalInstant = Instant.parse("2026-10-07T14:35:22+08:00")
        val originalMillis = originalInstant.toEpochMilli()

        val targetDate = LocalDate.of(2026, 9, 15)
        val combinedMillis = TransactionDateValidator.combineDateWithOriginalTime(targetDate, originalMillis, manilaZone)

        val combinedZdt = Instant.ofEpochMilli(combinedMillis).atZone(manilaZone)
        assertEquals(targetDate, combinedZdt.toLocalDate())
        assertEquals(14, combinedZdt.hour)
        assertEquals(35, combinedZdt.minute)
        assertEquals(22, combinedZdt.second)
    }
}
