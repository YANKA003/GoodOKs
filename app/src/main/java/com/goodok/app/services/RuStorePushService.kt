package com.goodok.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.goodok.app.R
import com.goodok.app.ui.MainActivity
import ru.rustore.sdk.pushclient.RuStoreMessagingService
import ru.rustore.sdk.pushclient.common.RemoteMessage

/**
 * RuStore Push Messaging Service
 *
 * This is the PRIMARY push notification service for GoodOK Messenger.
 * Works on devices without Google Play Services (Huawei, Russian devices, etc.)
 *
 * Setup:
 * 1. Create project in RuStore Developer Console: https://console.rustore.ru/
 * 2. Get Project ID and add to build.gradle as RUSTORE_PROJECT_ID
 * 3. Enable push notifications in RuStore Console
 * 4. Configure your backend to send pushes via RuStore API
 */
class RuStorePushService : RuStoreMessagingService() {

    companion object {
        private const val TAG = "RuStorePushService"
        private const val CHANNEL_ID = "goodok_messages"
        private const val CHANNEL_NAME = "Messages"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New RuStore push token received: ${token.take(20)}...")

        // Save token and send to backend
        PushManager.getInstance(applicationContext).saveRuStoreToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Push message received")

        // Extract data from message
        val data = message.data
        val title = data["title"] ?: data["notification_title"] ?: getString(R.string.app_name)
        val body = data["body"] ?: data["notification_body"] ?: ""
        val senderId = data["senderId"] ?: ""
        val senderName = data["senderName"] ?: ""
        val type = data["type"] ?: "message"
        val chatId = data["chatId"] ?: ""

        Log.d(TAG, "Message data: title=$title, body=$body, senderId=$senderId, type=$type")

        // Show notification
        sendNotification(title, body, senderId, senderName, type, chatId)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Some messages were deleted")
    }

    override fun onError(errors: List<ru.rustore.sdk.pushclient.common.errors.RuStorePushClientException>) {
        super.onError(errors)
        errors.forEach { error ->
            Log.e(TAG, "RuStore Push error: ${error.message}")
        }
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        senderId: String,
        senderName: String,
        type: String,
        chatId: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("senderId", senderId)
            putExtra("senderName", senderName)
            putExtra("type", type)
            putExtra("chatId", chatId)
            putExtra("openChat", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "GoodOK Messenger notifications"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Use unique notification ID based on time
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
