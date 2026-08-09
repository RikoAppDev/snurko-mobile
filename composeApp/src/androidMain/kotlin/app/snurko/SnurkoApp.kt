package app.snurko

import android.app.Application
import app.snurko.billing.data.PlayBillingGateway
import app.snurko.core.domain.DayRolloverScheduler
import app.snurko.di.challengeDataModule
import app.snurko.di.coreModule
import app.snurko.di.databaseModule
import app.snurko.di.platformModule
import app.snurko.di.presentationModule
import app.snurko.di.remindersModule
import app.snurko.di.widgetModule
import app.snurko.reminders.domain.ReminderSync
import app.snurko.widget.WidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SnurkoApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val reminderSync: ReminderSync by inject()
    private val widgetSync: WidgetSync by inject()
    private val dayRolloverScheduler: DayRolloverScheduler by inject()
    private val billingGateway: PlayBillingGateway by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SnurkoApp)
            modules(
                platformModule,
                coreModule,
                databaseModule,
                challengeDataModule,
                remindersModule,
                widgetModule,
                presentationModule,
            )
        }
        reminderSync.start(applicationScope)
        widgetSync.start(applicationScope)
        applicationScope.coroutineContext.job.invokeOnCompletion { billingGateway.close() }
        applicationScope.launch { dayRolloverScheduler.scheduleNextRollover() }
    }
}
