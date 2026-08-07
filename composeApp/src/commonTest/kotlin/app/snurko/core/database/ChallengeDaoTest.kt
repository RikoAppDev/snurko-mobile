package app.snurko.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChallengeDaoTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var dao: ChallengeDao

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<SnurkoDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.challengeDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertedChallengeIsReadBackById() = runTest {
        val id = dao.insert(ChallengeEntity(name = "90 days", startDateEpochDay = 20_675))

        val loaded = dao.getById(id)

        assertThat(loaded).isEqualTo(
            ChallengeEntity(id = id, name = "90 days", startDateEpochDay = 20_675),
        )
    }

    @Test
    fun observeAllEmitsInsertedChallenges() = runTest {
        dao.observeAll().test {
            assertThat(awaitItem()).isEqualTo(emptyList())

            val id = dao.insert(ChallengeEntity(name = "90 days", startDateEpochDay = 20_675))

            assertThat(awaitItem()).containsExactly(
                ChallengeEntity(id = id, name = "90 days", startDateEpochDay = 20_675),
            )
        }
    }
}
