package app.snurko.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.snurko.challenge.data.DocumentPickerNotebookDocumentGateway
import app.snurko.challenge.domain.NotebookDocumentGateway
import app.snurko.challenge.presentation.sticker.ActivityViewControllerDayStickerSharer
import app.snurko.challenge.presentation.sticker.DayStickerSharer
import app.snurko.core.database.SnurkoDatabase
import app.snurko.core.database.snurkoDatabasePath
import app.snurko.core.ios.RootViewControllerProvider
import app.snurko.reminders.data.UserNotificationPermissionGateway
import app.snurko.reminders.data.UserNotificationReminderScheduler
import app.snurko.reminders.domain.NotificationPermissionGateway
import app.snurko.reminders.domain.ReminderScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { RootViewControllerProvider() }
    single {
        Room.databaseBuilder<SnurkoDatabase>(name = snurkoDatabasePath())
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single<NotebookDocumentGateway> { DocumentPickerNotebookDocumentGateway(get()) }
    single<DayStickerSharer> { ActivityViewControllerDayStickerSharer(get()) }
    single<ReminderScheduler> { UserNotificationReminderScheduler(get()) }
    single<NotificationPermissionGateway> { UserNotificationPermissionGateway() }
}
