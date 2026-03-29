package com.goodok.app.util

import android.content.Context
import com.goodok.app.R

object IconHelper {

    const val THEME_DEFAULT = 0
    const val THEME_CLASSIC = 1
    const val THEME_MODERN = 2
    const val THEME_NEON = 3
    const val THEME_CHILDISH = 4

    // Theme icons simplified - all use default icon for now
    // To add alternate icons, create PNG files in mipmap directories

    fun setAppIcon(context: Context, theme: Int) {
        // Icon switching disabled - using default icon for all themes
        // To enable: add PNG icons and update AndroidManifest with activity-alias
    }

    fun getIconResource(theme: Int): Int {
        // All themes use default icon
        return R.mipmap.ic_launcher
    }
}
