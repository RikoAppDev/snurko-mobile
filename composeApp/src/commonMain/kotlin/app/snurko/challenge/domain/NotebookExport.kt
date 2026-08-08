package app.snurko.challenge.domain

const val NOTEBOOK_SCHEMA_VERSION = 1

data class NotebookExport(
    val schemaVersion: Int = NOTEBOOK_SCHEMA_VERSION,
    val challenge: Challenge,
    val attempts: List<AttemptRecord>,
)

/**
 * True when every attempt is numbered uniquely and every check points at a habit of this challenge.
 * A document that fails this would corrupt the notebook even though it parses as JSON.
 */
fun NotebookExport.isConsistent(): Boolean {
    val habitIds = challenge.habits.mapTo(mutableSetOf()) { it.id }
    val attemptNumbers = attempts.map { it.attempt.number }
    return challenge.habits.isNotEmpty() &&
        challenge.lengthDays > 0 &&
        attempts.isNotEmpty() &&
        attemptNumbers.toSet().size == attemptNumbers.size &&
        attempts.all { record ->
            record.checks.all { check ->
                check.day in 1..challenge.lengthDays && habitIds.containsAll(check.checkedHabitIds)
            }
        }
}
