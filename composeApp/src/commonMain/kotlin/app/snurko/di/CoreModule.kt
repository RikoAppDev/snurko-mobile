package app.snurko.di

import app.snurko.core.domain.DateProvider
import app.snurko.core.domain.SystemDateProvider
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule: Module = module {
    singleOf(::SystemDateProvider) { bind<DateProvider>() }
}
