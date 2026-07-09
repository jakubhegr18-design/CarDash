package com.cartablet.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Folder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String = "#4FC3F7",
    val apps: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("color", color)
        put("apps", JSONArray(apps))
    }

    companion object {
        fun fromJson(obj: JSONObject): Folder = Folder(
            id = obj.getString("id"),
            name = obj.getString("name"),
            color = obj.optString("color", "#4FC3F7"),
            apps = obj.optJSONArray("apps")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )
    }
}

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "\uD83D\uDC64",
    val iconUri: String? = null,
    val isGuest: Boolean = false,
    val pinnedApps: List<String> = emptyList(),
    val folders: List<Folder> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("icon", icon)
        put("iconUri", iconUri ?: JSONObject.NULL)
        put("isGuest", isGuest)
        put("pinnedApps", JSONArray(pinnedApps))
        put("folders", JSONArray(folders.map { it.toJson() }))
    }

    companion object {
        fun fromJson(obj: JSONObject): Profile = Profile(
            id = obj.getString("id"),
            name = obj.getString("name"),
            icon = obj.optString("icon", "\uD83D\uDC64"),
            iconUri = if (obj.isNull("iconUri")) null else obj.optString("iconUri"),
            isGuest = obj.optBoolean("isGuest", false),
            pinnedApps = obj.optJSONArray("pinnedApps")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            folders = obj.optJSONArray("folders")?.let { arr ->
                (0 until arr.length()).map { Folder.fromJson(arr.getJSONObject(it)) }
            } ?: emptyList()
        )
    }
}
