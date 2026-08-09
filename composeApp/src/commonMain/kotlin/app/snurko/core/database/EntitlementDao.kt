package app.snurko.core.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntitlementDao {

    @Query("SELECT isUnlocked FROM entitlements WHERE productId = :productId")
    fun observeUnlocked(productId: String): Flow<Boolean?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entitlement: EntitlementEntity)
}
