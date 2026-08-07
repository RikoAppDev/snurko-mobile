package app.snurko.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(entities = [ChallengeEntity::class], version = 1)
@ConstructedBy(SnurkoDatabaseConstructor::class)
abstract class SnurkoDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
}

@Suppress("KotlinNoActualForExpect")
expect object SnurkoDatabaseConstructor : RoomDatabaseConstructor<SnurkoDatabase> {
    override fun initialize(): SnurkoDatabase
}
