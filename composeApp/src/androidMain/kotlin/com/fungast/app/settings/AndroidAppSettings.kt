package com.fungast.app.settings

import android.content.Context

class AndroidAppSettings(context: Context) : AppSettings {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override var guideSeen: Boolean
        get() = prefs.getBoolean(KEY_GUIDE_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_GUIDE_SEEN, value).apply()

    private companion object {
        const val KEY_GUIDE_SEEN = "guide_seen"
    }
}
