package app.snurko.widget

import app.snurko.billing.domain.EntitlementRepository
import app.snurko.challenge.domain.ChallengeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Redraws whatever home-screen widgets the platform has placed. */
fun interface WidgetUpdater {

    suspend fun updateAll()
}

/** Keeps the placed widgets in step with the notebook and with the unlock. */
class WidgetSync(
    private val challengeRepository: ChallengeRepository,
    private val entitlementRepository: EntitlementRepository,
    private val widgetUpdater: WidgetUpdater,
) {

    fun start(scope: CoroutineScope) {
        combine(
            challengeRepository.observeNotebook(),
            entitlementRepository.observeUnlocked(),
        ) { _, _ -> }
            .onEach { widgetUpdater.updateAll() }
            .launchIn(scope)
    }
}
