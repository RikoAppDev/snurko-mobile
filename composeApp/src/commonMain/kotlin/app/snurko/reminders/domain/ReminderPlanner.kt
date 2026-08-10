package app.snurko.reminders.domain

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.first

class ReminderPlanner(
    private val challengeRepository: ChallengeRepository,
    private val reminderRepository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val dateProvider: DateProvider,
) {

    suspend fun rescheduleAll() {
        val schedule = reminderSchedule(
            active = activeChallenge(),
            reminders = reminderRepository.getReminders(),
            now = dateProvider.now(),
            rechecksAtFireTime = scheduler.rechecksAtFireTime,
        )
        scheduler.applySchedule(schedule)
    }

    suspend fun firingFor(habitId: Long): ReminderFiring? {
        val active = activeChallenge() ?: return null
        val today = dateProvider.today()
        if (!active.shouldRemind(habitId, today)) return null
        val habit = active.challenge.habits.first { it.id == habitId }
        return ReminderFiring(
            habitId = habitId,
            habitName = habit.name,
            attemptId = active.attempt.id,
            day = active.dayIndexOn(today),
        )
    }

    /** Marks the habit done straight from the notification, without opening the app. */
    suspend fun checkOff(habitId: Long): Boolean {
        val firing = firingFor(habitId) ?: return false
        val result = challengeRepository.setHabitChecked(
            attemptId = firing.attemptId,
            day = firing.day,
            habitId = habitId,
            checked = true,
        )
        return result is Result.Success
    }

    private suspend fun activeChallenge(): ActiveChallenge? =
        challengeRepository.observeActiveChallenge().first()
}
