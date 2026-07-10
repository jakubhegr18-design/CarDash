package com.cartablet.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartablet.companion.CompanionClient
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun RemoteControlScreen() {
    var tabletIp by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var client by remember { mutableStateOf<CompanionClient?>(null) }
    var status by remember { mutableStateOf<JSONObject?>(null) }
    var profiles by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var volumeLevel by remember { mutableIntStateOf(50) }
    var statusMessage by remember { mutableStateOf("") }

    fun connect() {
        val ip = tabletIp.trim()
        if (ip.isBlank()) return
        val c = CompanionClient(ip)
        c.getStatus { s ->
            if (s != null) {
                status = s
                client = c
                connected = true
                statusMessage = "Connected"
                // Load profiles
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

    fun disconnect() {
        client = null
        connected = false
        status = null
        profiles = emptyList()
        statusMessage = "Disconnected"
    }

    fun sendAction(action: (CompanionClient, (Boolean) -> Unit) -> Unit) {
        client?.let { c ->
            action(c) { success ->
                statusMessage = if (success) "OK" else "Failed"
                if (success) c.getStatus { s -> if (s != null) status = s }
            }
        }
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

            // Connection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!connected) {
                        OutlinedTextField(
                            value = tabletIp,
                            onValueChange = { tabletIp = it },
                            label = { Text("Tablet IP:Port") },
                            placeholder = { Text("192.168.1.100:8080") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = { connect() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("CONNECT") }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connected to $tabletIp", fontWeight = FontWeight.Bold)
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
                ProfileSwitcher(profiles, { sendAction { c, cb -> c.switchProfile(it, cb) } })

                // Control buttons
                ControlButtons(
                    isLocked = s.optBoolean("isLocked", false),
                    onLock = { sendAction { c, cb -> c.lock(cb) } },
                    onUnlock = { sendAction { c, cb -> c.unlock(cb) } },
                    onSleep = { sendAction { c, cb -> c.sleep(cb) } }
                )

                // Volume
                VolumeControl(volumeLevel) { level ->
                    volumeLevel = level
                    sendAction { c, cb -> c.setVolume(level, cb) }
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
