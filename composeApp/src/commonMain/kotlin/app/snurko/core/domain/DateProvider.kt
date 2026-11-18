package app.snurko.core.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration

fun interface DateProvider {
    fun now(): LocalDateTime

    fun today(): LocalDate = now().date
}

class SystemDateProvider : DateProvider {
    override fun now(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

/** Today's date, emitted again whenever a check after [every] finds the calendar has turned. */
fun DateProvider.dates(every: Duration): Flow<LocalDate> = flow {
    while (true) {
        emit(today())
        delay(every)
    }
}.distinctUntilChanged()
