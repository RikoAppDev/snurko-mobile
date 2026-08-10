package app.snurko.reminders.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import app.snurko.reminders.domain.NotificationPermission
import app.snurko.reminders.domain.NotificationPermissionGateway
import kotlinx.coroutines.CompletableDeferred

class AndroidNotificationPermissionGateway(
    private val context: Context,
) : NotificationPermissionGateway {

    private var launcher: ActivityResultLauncher<String>? = null
    private var pendingRequest: CompletableDeferred<Boolean>? = null

    fun attach(launcher: ActivityResultLauncher<String>) {
        this.launcher = launcher
    }

    fun detach() {
        launcher = null
    }

    fun onPermissionResult(isGranted: Boolean) {
        pendingRequest?.complete(isGranted)
        pendingRequest = null
    }

    override suspend fun current(): NotificationPermission =
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationPermission.GRANTED
        } else {
            NotificationPermission.DENIED
        }

    /** Once notifications are switched off in the settings the system dialog no longer appears. */
    override suspend fun request(): NotificationPermission {
        if (current() == NotificationPermission.GRANTED) return NotificationPermission.GRANTED
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return NotificationPermission.DENIED

        val current = launcher ?: return NotificationPermission.DENIED
        val awaited = CompletableDeferred<Boolean>()
        pendingRequest?.complete(false)
        pendingRequest = awaited
        current.launch(Manifest.permission.POST_NOTIFICATIONS)

        return if (awaited.await()) NotificationPermission.GRANTED else NotificationPermission.DENIED
    }

    override fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
