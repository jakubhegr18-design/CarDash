package com.cartablet.data

import android.content.SharedPreferences

class SettingsManager(private val prefs: SharedPreferences) {
    companion object {
        private const val KEY_SETTINGS_PASSWORD = "settings_password"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_SECRET_KEY = "auth_secret_key"
        
        // New settings
        private const val KEY_AUTO_LAUNCH = "auto_launch"
        private const val KEY_HIDE_GUEST = "hide_guest"
        private const val KEY_ANIMATION_ENABLED = "animation_enabled"
        private const val KEY_CLOCK_24H = "clock_24h"
        private const val KEY_DASHBOARD_GLOW = "dashboard_glow"
    }

    fun setPassword(password: String) {
        prefs.edit().putString(KEY_SETTINGS_PASSWORD, password).apply()
    }

    fun getPassword(): String? = prefs.getString(KEY_SETTINGS_PASSWORD, null)

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun getSecretKey(): String? = prefs.getString(KEY_SECRET_KEY, null)

    fun setSecretKey(key: String) {
        prefs.edit().putString(KEY_SECRET_KEY, key).apply()
    }

    // New settings methods
    fun isAutoLaunchEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_LAUNCH, false)
    fun setAutoLaunchEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, enabled).apply()

    fun isHideGuestEnabled(): Boolean = prefs.getBoolean(KEY_HIDE_GUEST, false)
    fun setHideGuestEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_HIDE_GUEST, enabled).apply()

    fun isAnimationEnabled(): Boolean = prefs.getBoolean(KEY_ANIMATION_ENABLED, true)
    fun setAnimationEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ANIMATION_ENABLED, enabled).apply()

    fun isClock24h(): Boolean = prefs.getBoolean(KEY_CLOCK_24H, true)
    fun setClock24h(enabled: Boolean) = prefs.edit().putBoolean(KEY_CLOCK_24H, enabled).apply()

    fun isDashboardGlowEnabled(): Boolean = prefs.getBoolean(KEY_DASHBOARD_GLOW, true)
    fun setDashboardGlowEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DASHBOARD_GLOW, enabled).apply()
}
