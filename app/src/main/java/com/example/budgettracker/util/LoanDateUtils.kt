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
     * Resolves the next upcoming due date given a list of monthly cycle days.
     * Automatically clamps cycle days to the valid length of the month (e.g. 30th in Feb -> 28/29).
     */
    fun calculateNextDueDate(
        today: LocalDate = LocalDate.now(),
        dueDays: List<Int>
    ): LocalDate {
        require(dueDays.isNotEmpty()) { "dueDays list must not be empty" }
        dueDays.forEach { require(it in 1..31) { "dueDay $it must be between 1 and 31" } }

        val currentYm = YearMonth.from(today)

        fun resolveDay(ym: YearMonth, targetDay: Int): LocalDate {
            val clampedDay = targetDay.coerceAtMost(ym.lengthOfMonth())
            return ym.atDay(clampedDay)
        }

        // Check candidate dates in current month
        val currentMonthCandidates = dueDays.map { resolveDay(currentYm, it) }.filter { !it.isBefore(today) }

        if (currentMonthCandidates.isNotEmpty()) {
            return currentMonthCandidates.minOrNull()!!
        }

        // All dates in current month passed; look at next month
        val nextYm = currentYm.plusMonths(1)
        val nextMonthCandidates = dueDays.map { resolveDay(nextYm, it) }
        return nextMonthCandidates.minOrNull()!!
    }

    /**
     * Overload for 1 or 2 cycle days.
     */
    fun calculateNextDueDate(
        today: LocalDate = LocalDate.now(),
        cycleDay1: Int,
        cycleDay2: Int? = null
    ): LocalDate = calculateNextDueDate(today, listOfNotNull(cycleDay1, cycleDay2))

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
