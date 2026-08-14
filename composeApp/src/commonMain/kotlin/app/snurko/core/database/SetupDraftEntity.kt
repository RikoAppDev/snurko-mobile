package app.snurko.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "setup_draft")
data class SetupDraftEntity(
    @PrimaryKey
    val position: Int,
    val name: String,
    val failCondition: String?,
)
