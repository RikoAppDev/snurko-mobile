package app.snurko.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.challenge.data.SafNotebookDocumentGateway
import app.snurko.challenge.domain.NotebookDocumentGateway
import app.snurko.core.android.AlarmManagerDayRolloverScheduler
import app.snurko.core.database.SNURKO_DATABASE_NAME
import app.snurko.core.database.SnurkoDatabase
import app.snurko.reminders.data.AlarmManagerReminderScheduler
import app.snurko.reminders.data.AndroidNotificationPermissionGateway
import app.snurko.reminders.data.SystemReminderNotifier
import app.snurko.reminders.domain.NotificationPermissionGateway
import app.snurko.reminders.domain.ReminderNotifier
import app.snurko.core.domain.DayRolloverScheduler
import app.snurko.reminders.domain.ReminderScheduler
import app.snurko.widget.GlanceWidgetUpdater
import app.snurko.widget.WidgetUpdater
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
    single { SafNotebookDocumentGateway(androidContext().contentResolver) }
    single<NotebookDocumentGateway> { get<SafNotebookDocumentGateway>() }
    single<ReminderScheduler> { AlarmManagerReminderScheduler(androidContext()) }
    single<ReminderNotifier> { SystemReminderNotifier(androidContext()) }
    single { AndroidNotificationPermissionGateway(androidContext()) }
    single<NotificationPermissionGateway> { get<AndroidNotificationPermissionGateway>() }
    single<WidgetUpdater> { GlanceWidgetUpdater(androidContext()) }
    single<DayRolloverScheduler> { AlarmManagerDayRolloverScheduler(androidContext(), get()) }
}
