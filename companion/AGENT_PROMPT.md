# CarDash Companion App — Agent Prompt

This document tells you everything you need to know to understand and continue development of the CarDash companion app. Read this first before making any changes.

---

## Project Overview

CarDash is a **custom Android tablet launcher** for back-seat car entertainment. It consists of two Android apps in one Git repo:

| Module | Path | Purpose |
|--------|------|---------|
| `:app` | `app/` | Tablet launcher (HOME activity replacement) |
| `:companion` | `companion/` | Phone remote control app |

The companion app connects to the tablet over **WiFi (HTTP)** or **Bluetooth Low Energy (BLE)** to remotely control the dashboard — switch profiles, lock/unlock, launch apps, control volume, put tablet to sleep.

---

## Architecture

### Communication Protocols

There are two independent transport layers. Both use **JSON commands and responses**. The tablet supports three modes: WiFi only, BLE only, or Both (selectable in tablet Settings).

#### 1. WiFi / HTTP (NanoHTTPD)

**Tablet side:** `app/src/main/java/com/cartablet/utils/RemoteControlServer.kt`
- Embedded HTTP server (NanoHTTPD library) on a configurable port (default 8080)
- RESTful JSON endpoints
- The server is started/stopped by `CarLauncherApp.updateRemoteServers()`

**Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/status` | Returns current profile, lock state, kids mode, etc. |
| `GET` | `/profiles` | Returns list of all profiles |
| `POST` | `/switch-profile` | Body: `{"profileId":"..."}` |
| `POST` | `/lock` | Lock the dashboard |
| `POST` | `/unlock` | Unlock the dashboard |
| `POST` | `/launch-app` | Body: `{"package":"com.example.app"}` |
| `POST` | `/volume` | Body: `{"level":50}` (0-100) |
| `POST` | `/sleep` | Put tablet to sleep |

**Phone side:** `companion/src/main/java/com/cartablet/companion/CompanionClient.kt`
- Uses Volley HTTP library for async requests
- Passes a callback for each request

#### 2. Bluetooth Low Energy (BLE)

**Tablet side:** `app/src/main/java/com/cartablet/utils/BleControlServer.kt`
- GATT server (peripheral mode) advertising a custom service
- The tablet advertises as "CarDash" with service UUID `cafebab0-0001-4c75-a8e9-d5e5b0a7a9e0`
- Starts/stops via `CarLauncherApp.updateRemoteServers()`

**GATT Service Structure:**

| Characteristic UUID | Properties | Purpose |
|---------------------|------------|---------|
| `cafebab0-0002-4c75-a8e9-d5e5b0a7a9e0` | Read | Returns current status JSON |
| `cafebab0-0003-4c75-a8e9-d5e5b0a7a9e0` | Read | Returns profiles JSON array |
| `cafebab0-0004-4c75-a8e9-d5e5b0a7a9e0` | Write | Accepts JSON command: `{"action":"...", ...}` |
| `cafebab0-0005-4c75-a8e9-d5e5b0a7a9e0` | Notify | Sends command result JSON |

**Available BLE commands (same as HTTP):**
- `{"action":"switch-profile", "profileId":"..."}`
- `{"action":"lock"}`
- `{"action":"unlock"}`
- `{"action":"launch-app", "package":"..."}`
- `{"action":"volume", "level":50}`
- `{"action":"sleep"}`

**Phone side:** `companion/src/main/java/com/cartablet/companion/BleCompanionClient.kt`
- Scans for BLE devices advertising the CarDash service UUID
- Connects and discovers GATT services
- Provides callback-based API: `onStatusUpdate`, `onProfilesUpdate`, `onConnected`, `onDisconnected`, `onCommandResult`
- After sending a write command, the result comes back on the notify characteristic, and then status/profiles are re-read automatically

---

## Key Classes

### App Module (Tablet)

| File | Responsibility |
|------|----------------|
| `CarLauncherApp.kt` | Application class; manages lifecycle of WiFi server and BLE server via `updateRemoteServers()` |
| `MainActivity.kt` | Entry point, sets up Compose theme and `HomeScreen` |
| `data/Profile.kt` | `Profile` and `Folder` data classes with JSON serialization |
| `data/ProfileManager.kt` | CRUD operations for profiles and folders (SharedPreferences + JSON) |
| `data/SettingsManager.kt` | All settings persistence including `isRemoteControlEnabled()`, `getRemoteControlMode()`, etc. |
| `ui/HomeScreen.kt` | Main launcher UI (dock, drawer, quick settings, profiles, speedometer, glow effects) |
| `ui/SettingsScreen.kt` | Settings UI including Remote Control section with mode selector (WiFi / BLE / Both) |
| `utils/RemoteControlServer.kt` | NanoHTTPD HTTP server |
| `utils/BleControlServer.kt` | BLE GATT server (peripheral) |
| `utils/TotpHelper.kt` | TOTP for password recovery |

### Companion Module (Phone)

| File | Responsibility |
|------|----------------|
| `CompanionApp.kt` | Application class |
| `CompanionActivity.kt` | Entry point, sets up `RemoteControlScreen` |
| `CompanionClient.kt` | WiFi HTTP client (Volley) |
| `BleCompanionClient.kt` | BLE client (scanner + GATT) |
| `ui/RemoteControlScreen.kt` | Full UI: WiFi/BLE mode selector, connection panel, status display, profile switcher, lock/unlock, sleep, volume slider |

---

## Settings (Tablet Side)

All settings are stored in SharedPreferences `cartablet_prefs`. Key settings relevant to remote control:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `remote_control_enabled` | Boolean | `false` | Master toggle |
| `remote_control_mode` | String | `"WIFI"` | One of `WIFI`, `BLE`, `BOTH` |
| `remote_control_port` | Int | `8080` | TCP port for HTTP server |

---

## How to Add a New Command

### 1. Add to the tablet's HTTP server
In `RemoteControlServer.kt`, add a new `case` in the `serve()` method and a handler method.

### 2. Add to the tablet's BLE server
In `BleControlServer.kt`, add a new `action` to the `when` block in `handleCommand()`.

### 3. Add to the phone WiFi client
In `CompanionClient.kt`, add a new method that calls the HTTP endpoint.

### 4. Add to the phone BLE client
In `BleCompanionClient.kt`, add a new convenience method that calls `sendCommand()`.

### 5. Add to the phone UI
In `RemoteControlScreen.kt`, add the UI element.

---

## Permissions

### App (Tablet)
- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_SCAN`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `WRITE_SETTINGS`

### Companion (Phone)
- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- `ACCESS_FINE_LOCATION`
- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES`

---

## Known Limitations & Next Steps

1. **WiFi auto-discovery** — The phone currently requires manual IP entry. Implement NSD/mDNS (Android's `NsdManager`) to discover the tablet automatically on the local network.

2. **TV mode UI** — The `LEANBACK_LAUNCHER` intent filter is registered but the UI doesn't adapt. Create a separate TV-optimized composable (leanback-style cards, d-pad navigation) when `tvMode` setting is on.

3. **BLE reconnection** — If BLE disconnects, the companion app doesn't auto-reconnect. Add connection retry logic.

4. **App launcher from phone** — The companion UI doesn't list or launch apps. Add an "Apps" tab that fetches the installed app list and lets the user tap to launch.

5. **Connection state persistence** — Remember last-used IP or BLE device so reconnection is one tap.

6. **TLS/security** — The HTTP server has no authentication. For untrusted networks, add a simple token-based auth or switch to HTTPS.

7. **Background service** — The tablet's server stops when the app is killed. Consider a foreground service to keep the server alive.

---

## Build & Run

```bash
# Build both apps
./gradlew assembleDebug

# APK locations:
#   app/build/outputs/apk/debug/app-debug.apk        (tablet launcher)
#   companion/build/outputs/apk/debug/companion-debug.apk  (phone remote)

# Install the tablet app, set it as default HOME app, then:
# Enable Settings > Remote Control, choose WiFi or BLE mode
# Open the companion app, enter IP or scan BLE, connect
```
