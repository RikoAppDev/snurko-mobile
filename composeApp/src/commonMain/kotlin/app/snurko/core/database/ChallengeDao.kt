package app.snurko.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChallengeDao {

    @Transaction
    open suspend fun createChallenge(
        challenge: ChallengeEntity,
        habits: List<HabitEntity>,
        firstAttempt: AttemptEntity,
    ): Long {
        val challengeId = insertChallenge(challenge)
        insertHabits(habits.map { it.copy(challengeId = challengeId) })
        insertAttempt(firstAttempt.copy(challengeId = challengeId))
        return challengeId
    }

    @Transaction
    open suspend fun replaceNotebook(
        challenge: ChallengeEntity,
        habits: List<HabitEntity>,
        attempts: List<AttemptEntity>,
        checks: List<DayCheckEntity>,
    ) {
        deleteAllReminders()
        deleteAllDayChecks()
        deleteAllAttempts()
        deleteAllHabits()
        deleteAllChallenges()
        insertChallenge(challenge)
        insertHabits(habits)
        insertAttempts(attempts)
        insertDayChecks(checks)
    }

    @Transaction
    @Query("SELECT * FROM challenges ORDER BY id DESC LIMIT 1")
    abstract fun observeNotebook(): Flow<ChallengeNotebook?>

    @Transaction
    @Query("SELECT * FROM challenges ORDER BY id DESC LIMIT 1")
    abstract suspend fun getNotebook(): ChallengeNotebook?

    @Insert
    abstract suspend fun insertChallenge(challenge: ChallengeEntity): Long

    @Insert
    abstract suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert
    abstract suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Insert
    abstract suspend fun insertAttempts(attempts: List<AttemptEntity>)

    @Insert
    abstract suspend fun insertDayChecks(checks: List<DayCheckEntity>)

    @Query("DELETE FROM reminders")
    abstract suspend fun deleteAllReminders()

    @Query("DELETE FROM day_checks")
    abstract suspend fun deleteAllDayChecks()

    @Query("DELETE FROM attempts")
    abstract suspend fun deleteAllAttempts()

    @Query("DELETE FROM habits")
    abstract suspend fun deleteAllHabits()

    @Query("DELETE FROM challenges")
    abstract suspend fun deleteAllChallenges()
}
