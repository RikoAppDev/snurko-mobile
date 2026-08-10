package app.snurko.reminders.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalTime

class FakeReminderRepository : ReminderRepository {

    data class SetCall(val habitId: Long, val time: LocalTime?)

    private val reminders = MutableStateFlow<List<Reminder>>(emptyList())

    val setCalls = mutableListOf<SetCall>()

    var setResult: EmptyResult<DataError.Local> = Result.Success(Unit)

    fun setReminders(value: List<Reminder>) {
        reminders.value = value
    }

    override fun observeReminders(): Flow<List<Reminder>> = reminders

    override suspend fun getReminders(): List<Reminder> = reminders.value

    override suspend fun setReminderTime(habitId: Long, time: LocalTime?): EmptyResult<DataError.Local> {
        setCalls += SetCall(habitId, time)
        if (setResult is Result.Error) return setResult
        reminders.update { current ->
            val without = current.filterNot { it.habitId == habitId }
            if (time == null) without else (without + Reminder(habitId, time)).sortedBy { it.habitId }
        }
        return setResult
    }
}

class FakeReminderScheduler(
    override var rechecksAtFireTime: Boolean = true,
) : ReminderScheduler {

    val appliedSchedules = mutableListOf<ReminderSchedule>()

    val lastSchedule: ReminderSchedule
        get() = appliedSchedules.last()

    override suspend fun applySchedule(schedule: ReminderSchedule) {
        appliedSchedules += schedule
    }
}
