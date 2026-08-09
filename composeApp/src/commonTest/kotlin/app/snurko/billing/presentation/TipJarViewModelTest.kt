package app.snurko.billing.presentation

import app.snurko.billing.domain.FakeBillingGateway
import app.snurko.billing.domain.TipProduct
import app.snurko.billing.domain.pending
import app.snurko.billing.domain.purchased
import app.snurko.core.domain.DataError
import app.snurko.core.domain.Result
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class TipJarViewModelTest {

    private lateinit var billing: FakeBillingGateway

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        billing = FakeBillingGateway()
        billing.setPrice(TipProduct.SMALL.productId, Result.Success("€1.99"))
        billing.setPrice(TipProduct.MEDIUM.productId, Result.Success("€4.99"))
        billing.setPrice(TipProduct.LARGE.productId, Result.Success("€9.99"))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun everyAmountCarriesThePriceTheStoreHandsBack() {
        val state = TipJarViewModel(billing).state.value

        assertThat(state.isPriceLoading).isFalse()
        assertThat(state.amounts.map { it.product })
            .containsExactly(TipProduct.SMALL, TipProduct.MEDIUM, TipProduct.LARGE)
        assertThat(state.amounts.map { it.price }).containsExactly("€1.99", "€4.99", "€9.99")
    }

    @Test
    fun anAmountTheStoreHasNoPriceForIsNotOffered() {
        billing.setPrice(
            TipProduct.MEDIUM.productId,
            Result.Error(DataError.Billing.PRODUCT_UNAVAILABLE),
        )

        val state = TipJarViewModel(billing).state.value

        assertThat(state.amounts.map { it.product })
            .containsExactly(TipProduct.SMALL, TipProduct.LARGE)
        assertThat(state.isPriceUnavailable).isFalse()
    }

    @Test
    fun noPricesAtAllIsSaidPlainly() {
        billing.defaultPrice = Result.Error(DataError.Billing.DISCONNECTED)
        TipProduct.entries.forEach {
            billing.setPrice(it.productId, Result.Error(DataError.Billing.DISCONNECTED))
        }

        val state = TipJarViewModel(billing).state.value

        assertThat(state.amounts).isEmpty()
        assertThat(state.isPriceUnavailable).isTrue()
    }

    @Test
    fun thePricesCanBeAskedForAgain() {
        TipProduct.entries.forEach {
            billing.setPrice(it.productId, Result.Error(DataError.Billing.DISCONNECTED))
        }
        val viewModel = TipJarViewModel(billing)
        TipProduct.entries.forEach { billing.setPrice(it.productId, Result.Success("€3.49")) }

        viewModel.onAction(TipJarAction.PriceRetryClicked)

        assertThat(viewModel.state.value.amounts).hasSize(3)
        assertThat(viewModel.state.value.isPriceUnavailable).isFalse()
    }

    @Test
    fun aTipIsHandedBackToTheStoreAndQuietlyThankedFor() {
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.MEDIUM))

        assertThat(billing.launchedProducts).containsExactly(TipProduct.MEDIUM.productId)
        assertThat(billing.consumedTokens).containsExactly("${TipProduct.MEDIUM.productId}-1")
        assertThat(viewModel.state.value.isThanksVisible).isTrue()
        assertThat(viewModel.state.value.isTipping).isFalse()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun aConsumedTipIsNeverAcknowledgedAndLeavesNothingOwned() {
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))

        assertThat(billing.acknowledgedTokens).isEmpty()
        assertThat(billing.ownedOf(TipProduct.SMALL.productId)).isEmpty()
    }

    @Test
    fun aSecondTipCanBeGivenOnceTheFirstOneIsConsumed() {
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))
        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))

        assertThat(billing.launchedProducts).hasSize(2)
        assertThat(billing.consumedTokens).containsExactly(
            "${TipProduct.SMALL.productId}-1",
            "${TipProduct.SMALL.productId}-2",
        )
        assertThat(viewModel.state.value.isThanksVisible).isTrue()
    }

    @Test
    fun aPendingTipIsSaidPlainlyAndThankedForOnlyOnceItLands() {
        billing.setLaunchResult(
            TipProduct.LARGE.productId,
            Result.Success(listOf(pending(token = "pending-tip"))),
        )
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.LARGE))

        assertThat(viewModel.state.value.isTipPending).isTrue()
        assertThat(viewModel.state.value.isThanksVisible).isFalse()
        assertThat(billing.consumedTokens).isEmpty()
    }

    @Test
    fun aTipTheStoreRefusesSaysWhatWentWrong() {
        billing.setLaunchResult(
            TipProduct.SMALL.productId,
            Result.Error(DataError.Billing.DISCONNECTED),
        )
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))

        assertThat(viewModel.state.value.error).isEqualTo(DataError.Billing.DISCONNECTED)
        assertThat(viewModel.state.value.isThanksVisible).isFalse()
        assertThat(viewModel.state.value.isTipping).isFalse()
    }

    @Test
    fun aTipTheUserCallsOffChargesNothing() {
        billing.setLaunchResult(
            TipProduct.SMALL.productId,
            Result.Error(DataError.Billing.CANCELLED),
        )
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))

        assertThat(viewModel.state.value.error).isEqualTo(DataError.Billing.CANCELLED)
        assertThat(billing.consumedTokens).isEmpty()
    }

    @Test
    fun aStoreSheetThatProducesNothingLeavesTheCardAsItWas() {
        billing.setLaunchResult(TipProduct.SMALL.productId, Result.Success(emptyList()))
        val viewModel = TipJarViewModel(billing)

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))

        assertThat(viewModel.state.value.isThanksVisible).isFalse()
        assertThat(viewModel.state.value.isTipPending).isFalse()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun anEarlierErrorIsClearedWhenTheNextTipStarts() {
        billing.setLaunchResult(
            TipProduct.SMALL.productId,
            Result.Error(DataError.Billing.UNKNOWN),
        )
        val viewModel = TipJarViewModel(billing)
        viewModel.onAction(TipJarAction.TipClicked(TipProduct.SMALL))

        viewModel.onAction(TipJarAction.TipClicked(TipProduct.LARGE))

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.isThanksVisible).isTrue()
    }

    @Test
    fun aTipLeftOverFromBeforeIsHandedBackWhenTheJarOpensAndThanksNobody() {
        billing.setOwned(TipProduct.MEDIUM.productId, listOf(purchased(token = "leftover")))

        val viewModel = TipJarViewModel(billing)

        assertThat(billing.consumedTokens).containsExactly("leftover")
        assertThat(viewModel.state.value.isThanksVisible).isFalse()
        assertThat(billing.launchedProducts).isEmpty()
    }
}
