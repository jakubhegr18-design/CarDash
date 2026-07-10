package com.cartablet.companion

import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class CompanionClient(private val baseUrl: String) {

    private val api get() = "http://$baseUrl"

    fun getStatus(onResult: (JSONObject?) -> Unit) {
        val req = JsonObjectRequest(api + "/status", null, { onResult(it) }, { onResult(null) })
        queue.add(req)
    }

    fun getProfiles(onResult: (JSONObject?) -> Unit) {
        val req = JsonObjectRequest(api + "/profiles", null, { onResult(it) }, { onResult(null) })
        queue.add(req)
    }

    fun switchProfile(profileId: String, onResult: (Boolean) -> Unit) {
        val body = JSONObject().apply { put("profileId", profileId) }
        val req = object : JsonObjectRequest(Method.POST, api + "/switch-profile", body,
            { onResult(true) }, { onResult(false) }) {}
        queue.add(req)
    }

    fun lock(onResult: (Boolean) -> Unit) {
        val req = object : JsonObjectRequest(Method.POST, api + "/lock", null,
            { onResult(true) }, { onResult(false) }) {}
        queue.add(req)
    }

    fun unlock(onResult: (Boolean) -> Unit) {
        val req = object : JsonObjectRequest(Method.POST, api + "/unlock", null,
            { onResult(true) }, { onResult(false) }) {}
        queue.add(req)
    }

    fun launchApp(packageName: String, onResult: (Boolean) -> Unit) {
        val body = JSONObject().apply { put("package", packageName) }
        val req = object : JsonObjectRequest(Method.POST, api + "/launch-app", body,
            { onResult(true) }, { onResult(false) }) {}
        queue.add(req)
    }

    fun setVolume(level: Int, onResult: (Boolean) -> Unit) {
        val body = JSONObject().apply { put("level", level) }
        val req = object : JsonObjectRequest(Method.POST, api + "/volume", body,
            { onResult(true) }, { onResult(false) }) {}
        queue.add(req)
    }

    fun sleep(onResult: (Boolean) -> Unit) {
        val req = object : JsonObjectRequest(Method.POST, api + "/sleep", null,
            { onResult(true) }, { onResult(false) }) {}
        queue.add(req)
    }

    companion object {
        private val queue: RequestQueue by lazy {
            Volley.newRequestQueue(CompanionApp.instance)
        }
    }
}
