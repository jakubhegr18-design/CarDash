package com.cartablet.data

import android.content.SharedPreferences

class SettingsManager(private val prefs: SharedPreferences) {
    companion object {
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_LOCK_TYPE = "lock_type" // NONE, PASSWORD, PIN
        private const val KEY_SETTINGS_PASSWORD = "settings_password"
        private const val KEY_SECRET_KEY = "auth_secret_key"
        
        // New settings
        private const val KEY_AUTO_LAUNCH = "auto_launch"
        private const val KEY_HIDE_GUEST = "hide_guest"
        private const val KEY_ANIMATION_ENABLED = "animation_enabled"
        private const val KEY_CLOCK_24H = "clock_24h"
        private const val KEY_DASHBOARD_GLOW = "dashboard_glow"
        private const val KEY_WALLPAPER_TYPE = "wallpaper_type"
        private const val KEY_KIDS_MODE_ENABLED = "kids_mode_enabled"
        private const val KEY_KIDS_MODE_COLLECTION = "kids_mode_collection"
        private const val KEY_SPEEDOMETER_ENABLED = "speedometer_enabled"
        private const val KEY_PROFILE_LOCK = "profile_lock_enabled"
        private const val KEY_STRICT_LOCK_ENABLED = "strict_lock_enabled"
        private const val KEY_STRICT_LOCK_COLLECTIONS = "strict_lock_collections"
        private const val KEY_REMOTE_CONTROL_ENABLED = "remote_control_enabled"
        private const val KEY_REMOTE_CONTROL_PORT = "remote_control_port"
        private const val KEY_REMOTE_CONTROL_MODE = "remote_control_mode"
        private const val KEY_TV_MODE = "tv_mode"
    }

    fun setPassword(password: String) {
        prefs.edit().putString(KEY_SETTINGS_PASSWORD, password).apply()
    }

    fun getPassword(): String? = prefs.getString(KEY_SETTINGS_PASSWORD, null)

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun getLockType(): String = prefs.getString(KEY_LOCK_TYPE, "PASSWORD") ?: "PASSWORD"

    fun setLockType(type: String) {
        prefs.edit().putString(KEY_LOCK_TYPE, type).apply()
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

    fun getWallpaperType(): String = prefs.getString(KEY_WALLPAPER_TYPE, "CYBER_GLOW") ?: "CYBER_GLOW"
    fun setWallpaperType(type: String) = prefs.edit().putString(KEY_WALLPAPER_TYPE, type).apply()

    fun isKidsModeEnabled(): Boolean = prefs.getBoolean(KEY_KIDS_MODE_ENABLED, false)
    fun setKidsModeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_KIDS_MODE_ENABLED, enabled).apply()

    fun getKidsModeCollectionId(): String? = prefs.getString(KEY_KIDS_MODE_COLLECTION, null)
    fun setKidsModeCollectionId(id: String?) = prefs.edit().putString(KEY_KIDS_MODE_COLLECTION, id).apply()

    fun isSpeedometerEnabled(): Boolean = prefs.getBoolean(KEY_SPEEDOMETER_ENABLED, true)
    fun setSpeedometerEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_SPEEDOMETER_ENABLED, enabled).apply()

    fun isProfileLockEnabled(): Boolean = prefs.getBoolean(KEY_PROFILE_LOCK, false)
    fun setProfileLockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PROFILE_LOCK, enabled).apply()

    fun isStrictLockEnabled(): Boolean = prefs.getBoolean(KEY_STRICT_LOCK_ENABLED, false)
    fun setStrictLockEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_STRICT_LOCK_ENABLED, enabled).apply()

    fun getStrictLockCollections(): Set<String> = prefs.getStringSet(KEY_STRICT_LOCK_COLLECTIONS, emptySet()) ?: emptySet()
    fun setStrictLockCollections(ids: Set<String>) = prefs.edit().putStringSet(KEY_STRICT_LOCK_COLLECTIONS, ids).apply()

    fun isRemoteControlEnabled(): Boolean = prefs.getBoolean(KEY_REMOTE_CONTROL_ENABLED, false)
    fun setRemoteControlEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_REMOTE_CONTROL_ENABLED, enabled).apply()

    fun getRemoteControlPort(): Int = prefs.getInt(KEY_REMOTE_CONTROL_PORT, 8080)
    fun setRemoteControlPort(port: Int) = prefs.edit().putInt(KEY_REMOTE_CONTROL_PORT, port).apply()

    fun getRemoteControlMode(): String = prefs.getString(KEY_REMOTE_CONTROL_MODE, "WIFI") ?: "WIFI"
    fun setRemoteControlMode(mode: String) = prefs.edit().putString(KEY_REMOTE_CONTROL_MODE, mode).apply()

    fun isTvModeEnabled(): Boolean = prefs.getBoolean(KEY_TV_MODE, false)
    fun setTvModeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_TV_MODE, enabled).apply()
}
