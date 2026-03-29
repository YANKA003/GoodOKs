package com.goodok.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.database.FirebaseDatabase
import com.goodok.app.data.local.PreferencesManager

class GoodOKApp : Application() {

    companion object {
        const val CHANNEL_ID_CALLS = "calls_channel"
        const val CHANNEL_ID_MESSAGES = "messages_channel"
        lateinit var instance: GoodOKApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Enable Firebase persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                getString(R.string.calls),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Звонки"
                enableVibration(true)
            }

            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                getString(R.string.chats),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сообщения"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(callChannel, messageChannel))
        }
    }
}
