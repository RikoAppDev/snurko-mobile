package app.snurko.reminders.domain

enum class NotificationPermission {
    GRANTED,
    DENIED,
}

/** Asks the platform for permission to post notifications, and points at its settings. */
interface NotificationPermissionGateway {

    suspend fun current(): NotificationPermission

    suspend fun request(): NotificationPermission

    fun openSettings()
}
