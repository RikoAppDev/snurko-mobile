package app.snurko.billing.data

import app.snurko.billing.domain.EntitlementStore
import app.snurko.billing.domain.UNLOCK_PRODUCT_ID
import app.snurko.core.database.EntitlementDao
import app.snurko.core.database.EntitlementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomEntitlementStore(
    private val entitlementDao: EntitlementDao,
) : EntitlementStore {

    override fun observeUnlocked(): Flow<Boolean> = entitlementDao
        .observeUnlocked(UNLOCK_PRODUCT_ID)
        .map { it == true }
        .distinctUntilChanged()

    override suspend fun setUnlocked(isUnlocked: Boolean) {
        entitlementDao.upsert(
            EntitlementEntity(productId = UNLOCK_PRODUCT_ID, isUnlocked = isUnlocked),
        )
    }
}
