package com.goodok.app

import android.app.Application
import android.util.Log
import com.goodok.app.services.PushManager
import com.goodok.app.util.LanguageHelper
import com.google.firebase.FirebaseApp

class ZChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase FIRST (for Auth and Database)
        try {
            FirebaseApp.initializeApp(this)
            Log.d("ZChatApp", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("ZChatApp", "Firebase initialization failed", e)
        }

        // Initialize RuStore Push Manager
        try {
            PushManager.getInstance(this).initialize()
            Log.d("ZChatApp", "RuStore Push Manager initialized")
        } catch (e: Exception) {
            Log.e("ZChatApp", "RuStore Push initialization failed", e)
        }

        // Apply saved language
        try {
            val prefs = getSharedPreferences("goodok_prefs", MODE_PRIVATE)
            val language = prefs.getString("language", "ru") ?: "ru"
            LanguageHelper.setLanguage(this, language)
            Log.d("ZChatApp", "Language applied: $language")
        } catch (e: Exception) {
            Log.e("ZChatApp", "Error applying language", e)
        }

        // Global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ZChatApp", "Uncaught exception in ${thread.name}", throwable)
        }
    }

    override fun attachBaseContext(base: android.content.Context) {
        try {
            val prefs = base.getSharedPreferences("goodok_prefs", android.content.Context.MODE_PRIVATE)
            val language = prefs.getString("language", "ru") ?: "ru"
            val context = LanguageHelper.setLocale(base, language)
            super.attachBaseContext(context)
        } catch (e: Exception) {
            Log.e("ZChatApp", "Error in attachBaseContext", e)
            super.attachBaseContext(base)
        }
    }
}
