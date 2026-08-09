package app.snurko.billing.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import app.snurko.billing.domain.EntitlementStore
import app.snurko.core.database.SnurkoDatabase
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RoomEntitlementStoreTest {

    private lateinit var database: SnurkoDatabase
    private lateinit var store: EntitlementStore

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<SnurkoDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        store = RoomEntitlementStore(database.entitlementDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun aDatabaseWithoutAnEntitlementRowReadsAsLocked() = runTest {
        store.observeUnlocked().test { assertThat(awaitItem()).isFalse() }
    }

    @Test
    fun anUnlockIsReadBack() = runTest {
        store.setUnlocked(true)

        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun anUnlockTakenBackIsReadBackAsLocked() = runTest {
        store.setUnlocked(true)
        store.setUnlocked(false)

        store.observeUnlocked().test { assertThat(awaitItem()).isFalse() }
    }

    @Test
    fun theUnlockIsWatchedAsItChanges() = runTest {
        store.observeUnlocked().test {
            assertThat(awaitItem()).isFalse()

            store.setUnlocked(true)

            assertThat(awaitItem()).isTrue()
        }
    }
}
