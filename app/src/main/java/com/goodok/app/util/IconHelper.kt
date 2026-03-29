package com.goodok.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.goodok.app.R

object IconHelper {

    const val THEME_DEFAULT = 0
    const val THEME_CLASSIC = 1
    const val THEME_MODERN = 2
    const val THEME_NEON = 3
    const val THEME_CHILDISH = 4

    private val iconAliases = mapOf(
        THEME_DEFAULT to "com.goodok.app.ui.auth.AuthActivityDefault",
        THEME_CLASSIC to "com.goodok.app.ui.auth.AuthActivityClassic",
        THEME_MODERN to "com.goodok.app.ui.auth.AuthActivityModern",
        THEME_NEON to "com.goodok.app.ui.auth.AuthActivityNeon",
        THEME_CHILDISH to "com.goodok.app.ui.auth.AuthActivityChildish"
    )

    fun setAppIcon(context: Context, theme: Int) {
        val pm = context.packageManager

        // Disable all aliases first
        iconAliases.values.forEach { alias ->
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context, alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Enable selected alias
        val selectedAlias = iconAliases[theme] ?: iconAliases[THEME_DEFAULT]
        try {
            pm.setComponentEnabledSetting(
                ComponentName(context, selectedAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            // Fallback to default
            pm.setComponentEnabledSetting(
                ComponentName(context, iconAliases[THEME_DEFAULT]!!),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun getIconResource(theme: Int): Int {
        return when (theme) {
            THEME_CLASSIC -> R.mipmap.ic_launcher_classic
            THEME_MODERN -> R.mipmap.ic_launcher_modern
            THEME_NEON -> R.mipmap.ic_launcher_neon
            THEME_CHILDISH -> R.mipmap.ic_launcher_childish
            else -> R.mipmap.ic_launcher
        }
    }
}
