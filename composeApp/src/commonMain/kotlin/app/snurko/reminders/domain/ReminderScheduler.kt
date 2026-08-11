package app.snurko.reminders.domain

/** Hands the reminder set to whatever wake-up mechanism the platform offers. */
interface ReminderScheduler {

    /**
     * True when the mechanism runs the app's own code as the reminder lands, so a habit checked
     * off in the meantime can still be dropped then. False when the reminder is frozen the moment
     * it is scheduled, which means an already-checked habit has to be left out of the set instead.
     */
    val rechecksAtFireTime: Boolean

    suspend fun applySchedule(schedule: ReminderSchedule)
}
