package com.fungast.app.settings

import platform.Foundation.NSUserDefaults

class IosAppSettings : AppSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    override var guideSeen: Boolean
        get() = defaults.boolForKey(KEY_GUIDE_SEEN)
        set(value) = defaults.setBool(value, KEY_GUIDE_SEEN)

    private companion object {
        const val KEY_GUIDE_SEEN = "guide_seen"
    }
}
