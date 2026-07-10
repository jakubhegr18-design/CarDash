package com.cartablet.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.app.admin.DevicePolicyManager
import android.os.PowerManager
import com.cartablet.data.ProfileManager
import com.cartablet.data.SettingsManager
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class RemoteControlServer(
    private val context: Context,
    private val profileManager: ProfileManager,
    private val settingsManager: SettingsManager,
    port: Int = 8080
) : NanoHTTPD(port) {

    private var running = false

    fun isRunning(): Boolean = running

    override fun start() {
        if (!running) {
            super.start()
            running = true
        }
    }

    override fun stop() {
        if (running) {
            super.stop()
            running = false
        }
    }

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.method == Method.GET && session.uri == "/status" -> handleStatus()
            session.method == Method.POST && session.uri == "/switch-profile" -> handleSwitchProfile(session)
            session.method == Method.POST && session.uri == "/lock" -> handleLock()
            session.method == Method.POST && session.uri == "/unlock" -> handleUnlock()
            session.method == Method.POST && session.uri == "/launch-app" -> handleLaunchApp(session)
            session.method == Method.POST && session.uri == "/volume" -> handleVolume(session)
            session.method == Method.POST && session.uri == "/sleep" -> handleSleep()
            session.method == Method.GET && session.uri == "/profiles" -> handleProfiles()
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"not found"}""")
        }
    }

    private fun jsonResponse(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", body)

    private fun handleStatus(): Response {
        val currentProfile = profileManager.getCurrentProfile()
        val json = JSONObject().apply {
            put("currentProfileId", currentProfile.id)
            put("currentProfileName", currentProfile.name)
            put("isLocked", settingsManager.isLockEnabled())
            put("kidsMode", settingsManager.isKidsModeEnabled())
            put("strictLock", settingsManager.isStrictLockEnabled())
            put("speedometer", settingsManager.isSpeedometerEnabled())
            put("clock24h", settingsManager.isClock24h())
        }
        return jsonResponse(json.toString())
    }

    private fun handleProfiles(): Response {
        val profiles = profileManager.getProfiles()
        val arr = org.json.JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("icon", p.icon)
                put("isGuest", p.isGuest)
                put("isKid", p.isKid)
            })
        }
        return jsonResponse(JSONObject().apply { put("profiles", arr) }.toString())
    }

    private fun handleSwitchProfile(session: IHTTPSession): Response {
        val body = parseBody(session)
        val profileId = body.optString("profileId", "")
        if (profileId.isBlank()) return jsonResponse("""{"error":"missing profileId"}""")
        val profile = profileManager.getProfiles().find { it.id == profileId }
        if (profile == null) return jsonResponse("""{"error":"profile not found"}""")
        profileManager.setCurrentProfile(profileId)
        return jsonResponse("""{"success":true,"profile":"${profile.name}"}""")
    }

    private fun handleLock(): Response {
        settingsManager.setLockEnabled(true)
        return jsonResponse("""{"success":true}""")
    }

    private fun handleUnlock(): Response {
        settingsManager.setLockEnabled(false)
        return jsonResponse("""{"success":true}""")
    }

    private fun handleLaunchApp(session: IHTTPSession): Response {
        val body = parseBody(session)
        val packageName = body.optString("package", "")
        if (packageName.isBlank()) return jsonResponse("""{"error":"missing package"}""")
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return jsonResponse("""{"success":true}""")
            }
            return jsonResponse("""{"error":"package not launchable"}""")
        } catch (e: Exception) {
            return jsonResponse("""{"error":"${e.message}"}""")
        }
    }

    private fun handleVolume(session: IHTTPSession): Response {
        val body = parseBody(session)
        val level = body.optInt("level", -1)
        if (level < 0 || level > 100) return jsonResponse("""{"error":"level must be 0-100"}""")
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVol = (level * maxVol / 100)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        return jsonResponse("""{"success":true,"volume":$level}""")
    }

    private fun handleSleep(): Response {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isInteractive) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            } catch (_: Exception) {}
        }
        try {
            val intent = Intent(Intent.ACTION_SCREEN_OFF).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
        return jsonResponse("""{"success":true}""")
    }

    private fun parseBody(session: IHTTPSession): JSONObject {
        return try {
            val bodyMap = HashMap<String, String>()
            session.parseBody(bodyMap)
            val body = bodyMap["postData"] ?: return JSONObject()
            JSONObject(body)
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
