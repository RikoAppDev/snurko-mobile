package app.snurko.core.database

import androidx.room3.Embedded
import androidx.room3.Relation

data class AttemptWithChecks(
    @Embedded val attempt: AttemptEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["attemptId"])
    val checks: List<DayCheckEntity>,
)

data class ChallengeNotebook(
    @Embedded val challenge: ChallengeEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["challengeId"])
    val habits: List<HabitEntity>,
    @Relation(entity = AttemptEntity::class, parentColumns = ["id"], entityColumns = ["challengeId"])
    val attempts: List<AttemptWithChecks>,
)
