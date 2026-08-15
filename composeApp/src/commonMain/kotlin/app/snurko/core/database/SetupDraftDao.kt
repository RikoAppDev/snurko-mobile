package app.snurko.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface SetupDraftDao {

    @Query("SELECT * FROM setup_draft ORDER BY position")
    suspend fun getAll(): List<SetupDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<SetupDraftEntity>)

    @Query("DELETE FROM setup_draft")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(habits: List<SetupDraftEntity>) {
        clear()
        insertAll(habits)
    }
}
