package app.snurko

import androidx.compose.ui.window.ComposeUIViewController
import app.snurko.core.ios.RootViewControllerProvider
import app.snurko.di.challengeDataModule
import app.snurko.di.coreModule
import app.snurko.di.databaseModule
import app.snurko.di.platformModule
import app.snurko.di.presentationModule
import app.snurko.di.remindersModule
import app.snurko.reminders.domain.ReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.Koin
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private var startedKoin: Koin? = null

fun MainViewController(): UIViewController {
    val koin = startedKoin ?: startSnurko()
    val controller = ComposeUIViewController { App() }
    koin.get<RootViewControllerProvider>().attach(controller)
    return controller
}

private fun startSnurko(): Koin {
    val koin = startKoin {
        modules(
            platformModule,
            coreModule,
            databaseModule,
            challengeDataModule,
            remindersModule,
            presentationModule,
        )
    }.koin
    startedKoin = koin
    koin.get<ReminderSync>().start(applicationScope)
    return koin
}
