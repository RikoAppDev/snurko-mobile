package app.snurko.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayCheckDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(check: DayCheckEntity)

    @Query("DELETE FROM day_checks WHERE attemptId = :attemptId AND day = :day AND habitId = :habitId")
    suspend fun delete(attemptId: Long, day: Int, habitId: Long)

    @Query("SELECT * FROM day_checks WHERE attemptId = :attemptId ORDER BY day, habitId")
    fun observeForAttempt(attemptId: Long): Flow<List<DayCheckEntity>>
}
