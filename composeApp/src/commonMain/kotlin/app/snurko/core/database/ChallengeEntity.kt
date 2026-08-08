package app.snurko.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDateEpochDay: Long,
    val lengthDays: Int,
)
