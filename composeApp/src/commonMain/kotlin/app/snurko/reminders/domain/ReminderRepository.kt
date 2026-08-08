package app.snurko.reminders.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

interface ReminderRepository {

    fun observeReminders(): Flow<List<Reminder>>

    suspend fun getReminders(): List<Reminder>

    /** A `null` [time] switches the habit's reminder off. */
    suspend fun setReminderTime(habitId: Long, time: LocalTime?): EmptyResult<DataError.Local>
}
