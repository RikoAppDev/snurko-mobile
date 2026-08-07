package app.snurko.di

import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.presentation.setup.ChallengeSetupViewModel
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.SystemDateProvider
import app.snurko.presentation.AppViewModel
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class PresentationModuleTest {

    private val koin = koinApplication {
        modules(
            module { single<ChallengeRepository> { FakeChallengeRepository() } },
            coreModule,
            presentationModule,
        )
    }.koin

    @Test
    fun theSystemDateProviderIsProvidedByTheCoreModule() {
        assertThat(koin.get<DateProvider>()).isInstanceOf(SystemDateProvider::class)
    }

    @Test
    fun theChallengeSetupViewModelIsResolvable() {
        assertThat(koin.get<ChallengeSetupViewModel>()).isNotNull()
    }

    @Test
    fun theAppViewModelIsResolvable() {
        assertThat(koin.get<AppViewModel>()).isNotNull()
    }
}
