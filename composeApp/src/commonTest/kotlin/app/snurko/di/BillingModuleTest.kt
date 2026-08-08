package app.snurko.di

import app.snurko.billing.data.BillingEntitlementRepository
import app.snurko.billing.domain.BillingGateway
import app.snurko.billing.domain.EntitlementRepository
import app.snurko.billing.domain.FakeBillingGateway
import app.snurko.core.database.EntitlementDao
import app.snurko.core.database.EntitlementEntity
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class BillingModuleTest {

    private val koin = koinApplication {
        modules(
            module {
                single<BillingGateway> { FakeBillingGateway() }
                single<EntitlementDao> { NoEntitlementDao() }
            },
            billingModule,
        )
    }.koin

    @Test
    fun theEntitlementRepositoryIsResolvable() {
        assertThat(koin.get<EntitlementRepository>()).isNotNull()
    }

    @Test
    fun theRepositoryTheAppStartsIsTheOneTheScreensRead() {
        assertThat(koin.get<EntitlementRepository>())
            .isSameInstanceAs(koin.get<BillingEntitlementRepository>())
    }
}

private class NoEntitlementDao : EntitlementDao {

    override fun observeUnlocked(productId: String): Flow<Boolean?> = flowOf(null)

    override suspend fun upsert(entitlement: EntitlementEntity) = Unit
}
