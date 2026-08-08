package app.snurko.core.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

fun nextMidnightAfter(now: LocalDateTime): LocalDateTime =
    now.date.plus(1, DateTimeUnit.DAY).atTime(hour = 0, minute = 0)

/** Wakes the app when the local date turns over, so day-bound surfaces stop showing yesterday. */
interface DayRolloverScheduler {

    suspend fun scheduleNextRollover()
}
