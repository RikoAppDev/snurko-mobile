package app.snurko.di

import app.snurko.billing.presentation.TipJarViewModel
import app.snurko.challenge.presentation.history.HistoryViewModel
import app.snurko.challenge.presentation.home.ChallengeHomeViewModel
import app.snurko.challenge.presentation.setup.ChallengeSetupViewModel
import app.snurko.presentation.AppViewModel
import app.snurko.reminders.presentation.RemindersViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule: Module = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::ChallengeSetupViewModel)
    viewModelOf(::ChallengeHomeViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::RemindersViewModel)
    viewModelOf(::TipJarViewModel)
}
