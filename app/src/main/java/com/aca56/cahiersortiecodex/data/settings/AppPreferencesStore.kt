package com.aca56.cahiersortiecodex.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val DefaultPrimaryColorHex = "#00684E"
const val DefaultSecondaryColorHex = "#1F7A5E"
const val DefaultTertiaryColorHex = "#2F8B68"
const val DefaultInactivityTimeoutMillis = 120_000L
const val DefaultSuccessPopupDurationMillis = 3_500L
const val DefaultErrorPopupDurationMillis = 6_000L

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val primaryColorHex: String = DefaultPrimaryColorHex,
    val secondaryColorHex: String = DefaultSecondaryColorHex,
    val tertiaryColorHex: String = DefaultTertiaryColorHex,
    val inactivityTimeoutMillis: Long = DefaultInactivityTimeoutMillis,
    val successPopupDurationMillis: Long = DefaultSuccessPopupDurationMillis,
    val errorPopupDurationMillis: Long = DefaultErrorPopupDurationMillis,
    val animationsEnabled: Boolean = true,
    val crewsEnabled: Boolean = false,
    val levelControlEnabled: Boolean = true,
)

class AppPreferencesStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val preferencesState = MutableStateFlow(loadPreferences())

    val preferencesFlow: StateFlow<AppPreferences> = preferencesState.asStateFlow()

    fun currentPreferences(): AppPreferences = preferencesState.value

    fun saveThemeMode(themeMode: ThemeMode) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
        refresh()
    }

    fun saveThemeColors(
        primaryColorHex: String,
        secondaryColorHex: String,
        tertiaryColorHex: String,
    ) {
        preferences.edit()
            .putString(KEY_PRIMARY_COLOR_HEX, normalizeHex(primaryColorHex))
            .putString(KEY_SECONDARY_COLOR_HEX, normalizeHex(secondaryColorHex))
            .putString(KEY_TERTIARY_COLOR_HEX, normalizeHex(tertiaryColorHex))
            .apply()
        refresh()
    }

    fun saveAdvancedBehavior(
        inactivityTimeoutMillis: Long,
        successPopupDurationMillis: Long,
        errorPopupDurationMillis: Long,
        animationsEnabled: Boolean,
        crewsEnabled: Boolean,
        levelControlEnabled: Boolean = currentPreferences().levelControlEnabled,
    ) {
        preferences.edit()
            .putLong(KEY_INACTIVITY_TIMEOUT_MILLIS, inactivityTimeoutMillis)
            .putLong(KEY_SUCCESS_POPUP_DURATION_MILLIS, successPopupDurationMillis)
            .putLong(KEY_ERROR_POPUP_DURATION_MILLIS, errorPopupDurationMillis)
            .putBoolean(KEY_ANIMATIONS_ENABLED, animationsEnabled)
            .putBoolean(KEY_CREWS_ENABLED, crewsEnabled)
            .putBoolean(KEY_LEVEL_CONTROL_ENABLED, levelControlEnabled)
            .apply()
        refresh()
    }

    fun resetToDefaults() {
        preferences.edit().clear().apply()
        refresh()
    }

    fun restorePreferences(appPreferences: AppPreferences) {
        preferences.edit()
            .putString(KEY_THEME_MODE, appPreferences.themeMode.name)
            .putString(KEY_PRIMARY_COLOR_HEX, normalizeHex(appPreferences.primaryColorHex))
            .putString(KEY_SECONDARY_COLOR_HEX, normalizeHex(appPreferences.secondaryColorHex))
            .putString(KEY_TERTIARY_COLOR_HEX, normalizeHex(appPreferences.tertiaryColorHex))
            .putLong(KEY_INACTIVITY_TIMEOUT_MILLIS, appPreferences.inactivityTimeoutMillis)
            .putLong(KEY_SUCCESS_POPUP_DURATION_MILLIS, appPreferences.successPopupDurationMillis)
            .putLong(KEY_ERROR_POPUP_DURATION_MILLIS, appPreferences.errorPopupDurationMillis)
            .putBoolean(KEY_ANIMATIONS_ENABLED, appPreferences.animationsEnabled)
            .putBoolean(KEY_CREWS_ENABLED, appPreferences.crewsEnabled)
            .putBoolean(KEY_LEVEL_CONTROL_ENABLED, appPreferences.levelControlEnabled)
            .apply()
        refresh()
    }

    private fun refresh() {
        preferencesState.value = loadPreferences()
    }

    private fun loadPreferences(): AppPreferences {
        val themeMode = preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
            ?: ThemeMode.SYSTEM

        return AppPreferences(
            themeMode = themeMode,
            primaryColorHex = normalizeHex(
                preferences.getString(KEY_PRIMARY_COLOR_HEX, DefaultPrimaryColorHex)
                    ?: DefaultPrimaryColorHex,
            ),
            secondaryColorHex = normalizeHex(
                preferences.getString(KEY_SECONDARY_COLOR_HEX, DefaultSecondaryColorHex)
                    ?: DefaultSecondaryColorHex,
            ),
            tertiaryColorHex = normalizeHex(
                preferences.getString(KEY_TERTIARY_COLOR_HEX, DefaultTertiaryColorHex)
                    ?: DefaultTertiaryColorHex,
            ),
            inactivityTimeoutMillis = preferences.getLong(
                KEY_INACTIVITY_TIMEOUT_MILLIS,
                DefaultInactivityTimeoutMillis,
            ),
            successPopupDurationMillis = preferences.getLong(
                KEY_SUCCESS_POPUP_DURATION_MILLIS,
                DefaultSuccessPopupDurationMillis,
            ),
            errorPopupDurationMillis = preferences.getLong(
                KEY_ERROR_POPUP_DURATION_MILLIS,
                DefaultErrorPopupDurationMillis,
            ),
            animationsEnabled = preferences.getBoolean(
                KEY_ANIMATIONS_ENABLED,
                true,
            ),
            crewsEnabled = preferences.getBoolean(
                KEY_CREWS_ENABLED,
                false,
            ),
            levelControlEnabled = preferences.getBoolean(
                KEY_LEVEL_CONTROL_ENABLED,
                true,
            ),
        )
    }

    private fun normalizeHex(value: String): String {
        val cleaned = value.trim().uppercase().removePrefix("#")
        return "#$cleaned"
    }

    companion object {
        private const val PREFERENCES_NAME = "app_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PRIMARY_COLOR_HEX = "primary_color_hex"
        private const val KEY_SECONDARY_COLOR_HEX = "secondary_color_hex"
        private const val KEY_TERTIARY_COLOR_HEX = "tertiary_color_hex"
        private const val KEY_INACTIVITY_TIMEOUT_MILLIS = "inactivity_timeout_millis"
        private const val KEY_SUCCESS_POPUP_DURATION_MILLIS = "success_popup_duration_millis"
        private const val KEY_ERROR_POPUP_DURATION_MILLIS = "error_popup_duration_millis"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_CREWS_ENABLED = "crews_enabled"
        private const val KEY_LEVEL_CONTROL_ENABLED = "level_control_enabled"
    }
}
