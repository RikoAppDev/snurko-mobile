package app.snurko.billing.presentation

import app.cash.turbine.test
import app.snurko.billing.domain.FakeEntitlementRepository
import app.snurko.billing.domain.PurchaseOutcome
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class UnlockViewModelTest {

    private lateinit var entitlements: FakeEntitlementRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        entitlements = FakeEntitlementRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun thePriceIsWhateverTheStoreQuotes() {
        entitlements.priceResult = Result.Success("4,99 €")

        val state = viewModel().state.value

        assertThat(state.price).isEqualTo("4,99 €")
        assertThat(state.isPriceLoading).isFalse()
        assertThat(state.canBuy).isTrue()
    }

    @Test
    fun aPriceTheStoreCannotQuoteLeavesNothingToBuy() {
        entitlements.priceResult = Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE)

        val state = viewModel().state.value

        assertThat(state.price).isNull()
        assertThat(state.isPriceUnavailable).isTrue()
        assertThat(state.canBuy).isFalse()
    }

    @Test
    fun thePriceCanBeAskedForAgain() {
        entitlements.priceResult = Result.Error(DataError.Billing.DISCONNECTED)
        val viewModel = viewModel()
        entitlements.priceResult = Result.Success("4,99 €")

        viewModel.onAction(UnlockAction.PriceRetryClicked)

        assertThat(viewModel.state.value.price).isEqualTo("4,99 €")
    }

    @Test
    fun aDeviceThatAlreadyPaidOpensUnlocked() {
        entitlements.setUnlocked(true)

        val state = viewModel().state.value

        assertThat(state.isUnlocked).isTrue()
        assertThat(state.canBuy).isFalse()
    }

    @Test
    fun buyingSaysSoOnceAndLeavesTheScreenUnlocked() = runTest {
        val viewModel = viewModel()
        entitlements.onPurchase = { entitlements.setUnlocked(true) }

        viewModel.events.test {
            viewModel.onAction(UnlockAction.BuyClicked)

            assertThat(awaitItem()).isEqualTo(UnlockEvent.Unlocked)
        }
        assertThat(entitlements.purchaseCalls).isEqualTo(1)
        assertThat(viewModel.state.value.isUnlocked).isTrue()
        assertThat(viewModel.state.value.isPurchasing).isFalse()
    }

    @Test
    fun aPaymentStillClearingIsSaidPlainlyAndUnlocksNothing() = runTest {
        entitlements.purchaseResult = Result.Success(PurchaseOutcome.PENDING)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnlockAction.BuyClicked)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.isPurchasePending).isTrue()
        assertThat(viewModel.state.value.isUnlocked).isFalse()
    }

    @Test
    fun aPurchaseTheUserBacksOutOfSaysNothing() = runTest {
        entitlements.purchaseResult = Result.Error(DataError.Billing.CANCELLED)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnlockAction.BuyClicked)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isEqualTo(DataError.Billing.CANCELLED)
        assertThat(viewModel.state.value.isPurchasing).isFalse()
    }

    @Test
    fun aStoreThatCannotBeReachedSaysWhatWentWrong() {
        entitlements.purchaseResult = Result.Error(DataError.Billing.DISCONNECTED)
        val viewModel = viewModel()

        viewModel.onAction(UnlockAction.BuyClicked)

        assertThat(viewModel.state.value.error).isEqualTo(DataError.Billing.DISCONNECTED)
    }

    @Test
    fun buyingTwiceOnlyEverOpensOneSheet() {
        val viewModel = viewModel()
        entitlements.onPurchase = { viewModel.onAction(UnlockAction.BuyClicked) }

        viewModel.onAction(UnlockAction.BuyClicked)

        assertThat(entitlements.purchaseCalls).isEqualTo(1)
    }

    @Test
    fun anAlreadyUnlockedDeviceDoesNotOpenTheSheetAgain() {
        entitlements.setUnlocked(true)
        val viewModel = viewModel()

        viewModel.onAction(UnlockAction.BuyClicked)

        assertThat(entitlements.purchaseCalls).isEqualTo(0)
    }

    @Test
    fun restoringAPurchaseUnlocksAndSaysSo() = runTest {
        val viewModel = viewModel()
        entitlements.restoreResult = Result.Success(Unit)

        viewModel.events.test {
            viewModel.onAction(UnlockAction.RestoreClicked)

            assertThat(awaitItem()).isEqualTo(UnlockEvent.Unlocked)
        }
        assertThat(entitlements.restoreCalls).isEqualTo(1)
        assertThat(viewModel.state.value.isRestoring).isFalse()
    }

    @Test
    fun restoringWithNothingToFindSaysSoHonestly() = runTest {
        entitlements.restoreResult = Result.Error(DataError.Billing.NOTHING_TO_RESTORE)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnlockAction.RestoreClicked)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isEqualTo(DataError.Billing.NOTHING_TO_RESTORE)
    }

    @Test
    fun aFreshAttemptClearsTheErrorTheLastOneLeft() {
        entitlements.purchaseResult = Result.Error(DataError.Billing.DISCONNECTED)
        val viewModel = viewModel()
        viewModel.onAction(UnlockAction.BuyClicked)

        entitlements.purchaseResult = Result.Success(PurchaseOutcome.UNLOCKED)
        viewModel.onAction(UnlockAction.BuyClicked)

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun aPurchaseThatClearsWhileTheScreenIsOpenUnlocksIt() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.isUnlocked).isFalse()

        entitlements.setUnlocked(true)

        assertThat(viewModel.state.value.isUnlocked).isTrue()
    }

    private fun viewModel() = UnlockViewModel(entitlementRepository = entitlements)
}
