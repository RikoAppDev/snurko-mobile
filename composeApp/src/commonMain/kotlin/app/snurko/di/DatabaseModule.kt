package app.snurko.di

import app.snurko.core.database.SnurkoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val databaseModule: Module = module {
    single { get<SnurkoDatabase>().challengeDao() }
    single { get<SnurkoDatabase>().attemptDao() }
    single { get<SnurkoDatabase>().dayCheckDao() }
    single { get<SnurkoDatabase>().reminderDao() }
    single { get<SnurkoDatabase>().setupDraftDao() }
}
