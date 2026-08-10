package app.snurko.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.core.database.SnurkoDatabase
import app.snurko.core.database.snurkoDatabasePath
import app.snurko.core.ios.RootViewControllerProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { RootViewControllerProvider() }
    single {
        Room.databaseBuilder<SnurkoDatabase>(name = snurkoDatabasePath())
            .setDriver(BundledSQLiteDriver())
            .build()
    }
}
