package app.snurko.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {

    @Insert
    suspend fun insert(challenge: ChallengeEntity): Long

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getById(id: Long): ChallengeEntity?

    @Query("SELECT * FROM challenges ORDER BY id")
    fun observeAll(): Flow<List<ChallengeEntity>>
}
