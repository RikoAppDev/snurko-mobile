package app.snurko.reminders.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.snurko.MainActivity
import app.snurko.reminders.domain.ReminderFiring
import app.snurko.reminders.domain.ReminderNotifier
import app.snurko.resources.Res
import app.snurko.resources.reminder_channel_description
import app.snurko.resources.reminder_channel_name
import app.snurko.resources.reminder_notification_done
import app.snurko.resources.reminder_notification_text
import org.jetbrains.compose.resources.getString

const val ACTION_REMINDER_CHECK_OFF = "app.snurko.action.REMINDER_CHECK_OFF"

private const val CHANNEL_ID = "habit_reminders"
private const val CONTENT_REQUEST_CODE = 0

class SystemReminderNotifier(
    private val context: Context,
) : ReminderNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun show(firing: ReminderFiring) {
        createChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(firing.habitName)
            .setContentText(getString(Res.string.reminder_notification_text, firing.day))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .addAction(
                0,
                getString(Res.string.reminder_notification_done),
                checkOff(firing.habitId),
            )
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(firing.habitId.toInt(), notification)
        }
    }

    override fun dismiss(habitId: Long) {
        notificationManager.cancel(habitId.toInt())
    }

    private suspend fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(Res.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(Res.string.reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openApp() = PendingIntent.getActivity(
        context,
        CONTENT_REQUEST_CODE,
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun checkOff(habitId: Long) = PendingIntent.getBroadcast(
        context,
        habitId.toInt(),
        Intent(context, ReminderCheckOffReceiver::class.java)
            .setAction(ACTION_REMINDER_CHECK_OFF)
            .putExtra(EXTRA_HABIT_ID, habitId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
