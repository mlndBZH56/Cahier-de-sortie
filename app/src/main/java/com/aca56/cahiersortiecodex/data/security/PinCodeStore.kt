package com.aca56.cahiersortiecodex.data.security

import android.content.Context
import java.util.Calendar
import java.util.Locale

class PinCodeStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(KEY_PIN)

    fun savePin(pin: String) {
        preferences.edit().putString(KEY_PIN, pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        return preferences.getString(KEY_PIN, null) == pin
    }

    fun saveNormalPin(pin: String) {
        savePin(pin)
    }

    fun verifyNormalPin(pin: String): Boolean {
        return verifyPin(pin)
    }

    fun saveSuperAdminPin(pin: String) {
        preferences.edit().putString(KEY_SUPER_ADMIN_PIN, pin).apply()
    }

    fun verifySuperAdminPin(pin: String): Boolean {
        return getSuperAdminPin() == pin
    }

    fun verifyEmergencySuperAdminPin(
        pin: String,
        calendar: Calendar = Calendar.getInstance(),
    ): Boolean {
        return buildEmergencySuperAdminPin(calendar) == pin
    }

    fun getSuperAdminPin(): String {
        return preferences.getString(KEY_SUPER_ADMIN_PIN, DEFAULT_SUPER_ADMIN_PIN)
            ?: DEFAULT_SUPER_ADMIN_PIN
    }

    fun getNormalPin(): String? {
        return preferences.getString(KEY_PIN, null)
    }

    fun restorePins(
        normalPin: String?,
        superAdminPin: String?,
    ) {
        preferences.edit().apply {
            if (normalPin.isNullOrBlank()) {
                remove(KEY_PIN)
            } else {
                putString(KEY_PIN, normalPin)
            }
            if (superAdminPin.isNullOrBlank()) {
                remove(KEY_SUPER_ADMIN_PIN)
            } else {
                putString(KEY_SUPER_ADMIN_PIN, superAdminPin)
            }
            apply()
        }
    }

    fun deletePin() {
        preferences.edit().remove(KEY_PIN).apply()
    }

    fun resetAllPins() {
        preferences.edit()
            .remove(KEY_PIN)
            .remove(KEY_SUPER_ADMIN_PIN)
            .apply()
    }

    companion object {
        const val DEFAULT_SUPER_ADMIN_PIN = "123456"

        private const val PREFERENCES_NAME = "settings_security"
        private const val KEY_PIN = "settings_pin"
        private const val KEY_SUPER_ADMIN_PIN = "super_admin_pin"

        private fun buildEmergencySuperAdminPin(calendar: Calendar): String {
            val day = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.DAY_OF_MONTH))
            val month = String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.MONTH) + 1)
            return "${day}${month}56"
        }
    }
}
