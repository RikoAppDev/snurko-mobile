package app.snurko.core.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun interface DateProvider {
    fun now(): LocalDateTime

    fun today(): LocalDate = now().date
}

class SystemDateProvider : DateProvider {
    override fun now(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}
