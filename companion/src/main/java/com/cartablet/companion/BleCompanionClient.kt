package com.cartablet.companion

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import org.json.JSONObject
import java.util.UUID

class BleCompanionClient(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var gattCallback: BleGattCallback? = null
    private var scanCallback: BleScanCallback? = null
    private var scanner: BluetoothLeScanner? = null

    private val SERVICE_UUID = UUID.fromString("cafebab0-0001-4c75-a8e9-d5e5b0a7a9e0")
    private val CHAR_STATUS_UUID = UUID.fromString("cafebab0-0002-4c75-a8e9-d5e5b0a7a9e0")
    private val CHAR_PROFILES_UUID = UUID.fromString("cafebab0-0003-4c75-a8e9-d5e5b0a7a9e0")
    private val CHAR_CONTROL_UUID = UUID.fromString("cafebab0-0004-4c75-a8e9-d5e5b0a7a9e0")
    private val CHAR_RESULT_UUID = UUID.fromString("cafebab0-0005-4c75-a8e9-d5e5b0a7a9e0")

    var onStatusUpdate: ((JSONObject?) -> Unit)? = null
    var onProfilesUpdate: ((JSONObject?) -> Unit)? = null
    var onDeviceFound: ((String, BluetoothDevice) -> Unit)? = null
    var onConnected: ((Boolean) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onCommandResult: ((JSONObject?) -> Unit)? = null

    private var characteristicStatus: BluetoothGattCharacteristic? = null
    private var characteristicProfiles: BluetoothGattCharacteristic? = null
    private var characteristicControl: BluetoothGattCharacteristic? = null
    private var characteristicResult: BluetoothGattCharacteristic? = null

    fun isConnected(): Boolean = characteristicControl != null

    fun startScan() {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        scanner = adapter?.bluetoothLeScanner
        if (scanner == null) return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        )

        scanCallback = BleScanCallback()
        try {
            scanner?.startScan(filters, settings, scanCallback)
        } catch (_: Exception) {}
    }

    fun stopScan() {
        try {
            scanCallback?.let { scanner?.stopScan(it) }
        } catch (_: Exception) {}
        scanCallback = null
    }

    fun connect(device: BluetoothDevice) {
        stopScan()
        gattCallback = BleGattCallback()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {}
        bluetoothGatt = null
        characteristicStatus = null
        characteristicProfiles = null
        characteristicControl = null
        characteristicResult = null
        onDisconnected?.invoke()
    }

    fun readStatus() {
        characteristicStatus?.let { bluetoothGatt?.readCharacteristic(it) }
    }

    fun readProfiles() {
        characteristicProfiles?.let { bluetoothGatt?.readCharacteristic(it) }
    }

    fun sendCommand(action: String, params: Map<String, Any> = emptyMap()): Boolean {
        val char = characteristicControl ?: return false
        val json = JSONObject().apply {
            put("action", action)
            params.forEach { (k, v) -> put(k, v) }
        }
        char.value = json.toString().toByteArray(Charsets.UTF_8)
        return bluetoothGatt?.writeCharacteristic(char) ?: false
    }

    fun switchProfile(profileId: String) {
        sendCommand("switch-profile", mapOf("profileId" to profileId))
    }

    fun lock() { sendCommand("lock") }
    fun unlock() { sendCommand("unlock") }
    fun sleep() { sendCommand("sleep") }
    fun setVolume(level: Int) { sendCommand("volume", mapOf("level" to level)) }
    fun launchApp(packageName: String) { sendCommand("launch-app", mapOf("package" to packageName)) }

    private inner class BleScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            onDeviceFound?.invoke(device.name ?: "CarDash", device)
        }
    }

    @SuppressLint("MissingPermission")
    private inner class BleGattCallback : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                characteristicControl = null
                characteristicStatus = null
                characteristicProfiles = null
                characteristicResult = null
                onDisconnected?.invoke()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val service = gatt?.getService(SERVICE_UUID)
            if (service != null) {
                characteristicStatus = service.getCharacteristic(CHAR_STATUS_UUID)
                characteristicProfiles = service.getCharacteristic(CHAR_PROFILES_UUID)
                characteristicControl = service.getCharacteristic(CHAR_CONTROL_UUID)
                characteristicResult = service.getCharacteristic(CHAR_RESULT_UUID)

                // Enable notifications on result characteristic
                characteristicResult?.let { char ->
                    gatt?.setCharacteristicNotification(char, true)
                }

                onConnected?.invoke(true)
                readStatus()
                readProfiles()
            } else {
                onConnected?.invoke(false)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val uuid = characteristic?.uuid ?: return
            val data = characteristic.value
            val jsonStr = String(data, Charsets.UTF_8)
            val json = try { JSONObject(jsonStr) } catch (_: Exception) { null }
            when (uuid) {
                CHAR_STATUS_UUID -> onStatusUpdate?.invoke(json)
                CHAR_PROFILES_UUID -> onProfilesUpdate?.invoke(json)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {}

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val char = characteristic ?: return
            if (char.uuid == CHAR_RESULT_UUID) {
                val data = char.value ?: return
                val jsonStr = String(data, Charsets.UTF_8)
                val json = try { JSONObject(jsonStr) } catch (_: Exception) { null }
                onCommandResult?.invoke(json)
                readStatus()
                readProfiles()
            }
        }
    }
}
