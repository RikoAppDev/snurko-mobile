package app.snurko.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import app.snurko.challenge.domain.AttemptStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AttemptDao {

    @Transaction
    open suspend fun failAndStartNext(failed: AttemptEntity, next: AttemptEntity) {
        update(failed)
        insert(next)
    }

    @Query("SELECT * FROM attempts WHERE challengeId = :challengeId ORDER BY number DESC LIMIT 1")
    abstract fun observeLatest(challengeId: Long): Flow<AttemptEntity?>

    @Query("SELECT * FROM attempts WHERE id = :id")
    abstract suspend fun getById(id: Long): AttemptEntity?

    @Insert
    abstract suspend fun insert(attempt: AttemptEntity): Long

    @Update
    abstract suspend fun update(attempt: AttemptEntity)
}
