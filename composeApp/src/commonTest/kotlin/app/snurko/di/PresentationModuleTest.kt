package app.snurko.di

import app.snurko.billing.domain.BillingGateway
import app.snurko.billing.domain.FakeBillingGateway
import app.snurko.billing.presentation.TipJarViewModel
import app.snurko.challenge.domain.ChallengeRepository
import app.snurko.challenge.domain.FakeChallengeRepository
import app.snurko.challenge.domain.FakeNotebookBackup
import app.snurko.challenge.domain.FakeSetupDraftRepository
import app.snurko.challenge.domain.NotebookBackup
import app.snurko.challenge.domain.SetupDraftRepository
import app.snurko.challenge.presentation.history.HistoryViewModel
import app.snurko.challenge.presentation.home.ChallengeHomeViewModel
import app.snurko.challenge.presentation.setup.ChallengeSetupViewModel
import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.SystemDateProvider
import app.snurko.presentation.AppViewModel
import app.snurko.reminders.domain.FakeNotificationPermissionGateway
import app.snurko.reminders.domain.FakeReminderRepository
import app.snurko.reminders.domain.NotificationPermissionGateway
import app.snurko.reminders.domain.ReminderRepository
import app.snurko.reminders.presentation.RemindersViewModel
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test

class PresentationModuleTest {

    private val koin = koinApplication {
        modules(
            module {
                single<ChallengeRepository> { FakeChallengeRepository() }
                single<NotebookBackup> { FakeNotebookBackup() }
                single<ReminderRepository> { FakeReminderRepository() }
                single<NotificationPermissionGateway> { FakeNotificationPermissionGateway() }
                single<BillingGateway> { FakeBillingGateway() }
                single<SetupDraftRepository> { FakeSetupDraftRepository() }
            },
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
    fun theChallengeHomeViewModelIsResolvable() {
        assertThat(koin.get<ChallengeHomeViewModel>()).isNotNull()
    }

    @Test
    fun theHistoryViewModelIsResolvable() {
        assertThat(koin.get<HistoryViewModel>()).isNotNull()
    }

    @Test
    fun theRemindersViewModelIsResolvable() {
        assertThat(koin.get<RemindersViewModel>()).isNotNull()
    }

    @Test
    fun theAppViewModelIsResolvable() {
        assertThat(koin.get<AppViewModel>()).isNotNull()
    }

    @Test
    fun theTipJarViewModelIsResolvable() {
        assertThat(koin.get<TipJarViewModel>()).isNotNull()
    }
}
