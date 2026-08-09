package app.snurko.di

import app.snurko.billing.data.BillingEntitlementRepository
import app.snurko.billing.data.RoomEntitlementStore
import app.snurko.billing.domain.EntitlementRepository
import app.snurko.billing.domain.EntitlementStore
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val billingModule: Module = module {
    singleOf(::RoomEntitlementStore) { bind<EntitlementStore>() }
    singleOf(::BillingEntitlementRepository)
    single<EntitlementRepository> { get<BillingEntitlementRepository>() }
}
