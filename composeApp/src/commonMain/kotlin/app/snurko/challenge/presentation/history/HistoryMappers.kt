package app.snurko.challenge.presentation.history

import app.snurko.challenge.domain.AttemptRecord
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.Challenge
import app.snurko.challenge.domain.Notebook
import app.snurko.challenge.domain.completionPercent
import app.snurko.challenge.domain.dayIndexOn
import app.snurko.challenge.domain.isDayComplete
import app.snurko.challenge.domain.uncheckedHabitsOn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

fun Notebook.toAttemptRows(today: LocalDate): List<AttemptRowUi> = attempts
    .sortedByDescending { it.attempt.number }
    .map { record -> record.toAttemptRowUi(challenge, today) }

private fun AttemptRecord.toAttemptRowUi(challenge: Challenge, today: LocalDate): AttemptRowUi {
    val outcome = outcomeUi(challenge, today)
    return AttemptRowUi(
        number = attempt.number,
        startDate = attempt.startDate,
        endDate = attempt.startDate.endOf(outcome, challenge.lengthDays),
        outcome = outcome,
        markedDays = (1..challenge.lengthDays).map { day -> isDayComplete(day, challenge.habits) },
        completionPercent = completionPercent(challenge),
    )
}

private fun AttemptRecord.outcomeUi(challenge: Challenge, today: LocalDate): AttemptOutcomeUi =
    when (attempt.status) {
        AttemptStatus.ACTIVE -> AttemptOutcomeUi.InProgress(
            day = attempt.dayIndexOn(today).coerceIn(1, challenge.lengthDays),
        )

        AttemptStatus.FAILED -> {
            val failedOnDay = (attempt.failedOnDay ?: 1).coerceIn(1, challenge.lengthDays)
            AttemptOutcomeUi.Failed(
                failedOnDay = failedOnDay,
                missedHabits = uncheckedHabitsOn(failedOnDay, challenge.habits).map { it.name },
            )
        }

        AttemptStatus.COMPLETED -> AttemptOutcomeUi.Completed
    }

private fun LocalDate.endOf(outcome: AttemptOutcomeUi, lengthDays: Int): LocalDate? = when (outcome) {
    is AttemptOutcomeUi.InProgress -> null
    is AttemptOutcomeUi.Failed -> plus(outcome.failedOnDay - 1, DateTimeUnit.DAY)
    AttemptOutcomeUi.Completed -> plus(lengthDays - 1, DateTimeUnit.DAY)
}
