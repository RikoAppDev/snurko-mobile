package app.snurko.reminders.domain

class FakeNotificationPermissionGateway : NotificationPermissionGateway {

    var permission: NotificationPermission = NotificationPermission.GRANTED

    var requestCount = 0
        private set

    var settingsOpenedCount = 0
        private set

    override suspend fun current(): NotificationPermission = permission

    override suspend fun request(): NotificationPermission {
        requestCount++
        return permission
    }

    override fun openSettings() {
        settingsOpenedCount++
    }
}
