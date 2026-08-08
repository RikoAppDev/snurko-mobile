package app.snurko.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.core.database.SnurkoDatabase
import app.snurko.reminders.data.RoomReminderRepository
import app.snurko.reminders.domain.FakeReminderScheduler
import app.snurko.reminders.domain.ReminderPlanner
import app.snurko.reminders.domain.ReminderRepository
import app.snurko.reminders.domain.ReminderScheduler
import app.snurko.reminders.domain.ReminderSync
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RemindersModuleTest {

    private lateinit var database: SnurkoDatabase

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<SnurkoDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun theReminderRepositoryIsResolvedFromTheDatabaseAndReminderModules() {
        assertThat(koin().get<ReminderRepository>()).isInstanceOf(RoomReminderRepository::class)
    }

    @Test
    fun theReminderPlannerIsResolvable() {
        assertThat(koin().get<ReminderPlanner>()).isNotNull()
    }

    @Test
    fun theReminderSyncIsResolvable() {
        assertThat(koin().get<ReminderSync>()).isNotNull()
    }

    private fun koin(): Koin = koinApplication {
        modules(
            module {
                single { database }
                single<ReminderScheduler> { FakeReminderScheduler() }
            },
            coreModule,
            databaseModule,
            challengeDataModule,
            remindersModule,
        )
    }.koin
}
