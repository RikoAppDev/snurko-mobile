package app.snurko.reminders.domain

import app.snurko.challenge.domain.ChallengeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Keeps the armed alarms in step with the notebook and with the reminder times. */
class ReminderSync(
    private val challengeRepository: ChallengeRepository,
    private val reminderRepository: ReminderRepository,
    private val planner: ReminderPlanner,
) {

    fun start(scope: CoroutineScope) {
        combine(
            challengeRepository.observeActiveChallenge(),
            reminderRepository.observeReminders(),
        ) { _, _ -> }
            .onEach { planner.rescheduleAll() }
            .launchIn(scope)
    }
}
