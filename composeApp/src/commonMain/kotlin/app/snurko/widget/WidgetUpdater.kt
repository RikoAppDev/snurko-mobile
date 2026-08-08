package app.snurko.widget

import app.snurko.challenge.domain.ChallengeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Redraws whatever home-screen widgets the platform has placed. */
fun interface WidgetUpdater {

    suspend fun updateAll()
}

/** Keeps the placed widgets in step with the notebook. */
class WidgetSync(
    private val challengeRepository: ChallengeRepository,
    private val widgetUpdater: WidgetUpdater,
) {

    fun start(scope: CoroutineScope) {
        challengeRepository.observeNotebook()
            .onEach { widgetUpdater.updateAll() }
            .launchIn(scope)
    }
}
