package app.snurko.di

import app.snurko.widget.WidgetSync
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val widgetModule: Module = module {
    singleOf(::WidgetSync)
}
