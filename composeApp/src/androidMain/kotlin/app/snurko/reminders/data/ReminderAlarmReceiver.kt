package app.snurko.reminders.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.snurko.core.android.receiveAsync
import app.snurko.reminders.domain.ReminderNotifier
import app.snurko.reminders.domain.ReminderPlanner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val NO_HABIT_ID = -1L

class ReminderAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val planner: ReminderPlanner by inject()
    private val notifier: ReminderNotifier by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER_DUE) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, NO_HABIT_ID)
        if (habitId == NO_HABIT_ID) return

        receiveAsync {
            planner.firingFor(habitId)?.let { firing -> notifier.show(firing) }
            planner.rescheduleAll()
        }
    }
}
