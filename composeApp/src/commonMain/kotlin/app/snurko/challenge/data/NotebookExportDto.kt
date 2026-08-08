package app.snurko.challenge.data

import app.snurko.challenge.domain.Attempt
import app.snurko.challenge.domain.AttemptRecord
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.Challenge
import app.snurko.challenge.domain.DayCheck
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.NotebookExport
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotebookExportDto(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("challenge") val challenge: ChallengeDto,
    @SerialName("attempts") val attempts: List<AttemptDto>,
)

@Serializable
data class ChallengeDto(
    val id: Long,
    val name: String,
    val startDate: String,
    val lengthDays: Int,
    val habits: List<HabitDto>,
)

@Serializable
data class HabitDto(
    val id: Long,
    val name: String,
    val failCondition: String? = null,
)

@Serializable
data class AttemptDto(
    val id: Long,
    val number: Int,
    val startDate: String,
    val status: AttemptStatus,
    val failedOnDay: Int? = null,
    val checks: List<DayCheckDto>,
)

@Serializable
data class DayCheckDto(
    val day: Int,
    val checkedHabitIds: List<Long>,
)

fun NotebookExport.toDto(): NotebookExportDto = NotebookExportDto(
    schemaVersion = schemaVersion,
    challenge = ChallengeDto(
        id = challenge.id,
        name = challenge.name,
        startDate = challenge.startDate.toString(),
        lengthDays = challenge.lengthDays,
        habits = challenge.habits.map { habit ->
            HabitDto(id = habit.id, name = habit.name, failCondition = habit.failCondition)
        },
    ),
    attempts = attempts.map { record ->
        AttemptDto(
            id = record.attempt.id,
            number = record.attempt.number,
            startDate = record.attempt.startDate.toString(),
            status = record.attempt.status,
            failedOnDay = record.attempt.failedOnDay,
            checks = record.checks.map { check ->
                DayCheckDto(day = check.day, checkedHabitIds = check.checkedHabitIds.sorted())
            },
        )
    },
)

fun NotebookExportDto.toNotebookExport(): NotebookExport = NotebookExport(
    schemaVersion = schemaVersion,
    challenge = Challenge(
        id = challenge.id,
        name = challenge.name,
        habits = challenge.habits.map { habit ->
            Habit(id = habit.id, name = habit.name, failCondition = habit.failCondition)
        },
        startDate = LocalDate.parse(challenge.startDate),
        lengthDays = challenge.lengthDays,
    ),
    attempts = attempts.map { attempt ->
        AttemptRecord(
            attempt = Attempt(
                id = attempt.id,
                number = attempt.number,
                startDate = LocalDate.parse(attempt.startDate),
                status = attempt.status,
                failedOnDay = attempt.failedOnDay,
            ),
            checks = attempt.checks.map { check ->
                DayCheck(day = check.day, checkedHabitIds = check.checkedHabitIds.toSet())
            },
        )
    },
)
