package app.snurko.billing.data

import app.cash.turbine.test
import app.snurko.billing.domain.FakeBillingGateway
import app.snurko.billing.domain.FakeEntitlementStore
import app.snurko.billing.domain.PurchaseOutcome
import app.snurko.billing.domain.pending
import app.snurko.billing.domain.purchased
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BillingEntitlementRepositoryTest {

    private val gateway = FakeBillingGateway()

    @Test
    fun anUntouchedDeviceIsLocked() = runTest {
        val repository = repository()

        repository.observeUnlocked().test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun aDeviceThatUnlockedBeforeOpensUnlocked() = runTest {
        val repository = repository(store = FakeEntitlementStore(isUnlocked = true))

        repository.observeUnlocked().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun thePriceIsWhateverTheStoreSays() = runTest {
        gateway.priceResult = Result.Success("4,99 €")

        assertThat(repository().unlockPrice()).isEqualTo(Result.Success("4,99 €"))
    }

    @Test
    fun aStoreThatCannotQuoteTheProductSaysSo() = runTest {
        gateway.priceResult = Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE)

        assertThat(repository().unlockPrice())
            .isEqualTo(Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE))
    }

    @Test
    fun buyingUnlocksTheDeviceAndAcknowledgesThePurchase() = runTest {
        gateway.launchResult = Result.Success(listOf(purchased("t1", isAcknowledged = false)))
        val store = FakeEntitlementStore()
        val repository = repository(store)

        assertThat(repository.purchase()).isEqualTo(Result.Success(PurchaseOutcome.UNLOCKED))
        assertThat(gateway.launchCalls).isEqualTo(1)
        assertThat(gateway.acknowledgedTokens).containsExactly("t1")
        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun aPurchaseWaitingOnPaymentUnlocksNothingYet() = runTest {
        gateway.launchResult = Result.Success(listOf(pending("t1")))
        val store = FakeEntitlementStore()
        val repository = repository(store)

        assertThat(repository.purchase()).isEqualTo(Result.Success(PurchaseOutcome.PENDING))
        assertThat(store.writes).isEmpty()
        assertThat(gateway.acknowledgedTokens).isEmpty()
    }

    @Test
    fun aSheetTheUserBacksOutOfLeavesTheUnlockAlone() = runTest {
        gateway.launchResult = Result.Error(DataError.Billing.CANCELLED)
        val store = FakeEntitlementStore()
        val repository = repository(store)

        assertThat(repository.purchase()).isEqualTo(Result.Error(DataError.Billing.CANCELLED))
        assertThat(store.writes).isEmpty()
    }

    @Test
    fun aSheetThatClosesWithNothingBoughtCountsAsBackingOut() = runTest {
        gateway.launchResult = Result.Success(emptyList())
        val store = FakeEntitlementStore()

        assertThat(repository(store).purchase())
            .isEqualTo(Result.Error(DataError.Billing.CANCELLED))
        assertThat(store.writes).isEmpty()
    }

    @Test
    fun restoringFindsAPurchaseMadeOnAnotherDevice() = runTest {
        gateway.owned = listOf(purchased())
        val store = FakeEntitlementStore()
        val repository = repository(store)

        assertThat(repository.restore()).isEqualTo(Result.Success(Unit))
        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun restoringWithNothingToFindSaysSo() = runTest {
        val store = FakeEntitlementStore()

        assertThat(repository(store).restore())
            .isEqualTo(Result.Error(DataError.Billing.NOTHING_TO_RESTORE))
    }

    @Test
    fun aStoreThatCannotBeReachedLeavesTheCachedUnlockStanding() = runTest {
        gateway.queryResult = Result.Error(DataError.Billing.DISCONNECTED)
        val store = FakeEntitlementStore(isUnlocked = true)
        val repository = repository(store)

        assertThat(repository.restore()).isEqualTo(Result.Error(DataError.Billing.DISCONNECTED))
        assertThat(store.writes).isEmpty()
        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun anUnlockTheStoreNoLongerReportsIsTakenBack() = runTest {
        gateway.owned = emptyList()
        val store = FakeEntitlementStore(isUnlocked = true)

        repository(store).restore()

        store.observeUnlocked().test { assertThat(awaitItem()).isFalse() }
    }

    @Test
    fun aPurchaseStillAwaitingPaymentDoesNotUnlockAnything() = runTest {
        gateway.owned = listOf(pending())
        val store = FakeEntitlementStore()

        repository(store).restore()

        store.observeUnlocked().test { assertThat(awaitItem()).isFalse() }
        assertThat(gateway.acknowledgedTokens).isEmpty()
    }

    @Test
    fun aPurchaseThatClearsIsAcknowledgedAndUnlocks() = runTest {
        val store = FakeEntitlementStore()
        val repository = repository(store)
        gateway.owned = listOf(pending("t1"))
        repository.restore()

        gateway.owned = listOf(purchased("t1", isAcknowledged = false))
        repository.restore()

        assertThat(gateway.acknowledgedTokens).containsExactly("t1")
        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun anAlreadyAcknowledgedPurchaseIsNotAcknowledgedTwice() = runTest {
        gateway.owned = listOf(purchased("t1", isAcknowledged = true))
        val repository = repository()

        repository.restore()
        repository.restore()

        assertThat(gateway.acknowledgedTokens).isEmpty()
    }

    @Test
    fun aPurchaseThatFailsToAcknowledgeStillUnlocksTheDevice() = runTest {
        gateway.owned = listOf(purchased("t1", isAcknowledged = false))
        gateway.acknowledgeResult = Result.Error(DataError.Billing.DISCONNECTED)
        val store = FakeEntitlementStore()

        repository(store).restore()

        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun aPurchaseComingBackFromTheSheetUnlocksTheDevice() = runTest {
        val store = FakeEntitlementStore()
        val repository = repository(store)
        repository.start(scope())

        gateway.emitPurchases(listOf(purchased("t1", isAcknowledged = false)))

        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
        assertThat(gateway.acknowledgedTokens).containsExactly("t1")
    }

    @Test
    fun aSheetThatEndsWithNothingBoughtNeverTakesAnUnlockAway() = runTest {
        gateway.owned = listOf(purchased())
        val store = FakeEntitlementStore()
        val repository = repository(store)
        repository.start(scope())

        gateway.emitPurchases(emptyList())

        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    @Test
    fun aSheetLeftPendingDoesNotUnlockUntilItClears() = runTest {
        val store = FakeEntitlementStore()
        val repository = repository(store)
        repository.start(scope())

        gateway.emitPurchases(listOf(pending("t1")))

        store.observeUnlocked().test { assertThat(awaitItem()).isFalse() }
        assertThat(gateway.acknowledgedTokens).isEmpty()

        gateway.emitPurchases(listOf(purchased("t1", isAcknowledged = false)))

        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
        assertThat(gateway.acknowledgedTokens).containsExactly("t1")
    }

    @Test
    fun theStoreIsAskedAgainWhenTheAppStarts() = runTest {
        gateway.owned = listOf(purchased())
        val store = FakeEntitlementStore()
        val repository = repository(store)

        repository.start(scope())

        assertThat(gateway.queryCalls).isEqualTo(1)
        store.observeUnlocked().test { assertThat(awaitItem()).isTrue() }
    }

    private fun repository(store: FakeEntitlementStore = FakeEntitlementStore()) =
        BillingEntitlementRepository(gateway = gateway, entitlementStore = store)

    private fun TestScope.scope() = TestScope(UnconfinedTestDispatcher(testScheduler))
}
