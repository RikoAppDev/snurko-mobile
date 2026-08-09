package app.snurko.billing.presentation

import androidx.lifecycle.viewModelScope
import app.snurko.billing.domain.EntitlementRepository
import app.snurko.billing.domain.PurchaseOutcome
import app.snurko.core.domain.Result
import app.snurko.core.presentation.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class UnlockViewModel(
    private val entitlementRepository: EntitlementRepository,
) : MviViewModel<UnlockState, UnlockAction, UnlockEvent>(initialState = UnlockState()) {

    init {
        entitlementRepository.observeUnlocked()
            .onEach { isUnlocked -> updateState { it.copy(isUnlocked = isUnlocked) } }
            .launchIn(viewModelScope)
        loadPrice()
    }

    override fun onAction(action: UnlockAction) {
        when (action) {
            UnlockAction.BuyClicked -> buy()
            UnlockAction.RestoreClicked -> restore()
            UnlockAction.PriceRetryClicked -> loadPrice()
        }
    }

    private fun loadPrice() {
        updateState { it.copy(isPriceLoading = true) }
        viewModelScope.launch {
            val price = (entitlementRepository.unlockPrice() as? Result.Success)?.data
            updateState { it.copy(price = price, isPriceLoading = false) }
        }
    }

    private fun buy() {
        if (state.value.isBusy || state.value.isUnlocked) return
        updateState { it.copy(isPurchasing = true, isPurchasePending = false, error = null) }
        viewModelScope.launch {
            when (val outcome = entitlementRepository.purchase()) {
                is Result.Error -> updateState { it.copy(error = outcome.error) }
                is Result.Success -> when (outcome.data) {
                    PurchaseOutcome.UNLOCKED -> sendEvent(UnlockEvent.Unlocked)
                    PurchaseOutcome.PENDING -> updateState { it.copy(isPurchasePending = true) }
                }
            }
            updateState { it.copy(isPurchasing = false) }
        }
    }

    private fun restore() {
        if (state.value.isBusy) return
        updateState { it.copy(isRestoring = true, error = null) }
        viewModelScope.launch {
            when (val restored = entitlementRepository.restore()) {
                is Result.Error -> updateState { it.copy(error = restored.error) }
                is Result.Success -> sendEvent(UnlockEvent.Unlocked)
            }
            updateState { it.copy(isRestoring = false) }
        }
    }
}
