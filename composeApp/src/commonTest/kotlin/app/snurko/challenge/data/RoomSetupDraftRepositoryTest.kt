package app.snurko.challenge.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.challenge.domain.Habit
import app.snurko.challenge.domain.SetupDraftRepository
import app.snurko.core.database.SnurkoDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomSetupDraftRepositoryTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var repository: SetupDraftRepository

    private val habits = listOf(
        Habit(name = "Workout", failCondition = "less than 30 minutes"),
        Habit(name = "Read", failCondition = null),
        Habit(name = "No alcohol", failCondition = "any drink"),
    )

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<SnurkoDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        repository = RoomSetupDraftRepository(database.setupDraftDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun anEmptyDraftLoadsAsNoHabits() = runTest {
        assertThat(repository.load()).isEmpty()
    }

    @Test
    fun aSavedDraftRoundTripsInOrder() = runTest {
        repository.save(habits)

        assertThat(repository.load()).containsExactly(*habits.toTypedArray())
    }

    @Test
    fun savingAgainReplacesTheWholeDraft() = runTest {
        repository.save(habits)

        repository.save(listOf(Habit(name = "Read", failCondition = null)))

        assertThat(repository.load()).containsExactly(Habit(name = "Read", failCondition = null))
    }

    @Test
    fun clearingLeavesNothingBehind() = runTest {
        repository.save(habits)

        repository.clear()

        assertThat(repository.load()).isEmpty()
    }
}
