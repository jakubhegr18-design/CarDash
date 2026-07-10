package com.cartablet

import android.app.Application
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.cartablet.data.ProfileManager
import com.cartablet.data.SettingsManager
import com.cartablet.utils.RemoteControlServer
import java.net.InetAddress
import java.net.NetworkInterface

class CarLauncherApp : Application() {

    lateinit var prefs: SharedPreferences
    lateinit var profileManager: ProfileManager
    lateinit var settingsManager: SettingsManager
    var remoteServer: RemoteControlServer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences("cartablet_prefs", MODE_PRIVATE)
        profileManager = ProfileManager(prefs)
        settingsManager = SettingsManager(prefs)
        updateRemoteServer()
    }

    fun updateRemoteServer() {
        remoteServer?.stop()
        remoteServer = null
        if (settingsManager.isRemoteControlEnabled()) {
            val port = settingsManager.getRemoteControlPort()
            val server = RemoteControlServer(this, profileManager, settingsManager, port)
            try {
                server.start()
                remoteServer = server
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
