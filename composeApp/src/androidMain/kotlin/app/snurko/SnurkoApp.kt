package app.snurko

import android.app.Application
import app.snurko.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SnurkoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SnurkoApp)
            modules(appModule)
        }
    }
}
