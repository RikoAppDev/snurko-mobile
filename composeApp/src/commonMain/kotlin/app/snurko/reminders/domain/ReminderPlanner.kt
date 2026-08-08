package app.snurko.reminders.domain

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.core.domain.DateProvider
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

    private suspend fun activeChallenge(): ActiveChallenge? =
        challengeRepository.observeActiveChallenge().first()
}
