package app.snurko.reminders.domain

import app.snurko.challenge.domain.ChallengeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/** Keeps the armed reminders in step with the notebook, the reminder times and the clock. */
class ReminderSync(
    private val challengeRepository: ChallengeRepository,
    private val reminderRepository: ReminderRepository,
    private val planner: ReminderPlanner,
) {

    private val foregrounds = MutableStateFlow(0)

    fun start(scope: CoroutineScope) {
        combine(
            challengeRepository.observeActiveChallenge(),
            reminderRepository.observeReminders(),
            foregrounds,
        ) { _, _, _ -> }
            .onEach { planner.rescheduleAll() }
            .launchIn(scope)
    }

    /**
     * Where a reminder is frozen once it is scheduled, time passing alone makes the set stale:
     * the day rolls over, and yesterday's checks no longer say anything about today.
     */
    fun onForeground() {
        foregrounds.update { it + 1 }
    }
}
