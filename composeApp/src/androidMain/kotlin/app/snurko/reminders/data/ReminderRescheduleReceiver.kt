package app.snurko.reminders.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.snurko.core.android.receiveAsync
import app.snurko.core.domain.DayRolloverScheduler
import app.snurko.reminders.domain.ReminderPlanner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Alarms do not survive a reboot, and a wall-clock reminder set for 08:00 is stored as an
 * absolute instant, so a clock or time-zone change moves it off the hour the user chose.
 */
class ReminderRescheduleReceiver : BroadcastReceiver(), KoinComponent {

    private val planner: ReminderPlanner by inject()
    private val rolloverScheduler: DayRolloverScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        receiveAsync {
            planner.rescheduleAll()
            rolloverScheduler.scheduleNextRollover()
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
