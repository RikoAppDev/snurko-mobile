package app.snurko.reminders.data

import app.snurko.reminders.domain.NotificationPermission
import app.snurko.reminders.domain.NotificationPermissionGateway
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserNotificationPermissionGateway : NotificationPermissionGateway {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun current(): NotificationPermission = authorization().toPermission()

    /** The system dialog only ever appears once; after that this settles for what is already set. */
    override suspend fun request(): NotificationPermission {
        if (authorization() == UNAuthorizationStatusNotDetermined) {
            askForAuthorization()
        }
        return authorization().toPermission()
    }

    override fun openSettings() {
        val settings = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(settings, emptyMap<Any?, Any>(), null)
    }

    private suspend fun authorization(): UNAuthorizationStatus = suspendCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusDenied)
        }
    }

    private suspend fun askForAuthorization(): Boolean = suspendCoroutine { continuation ->
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { isGranted, _ -> continuation.resume(isGranted) }
    }
}

private fun UNAuthorizationStatus.toPermission(): NotificationPermission = when (this) {
    UNAuthorizationStatusAuthorized,
    UNAuthorizationStatusProvisional,
    UNAuthorizationStatusEphemeral,
    -> NotificationPermission.GRANTED

    else -> NotificationPermission.DENIED
}
