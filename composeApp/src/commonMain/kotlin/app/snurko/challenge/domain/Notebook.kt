package app.snurko.challenge.domain

data class AttemptRecord(
    val attempt: Attempt,
    val checks: List<DayCheck>,
)

data class Notebook(
    val challenge: Challenge,
    val attempts: List<AttemptRecord>,
)

fun Notebook.latestAttempt(): AttemptRecord? = attempts.maxByOrNull { it.attempt.number }

fun Notebook.toActiveChallenge(): ActiveChallenge? {
    val latest = latestAttempt() ?: return null
    return ActiveChallenge(
        challenge = challenge,
        attempt = latest.attempt,
        checks = latest.checks,
    )
}

fun AttemptRecord.checkedHabitIdsOn(day: Int): Set<Long> =
    checks.firstOrNull { it.day == day }?.checkedHabitIds.orEmpty()

fun AttemptRecord.isDayComplete(day: Int, habits: List<Habit>): Boolean {
    val checkedHabitIds = checkedHabitIdsOn(day)
    return habits.isNotEmpty() && habits.all { it.id in checkedHabitIds }
}

fun AttemptRecord.uncheckedHabitsOn(day: Int, habits: List<Habit>): List<Habit> {
    val checkedHabitIds = checkedHabitIdsOn(day)
    return habits.filterNot { it.id in checkedHabitIds }
}
