package com.goodok.app.services

import android.content.Context
import android.util.Log
import com.goodok.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.rustore.sdk.pushclient.RuStorePushClient
import ru.rustore.sdk.pushclient.common.errors.RuStorePushClientException

/**
 * Push Manager for RuStore Push Notifications
 *
 * PRIMARY push notification service for GoodOK Messenger.
 * Works on devices without Google Play Services (Huawei, Russian devices, etc.)
 *
 * Setup:
 * 1. Create project in RuStore Developer Console: https://console.rustore.ru/
 * 2. Get Project ID and add to build.gradle as RUSTORE_PROJECT_ID
 * 3. Enable push notifications in RuStore Console
 * 4. Send pushes from your backend using RuStore API
 *
 * RuStore API for sending pushes:
 * POST https://push.rustore.ru/api/v1/projects/{projectId}/messages
 * Authorization: Bearer {your_api_key}
 * {
 *   "message": {
 *     "token": "user_device_token",
 *     "data": {
 *       "title": "Sender Name",
 *       "body": "Message text",
 *       "senderId": "sender_uid",
 *       "type": "message"
 *     }
 *   }
 * }
 */
class PushManager(private val context: Context) {

    companion object {
        private const val TAG = "PushManager"
        private const val PREFS_NAME = "goodok_prefs"
        private const val KEY_RUSTORE_TOKEN = "rustore_push_token"

        // RuStore Project ID from BuildConfig
        val RUSTORE_PROJECT_ID: String = BuildConfig.RUSTORE_PROJECT_ID

        @Volatile
        private var instance: PushManager? = null

        fun getInstance(context: Context): PushManager {
            return instance ?: synchronized(this) {
                instance ?: PushManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Initialize RuStore Push SDK.
     * Call this in your Application class onCreate()
     */
    fun initialize() {
        Log.d(TAG, "Initializing RuStore Push Manager...")
        Log.d(TAG, "RuStore Project ID: ${RUSTORE_PROJECT_ID.take(20)}...")

        initRuStorePush()
    }

    private fun initRuStorePush() {
        try {
            Log.d(TAG, "Initializing RuStore Push SDK...")

            // Initialize RuStore Push Client
            RuStorePushClient.init(
                context = context,
                projectId = RUSTORE_PROJECT_ID,
                internalPrefix = "goodok_"
            )

            Log.d(TAG, "RuStore Push SDK initialized, getting token...")

            // Get current token
            RuStorePushClient.getToken()
                .addOnSuccessListener { token ->
                    if (token != null) {
                        Log.d(TAG, "RuStore token received: ${token.take(30)}...")
                        saveRuStoreToken(token)
                    } else {
                        Log.d(TAG, "RuStore token is null, will be generated")
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Failed to get RuStore token: ${error.message}")
                    // Token will be generated and delivered via onNewToken in service
                }

            Log.d(TAG, "RuStore Push SDK initialization complete")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing RuStore Push: ${e.message}", e)
        }
    }

    /**
     * Save RuStore token to preferences and send to backend
     */
    fun saveRuStoreToken(token: String) {
        prefs.edit().putString(KEY_RUSTORE_TOKEN, token).apply()
        Log.d(TAG, "RuStore token saved")

        // Send to backend
        sendTokenToBackend(token)
    }

    /**
     * Get the current RuStore push token
     */
    fun getCurrentToken(): String? {
        return prefs.getString(KEY_RUSTORE_TOKEN, null)
    }

    /**
     * Subscribe to a topic (if supported by RuStore)
     * Note: RuStore Push topic subscriptions may differ from FCM
     */
    fun subscribeToTopic(topic: String) {
        Log.d(TAG, "Topic subscription requested: $topic")
        // RuStore Push may have different topic subscription mechanism
        // Check RuStore documentation for topic messaging
    }

    /**
     * Unsubscribe from a topic
     */
    fun unsubscribeFromTopic(topic: String) {
        Log.d(TAG, "Topic unsubscription requested: $topic")
    }

    private fun sendTokenToBackend(token: String) {
        scope.launch {
            try {
                // TODO: Send token to your backend server
                // Example with Firebase Realtime Database:
                //
                // val userId = getCurrentUserId()
                // if (userId != null) {
                //     val db = FirebaseDatabase.getInstance().reference
                //     db.child("users").child(userId).child("rustoreToken").setValue(token)
                // }

                Log.d(TAG, "RuStore token ready: ${token.take(20)}...")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send token to backend: ${e.message}")
            }
        }
    }

    /**
     * Delete push token (for logout)
     */
    fun deleteToken() {
        prefs.edit().remove(KEY_RUSTORE_TOKEN).apply()

        try {
            RuStorePushClient.deleteToken()
                .addOnSuccessListener {
                    Log.d(TAG, "RuStore token deleted")
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Failed to delete RuStore token: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting RuStore token: ${e.message}")
        }
    }

    /**
     * Check if RuStore Push is available
     */
    fun isAvailable(): Boolean {
        return try {
            // Check if RuStore is installed and available
            val intent = context.packageManager.getLaunchIntentForPackage("ru.rustore.app")
            intent != null
        } catch (e: Exception) {
            false
        }
    }
}
