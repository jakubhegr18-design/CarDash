package com.cartablet.companion.ui

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cartablet.companion.BleCompanionClient
import com.cartablet.companion.CompanionClient
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen() {
    val context = LocalContext.current
    var connectionMode by remember { mutableStateOf("WIFI") }

    // WiFi state
    var tabletIp by remember { mutableStateOf("") }
    var wifiClient by remember { mutableStateOf<CompanionClient?>(null) }

    // BLE state
    var bleClient by remember { mutableStateOf<BleCompanionClient?>(null) }
    var discoveredDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    // Shared state
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<JSONObject?>(null) }
    var profiles by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var volumeLevel by remember { mutableIntStateOf(50) }
    var statusMessage by remember { mutableStateOf("") }
    var blePermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> blePermissionGranted = granted }

    fun connectWifi() {
        val ip = tabletIp.trim()
        if (ip.isBlank()) return
        val c = CompanionClient(ip)
        c.getStatus { s ->
            if (s != null) {
                status = s
                wifiClient = c
                connected = true
                statusMessage = "Connected"
                c.getProfiles { p ->
                    if (p != null) {
                        val arr = p.optJSONArray("profiles")
                        profiles = if (arr != null) {
                            (0 until arr.length()).map { arr.getJSONObject(it) }
                        } else emptyList()
                    }
                }
            } else {
                statusMessage = "Connection failed"
            }
        }
    }

    fun startBleScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
            return
        }
        blePermissionGranted = true
        val client = BleCompanionClient(context)
        client.onDeviceFound = { _, device ->
            discoveredDevices = (discoveredDevices + device).distinctBy { it.address }
        }
        client.onConnected = { success ->
            connected = success
            statusMessage = if (success) "Connected" else "Service not found"
        }
        client.onDisconnected = {
            connected = false
            statusMessage = "Disconnected"
        }
        client.onStatusUpdate = { s ->
            if (s != null) status = s
        }
        client.onProfilesUpdate = { p ->
            if (p != null) {
                val arr = p.optJSONArray("profiles")
                profiles = if (arr != null) {
                    (0 until arr.length()).map { arr.getJSONObject(it) }
                } else emptyList()
            }
        }
        client.onCommandResult = { r ->
            statusMessage = r?.optString("success", "false")?.let {
                if (it == "true") "OK" else "Failed: ${r.optString("error")}"
            } ?: "Failed"
        }
        bleClient = client
        isScanning = true
        discoveredDevices = emptyList()
        client.startScan()
    }

    fun connectBle(device: BluetoothDevice) {
        bleClient?.connect(device)
        isScanning = false
        statusMessage = "Connecting..."
    }

    fun disconnect() {
        wifiClient = null
        bleClient?.disconnect()
        bleClient = null
        connected = false
        status = null
        profiles = emptyList()
        statusMessage = "Disconnected"
        isScanning = false
        discoveredDevices = emptyList()
    }

    // WiFi actions
    fun wifiAction(action: (CompanionClient, (Boolean) -> Unit) -> Unit) {
        wifiClient?.let { c ->
            action(c) { success ->
                statusMessage = if (success) "OK" else "Failed"
                if (success) c.getStatus { s -> if (s != null) status = s }
            }
        }
    }

    // BLE actions
    fun bleAction(action: (BleCompanionClient) -> Unit) {
        bleClient?.let { action(it) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "CarDash Remote",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val modes = listOf("WIFI" to "WiFi", "BLE" to "BLE")
                modes.forEach { (mode, label) ->
                    FilterChip(
                        selected = connectionMode == mode,
                        onClick = { if (!connected) { connectionMode = mode; statusMessage = "" } },
                        label = { Text(label) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Connection panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!connected) {
                        if (connectionMode == "WIFI") {
                            OutlinedTextField(
                                value = tabletIp,
                                onValueChange = { tabletIp = it },
                                label = { Text("Tablet IP:Port") },
                                placeholder = { Text("192.168.1.100:8080") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Button(
                                onClick = { connectWifi() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("CONNECT") }
                        } else {
                            Button(
                                onClick = {
                                    if (isScanning) {
                                        bleClient?.stopScan()
                                        isScanning = false
                                    } else {
                                        startBleScan()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (isScanning) "STOP SCAN" else "SCAN FOR DEVICES") }

                            if (isScanning) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            if (discoveredDevices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Found devices:", style = MaterialTheme.typography.labelMedium)
                                discoveredDevices.forEach { device ->
                                    OutlinedButton(
                                        onClick = { connectBle(device) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(device.name ?: "CarDash")
                                    }
                                }
                            }

                            if (!isScanning && discoveredDevices.isEmpty() && !connected) {
                                Text(
                                    "Make sure Bluetooth is on and tablet has BLE mode enabled in settings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (connectionMode == "WIFI") "Connected to $tabletIp"
                                else "Connected via BLE",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            FilledTonalButton(onClick = { disconnect() }) { Text("DISCONNECT") }
                        }
                    }
                }
            }

            // Status message
            if (statusMessage.isNotEmpty()) {
                Text(
                    statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            if (connected && status != null) {
                val s = status!!

                // Status card
                StatusCard(s)

                // Profile switching
                ProfileSwitcher(profiles,
                    onSwitch = { id ->
                        if (connectionMode == "WIFI") {
                            wifiAction { c, cb -> c.switchProfile(id, cb) }
                        } else {
                            bleClient?.switchProfile(id)
                        }
                    }
                )

                // Control buttons
                ControlButtons(
                    isLocked = s.optBoolean("isLocked", false),
                    onLock = {
                        if (connectionMode == "WIFI") wifiAction { c, cb -> c.lock(cb) }
                        else bleClient?.lock()
                    },
                    onUnlock = {
                        if (connectionMode == "WIFI") wifiAction { c, cb -> c.unlock(cb) }
                        else bleClient?.unlock()
                    },
                    onSleep = {
                        if (connectionMode == "WIFI") wifiAction { c, cb -> c.sleep(cb) }
                        else bleClient?.sleep()
                    }
                )

                // Volume
                VolumeControl(volumeLevel) { level ->
                    volumeLevel = level
                    if (connectionMode == "WIFI") {
                        wifiAction { c, cb -> c.setVolume(level, cb) }
                    } else {
                        bleClient?.setVolume(level)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(status: JSONObject) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("STATUS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            StatusRow("Profile", status.optString("currentProfileName", "Unknown"))
            StatusRow("Locked", if (status.optBoolean("isLocked", false)) "Yes" else "No")
            StatusRow("Kids Mode", if (status.optBoolean("kidsMode", false)) "On" else "Off")
            StatusRow("Strict Lock", if (status.optBoolean("strictLock", false)) "On" else "Off")
            StatusRow("Speedometer", if (status.optBoolean("speedometer", false)) "On" else "Off")
            StatusRow("24h Clock", if (status.optBoolean("clock24h", false)) "Yes" else "No")
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileSwitcher(profiles: List<JSONObject>, onSwitch: (String) -> Unit) {
    if (profiles.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SWITCH PROFILE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profiles.forEach { p ->
                    OutlinedButton(
                        onClick = { onSwitch(p.getString("id")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${p.optString("icon", "")} ${p.optString("name", "")}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButtons(isLocked: Boolean, onLock: () -> Unit, onUnlock: () -> Unit, onSleep: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTROLS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    icon = if (isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    label = if (isLocked) "Unlock" else "Lock",
                    onClick = if (isLocked) onUnlock else onLock,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Filled.PowerSettingsNew,
                    label = "Sleep",
                    onClick = onSleep,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun VolumeControl(volume: Int, onVolumeChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("VOLUME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = null)
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                Text("$volume%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
