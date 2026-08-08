package app.snurko.core.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

const val SNURKO_DATABASE_NAME = "snurko.db"

@Database(
    entities = [
        ChallengeEntity::class,
        HabitEntity::class,
        AttemptEntity::class,
        DayCheckEntity::class,
        ReminderEntity::class,
        EntitlementEntity::class,
    ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(SnurkoDatabaseConstructor::class)
abstract class SnurkoDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
    abstract fun attemptDao(): AttemptDao
    abstract fun dayCheckDao(): DayCheckDao
    abstract fun reminderDao(): ReminderDao
    abstract fun entitlementDao(): EntitlementDao
}

@Suppress("KotlinNoActualForExpect")
expect object SnurkoDatabaseConstructor : RoomDatabaseConstructor<SnurkoDatabase> {
    override fun initialize(): SnurkoDatabase
}
