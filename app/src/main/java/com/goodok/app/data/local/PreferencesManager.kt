package com.goodok.app.data.local

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("goodok_prefs", Context.MODE_PRIVATE)

    var theme: Int
        get() = prefs.getInt("theme", 0)
        set(value) = prefs.edit().putInt("theme", value).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var isPremium: Boolean
        get() = prefs.getBoolean("isPremium", false)
        set(value) = prefs.edit().putBoolean("isPremium", value).apply()

    var premiumType: String
        get() = prefs.getString("premiumType", "") ?: ""
        set(value) = prefs.edit().putString("premiumType", value).apply()

    var premiumExpiry: Long
        get() = prefs.getLong("premiumExpiry", 0)
        set(value) = prefs.edit().putLong("premiumExpiry", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notificationsEnabled", true)
        set(value) = prefs.edit().putBoolean("notificationsEnabled", value).apply()

    var showOnlineStatus: Boolean
        get() = prefs.getBoolean("showOnlineStatus", true)
        set(value) = prefs.edit().putBoolean("showOnlineStatus", value).apply()

    var currentUserId: String?
        get() = prefs.getString("currentUserId", null)
        set(value) = prefs.edit().putString("currentUserId", value ?: "").apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
