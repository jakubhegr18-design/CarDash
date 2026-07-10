package com.cartablet.utils

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.cartablet.data.ProfileManager
import com.cartablet.data.SettingsManager
import org.json.JSONObject
import java.util.UUID

class BleControlServer(
    private val context: Context,
    private val profileManager: ProfileManager,
    private val settingsManager: SettingsManager
) {
    private val gattServer: BluetoothGattServer?
    private val advertiser: BluetoothLeAdvertiser?
    private var running = false

    private var connectedDevice: BluetoothDevice? = null

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("cafebab0-0001-4c75-a8e9-d5e5b0a7a9e0")
        val CHAR_STATUS_UUID: UUID = UUID.fromString("cafebab0-0002-4c75-a8e9-d5e5b0a7a9e0")
        val CHAR_PROFILES_UUID: UUID = UUID.fromString("cafebab0-0003-4c75-a8e9-d5e5b0a7a9e0")
        val CHAR_CONTROL_UUID: UUID = UUID.fromString("cafebab0-0004-4c75-a8e9-d5e5b0a7a9e0")
        val CHAR_RESULT_UUID: UUID = UUID.fromString("cafebab0-0005-4c75-a8e9-d5e5b0a7a9e0")
    }

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter
        gattServer = bluetoothManager?.let { mgr ->
            try {
                mgr.openGattServer(context, gattCallback)
            } catch (_: Exception) { null }
        }
        advertiser = adapter?.bluetoothLeAdvertiser
        setupService()
    }

    fun isRunning(): Boolean = running

    fun start() {
        if (running) return
        startAdvertising()
        running = true
    }

    fun stop() {
        if (!running) return
        stopAdvertising()
        gattServer?.close()
        running = false
    }

    private fun setupService() {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val statusChar = BluetoothGattCharacteristic(
            CHAR_STATUS_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(statusChar)

        val profilesChar = BluetoothGattCharacteristic(
            CHAR_PROFILES_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(profilesChar)

        val controlChar = BluetoothGattCharacteristic(
            CHAR_CONTROL_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(controlChar)

        val resultChar = BluetoothGattCharacteristic(
            CHAR_RESULT_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(resultChar)

        gattServer?.addService(service)
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (_: Exception) {}
    }

    private fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (_: Exception) {}
    }

    private fun buildStatusJson(): ByteArray {
        val profile = profileManager.getCurrentProfile()
        return JSONObject().apply {
            put("currentProfileId", profile.id)
            put("currentProfileName", profile.name)
            put("currentProfileIcon", profile.icon)
            put("isLocked", settingsManager.isLockEnabled())
            put("kidsMode", settingsManager.isKidsModeEnabled())
            put("strictLock", settingsManager.isStrictLockEnabled())
            put("speedometer", settingsManager.isSpeedometerEnabled())
            put("clock24h", settingsManager.isClock24h())
        }.toString().toByteArray(Charsets.UTF_8)
    }

    private fun buildProfilesJson(): ByteArray {
        val arr = org.json.JSONArray()
        profileManager.getProfiles().forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("icon", p.icon)
                put("isGuest", p.isGuest)
                put("isKid", p.isKid)
            })
        }
        return JSONObject().apply { put("profiles", arr) }
            .toString().toByteArray(Charsets.UTF_8)
    }

    private fun notifyResult(json: String) {
        val device = connectedDevice ?: return
        val char = gattServer?.getService(SERVICE_UUID)?.getCharacteristic(CHAR_RESULT_UUID) ?: return
        char.value = json.toByteArray(Charsets.UTF_8)
        gattServer?.notifyCharacteristicChanged(device, char, false)
    }

    private fun handleCommand(json: JSONObject): String {
        return try {
            val action = json.optString("action", "")
            when (action) {
                "switch-profile" -> {
                    val id = json.optString("profileId", "")
                    val profile = profileManager.getProfiles().find { it.id == id }
                    if (profile != null) {
                        profileManager.setCurrentProfile(id)
                        """{"success":true,"profile":"${profile.name}"}"""
                    } else """{"success":false,"error":"profile not found"}"""
                }
                "lock" -> { settingsManager.setLockEnabled(true); """{"success":true}""" }
                "unlock" -> { settingsManager.setLockEnabled(false); """{"success":true}""" }
                "volume" -> {
                    val level = json.optInt("level", -1)
                    if (level in 0..100) {
                        val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                        val maxVol = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                        audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, level * maxVol / 100, 0)
                        """{"success":true,"volume":$level}"""
                    } else """{"success":false,"error":"level 0-100"}"""
                }
                "launch-app" -> {
                    val pkg = json.optString("package", "")
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        """{"success":true}"""
                    } else """{"success":false,"error":"not launchable"}"""
                }
                "sleep" -> { """{"success":true}""" }
                else -> """{"success":false,"error":"unknown action"}"""
            }
        } catch (e: Exception) {
            """{"success":false,"error":"${e.message}"}"""
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (device == connectedDevice) connectedDevice = null
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val charUuid = characteristic?.uuid ?: return
            val data = when (charUuid) {
                CHAR_STATUS_UUID -> buildStatusJson()
                CHAR_PROFILES_UUID -> buildProfilesJson()
                else -> return
            }
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, data)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic?.uuid != CHAR_CONTROL_UUID || value == null) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                return
            }
            val jsonStr = String(value, Charsets.UTF_8)
            val result = try {
                handleCommand(JSONObject(jsonStr))
            } catch (e: Exception) {
                """{"success":false,"error":"${e.message}"}"""
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
            notifyResult(result)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
        override fun onStartFailure(errorCode: Int) {}
    }
}
