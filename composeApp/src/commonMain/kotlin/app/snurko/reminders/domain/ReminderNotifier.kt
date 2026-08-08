package app.snurko.reminders.domain

interface ReminderNotifier {

    suspend fun show(firing: ReminderFiring)

    fun dismiss(habitId: Long)
}
