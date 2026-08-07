package app.snurko.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.challenge.data.RoomChallengeRepository
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.core.database.SnurkoDatabase
import assertk.assertThat
import assertk.assertions.isInstanceOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChallengeDataModuleTest {

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
    fun challengeRepositoryIsResolvedFromTheDatabaseAndChallengeModules() {
        val koin = koinApplication {
            modules(
                module { single { database } },
                databaseModule,
                challengeDataModule,
            )
        }.koin

        assertThat(koin.get<ChallengeRepository>()).isInstanceOf(RoomChallengeRepository::class)
    }
}
