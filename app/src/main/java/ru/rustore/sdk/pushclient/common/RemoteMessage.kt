package ru.rustore.sdk.pushclient.common

/**
 * STUB IMPLEMENTATION
 * Remote message from push notification
 */
data class RemoteMessage(
    val data: Map<String, String> = emptyMap(),
    val notification: Notification? = null
) {
    data class Notification(
        val title: String? = null,
        val body: String? = null
    )
}
