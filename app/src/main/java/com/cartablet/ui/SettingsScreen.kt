package com.cartablet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartablet.data.SettingsManager
import com.cartablet.data.ProfileManager
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    profileManager: ProfileManager,
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf(settingsManager.getPassword() ?: "") }
    var isLockEnabled by remember { mutableStateOf(settingsManager.isLockEnabled()) }
    var lockType by remember { mutableStateOf(settingsManager.getLockType()) }
    var secretKey by remember { mutableStateOf(settingsManager.getSecretKey() ?: "") }
    
    var autoLaunch by remember { mutableStateOf(settingsManager.isAutoLaunchEnabled()) }
    var hideGuest by remember { mutableStateOf(settingsManager.isHideGuestEnabled()) }
    var animations by remember { mutableStateOf(settingsManager.isAnimationEnabled()) }
    var clock24h by remember { mutableStateOf(settingsManager.isClock24h()) }
    var dashGlow by remember { mutableStateOf(settingsManager.isDashboardGlowEnabled()) }
    
    var wallpaperType by remember { mutableStateOf(settingsManager.getWallpaperType()) }
    var kidsModeEnabled by remember { mutableStateOf(settingsManager.isKidsModeEnabled()) }
    var kidsModeCollectionId by remember { mutableStateOf(settingsManager.getKidsModeCollectionId()) }
    var speedometerEnabled by remember { mutableStateOf(settingsManager.isSpeedometerEnabled()) }
    var profileLockEnabled by remember { mutableStateOf(settingsManager.isProfileLockEnabled()) }
    var strictLockEnabled by remember { mutableStateOf(settingsManager.isStrictLockEnabled()) }
    var strictLockCollections by remember { mutableStateOf(settingsManager.getStrictLockCollections()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

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
                Text("Lock Method", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = listOf("NONE" to "Unlocked", "PASSWORD" to "Password", "PIN" to "PIN")
                    types.forEach { (type, label) ->
                        val isSelected = (if (!isLockEnabled) "NONE" else lockType) == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (type == "NONE") {
                                    isLockEnabled = false
                                    settingsManager.setLockEnabled(false)
                                } else {
                                    isLockEnabled = true
                                    lockType = type
                                    settingsManager.setLockEnabled(true)
                                    settingsManager.setLockType(type)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                SettingsSwitchItem(
                    title = "Lock Profile Switching",
                    description = "Require code to change passengers",
                    checked = profileLockEnabled,
                    onCheckedChange = {
                        profileLockEnabled = it
                        settingsManager.setProfileLockEnabled(it)
                    }
                )

                if (isLockEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            settingsManager.setPassword(it)
                        },
                        label = { Text(if (lockType == "PIN") "Enter PIN" else "Settings Password") },
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

            // KIDS MODE SECTION
            SettingsSection(title = "KIDS MODE", icon = Icons.Filled.ChildCare) {
                SettingsSwitchItem(
                    title = "Enable Kids Mode",
                    description = "Locks tablet to a specific collection",
                    checked = kidsModeEnabled,
                    onCheckedChange = {
                        kidsModeEnabled = it
                        settingsManager.setKidsModeEnabled(it)
                    }
                )
                
                if (kidsModeEnabled) {
                    Text("Select Restricted Collection:", style = MaterialTheme.typography.labelMedium)
                    val folders = profileManager.getCurrentProfile().folders
                    if (folders.isEmpty()) {
                        Text("Create a collection first to use Kids Mode.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            folders.forEach { folder ->
                                val isSelected = folder.id == kidsModeCollectionId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        kidsModeCollectionId = folder.id
                                        settingsManager.setKidsModeCollectionId(folder.id)
                                    },
                                    label = { Text(folder.name) }
                                )
                            }
                        }
                    }
                }
            }

            // LOCK MODE SECTION
            SettingsSection(title = "STRICT LOCK MODE", icon = Icons.Filled.LockPerson) {
                SettingsSwitchItem(
                    title = "Enable Lock Mode",
                    description = "Locks dashboard to chosen collections",
                    checked = strictLockEnabled,
                    onCheckedChange = {
                        strictLockEnabled = it
                        settingsManager.setStrictLockEnabled(it)
                    }
                )
                
                Text("Chosen Collections:", style = MaterialTheme.typography.labelMedium)
                val folders = profileManager.getCurrentProfile().folders
                if (folders.isEmpty()) {
                    Text("Create a collection first.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        folders.forEach { folder ->
                            val isSelected = strictLockCollections.contains(folder.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = { 
                                    val newSet = strictLockCollections.toMutableSet()
                                    if (isSelected) newSet.remove(folder.id) else newSet.add(folder.id)
                                    strictLockCollections = newSet
                                    settingsManager.setStrictLockCollections(newSet)
                                },
                                label = { Text(folder.name) }
                            )
                        }
                    }
                }
            }

            // GENERAL SECTION
            SettingsSection(title = "GENERAL", icon = Icons.Filled.Tune) {
                SettingsSwitchItem(
                    title = "GPS Speedometer",
                    description = "Show real-time speed on dashboard",
                    checked = speedometerEnabled,
                    onCheckedChange = {
                        speedometerEnabled = it
                        settingsManager.setSpeedometerEnabled(it)
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

            // DISPLAY & WALLPAPER SECTION
            SettingsSection(title = "DISPLAY & CYBER THEMES", icon = Icons.Filled.Palette) {
                Text("Background Style", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                
                val wallTypes = listOf("CYBER_GLOW" to "Pulse Glow", "NEBULA" to "Nebula", "MATRIX" to "Matrix Rain")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    wallTypes.forEach { (type, label) ->
                        val isSelected = wallpaperType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { 
                                    wallpaperType = type
                                    settingsManager.setWallpaperType(type)
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsSwitchItem(
                    title = "Dynamic Glow Effects",
                    description = "Enable animated neon background pulses",
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
                    key(profile.id, refreshTrigger) {
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
                                    else if (profile.isKid) Text("Kids Profile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!profile.isGuest) {
                                    IconButton(onClick = {
                                        profileManager.updateProfile(profile.copy(isProtected = !profile.isProtected))
                                        refreshTrigger++
                                    }) {
                                        Icon(
                                            if (profile.isProtected) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                            contentDescription = "Toggle Lock",
                                            tint = if (profile.isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (!profile.isGuest && profileManager.getProfiles().size > 1) {
                                    IconButton(onClick = { 
                                        profileManager.removeProfile(profile.id)
                                        refreshTrigger++
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Version 1.3.0 • Cyber Drive Edition",
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
