package app.snurko.reminders.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.snurko.core.android.receiveAsync
import app.snurko.reminders.domain.ReminderPlanner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val planner: ReminderPlanner by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_DUE) return
        receiveAsync { planner.rescheduleAll() }
    }
}
