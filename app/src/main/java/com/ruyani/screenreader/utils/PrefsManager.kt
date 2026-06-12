package com.ruyani.screenreader.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ruyani_prefs"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SPEECH_PITCH = "speech_pitch"
        private const val KEY_TTS_ENGINE = "tts_engine"
        private const val KEY_READ_NOTIFICATIONS = "read_notifications"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_HIGHLIGHT_ENABLED = "highlight_enabled"
        private const val KEY_NAVIGATION_MODE = "navigation_mode"

        const val NAV_MODE_ALL = 0
        const val NAV_MODE_HEADINGS = 1
        const val NAV_MODE_LINKS = 2
        const val NAV_MODE_CONTROLS = 3
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var ttsEngine: String?
        get() = prefs.getString(KEY_TTS_ENGINE, null)
        set(value) = prefs.edit().putString(KEY_TTS_ENGINE, value).apply()

    var readNotifications: Boolean
        get() = prefs.getBoolean(KEY_READ_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_READ_NOTIFICATIONS, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var highlightEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIGHLIGHT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HIGHLIGHT_ENABLED, value).apply()

    var navigationMode: Int
        get() = prefs.getInt(KEY_NAVIGATION_MODE, NAV_MODE_ALL)
        set(value) = prefs.edit().putInt(KEY_NAVIGATION_MODE, value).apply()
}
