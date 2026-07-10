# Build the CarDash Companion App

This prompt tells you everything you need to build a phone remote control app for the **CarDash** tablet launcher. The companion app connects to the tablet over WiFi (HTTP) or Bluetooth LE and lets the user switch profiles, lock/unlock, control volume, launch apps, and put the tablet to sleep.

## How to use this prompt

Feed this entire document to your AI coding agent. It contains:
- The full API contract (no need to read the tablet source)
- UI requirements
- Android project setup
- Implementation order
- Deliverable checklist

## Where to build

**Option A — Inside CarDash repo (preferred):**
If you have access to the existing CarDash project at `https://github.com/jakubhegr18-design/CarDash.git`:
- Clone the repo
- Add a new `companion/` module (update `settings.gradle.kts` to `include(":companion")`)
- Place source files under `companion/src/main/java/com/cartablet/companion/`
- The root `build.gradle.kts` already has the `com.android.library` plugin. Add `android-application` for the module.

**Option B — Standalone new project:**
If building from scratch:
- Create a new Android project with application ID `com.cartablet.companion`
- Use Kotlin + Jetpack Compose (BOM 2024.02.00, Compose Compiler 1.5.10, Kotlin 1.9.22, AGP 8.2.2)
- Min SDK 26, Target SDK 34, Compile SDK 34
- App name "CarDash Remote"

---

## API Contract

### WiFi / HTTP

The tablet runs an embedded HTTP server (NanoHTTPD) on port 8080 (configurable). Base URL format: `http://<ip>:<port>`

Base URL format: `http://<ip>:<port>`

#### Endpoints

| Method | Path | Request Body | Response |
|--------|------|-------------|----------|
| GET | `/status` | — | `{"currentProfileId":"...","currentProfileName":"...","currentProfileIcon":"...","isLocked":false,"kidsMode":false,"strictLock":false,"speedometer":false,"clock24h":true}` |
| GET | `/profiles` | — | `{"profiles":[{"id":"...","name":"...","icon":"...","isGuest":false,"isKid":false}]}` |
| POST | `/switch-profile` | `{"profileId":"..."}` | `{"success":true,"profile":"ProfileName"}` |
| POST | `/lock` | — | `{"success":true}` |
| POST | `/unlock` | — | `{"success":true}` |
| POST | `/launch-app` | `{"package":"com.example.app"}` | `{"success":true}` |
| POST | `/volume` | `{"level":50}` | `{"success":true,"volume":50}` |
| POST | `/sleep` | — | `{"success":true}` |

**Error responses:** `{"success":false,"error":"message"}`

Use an async HTTP library (Volley, OkHttp, or Ktor client). Make requests on a background thread.

### Bluetooth Low Energy

The tablet advertises as a BLE peripheral with:

- **Service UUID:** `cafebab0-0001-4c75-a8e9-d5e5b0a7a9e0`
- **Device name:** "CarDash"

#### GATT Characteristics

| Characteristic UUID | Properties | Description |
|---------------------|------------|-------------|
| `cafebab0-0002-4c75-a8e9-d5e5b0a7a9e0` | Read | Returns status JSON (same shape as `/status` HTTP endpoint) |
| `cafebab0-0003-4c75-a8e9-d5e5b0a7a9e0` | Read | Returns profiles JSON (same shape as `/profiles` HTTP endpoint) |
| `cafebab0-0004-4c75-a8e9-d5e5b0a7a9e0` | Write | Accepts a JSON command string (see below) |
| `cafebab0-0005-4c75-a8e9-d5e5b0a7a9e0` | Notify | Sends command result JSON back to the phone |

#### BLE Command Format (write to Control characteristic)

Write the command as a UTF-8 byte array containing JSON:

| Action | JSON |
|--------|------|
| Switch profile | `{"action":"switch-profile","profileId":"..."}` |
| Lock | `{"action":"lock"}` |
| Unlock | `{"action":"unlock"}` |
| Launch app | `{"action":"launch-app","package":"com.example.app"}` |
| Set volume | `{"action":"volume","level":50}` |
| Sleep | `{"action":"sleep"}` |

**Responses come back:** on the Notify characteristic as JSON bytes: `{"success":true}` or `{"success":false,"error":"message"}`.

After a command completes, re-read the Status and Profiles characteristics to refresh.

---

## UI Specification

Build a **portrait, scrollable** app with Material 3 (Jetpack Compose). Below is the exact layout, top to bottom.

### Screen: RemoteControlScreen

#### 1. Title Bar
- Centered text: "CarDash Remote"
- Headline Medium, bold

#### 2. Connection Mode Selector
- Two FilterChips: "WiFi" and "BLE"
- Only one mode active at a time
- Switching mode is disabled while connected

#### 3. Connection Panel (Card with rounded corners)

**When NOT connected:**

*WiFi mode:*
- `OutlinedTextField` for tablet IP:Port (placeholder: "192.168.1.100:8080")
- Full-width "CONNECT" button

*BLE mode:*
- Full-width "SCAN FOR DEVICES" button (toggles to "STOP SCAN" while scanning)
- While scanning: show `LinearProgressIndicator` and list discovered devices
- Each discovered device is a full-width `OutlinedButton` with a Bluetooth icon + device name
- When not scanning and no devices found: show hint text "Make sure Bluetooth is on and tablet has BLE mode enabled"

**When connected:**
- Green checkmark icon + "Connected to {ip}" (WiFi) or "Connected via BLE"
- "DISCONNECT" button on the right

#### 4. Status message area
- Small text showing "OK", "Failed", "Connection failed", etc.
- Centered, muted color

#### 5. Status Card
(Only visible when connected and status data exists)
- Card with surfaceVariant background
- Label "STATUS"
- Rows: Profile name, Locked (Yes/No), Kids Mode (On/Off), Strict Lock (On/Off), Speedometer (On/Off), 24h Clock (Yes/No)

#### 6. Profile Switcher
(Only visible if profiles list is non-empty)
- Card titled "SWITCH PROFILE"
- Row of `OutlinedButton`s, one per profile, showing icon + name
- Tapping switches to that profile

#### 7. Control Buttons
- Card titled "CONTROLS"
- Row of two buttons side by side:
  - Lock/Unlock button (shows lock icon when unlocked, unlock icon when locked)
  - Sleep button (red, power icon)

#### 8. Volume Control
- Card titled "VOLUME"
- Row: VolumeDown icon | Slider (0-100) | VolumeUp icon | percentage text

---

## Implementation Order

### Step 1: Project Scaffolding
- Set up the Android project (or `companion/` module if inside CarDash repo)
- Configure `build.gradle.kts` with dependencies:
  - Material 3 Compose
  - Compose BOM 2024.02.00
  - Compose Compiler Extension 1.5.10
  - Kotlin 1.9.22
  - Volley (or OkHttp) for HTTP
  - Activity Compose
- Configure `AndroidManifest.xml` with:
  - Internet + Network state permissions
  - BLE permissions: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
  - `ACCESS_FINE_LOCATION` (required for BLE scanning on Android 10-)
  - `android.hardware.bluetooth_le` feature with `required="false"`
  - No BLE hardware feature requirement (graceful fallback if device lacks BLE)
- Verify it compiles with a blank Activity

### Step 2: WiFi Client
- Create `CompanionClient.kt`
- Constructor takes base URL string (ip:port, e.g. "192.168.1.100:8080")
- Async methods with callbacks (or coroutines with callbacks):
  - `getStatus(callback: (JSONObject?) -> Unit)`
  - `getProfiles(callback: (JSONObject?) -> Unit)`
  - `switchProfile(profileId: String, callback: (Boolean) -> Unit)`
  - `lock(callback: (Boolean) -> Unit)`
  - `unlock(callback: (Boolean) -> Unit)`
  - `launchApp(packageName: String, callback: (Boolean) -> Unit)`
  - `setVolume(level: Int, callback: (Boolean) -> Unit)`
  - `sleep(callback: (Boolean) -> Unit)`

### Step 3: BLE Client
- Create `BleCompanionClient.kt`
- Callback interface:
  - `onStatusUpdate: ((JSONObject?) -> Unit)?`
  - `onProfilesUpdate: ((JSONObject?) -> Unit)?`
  - `onDeviceFound: ((String, BluetoothDevice) -> Unit)?`
  - `onConnected: ((Boolean) -> Unit)?`
  - `onDisconnected: (() -> Unit)?`
  - `onCommandResult: ((JSONObject?) -> Unit)?`
- Methods:
  - `startScan()` — scan for devices advertising the CarDash service UUID
  - `stopScan()`
  - `connect(device: BluetoothDevice)` — stops scan, connects GATT, discovers services, enables notifications on Result characteristic, reads Status + Profiles
  - `disconnect()`
  - `readStatus()` — manually re-read status
  - `readProfiles()` — manually re-read profiles
  - `sendCommand(action, params)` — write JSON to Control characteristic
  - Convenience methods: `switchProfile()`, `lock()`, `unlock()`, `sleep()`, `setVolume()`, `launchApp()`
- Important: on `onCharacteristicChanged` for the Result characteristic, after handling the result JSON, automatically call `readStatus()` and `readProfiles()` to refresh

### Step 4: Compose UI
- Create `ui/RemoteControlScreen.kt`
- Implement the full screen as specified above
- Use `remember { mutableStateOf() }` for all state variables
- For BLE scanning, use `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` to request `BLUETOOTH_SCAN` permission on Android 12+
- Separate reusable composables:
  - `StatusCard(status: JSONObject)`
  - `ProfileSwitcher(profiles: List<JSONObject>, onSwitch: (String) -> Unit)`
  - `ControlButtons(isLocked, onLock, onUnlock, onSleep)`
  - `VolumeControl(volume, onVolumeChange)`

### Step 5: Wire It Together
- Create `CompanionActivity.kt` — single activity that sets content to `RemoteControlScreen()`
- Create `CompanionApp.kt` — Application class (needed for Volley singleton pattern)
- Ensure the BLE client lifecycle is properly managed (stop scan on disconnect, cleanup on Activity destroy)

### Step 6: Build & Verify
- Run `./gradlew :companion:assembleDebug` (or `./gradlew assembleDebug` for standalone)
- Confirm APK is generated at `companion/build/outputs/apk/debug/companion-debug.apk` (or `app/build/...` for standalone)
- Fix any compilation errors

---

## Deliverables

The agent should produce these files:

| File | Description |
|------|-------------|
| `build.gradle.kts` | Module-level build config |
| `src/main/AndroidManifest.xml` | Manifest with permissions |
| `src/main/res/values/themes.xml` | Basic theme (android:Theme.Material.NoActionBar is fine) |
| `src/main/java/com/cartablet/companion/CompanionApp.kt` | Application class |
| `src/main/java/com/cartablet/companion/CompanionActivity.kt` | Entry point |
| `src/main/java/com/cartablet/companion/CompanionClient.kt` | WiFi HTTP client |
| `src/main/java/com/cartablet/companion/BleCompanionClient.kt` | BLE client |
| `src/main/java/com/cartablet/companion/ui/RemoteControlScreen.kt` | Full Compose UI |

**Final APK:** `companion/build/outputs/apk/debug/companion-debug.apk`

---

## Edge Cases & Error Handling

- **No WiFi:** Show a hint that connection requires both devices on the same network
- **BLE not supported:** The scan button should still appear but show "BLE not available on this device" if `bluetoothLeAdvertiser` is null
- **Permission denied:** Show a message asking the user to grant BLE/location permissions
- **Connection timeout:** Show "Connection failed" after a reasonable timeout
- **Disconnected during use:** Show the disconnected state, allow reconnection
- **Empty profiles list:** Hide the Profile Switcher section
- **Invalid JSON response:** Handle parse errors gracefully (null/fallback values)

---

## Dependencies

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.android.volley:volley:1.2.1")
}
```
