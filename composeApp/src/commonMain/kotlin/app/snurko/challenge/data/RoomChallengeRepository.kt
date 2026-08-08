package app.snurko.challenge.data

import app.snurko.challenge.domain.ActiveChallenge
import app.snurko.challenge.domain.AttemptStatus
import app.snurko.challenge.domain.CHALLENGE_LENGTH_DAYS
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.completed
import app.snurko.challenge.domain.failedOn
import app.snurko.challenge.domain.nextAttempt
import app.snurko.core.data.localCall
import app.snurko.core.database.AttemptDao
import app.snurko.core.database.AttemptEntity
import app.snurko.core.database.ChallengeDao
import app.snurko.core.database.ChallengeEntity
import app.snurko.core.database.DayCheckDao
import app.snurko.core.database.DayCheckEntity
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomChallengeRepository(
    private val challengeDao: ChallengeDao,
    private val attemptDao: AttemptDao,
    private val dayCheckDao: DayCheckDao,
) : ChallengeRepository {

    override fun observeActiveChallenge(): Flow<ActiveChallenge?> = challengeDao
        .observeNotebook()
        .map { notebook -> notebook?.toActiveChallenge() }
        .distinctUntilChanged()

    override suspend fun createChallenge(
        name: String,
        startDate: LocalDate,
        habits: List<Habit>,
    ): Result<Long, DataError.Local> = localCall {
        val challengeId = challengeDao.createChallenge(
            challenge = ChallengeEntity(
                name = name,
                startDateEpochDay = startDate.toEpochDays(),
                lengthDays = CHALLENGE_LENGTH_DAYS,
            ),
            habits = habits.mapIndexed { index, habit -> habit.toEntity(challengeId = 0, position = index) },
            firstAttempt = AttemptEntity(
                challengeId = 0,
                number = 1,
                startDateEpochDay = startDate.toEpochDays(),
                status = AttemptStatus.ACTIVE,
                failedOnDay = null,
            ),
        )
        Result.Success(challengeId)
    }

    override suspend fun setHabitChecked(
        attemptId: Long,
        day: Int,
        habitId: Long,
        checked: Boolean,
    ): EmptyResult<DataError.Local> = localCall {
        attemptDao.getById(attemptId) ?: return@localCall Result.Error(DataError.Local.NOT_FOUND)
        if (checked) {
            dayCheckDao.insert(DayCheckEntity(attemptId = attemptId, day = day, habitId = habitId))
        } else {
            dayCheckDao.delete(attemptId = attemptId, day = day, habitId = habitId)
        }
        Result.Success(Unit)
    }

    override suspend fun failAttemptAndStartNext(
        attemptId: Long,
        failedOnDay: Int,
        nextAttemptStartDate: LocalDate,
    ): EmptyResult<DataError.Local> = localCall {
        val entity = attemptDao.getById(attemptId)
            ?: return@localCall Result.Error(DataError.Local.NOT_FOUND)
        val attempt = entity.toAttempt()
        attemptDao.failAndStartNext(
            failed = attempt.failedOn(failedOnDay).toEntity(entity.challengeId),
            next = attempt.nextAttempt(nextAttemptStartDate).toEntity(entity.challengeId),
        )
        Result.Success(Unit)
    }

    override suspend fun completeAttempt(attemptId: Long): EmptyResult<DataError.Local> = localCall {
        val entity = attemptDao.getById(attemptId)
            ?: return@localCall Result.Error(DataError.Local.NOT_FOUND)
        attemptDao.update(entity.toAttempt().completed().toEntity(entity.challengeId))
        Result.Success(Unit)
    }
}
