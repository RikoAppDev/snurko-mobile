package app.snurko.billing.domain

enum class PurchaseState {
    PURCHASED,
    PENDING,
}

data class BillingPurchase(
    val token: String,
    val state: PurchaseState,
    val isAcknowledged: Boolean,
)
