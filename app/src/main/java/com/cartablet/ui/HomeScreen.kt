package com.cartablet.ui

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.cartablet.data.Profile
import com.cartablet.data.ProfileManager
import com.cartablet.data.SettingsManager
import com.cartablet.data.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Painter?
)

private data class NavShortcut(
    val name: String,
    val icon: ImageVector,
    val packageName: String?
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val prefs = remember { context.getSharedPreferences("cardash_prefs", Context.MODE_PRIVATE) }
    val profileManager = remember { ProfileManager(prefs) }
    val settingsManager = remember { SettingsManager(prefs) }

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var isBluetoothOn by remember { mutableStateOf(false) }
    var isWifiOn by remember { mutableStateOf(false) }

    var currentProfile by remember { mutableStateOf(profileManager.getCurrentProfile()) }
    var showAllApps by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var showAddProfile by remember { mutableStateOf(false) }
    var showAddFolder by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showGuide by remember { mutableStateOf(false) }
    var pinnedFeedback by remember { mutableStateOf<String?>(null) }
    var showInAppSettings by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    
    var activeFolder by remember { mutableStateOf<Folder?>(null) }
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var showFolderOptions by remember { mutableStateOf<Folder?>(null) }

    val profProfiles = remember { mutableStateListOf<Profile>() }

    fun switchProfile(profile: Profile) {
        currentProfile = profile
        profileManager.setCurrentProfile(profile.id)
        showAllApps = false
        activeFolder = null
    }

    fun launchApp(pkgName: String) {
        try {
            val launchIntent = pm.getLaunchIntentForPackage(pkgName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                activeFolder = null
                showAllApps = false
            } else {
                val playIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$pkgName")
                }
                if (playIntent.resolveActivity(pm) != null) context.startActivity(playIntent)
            }
        } catch (e: Exception) {}
    }

    fun togglePin(packageName: String) {
        if (currentProfile.isGuest) return
        profileManager.togglePinApp(currentProfile.id, packageName)
        currentProfile = profileManager.getCurrentProfile()
        pinnedFeedback = if (profileManager.isAppPinned(currentProfile.id, packageName)) "Pinned" else "Unpinned"
        val idx = profProfiles.indexOfFirst { it.id == currentProfile.id }
        if (idx != -1) profProfiles[idx] = currentProfile
    }

    fun refreshProfiles() {
        profProfiles.clear()
        profProfiles.addAll(profileManager.getProfiles())
        currentProfile = profileManager.getCurrentProfile()
    }

    LaunchedEffect(Unit) {
        profProfiles.addAll(profileManager.getProfiles())
        if (prefs.getBoolean("first_run", true)) {
            showGuide = true
            prefs.edit().putBoolean("first_run", false).apply()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val loaded = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    AppInfo(
                        packageName = info.activityInfo.packageName,
                        label = info.loadLabel(pm).toString(),
                        icon = BitmapPainter(info.loadIcon(pm).toBitmap(128, 128).asImageBitmap())
                    )
                }
                .sortedBy { it.label }
            allApps = loaded
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try { brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f } catch (_: Exception) {}
            try { isBluetoothOn = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true } catch (_: Exception) {}
            try { isWifiOn = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (settingsManager.isDashboardGlowEnabled()) BackgroundGlow(isDarkTheme)
        else Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ProfileHeader(
                    profile = currentProfile,
                    onSwitchClick = { showProfileSwitcher = true },
                    onOpenGuide = { showGuide = true }
                )
                ClockWidget(settingsManager.isClock24h())
            }

            Spacer(modifier = Modifier.height(16.dp))

            QuickSettingsBar(
                brightness = brightness,
                onBrightnessChange = { v ->
                    brightness = v
                    (context as? Activity)?.window?.attributes = (context as? Activity)?.window?.attributes?.apply { screenBrightness = v }
                },
                isBluetoothOn = isBluetoothOn,
                onToggleBluetooth = {
                    try {
                        val bt = BluetoothAdapter.getDefaultAdapter()
                        if (bt != null) { 
                            if (bt.isEnabled) {
                                bt.disable()
                                isBluetoothOn = false
                            } else {
                                bt.enable()
                                isBluetoothOn = true
                            }
                        }
                    } catch (_: SecurityException) {}
                },
                isWifiOn = isWifiOn,
                onToggleWifi = {
                    try {
                        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        wifi.isWifiEnabled = !wifi.isWifiEnabled
                        isWifiOn = !isWifiOn
                    } catch (_: Exception) {}
                },
                onOpenSettings = {
                    if (settingsManager.isLockEnabled()) showLockDialog = true else showInAppSettings = true
                },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )

            Spacer(modifier = Modifier.height(24.dp))

            FolderSection(
                folders = currentProfile.folders,
                onFolderClick = { activeFolder = it },
                onFolderLongClick = { showFolderOptions = it },
                onAddFolderClick = { showAddFolder = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AppGridSection(
                modifier = Modifier.weight(1f),
                title = "Shortcuts",
                apps = allApps.filter { it.packageName in currentProfile.pinnedApps },
                onAppClick = { launchApp(it.packageName) },
                onAppLongClick = { togglePin(it.packageName) },
                isPinned = { true },
                isGuest = currentProfile.isGuest,
                animationsEnabled = settingsManager.isAnimationEnabled()
            )

            Spacer(modifier = Modifier.height(20.dp))

            NavigationDock(
                onAppClick = { launchApp(it) },
                onAllAppsClick = { showAllApps = true },
                isAllAppsOpen = showAllApps,
                animationsEnabled = settingsManager.isAnimationEnabled()
            )
        }

        if (showAllApps) {
            LaunchMenuPopup(
                apps = allApps,
                onClose = { showAllApps = false },
                onAppClick = { launchApp(it.packageName) },
                onAppLongClick = { togglePin(it.packageName) },
                isPinned = { pkg -> currentProfile.pinnedApps.contains(pkg) }
            )
        }

        activeFolder?.let { folder ->
            CollectionPopup(
                folder = folder,
                allApps = allApps,
                onClose = { activeFolder = null },
                onAppClick = { launchApp(it.packageName) },
                onAppLongClick = { /* No long click for now */ },
                onAddApps = { 
                    folderToEdit = folder
                    activeFolder = null
                }
            )
        }

        showFolderOptions?.let { folder ->
            AlertDialog(
                onDismissRequest = { showFolderOptions = null },
                title = { Text(folder.name, fontWeight = FontWeight.Black) },
                text = { Text("Choose an action for this collection.") },
                confirmButton = {
                    TextButton(onClick = {
                        profileManager.removeFolder(currentProfile.id, folder.id)
                        refreshProfiles()
                        showFolderOptions = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        folderToEdit = folder
                        showFolderOptions = null 
                    }) { Text("Edit Apps") }
                }
            )
        }
        
        folderToEdit?.let { folder ->
            AppSelectorOverlay(
                allApps = allApps,
                selectedPkgs = folder.apps,
                onClose = { folderToEdit = null },
                onToggleApp = { pkg ->
                    if (folder.apps.contains(pkg)) {
                        profileManager.removeAppFromFolder(currentProfile.id, folder.id, pkg)
                    } else {
                        profileManager.addAppToFolder(currentProfile.id, folder.id, pkg)
                    }
                    currentProfile = profileManager.getCurrentProfile()
                    folderToEdit = currentProfile.folders.find { it.id == folder.id }
                }
            )
        }

        if (showGuide) QuickStartGuide(onDismiss = { showGuide = false })
        if (showInAppSettings) SettingsScreen(settingsManager, profileManager, onBack = { showInAppSettings = false })
        if (showLockDialog) LockDialog(onSuccess = { showLockDialog = false; showInAppSettings = true }, onDismiss = { showLockDialog = false }, settingsManager = settingsManager)
        
        if (showProfileSwitcher) {
            ProfileSwitcherDialog(
                profiles = if (settingsManager.isHideGuestEnabled()) profProfiles.filter { !it.isGuest } else profProfiles.toList(),
                currentId = currentProfile.id,
                onSelect = { switchProfile(it); showProfileSwitcher = false },
                onAddProfile = { showAddProfile = true },
                onStartGuest = { val g = profileManager.createGuestProfile(); profProfiles.add(g); switchProfile(g); showProfileSwitcher = false },
                onDeleteProfile = { showDeleteConfirm = it },
                onDismiss = { showProfileSwitcher = false },
                isGuestHidden = settingsManager.isHideGuestEnabled()
            )
        }

        if (showAddProfile) AddProfileDialog(onConfirm = { n, e, u -> profileManager.addProfile(Profile(name = n, icon = e, iconUri = u?.toString())); refreshProfiles(); showAddProfile = false }, onDismiss = { showAddProfile = false })
        if (showAddFolder) AddFolderDialog(onConfirm = { n, c -> profileManager.addFolder(currentProfile.id, Folder(name = n, color = c)); refreshProfiles(); showAddFolder = false }, onDismiss = { showAddFolder = false })

        showDeleteConfirm?.let { id ->
            val target = profProfiles.find { it.id == id }
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete Profile", fontWeight = FontWeight.Bold) },
                text = { Text("Delete \"${target?.name}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        profileManager.removeProfile(id)
                        refreshProfiles()
                        showDeleteConfirm = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
                }
            )
        }
    }

    pinnedFeedback?.let { msg -> LaunchedEffect(msg) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); pinnedFeedback = null } }
}

@Composable
private fun LaunchMenuPopup(
    apps: List<AppInfo>,
    onClose: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    isPinned: (String) -> Boolean
) {
    BackHandler(onBack = onClose)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LAUNCHPAD", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, modifier = Modifier.size(32.dp)) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                AppGridSection(
                    apps = apps,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    isPinned = isPinned,
                    isGuest = false,
                    animationsEnabled = true
                )
            }
        }
    }
}

@Composable
private fun CollectionPopup(
    folder: Folder,
    allApps: List<AppInfo>,
    onClose: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onAddApps: () -> Unit
) {
    val folderApps = allApps.filter { it.packageName in folder.apps }
    val color = try { Color(AndroidColor.parseColor(folder.color)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, color.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(folder.name.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onAddApps) { Icon(Icons.Filled.Edit, null, tint = color) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (folderApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No apps here. Tap the edit icon to add some.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(100.dp),
                        modifier = Modifier.heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        gridItems(folderApps) { app ->
                            AppCard(app = app, onClick = { onAppClick(app) }, onLongClick = { onAppLongClick(app) }, isPinned = false, isGuest = false, animationsEnabled = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectorOverlay(
    allApps: List<AppInfo>,
    selectedPkgs: List<String>,
    onClose: () -> Unit,
    onToggleApp: (String) -> Unit
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MANAGE COLLECTION", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onClose, shape = RoundedCornerShape(12.dp)) { Text("DONE") }
                }
                Spacer(modifier = Modifier.height(24.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    gridItems(allApps) { app ->
                        val isSelected = selectedPkgs.contains(app.packageName)
                        Card(
                            modifier = Modifier.size(width = 110.dp, height = 110.dp).clickable { onToggleApp(app.packageName) },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(modifier = Modifier.size(48.dp)) {
                                    val icon = app.icon ?: BitmapPainter(android.graphics.Bitmap.createBitmap(1,1, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap())
                                    Icon(painter = icon, null, modifier = Modifier.size(48.dp), tint = Color.Unspecified)
                                    if (isSelected) Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.align(Alignment.TopEnd).size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(app.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundGlow(isDarkTheme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isDarkTheme) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.15f * glowAlpha), Color.Transparent), center = center.copy(x = size.width * 0.2f, y = size.height * 0.2f), radius = size.width * 0.8f))
                drawCircle(brush = Brush.radialGradient(colors = listOf(Color(0xFFD500F9).copy(alpha = 0.12f * glowAlpha), Color.Transparent), center = center.copy(x = size.width * 0.8f, y = size.height * 0.8f), radius = size.width * 0.7f))
            }
        }
    }
}

@Composable
fun ClockWidget(is24h: Boolean) {
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    LaunchedEffect(is24h) {
        while (true) {
            val cal = Calendar.getInstance()
            time = SimpleDateFormat(if (is24h) "HH:mm" else "hh:mm a", Locale.getDefault()).format(cal.time)
            date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(cal.time)
            delay(10000L)
        }
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(text = time, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = (-2).sp)
        Text(text = date.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
    }
}

@Composable
private fun LockDialog(onSuccess: () -> Unit, onDismiss: () -> Unit, settingsManager: SettingsManager) {
    var passIn by remember { mutableStateOf("") }
    var authIn by remember { mutableStateOf("") }
    var isReset by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isReset) "Reset Password" else "Enter Password", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (!isReset) {
                    OutlinedTextField(value = passIn, onValueChange = { passIn = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    TextButton(onClick = { isReset = true }) { Text("Forgot password?") }
                } else {
                    Text("Enter code from Google Authenticator.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = authIn, onValueChange = { authIn = it }, label = { Text("Auth Code") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }
        },
        confirmButton = { Button(onClick = { if (!isReset) { if (passIn == settingsManager.getPassword()) onSuccess() } else { if (authIn.length == 6 && settingsManager.getSecretKey() != null) { settingsManager.setPassword(""); onSuccess() } } }, shape = RoundedCornerShape(12.dp)) { Text(if (isReset) "Reset & Enter" else "Unlock") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileIcon(icon: String, uri: String?, size: Int = 40) {
    Box(modifier = Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        if (uri != null) AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(icon, fontSize = (size * 0.5).sp)
    }
}

@Composable
private fun ProfileHeader(profile: Profile, onSwitchClick: () -> Unit, onOpenGuide: () -> Unit) {
    Surface(modifier = Modifier.wrapContentWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), tonalElevation = 8.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            var isFoc by remember { mutableStateOf(false) }
            Surface(modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onSwitchClick).onFocusChanged { isFoc = it.isFocused }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isFoc) 0.5f else 0.3f), border = BorderStroke(2.dp, if (isFoc) MaterialTheme.colorScheme.primary else Color.Transparent)) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileIcon(profile.icon, profile.iconUri, size = 32)
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onOpenGuide) { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, isPinned: Boolean, isGuest: Boolean, animationsEnabled: Boolean) {
    var isFoc by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFoc && animationsEnabled) 1.05f else 1f)
    Card(modifier = Modifier.size(width = 110.dp, height = 120.dp).combinedClickable(onClick = onClick, onLongClick = if (!isGuest) onLongClick else null).onFocusChanged { isFoc = it.isFocused }.scale(scale), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)), elevation = CardDefaults.cardElevation(defaultElevation = if (isFoc) 12.dp else 2.dp), border = BorderStroke(2.dp, if (isFoc) MaterialTheme.colorScheme.primary else Color.Transparent)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Box(modifier = Modifier.size(56.dp)) {
                    val icon = app.icon ?: BitmapPainter(android.graphics.Bitmap.createBitmap(1,1, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap())
                    Icon(painter = icon, contentDescription = app.label, modifier = Modifier.size(56.dp), tint = Color.Unspecified)
                    if (isPinned && !isGuest) Box(modifier = Modifier.size(20.dp).align(Alignment.TopEnd).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Star, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiary) }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(app.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AppGridSection(modifier: Modifier = Modifier, title: String? = null, apps: List<AppInfo>, onAppClick: (AppInfo) -> Unit, onAppLongClick: (AppInfo) -> Unit, isPinned: (String) -> Boolean, isGuest: Boolean, animationsEnabled: Boolean) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), thickness = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            }
        }
        if (apps.isEmpty() && title == "Shortcuts") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pinned apps yet.\nUse the Launchpad to pin favorites.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 110.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                gridItems(apps, key = { it.packageName }) { app -> AppCard(app, { onAppClick(app) }, { onAppLongClick(app) }, isPinned(app.packageName), isGuest, animationsEnabled) }
            }
        }
    }
}

@Composable
private fun NavigationDock(onAppClick: (String) -> Unit, onAllAppsClick: () -> Unit, isAllAppsOpen: Boolean, animationsEnabled: Boolean) {
    val shortcuts = remember { listOf(NavShortcut("Maps", Icons.Filled.Navigation, "com.google.android.apps.maps"), NavShortcut("Music", Icons.Filled.MusicNote, "com.spotify.music"), NavShortcut("Shop", Icons.Filled.ShoppingCart, "com.android.vending"), NavShortcut("Web", Icons.Filled.Language, "com.android.chrome")) }
    Surface(modifier = Modifier.fillMaxWidth().height(90.dp), shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), tonalElevation = 16.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NavDockButton(shortcut = NavShortcut("Launch", Icons.Filled.Dashboard, null), onClick = onAllAppsClick, isSelected = isAllAppsOpen, animationsEnabled = animationsEnabled)
            shortcuts.forEach { NavDockButton(shortcut = it, onClick = { if (it.packageName != null) onAppClick(it.packageName) }, animationsEnabled = animationsEnabled) }
        }
    }
}

@Composable
private fun RowScope.NavDockButton(shortcut: NavShortcut, onClick: () -> Unit, isSelected: Boolean = false, animationsEnabled: Boolean) {
    var isFoc by remember { mutableStateOf(false) }
    val isHi = isSelected || isFoc
    val iconSize by animateDpAsState(if (isHi && animationsEnabled) 32.dp else 28.dp)
    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).onFocusChanged { isFoc = it.isFocused }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(shortcut.icon, null, modifier = Modifier.size(iconSize), tint = if (isHi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(shortcut.name, style = MaterialTheme.typography.labelMedium, fontWeight = if (isHi) FontWeight.Black else FontWeight.Bold, color = if (isHi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderSection(folders: List<Folder>, onFolderClick: (Folder) -> Unit, onFolderLongClick: (Folder) -> Unit, onAddFolderClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("COLLECTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
            IconButton(onClick = onAddFolderClick, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            rowItems(folders) { f ->
                var isFoc by remember { mutableStateOf(false) }
                val color = try { Color(AndroidColor.parseColor(f.color)) } catch (_: Exception) { MaterialTheme.colorScheme.secondary }
                Card(modifier = Modifier.size(width = 150.dp, height = 85.dp).combinedClickable(onClick = { onFolderClick(f) }, onLongClick = { onFolderLongClick(f) }).onFocusChanged { isFoc = it.isFocused }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), border = BorderStroke(2.dp, if (isFoc) color else Color.Transparent)) {
                    Row(modifier = Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Folder, null, modifier = Modifier.size(24.dp), tint = color) }
                        Column { Text(f.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${f.apps.size} APPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsBar(brightness: Float, onBrightnessChange: (Float) -> Unit, isBluetoothOn: Boolean, onToggleBluetooth: () -> Unit, isWifiOn: Boolean, onToggleWifi: () -> Unit, onOpenSettings: () -> Unit, isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), tonalElevation = 12.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(0.4f)) {
                Text("CONTROLS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WbSunny, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = brightness, onValueChange = onBrightnessChange, modifier = Modifier.padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QsToggle(icon = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode, isOn = isDarkTheme, label = "Theme", onClick = onThemeToggle)
                QsToggle(icon = Icons.Filled.Bluetooth, isOn = isBluetoothOn, label = "BT", onClick = onToggleBluetooth)
                QsToggle(icon = Icons.Filled.Wifi, isOn = isWifiOn, label = "WiFi", onClick = onToggleWifi)
                QsToggle(icon = Icons.Filled.Settings, isOn = false, label = "Setup", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun QsToggle(icon: ImageVector, isOn: Boolean, label: String, onClick: () -> Unit) {
    var isFoc by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).onFocusChanged { isFoc = it.isFocused }, shape = RoundedCornerShape(18.dp), color = if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), border = BorderStroke(1.5.dp, if (isFoc) MaterialTheme.colorScheme.primary else Color.Transparent)) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.Center) { Icon(icon, label, modifier = Modifier.size(24.dp), tint = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun AddFolderDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val colors = listOf("#00E5FF", "#00FF88", "#FF9100", "#FF1744", "#D500F9", "#2979FF")
    var selectedColor by remember { mutableStateOf(colors[0]) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Smart Collection", fontWeight = FontWeight.Black) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 15) name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    colors.forEach { c -> val isSel = c == selectedColor; Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(AndroidColor.parseColor(c))).clickable { selectedColor = c }.border(3.dp, if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)) }
                }
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) }, enabled = name.isNotBlank(), shape = RoundedCornerShape(12.dp)) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileSwitcherDialog(profiles: List<Profile>, currentId: String, onSelect: (Profile) -> Unit, onAddProfile: () -> Unit, onStartGuest: () -> Unit, onDeleteProfile: (String) -> Unit, onDismiss: () -> Unit, isGuestHidden: Boolean) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Driver Selection", fontWeight = FontWeight.Black) },
        text = {
            Column {
                profiles.forEach { p ->
                    val isCur = p.id == currentId
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onSelect(p) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (isCur) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), border = if (isCur) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { ProfileIcon(p.icon, p.iconUri, size = 52); Column { Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(if (p.isGuest) "Guest" else "${p.pinnedApps.size} shortcuts", style = MaterialTheme.typography.labelMedium) } }
                            if (!p.isGuest && !isCur) IconButton(onClick = { onDeleteProfile(p.id) }) { Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onAddProfile, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Add New") }
                    if (!isGuestHidden) OutlinedButton(onClick = onStartGuest, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), enabled = !profiles.any { it.isGuest }) { Text("Guest Mode") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun AddProfileDialog(onConfirm: (String, String, Uri?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val emojis = listOf("\uD83D\uDC64", "\uD83D\uDE97", "\uD83C\uDFCE\uFE0F", "\uD83D\uDDFA\uFE0F", "\uD83C\uDFB5", "\uD83D\uDC69\u200D\u2708\uFE0F", "\uD83D\uDC68\u200D\u2708\uFE0F")
    var selectedEmoji by remember { mutableStateOf(emojis[0]) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val l = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Passenger Profile", fontWeight = FontWeight.Black) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)).clickable { l.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (selectedUri != null) AsyncImage(model = selectedUri, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(selectedEmoji, fontSize = 44.sp)
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.BottomCenter) { Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(22.dp).padding(bottom = 6.dp)) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = name, onValueChange = { if (it.length <= 15) name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                Spacer(modifier = Modifier.height(20.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { emojis.forEach { e -> val isS = e == selectedEmoji && selectedUri == null; Surface(modifier = Modifier.size(44.dp).clip(CircleShape).clickable { selectedEmoji = e; selectedUri = null }, shape = CircleShape, color = if (isS) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent, border = if (isS) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) { Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 24.sp) } } } }
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedEmoji, selectedUri) }, enabled = name.isNotBlank(), shape = RoundedCornerShape(12.dp)) { Text("Create Profile") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun QuickStartGuide(onDismiss: () -> Unit) {
    var s by remember { mutableIntStateOf(0) }
    val steps = listOf(GuideStep("Welcome", "Your ultimate companion.", Icons.Filled.DirectionsCar), GuideStep("Profiles", "Customize for each passenger.", Icons.Filled.Person), GuideStep("Collections", "Group apps into smart folders.", Icons.Filled.CreateNewFolder), GuideStep("Quick Access", "Long press to pin shortcuts.", Icons.Filled.Star), GuideStep("Stealth", "Toggle Night Mode for focus.", Icons.Filled.DarkMode))
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.width(420.dp).padding(20.dp), shape = RoundedCornerShape(40.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(steps[s].icon, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary) }
                Spacer(modifier = Modifier.height(32.dp)); Text(steps[s].title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center); Text(steps[s].description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.height(40.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = onDismiss) { Text("Skip") }; Button(onClick = { if (s < steps.size - 1) s++ else onDismiss() }, shape = RoundedCornerShape(16.dp)) { Text(if (s < steps.size - 1) "Next" else "Start Engine") } }
            }
        }
    }
}

private data class GuideStep(val title: String, val description: String, val icon: ImageVector)
fun Modifier.scale(s: Float) = this.then(Modifier.graphicsLayer(scaleX = s, scaleY = s))
fun Modifier.size(s: androidx.compose.ui.unit.Dp) = this.then(Modifier.size(s, s))
