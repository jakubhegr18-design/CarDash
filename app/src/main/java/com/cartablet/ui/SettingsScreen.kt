package com.cartablet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartablet.data.SettingsManager
import com.cartablet.data.ProfileManager
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    profileManager: ProfileManager,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf(settingsManager.getPassword() ?: "") }
    var isLockEnabled by remember { mutableStateOf(settingsManager.isLockEnabled()) }
    var secretKey by remember { mutableStateOf(settingsManager.getSecretKey() ?: "") }
    
    var autoLaunch by remember { mutableStateOf(settingsManager.isAutoLaunchEnabled()) }
    var hideGuest by remember { mutableStateOf(settingsManager.isHideGuestEnabled()) }
    var animations by remember { mutableStateOf(settingsManager.isAnimationEnabled()) }
    var clock24h by remember { mutableStateOf(settingsManager.isClock24h()) }
    var dashGlow by remember { mutableStateOf(settingsManager.isDashboardGlowEnabled()) }

    fun generateSecretKey() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val random = SecureRandom()
        val key = (1..16).map { chars[random.nextInt(chars.length)] }.joinToString("")
        secretKey = key
        settingsManager.setSecretKey(key)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CAR DASHBOARD SETTINGS", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SECURITY SECTION
            SettingsSection(title = "SECURITY & ACCESS", icon = Icons.Filled.Security) {
                SettingsSwitchItem(
                    title = "Lock Settings Access",
                    description = "Require password to enter this menu",
                    checked = isLockEnabled,
                    onCheckedChange = {
                        isLockEnabled = it
                        settingsManager.setLockEnabled(it)
                    }
                )

                if (isLockEnabled) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            settingsManager.setPassword(it)
                        },
                        label = { Text("Settings Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Text("Recovery Secret Key", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Used with Google Authenticator to reset password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = secretKey,
                            onValueChange = {
                                secretKey = it
                                settingsManager.setSecretKey(it)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Enter or generate key...") }
                        )
                        Button(
                            onClick = { generateSecretKey() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("GENERATE")
                        }
                    }
                }
            }

            // GENERAL SECTION
            SettingsSection(title = "GENERAL", icon = Icons.Filled.Tune) {
                SettingsSwitchItem(
                    title = "Auto-launch on Boot",
                    description = "Start CarDash when device turns on",
                    checked = autoLaunch,
                    onCheckedChange = {
                        autoLaunch = it
                        settingsManager.setAutoLaunchEnabled(it)
                    }
                )
                SettingsSwitchItem(
                    title = "24-Hour Clock",
                    description = "Use 24h format for dashboard time",
                    checked = clock24h,
                    onCheckedChange = {
                        clock24h = it
                        settingsManager.setClock24h(it)
                    }
                )
            }

            // DISPLAY SECTION
            SettingsSection(title = "DISPLAY & EFFECTS", icon = Icons.Filled.Palette) {
                SettingsSwitchItem(
                    title = "Dashboard Glow",
                    description = "Enable animated neon background effects",
                    checked = dashGlow,
                    onCheckedChange = {
                        dashGlow = it
                        settingsManager.setDashboardGlowEnabled(it)
                    }
                )
                SettingsSwitchItem(
                    title = "UI Animations",
                    description = "Enable smooth transitions and scaling",
                    checked = animations,
                    onCheckedChange = {
                        animations = it
                        settingsManager.setAnimationEnabled(it)
                    }
                )
            }

            // PASSENGER MODE SECTION
            SettingsSection(title = "PASSENGER MODE", icon = Icons.Filled.People) {
                SettingsSwitchItem(
                    title = "Hide Guest Profile",
                    description = "Remove the guest option from switcher",
                    checked = hideGuest,
                    onCheckedChange = {
                        hideGuest = it
                        settingsManager.setHideGuestEnabled(it)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                Text("Manage Accounts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(8.dp))

                profileManager.getProfiles().forEach { profile ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(profile.icon, fontSize = 20.sp)
                            Column {
                                Text(profile.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                if (profile.isGuest) Text("Guest Account", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        // Don't allow deleting the last profile or guest easily from here to prevent lockouts
                        if (!profile.isGuest && profileManager.getProfiles().size > 1) {
                            IconButton(onClick = {
                                profileManager.removeProfile(profile.id)
                                // We trigger a back navigation or refresh if needed, 
                                // but for now simple removal works as getProfiles is called every recompose
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Version 1.2.0 • Cyber Edition",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}
