package app.snurko.core.database

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "day_checks",
    primaryKeys = ["attemptId", "day", "habitId"],
    foreignKeys = [
        ForeignKey(
            entity = AttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["habitId"])],
)
data class DayCheckEntity(
    val attemptId: Long,
    val day: Int,
    val habitId: Long,
)
