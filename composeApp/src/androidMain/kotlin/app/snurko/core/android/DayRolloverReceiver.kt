package app.snurko.core.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.snurko.core.domain.DayRolloverScheduler
import app.snurko.reminders.domain.ReminderPlanner
import app.snurko.widget.WidgetUpdater
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DayRolloverReceiver : BroadcastReceiver(), KoinComponent {

    private val widgetUpdater: WidgetUpdater by inject()
    private val planner: ReminderPlanner by inject()
    private val rolloverScheduler: DayRolloverScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DAY_ROLLOVER) return
        receiveAsync {
            widgetUpdater.updateAll()
            planner.rescheduleAll()
            rolloverScheduler.scheduleNextRollover()
        }
    }
}
