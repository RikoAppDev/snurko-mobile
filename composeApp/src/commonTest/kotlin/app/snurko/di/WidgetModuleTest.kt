package app.snurko.di

import app.snurko.billing.domain.EntitlementRepository
import app.snurko.billing.domain.FakeEntitlementRepository
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.widget.WidgetSync
import app.snurko.widget.WidgetUpdater
import assertk.assertThat
import assertk.assertions.isNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class WidgetModuleTest {

    @Test
    fun theWidgetSyncIsResolvable() {
        val koin = koinApplication {
            modules(
                module {
                    single<ChallengeRepository> { FakeChallengeRepository() }
                    single<EntitlementRepository> { FakeEntitlementRepository() }
                    single<WidgetUpdater> { WidgetUpdater { } }
                },
                widgetModule,
            )
        }.koin

        assertThat(koin.get<WidgetSync>()).isNotNull()
    }
}
