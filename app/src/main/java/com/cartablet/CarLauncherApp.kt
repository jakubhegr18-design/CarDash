package com.cartablet

import android.app.Application
import android.content.SharedPreferences
import com.cartablet.data.ProfileManager
import com.cartablet.data.SettingsManager
import com.cartablet.utils.RemoteControlServer
import com.cartablet.utils.BleControlServer
import java.net.InetAddress
import java.net.NetworkInterface

class CarLauncherApp : Application() {

    lateinit var prefs: SharedPreferences
    lateinit var profileManager: ProfileManager
    lateinit var settingsManager: SettingsManager
    var remoteServer: RemoteControlServer? = null
        private set
    var bleServer: BleControlServer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences("cartablet_prefs", MODE_PRIVATE)
        profileManager = ProfileManager(prefs)
        settingsManager = SettingsManager(prefs)
        updateRemoteServers()
    }

    fun updateRemoteServers() {
        val enabled = settingsManager.isRemoteControlEnabled()
        val mode = settingsManager.getRemoteControlMode()

        // WiFi HTTP server
        remoteServer?.stop()
        remoteServer = null
        if (enabled && (mode == "WIFI" || mode == "BOTH")) {
            val port = settingsManager.getRemoteControlPort()
            val server = RemoteControlServer(this, profileManager, settingsManager, port)
            try {
                server.start()
                remoteServer = server
            } catch (_: Exception) {}
        }

        // BLE server
        bleServer?.stop()
        bleServer = null
        if (enabled && (mode == "BLE" || mode == "BOTH")) {
            val server = BleControlServer(this, profileManager, settingsManager)
            try {
                server.start()
                bleServer = server
            } catch (_: Exception) {}
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is InetAddress && !addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    companion object {
        lateinit var instance: CarLauncherApp
            private set
    }
}
