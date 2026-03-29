package com.goodok.app.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.goodok.app.GoodOKApp
import com.goodok.app.R
import com.goodok.app.data.local.PreferencesManager
import com.goodok.app.ui.main.MainActivity

class GoodOKMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MessagingService"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        // Save token locally
        val prefs = PreferencesManager(applicationContext)
        prefs.pushToken = token
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if message contains data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification: ${it.title} - ${it.body}")
            showNotification(it.title, it.body, remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return

        when (type) {
            "call" -> {
                // Handle incoming call
                val callId = data["call_id"]
                val callerId = data["caller_id"]
                val callerName = data["caller_name"]
                val isVideo = data["is_video"]?.toBoolean() ?: false

                showCallNotification(callId, callerName, isVideo)
            }
            "message" -> {
                val chatId = data["chat_id"]
                val senderName = data["sender_name"]
                val message = data["message"]

                showMessageNotification(senderName, message, chatId)
            }
        }
    }

    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        data["chat_id"]?.let { intent.putExtra("chat_id", it) }
        data["call_id"]?.let { intent.putExtra("call_id", it) }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, GoodOKApp.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: getString(R.string.app_name))
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun showCallNotification(callId: String?, callerName: String?, isVideo: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("call_id", callId)
            putExtra("caller_name", callerName)
            putExtra("is_video", isVideo)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isVideo) getString(R.string.video_call) else getString(R.string.voice_call)
        val body = getString(R.string.incoming_call) + (callerName?.let { " от $it" } ?: "")

        val notificationBuilder = NotificationCompat.Builder(this, GoodOKApp.CHANNEL_ID_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(callId?.hashCode() ?: 0, notificationBuilder.build())
    }

    private fun showMessageNotification(senderName: String?, message: String?, chatId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("chat_id", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, GoodOKApp.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName ?: getString(R.string.app_name))
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
