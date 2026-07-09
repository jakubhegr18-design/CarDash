package com.cartablet.data

import android.content.SharedPreferences
import org.json.JSONArray
import java.util.UUID

class ProfileManager(private val prefs: SharedPreferences) {

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_CURRENT_PROFILE_ID = "current_profile_id"
    }

    fun getProfiles(): List<Profile> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return createDefaults()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { Profile.fromJson(array.getJSONObject(it)) }
        } catch (_: Exception) {
            createDefaults()
        }
    }

    fun getCurrentProfile(): Profile {
        val currentId = prefs.getString(KEY_CURRENT_PROFILE_ID, null)
        val profiles = getProfiles()
        return profiles.find { it.id == currentId } ?: profiles.firstOrNull() ?: createDefaults().first()
    }

    fun setCurrentProfile(id: String) {
        prefs.edit().putString(KEY_CURRENT_PROFILE_ID, id).apply()
    }

    fun addProfile(profile: Profile) {
        val profiles = getProfiles().toMutableList()
        profiles.add(profile)
        saveProfiles(profiles)
    }

    fun removeProfile(id: String) {
        val profiles = getProfiles().toMutableList()
        profiles.removeAll { it.id == id }
        val current = prefs.getString(KEY_CURRENT_PROFILE_ID, null)
        if (current == id) {
            prefs.edit().putString(KEY_CURRENT_PROFILE_ID, profiles.firstOrNull()?.id).apply()
        }
        saveProfiles(profiles)
    }

    fun updateProfile(profile: Profile) {
        val profiles = getProfiles().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
            saveProfiles(profiles)
        }
    }

    fun togglePinApp(profileId: String, packageName: String) {
        val profiles = getProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx < 0) return
        val p = profiles[idx]
        val pinned = p.pinnedApps.toMutableList()
        if (pinned.contains(packageName)) pinned.remove(packageName) else pinned.add(packageName)
        profiles[idx] = p.copy(pinnedApps = pinned)
        saveProfiles(profiles)
    }

    fun isAppPinned(profileId: String, packageName: String): Boolean {
        val profile = getProfiles().find { it.id == profileId } ?: return false
        return profile.pinnedApps.contains(packageName) || profile.folders.any { it.apps.contains(packageName) }
    }

    fun addFolder(profileId: String, folder: Folder) {
        val profiles = getProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx < 0) return
        profiles[idx] = profiles[idx].copy(folders = profiles[idx].folders + folder)
        saveProfiles(profiles)
    }

    fun removeFolder(profileId: String, folderId: String) {
        val profiles = getProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx < 0) return
        profiles[idx] = profiles[idx].copy(folders = profiles[idx].folders.filter { it.id != folderId })
        saveProfiles(profiles)
    }

    fun addAppToFolder(profileId: String, folderId: String, packageName: String) {
        val profiles = getProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx < 0) return
        val updatedFolders = profiles[idx].folders.map { f ->
            if (f.id == folderId) f.copy(apps = f.apps + packageName) else f
        }
        profiles[idx] = profiles[idx].copy(folders = updatedFolders)
        saveProfiles(profiles)
    }

    fun removeAppFromFolder(profileId: String, folderId: String, packageName: String) {
        val profiles = getProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx < 0) return
        val updatedFolders = profiles[idx].folders.map { f ->
            if (f.id == folderId) f.copy(apps = f.apps - packageName) else f
        }
        profiles[idx] = profiles[idx].copy(folders = updatedFolders)
        saveProfiles(profiles)
    }

    fun getFolder(profileId: String, folderId: String): Folder? {
        return getProfiles().find { it.id == profileId }?.folders?.find { it.id == folderId }
    }

    fun createGuestProfile(): Profile {
        val guest = Profile(
            id = UUID.randomUUID().toString(),
            name = "Guest",
            icon = "\uD83D\uDC65",
            isGuest = true
        )
        setCurrentProfile(guest.id)
        return guest
    }

    private fun saveProfiles(profiles: List<Profile>) {
        val array = JSONArray(profiles.map { it.toJson() })
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    private fun createDefaults(): List<Profile> {
        val navFolder = Folder(name = "Nav", color = "#4CAF50", apps = listOf("com.google.android.apps.maps", "com.waze"))
        val mediaFolder = Folder(name = "Media", color = "#2196F3", apps = listOf("com.spotify.music", "com.google.android.youtube"))
        val default = Profile(
            name = "Default",
            icon = "\uD83D\uDC64",
            folders = listOf(navFolder, mediaFolder)
        )
        val array = JSONArray(listOf(default.toJson()))
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
        return listOf(default)
    }
}
