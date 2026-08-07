package app.snurko.core.database

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import app.snurko.challenge.domain.AttemptStatus

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = ChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["challengeId", "number"], unique = true),
        Index(value = ["challengeId", "status"]),
    ],
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challengeId: Long,
    val number: Int,
    val startDateEpochDay: Long,
    val status: AttemptStatus,
    val failedOnDay: Int?,
)
