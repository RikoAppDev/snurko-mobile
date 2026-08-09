package app.snurko.billing.domain

const val UNLOCK_PRODUCT_ID = "snurko_unlock"

enum class PurchaseState {
    PURCHASED,
    PENDING,
}

data class UnlockPurchase(
    val token: String,
    val state: PurchaseState,
    val isAcknowledged: Boolean,
)
