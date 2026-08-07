package app.snurko.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.core.database.SNURKO_DATABASE_NAME
import app.snurko.core.database.SnurkoDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        val context = androidContext()
        Room.databaseBuilder<SnurkoDatabase>(
            context,
            context.getDatabasePath(SNURKO_DATABASE_NAME).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
