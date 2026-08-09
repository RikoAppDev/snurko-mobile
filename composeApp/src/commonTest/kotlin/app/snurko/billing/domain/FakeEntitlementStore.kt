package app.snurko.billing.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeEntitlementStore(isUnlocked: Boolean = false) : EntitlementStore {

    private val unlocked = MutableStateFlow(isUnlocked)

    val writes = mutableListOf<Boolean>()

    override fun observeUnlocked(): Flow<Boolean> = unlocked

    override suspend fun setUnlocked(isUnlocked: Boolean) {
        writes += isUnlocked
        unlocked.value = isUnlocked
    }
}
