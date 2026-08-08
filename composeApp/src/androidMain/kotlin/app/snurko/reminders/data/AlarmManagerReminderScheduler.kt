package app.snurko.reminders.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.snurko.reminders.domain.ReminderSchedule
import app.snurko.reminders.domain.ReminderScheduler
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

const val ACTION_REMINDER_DUE = "app.snurko.action.REMINDER_DUE"
const val EXTRA_HABIT_ID = "app.snurko.extra.HABIT_ID"

class AlarmManagerReminderScheduler(
    private val context: Context,
) : ReminderScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun applySchedule(schedule: ReminderSchedule) {
        schedule.cancelledHabitIds.forEach { habitId ->
            existingAlarm(habitId)?.let { alarm ->
                alarmManager.cancel(alarm)
                alarm.cancel()
            }
        }
        schedule.scheduled.forEach { reminder ->
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.fireAt.toEpochMilliseconds(),
                alarm(reminder.habitId, PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
    }

    private fun existingAlarm(habitId: Long): PendingIntent? =
        alarm(habitId, PendingIntent.FLAG_NO_CREATE)

    private fun alarm(habitId: Long, flags: Int) = PendingIntent.getBroadcast(
        context,
        habitId.toInt(),
        Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(ACTION_REMINDER_DUE)
            .putExtra(EXTRA_HABIT_ID, habitId),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )
}

fun LocalDateTime.toEpochMilliseconds(): Long =
    toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
