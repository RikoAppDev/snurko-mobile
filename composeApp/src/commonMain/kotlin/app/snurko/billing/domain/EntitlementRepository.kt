package app.snurko.billing.domain

import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.flow.Flow

enum class PurchaseOutcome {
    UNLOCKED,

    /** Paid for by a method that clears later — bank transfer, cash. Nothing unlocks yet. */
    PENDING,
}

interface EntitlementRepository {

    fun observeUnlocked(): Flow<Boolean>

    suspend fun unlockPrice(): Result<String, DataError.Billing>

    /** Opens the store's purchase sheet on top of the screen the user is on. */
    suspend fun purchase(): Result<PurchaseOutcome, DataError.Billing>

    suspend fun restore(): EmptyResult<DataError.Billing>
}
