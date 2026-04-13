package de.moritz.waschzeitrechner

import android.content.Context

const val SETTINGS_NAME = "wash_settings"

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromString(value: String): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class SupportedLanguage(
    val tag: String,
    val nativeName: String
)

val SupportedLanguages: List<SupportedLanguage> = listOf(
    SupportedLanguage("", "System Default"),
    SupportedLanguage("en", "English"),
    SupportedLanguage("de", "Deutsch"),
    SupportedLanguage("fr", "Français"),
    SupportedLanguage("es", "Español"),
    SupportedLanguage("it", "Italiano"),
    SupportedLanguage("pt", "Português"),
    SupportedLanguage("nl", "Nederlands"),
    SupportedLanguage("ru", "Русский"),
    SupportedLanguage("zh-Hans", "中文（简体）"),
    SupportedLanguage("ja", "日本語"),
    SupportedLanguage("ko", "한국어"),
    SupportedLanguage("ar", "العربية"),
    SupportedLanguage("tr", "Türkçe"),
    SupportedLanguage("pl", "Polski"),
    SupportedLanguage("sv", "Svenska"),
    SupportedLanguage("no", "Norsk"),
    SupportedLanguage("da", "Dansk"),
    SupportedLanguage("fi", "Suomi")
)

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode =
        ThemeMode.fromString(
            prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getDefaultProgram(): WashProgram =
        WashProgram.fromStorageKey(
            prefs.getString(KEY_DEFAULT_PROGRAM, WashProgram.STANDARD.storageKey)
        )

    fun setDefaultProgram(program: WashProgram) {
        prefs.edit().putString(KEY_DEFAULT_PROGRAM, program.storageKey).apply()
    }

    fun getSmartRecommendationsEnabled(): Boolean =
        prefs.getBoolean(KEY_SMART_RECOMMENDATIONS_ENABLED, false)

    fun setSmartRecommendationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_RECOMMENDATIONS_ENABLED, enabled).apply()
    }

    fun getPreferredFinishTime(): PreferredFinishTime? {
        val hour = prefs.getInt(KEY_DEFAULT_FINISH_HOUR, UNSET_TIME_VALUE)
        val minute = prefs.getInt(KEY_DEFAULT_FINISH_MINUTE, UNSET_TIME_VALUE)

        return if (hour in 0..23 && minute in 0..59) {
            PreferredFinishTime(hour = hour, minute = minute)
        } else {
            null
        }
    }

    fun setPreferredFinishTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DEFAULT_FINISH_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_DEFAULT_FINISH_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun getInitialFinishDateTime(now: java.time.LocalDateTime): java.time.LocalDateTime =
        calculateInitialFinishDateTime(now, getPreferredFinishTime())

    fun getProgramDurationMinutes(program: WashProgram): Int =
        prefs.getInt(program.durationPreferenceKey, program.defaultDurationMinutes).coerceAtLeast(1)

    fun setProgramDurationMinutes(program: WashProgram, durationMinutes: Int) {
        prefs.edit()
            .putInt(program.durationPreferenceKey, durationMinutes.coerceAtLeast(1))
            .apply()
    }

    companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DEFAULT_PROGRAM = "default_program"
        const val KEY_SMART_RECOMMENDATIONS_ENABLED = "smart_recommendations_enabled"
        const val KEY_DEFAULT_FINISH_HOUR = "default_finish_hour"
        const val KEY_DEFAULT_FINISH_MINUTE = "default_finish_minute"

        private const val UNSET_TIME_VALUE = -1
    }
}
