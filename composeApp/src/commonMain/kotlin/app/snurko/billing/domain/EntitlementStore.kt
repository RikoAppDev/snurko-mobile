package app.snurko.billing.domain

import kotlinx.coroutines.flow.Flow

/** The unlock as this device last saw it, so the app opens without waiting on the store. */
interface EntitlementStore {

    fun observeUnlocked(): Flow<Boolean>

    suspend fun setUnlocked(isUnlocked: Boolean)
}
