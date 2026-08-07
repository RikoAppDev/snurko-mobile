package app.snurko.challenge.data

import app.snurko.challenge.domain.Attempt
import app.snurko.challenge.domain.Challenge
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.core.database.AttemptEntity
import app.snurko.core.database.ChallengeWithHabits
import app.snurko.core.database.DayCheckEntity
import app.snurko.core.database.HabitEntity
import kotlinx.datetime.LocalDate

fun ChallengeWithHabits.toChallenge(): Challenge = Challenge(
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
