package app.snurko.billing.domain

/** Consumable amounts: each one can be given again once the store has taken it back. */
enum class TipProduct(val productId: String) {
    SMALL("snurko_tip_small"),
    MEDIUM("snurko_tip_medium"),
    LARGE("snurko_tip_large"),
}
