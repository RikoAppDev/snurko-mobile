package app.snurko.di

import app.snurko.reminders.data.RoomReminderRepository
import app.snurko.reminders.domain.ReminderPlanner
import app.snurko.reminders.domain.ReminderRepository
import app.snurko.reminders.domain.ReminderSync
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val remindersModule: Module = module {
    singleOf(::RoomReminderRepository) { bind<ReminderRepository>() }
    singleOf(::ReminderPlanner)
    singleOf(::ReminderSync)
}
