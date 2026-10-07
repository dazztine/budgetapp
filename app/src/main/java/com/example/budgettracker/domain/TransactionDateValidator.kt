package com.example.budgettracker.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed class TransactionDateValidationResult {
    object Valid : TransactionDateValidationResult()
    data class ValidWithWarning(val warningMessage: String) : TransactionDateValidationResult()
    data class FutureDateRejected(val errorMessage: String) : TransactionDateValidationResult()
}

object TransactionDateValidator {
    val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("Asia/Manila")

    /**
     * Validates whether a given timestamp (in epoch millis) is permissible for a transaction.
     * Rules:
     * - Default is today in Asia/Manila.
     * - Past dates are allowed.
     * - Future dates (after today's local date in Asia/Manila) are rejected with an error message.
     * - Dates older than 365 days produce a non-blocking warning.
     */
    fun validate(
        timestampMillis: Long,
        clock: Clock = Clock.system(DEFAULT_ZONE_ID)
    ): TransactionDateValidationResult {
        val today = LocalDate.now(clock)
        val selectedDate = Instant.ofEpochMilli(timestampMillis)
            .atZone(clock.zone)
            .toLocalDate()

        if (selectedDate.isAfter(today)) {
            return TransactionDateValidationResult.FutureDateRejected("Future dates are not allowed for transactions")
        }

        val daysBetween = ChronoUnit.DAYS.between(selectedDate, today)
        if (daysBetween > 365) {
            return TransactionDateValidationResult.ValidWithWarning("Transaction date is more than 1 year in the past")
        }

        return TransactionDateValidationResult.Valid
    }

    /**
     * Combines a chosen LocalDate with the original timestamp's time-of-day
     * within the specified zone (default Asia/Manila).
     */
    fun combineDateWithOriginalTime(
        newDate: LocalDate,
        originalTimestampMillis: Long,
        zoneId: ZoneId = DEFAULT_ZONE_ID
    ): Long {
        val originalZdt = Instant.ofEpochMilli(originalTimestampMillis).atZone(zoneId)
        val localTime = originalZdt.toLocalTime()
        return newDate.atTime(localTime).atZone(zoneId).toInstant().toEpochMilli()
    }
}
