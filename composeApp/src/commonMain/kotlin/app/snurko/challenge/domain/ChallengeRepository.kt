package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ChallengeRepository {

    fun observeActiveChallenge(): Flow<ActiveChallenge?>

    suspend fun createChallenge(
        name: String,
        startDate: LocalDate,
        habits: List<Habit>,
    ): Result<Long, DataError.Local>

    suspend fun setHabitChecked(
        attemptId: Long,
        day: Int,
        habitId: Long,
        checked: Boolean,
    ): EmptyResult<DataError.Local>

    suspend fun failAttemptAndStartNext(
        attemptId: Long,
        failedOnDay: Int,
        nextAttemptStartDate: LocalDate,
    ): EmptyResult<DataError.Local>

    suspend fun completeAttempt(attemptId: Long): EmptyResult<DataError.Local>
}
