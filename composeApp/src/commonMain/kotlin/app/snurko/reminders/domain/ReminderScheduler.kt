package app.snurko.reminders.domain

/** Hands the reminder set to whatever wake-up mechanism the platform offers. */
interface ReminderScheduler {

    suspend fun applySchedule(schedule: ReminderSchedule)
}
