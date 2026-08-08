package app.snurko.challenge.data

import app.snurko.challenge.domain.Attempt
import app.snurko.challenge.domain.AttemptRecord
import app.snurko.challenge.domain.Challenge
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.NOTEBOOK_SCHEMA_VERSION
import app.snurko.challenge.domain.Notebook
import app.snurko.challenge.domain.NotebookExport
import app.snurko.core.database.AttemptEntity
import app.snurko.core.database.ChallengeEntity
import app.snurko.core.database.ChallengeNotebook
import app.snurko.core.database.DayCheckEntity
import app.snurko.core.database.HabitEntity
import kotlinx.datetime.LocalDate

fun ChallengeNotebook.toNotebook(): Notebook = Notebook(
    challenge = toChallenge(),
    attempts = attempts.sortedBy { it.attempt.number }.map { record ->
        AttemptRecord(
            attempt = record.attempt.toAttempt(),
            checks = record.checks.toDayChecks(),
        )
    },
)

fun Notebook.toNotebookExport(): NotebookExport = NotebookExport(
    schemaVersion = NOTEBOOK_SCHEMA_VERSION,
    challenge = challenge,
    attempts = attempts,
)

fun NotebookExport.toChallengeEntity(): ChallengeEntity = ChallengeEntity(
    id = challenge.id,
    name = challenge.name,
    startDateEpochDay = challenge.startDate.toEpochDays(),
    lengthDays = challenge.lengthDays,
)

fun NotebookExport.toHabitEntities(): List<HabitEntity> = challenge.habits
    .mapIndexed { position, habit -> habit.toEntity(challengeId = challenge.id, position = position) }

fun NotebookExport.toAttemptEntities(): List<AttemptEntity> = attempts
    .map { it.attempt.toEntity(challengeId = challenge.id) }

fun NotebookExport.toDayCheckEntities(): List<DayCheckEntity> = attempts.flatMap { record ->
    record.checks.flatMap { check ->
        check.checkedHabitIds.map { habitId ->
            DayCheckEntity(attemptId = record.attempt.id, day = check.day, habitId = habitId)
        }
    }
}

fun ChallengeNotebook.toChallenge(): Challenge = Challenge(
    id = challenge.id,
    name = challenge.name,
    habits = habits.sortedBy { it.position }.map { it.toHabit() },
    startDate = LocalDate.fromEpochDays(challenge.startDateEpochDay),
    lengthDays = challenge.lengthDays,
)

fun HabitEntity.toHabit(): Habit = Habit(
    id = id,
    name = name,
    failCondition = failCondition,
)

fun Habit.toEntity(challengeId: Long, position: Int): HabitEntity = HabitEntity(
    id = id,
    challengeId = challengeId,
    name = name,
    failCondition = failCondition,
    position = position,
)

fun AttemptEntity.toAttempt(): Attempt = Attempt(
    id = id,
    number = number,
    startDate = LocalDate.fromEpochDays(startDateEpochDay),
    status = status,
    failedOnDay = failedOnDay,
)

fun Attempt.toEntity(challengeId: Long): AttemptEntity = AttemptEntity(
    id = id,
    challengeId = challengeId,
    number = number,
    startDateEpochDay = startDate.toEpochDays(),
    status = status,
    failedOnDay = failedOnDay,
)

fun List<DayCheckEntity>.toDayChecks(): List<DayCheck> = groupBy { it.day }
    .map { (day, checks) -> DayCheck(day = day, checkedHabitIds = checks.mapTo(mutableSetOf()) { it.habitId }) }
    .sortedBy { it.day }
