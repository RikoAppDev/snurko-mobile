package app.snurko.challenge.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

class FakeChallengeRepository : ChallengeRepository {

    data class CreateCall(
        val name: String,
        val startDate: LocalDate,
        val habits: List<Habit>,
    )

    private val activeChallenge = MutableStateFlow<ActiveChallenge?>(null)

    val createCalls = mutableListOf<CreateCall>()
    var createResult: Result<Long, DataError.Local> = Result.Success(1L)

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
    ): EmptyResult<DataError.Local> = Result.Success(Unit)

    override suspend fun failAttemptAndStartNext(
        attemptId: Long,
        failedOnDay: Int,
        nextAttemptStartDate: LocalDate,
    ): EmptyResult<DataError.Local> = Result.Success(Unit)

    override suspend fun completeAttempt(attemptId: Long): EmptyResult<DataError.Local> =
        Result.Success(Unit)
}
