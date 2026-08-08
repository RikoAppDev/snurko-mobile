package app.snurko.core.android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.DayRolloverScheduler
import app.snurko.core.domain.nextMidnightAfter
import app.snurko.reminders.data.toEpochMilliseconds

const val ACTION_DAY_ROLLOVER = "app.snurko.action.DAY_ROLLOVER"

private const val ROLLOVER_REQUEST_CODE = 1_000_000

class AlarmManagerDayRolloverScheduler(
    private val context: Context,
    private val dateProvider: DateProvider,
) : DayRolloverScheduler {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun scheduleNextRollover() {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnightAfter(dateProvider.now()).toEpochMilliseconds(),
            rolloverAlarm(),
        )
    }

    private fun rolloverAlarm() = PendingIntent.getBroadcast(
        context,
        ROLLOVER_REQUEST_CODE,
        Intent(context, DayRolloverReceiver::class.java).setAction(ACTION_DAY_ROLLOVER),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
