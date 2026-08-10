package app.snurko.reminders.data

import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.reminders.domain.ReminderSchedule
import app.snurko.reminders.domain.ReminderScheduler
import app.snurko.reminders.domain.ScheduledReminder
import app.snurko.resources.Res
import app.snurko.resources.reminder_notification_text
import kotlinx.coroutines.flow.first
import kotlinx.datetime.number
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS keeps scheduled notifications across restarts, so nothing re-arms them at boot. What the
 * system cannot do is re-read the notebook when the reminder lands: the text is fixed when the
 * reminder is scheduled, and the schedule is only re-evaluated when the app runs again.
 */
class UserNotificationReminderScheduler(
    private val challengeRepository: ChallengeRepository,
) : ReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun applySchedule(schedule: ReminderSchedule) {
        val cancelled = schedule.cancelledHabitIds.map(::reminderIdentifier)
        if (cancelled.isNotEmpty()) {
            center.removePendingNotificationRequestsWithIdentifiers(cancelled)
            center.removeDeliveredNotificationsWithIdentifiers(cancelled)
        }
        if (schedule.scheduled.isEmpty()) return

        val active = challengeRepository.observeActiveChallenge().first() ?: return
        val habitNames = active.challenge.habits.associate { it.id to it.name }

        for (reminder in schedule.scheduled) {
            val habitName = habitNames[reminder.habitId] ?: continue
            val identifier = reminderIdentifier(reminder.habitId)
            center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
            center.addNotificationRequest(
                request = UNNotificationRequest.requestWithIdentifier(
                    identifier = identifier,
                    content = notificationContent(
                        habitName = habitName,
                        day = active.dayIndexOn(reminder.fireAt.date),
                    ),
                    trigger = trigger(reminder),
                ),
                withCompletionHandler = null,
            )
        }
    }

    private suspend fun notificationContent(habitName: String, day: Int) =
        UNMutableNotificationContent().apply {
            setTitle(habitName)
            setBody(getString(Res.string.reminder_notification_text, day))
            setSound(UNNotificationSound.defaultSound)
        }

    private fun trigger(reminder: ScheduledReminder): UNCalendarNotificationTrigger {
        val fireAt = NSDateComponents().apply {
            year = reminder.fireAt.year.toLong()
            month = reminder.fireAt.month.number.toLong()
            day = reminder.fireAt.day.toLong()
            hour = reminder.fireAt.hour.toLong()
            minute = reminder.fireAt.minute.toLong()
        }
        return UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(fireAt, repeats = false)
    }
}

private fun reminderIdentifier(habitId: Long): String = "habit-reminder-$habitId"
