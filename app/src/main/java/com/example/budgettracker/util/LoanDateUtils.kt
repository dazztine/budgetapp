package com.example.budgettracker.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

enum class DueDateStatus {
    OVERDUE,
    DUE_SOON,
    UPCOMING
}

fun getDueDateStatus(dueDate: LocalDate, today: LocalDate = LocalDate.now()): DueDateStatus =
    LoanDateUtils.getDueDateStatus(dueDate, today)

object LoanDateUtils {

    /**
     * Resolves the next upcoming due date given 1 or 2 monthly cycle days.
     * Automatically clamps cycle days to the valid length of the month (e.g. 30th in Feb -> 28/29).
     */
    fun calculateNextDueDate(
        today: LocalDate = LocalDate.now(),
        cycleDay1: Int,
        cycleDay2: Int? = null
    ): LocalDate {
        require(cycleDay1 in 1..31) { "cycleDay1 must be between 1 and 31" }
        if (cycleDay2 != null) {
            require(cycleDay2 in 1..31) { "cycleDay2 must be between 1 and 31" }
        }

        val currentYm = YearMonth.from(today)

        fun resolveDay(ym: YearMonth, targetDay: Int): LocalDate {
            val clampedDay = targetDay.coerceAtMost(ym.lengthOfMonth())
            return ym.atDay(clampedDay)
        }

        // Check candidate dates in current month
        val currentMonthCandidates = listOfNotNull(
            resolveDay(currentYm, cycleDay1),
            cycleDay2?.let { resolveDay(currentYm, it) }
        ).filter { !it.isBefore(today) }

        if (currentMonthCandidates.isNotEmpty()) {
            return currentMonthCandidates.minOrNull()!!
        }

        // All dates in current month passed; look at next month
        val nextYm = currentYm.plusMonths(1)
        val nextMonthCandidates = listOfNotNull(
            resolveDay(nextYm, cycleDay1),
            cycleDay2?.let { resolveDay(nextYm, it) }
        )
        return nextMonthCandidates.minOrNull()!!
    }

    /**
     * Checks whether a due date falls within the reminder threshold.
     */
    fun isDueSoon(
        dueDate: LocalDate,
        today: LocalDate = LocalDate.now(),
        daysBefore: Int = 7
    ): Boolean {
        if (dueDate.isBefore(today)) return true // Overdue
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
        return daysUntilDue <= daysBefore
    }

    /**
     * Determines due date status category: OVERDUE, DUE_SOON (<= 7 days), or UPCOMING (> 7 days).
     */
    fun getDueDateStatus(
        dueDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): DueDateStatus {
        val days = ChronoUnit.DAYS.between(today, dueDate)
        return when {
            days < 0 -> DueDateStatus.OVERDUE
            days in 0..7 -> DueDateStatus.DUE_SOON
            else -> DueDateStatus.UPCOMING
        }
    }
}
