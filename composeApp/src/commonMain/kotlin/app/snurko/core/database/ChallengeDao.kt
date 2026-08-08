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
    @Query("SELECT * FROM challenges ORDER BY id DESC LIMIT 1")
    abstract fun observeLatestChallenge(): Flow<ChallengeWithHabits?>

    @Insert
    abstract suspend fun insertChallenge(challenge: ChallengeEntity): Long

    @Insert
    abstract suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert
    abstract suspend fun insertAttempt(attempt: AttemptEntity): Long
}
