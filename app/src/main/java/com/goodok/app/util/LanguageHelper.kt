package com.goodok.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LanguageHelper {

    private val supportedLanguages = listOf(
        "en", "ru", "be", "uk", "de", "fr", "es", "pt", "zh", "en-rGB"
    )

    fun setLanguage(context: Context, languageCode: String): Context {
        return updateResources(context, languageCode)
    }

    fun setLocale(context: Context, languageCode: String): Context {
        return updateResources(context, languageCode)
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "en-rGB" -> Locale("en", "GB")
            "zh" -> Locale.CHINESE
            else -> Locale(languageCode)
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    fun getLanguageDisplayName(code: String): String {
        return when (code) {
            "en" -> "English"
            "ru" -> "Русский"
            "be" -> "Беларуская"
            "uk" -> "Українська"
            "de" -> "Deutsch"
            "fr" -> "Français"
            "es" -> "Español"
            "pt" -> "Português"
            "zh" -> "中文"
            "en-rGB" -> "English (UK)"
            else -> code.uppercase()
        }
    }

    fun getSupportedLanguages(): List<String> = supportedLanguages
}
