package ru.rustore.sdk.pushclient

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.rustore.sdk.pushclient.common.RemoteMessage

/**
 * STUB IMPLEMENTATION
 * Base service for handling RuStore push notifications.
 * Extend this class to receive push notifications.
 */
abstract class RuStoreMessagingService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Called when a new push token is generated.
     */
    open fun onNewToken(token: String) {}
    
    /**
     * Called when a push message is received.
     */
    open fun onMessageReceived(message: RemoteMessage) {}
    
    /**
     * Called when messages are deleted on the server.
     */
    open fun onDeletedMessages() {}
    
    /**
     * Called when an error occurs.
     */
    open fun onError(errors: List<ru.rustore.sdk.pushclient.common.errors.RuStorePushClientException>) {}
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle push message intent
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }
    
    private fun handleIntent(intent: Intent) {
        // Stub implementation - parse push data from intent
        val data = intent.extras?.keySet()
            ?.filter { it != null }
            ?.associateWith { intent.extras?.getString(it) ?: "" }
            ?: emptyMap()
        
        if (data.isNotEmpty()) {
            val message = RemoteMessage(data = data)
            onMessageReceived(message)
        }
    }
}
