package app.snurko.di

import app.snurko.challenge.presentation.setup.ChallengeSetupViewModel
import app.snurko.presentation.AppViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule: Module = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::ChallengeSetupViewModel)
}
