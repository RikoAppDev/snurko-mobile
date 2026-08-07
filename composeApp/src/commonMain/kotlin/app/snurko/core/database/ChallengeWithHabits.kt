package app.snurko.core.database

import androidx.room3.Embedded
import androidx.room3.Relation

data class ChallengeWithHabits(
    @Embedded val challenge: ChallengeEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["challengeId"])
    val habits: List<HabitEntity>,
)
