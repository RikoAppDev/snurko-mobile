package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeChallengeRepository : ChallengeRepository {

    data class CreateCall(
        val name: String,
        val startDate: LocalDate,
        val habits: List<Habit>,
    )

    data class CheckCall(
        val attemptId: Long,
        val day: Int,
        val habitId: Long,
        val checked: Boolean,
    )

    data class FailCall(
        val attemptId: Long,
        val failedOnDay: Int,
        val nextAttemptStartDate: LocalDate,
    )

    private val activeChallenge = MutableStateFlow<ActiveChallenge?>(null)

    val createCalls = mutableListOf<CreateCall>()
    val checkCalls = mutableListOf<CheckCall>()
    val failCalls = mutableListOf<FailCall>()
    val completeCalls = mutableListOf<Long>()

    var createResult: Result<Long, DataError.Local> = Result.Success(1L)
    var checkResult: EmptyResult<DataError.Local> = Result.Success(Unit)
    var failResult: EmptyResult<DataError.Local> = Result.Success(Unit)

    /** Start dates handed to the attempts created by [failAttemptAndStartNext], oldest first. */
    val restartDates = ArrayDeque<LocalDate>()

    fun setActiveChallenge(active: ActiveChallenge?) {
        activeChallenge.value = active
    }

    override fun observeActiveChallenge(): Flow<ActiveChallenge?> = activeChallenge

    override suspend fun createChallenge(
        name: String,
        startDate: LocalDate,
        habits: List<Habit>,
    ): Result<Long, DataError.Local> {
        createCalls += CreateCall(name, startDate, habits)
        return createResult
    }

    override suspend fun setHabitChecked(
        attemptId: Long,
        day: Int,
        habitId: Long,
        checked: Boolean,
    ): EmptyResult<DataError.Local> {
        checkCalls += CheckCall(attemptId, day, habitId, checked)
        if (checkResult is Result.Error) return checkResult
        updateAttempt(attemptId) { active ->
            active.copy(checks = active.checks.withHabit(day, habitId, checked))
        }
        return checkResult
    }

    override suspend fun failAttemptAndStartNext(
        attemptId: Long,
        failedOnDay: Int,
        nextAttemptStartDate: LocalDate,
    ): EmptyResult<DataError.Local> {
        failCalls += FailCall(attemptId, failedOnDay, nextAttemptStartDate)
        if (failResult is Result.Error) return failResult
        val startDate = restartDates.removeFirstOrNull() ?: nextAttemptStartDate
        updateAttempt(attemptId) { active ->
            active.copy(
                attempt = active.attempt.nextAttempt(startDate).copy(id = active.attempt.id + 1),
                checks = emptyList(),
            )
        }
        return failResult
    }

    override suspend fun completeAttempt(attemptId: Long): EmptyResult<DataError.Local> {
        completeCalls += attemptId
        updateAttempt(attemptId) { active -> active.copy(attempt = active.attempt.completed()) }
        return Result.Success(Unit)
    }

    private fun updateAttempt(attemptId: Long, transform: (ActiveChallenge) -> ActiveChallenge) {
        activeChallenge.update { active ->
            if (active?.attempt?.id == attemptId) transform(active) else active
        }
    }

    private fun List<DayCheck>.withHabit(day: Int, habitId: Long, checked: Boolean): List<DayCheck> {
        val existing = firstOrNull { it.day == day }?.checkedHabitIds.orEmpty()
        val updated = if (checked) existing + habitId else existing - habitId
        return (filterNot { it.day == day } + DayCheck(day = day, checkedHabitIds = updated))
            .filter { it.checkedHabitIds.isNotEmpty() }
            .sortedBy { it.day }
    }
}
