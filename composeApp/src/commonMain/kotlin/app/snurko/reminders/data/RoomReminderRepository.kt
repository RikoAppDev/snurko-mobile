package app.snurko.reminders.data

import app.snurko.core.data.localCall
import app.snurko.core.database.ReminderDao
import app.snurko.core.database.ReminderEntity
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import app.snurko.reminders.domain.Reminder
import app.snurko.reminders.domain.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime

class RoomReminderRepository(
    private val reminderDao: ReminderDao,
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> = reminderDao
        .observeAll()
        .map { reminders -> reminders.map { it.toReminder() } }
        .distinctUntilChanged()

    override suspend fun getReminders(): List<Reminder> =
        reminderDao.getAll().map { it.toReminder() }

    override suspend fun setReminderTime(
        habitId: Long,
        time: LocalTime?,
    ): EmptyResult<DataError.Local> = localCall {
        if (time == null) {
            reminderDao.delete(habitId)
        } else {
            reminderDao.upsert(ReminderEntity(habitId = habitId, minuteOfDay = time.toMinuteOfDay()))
        }
        Result.Success(Unit)
    }
}
