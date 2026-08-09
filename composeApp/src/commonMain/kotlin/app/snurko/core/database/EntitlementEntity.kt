package app.snurko.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "entitlements")
data class EntitlementEntity(
    @PrimaryKey
    val productId: String,
    val isUnlocked: Boolean,
)
