# CarDash

Custom Android tablet launcher for back-seat car entertainment.

Registered as a HOME activity — set it as the default launcher, mount the tablet in the car, and kids/passengers get a kid-friendly interface with profiles, pinned apps, folders, and quick settings.

## Features

- **Profiles + Guest mode** — each family member gets their own profile with custom name, emoji icon, pinned apps, and folders. Guest mode for temporary use.
- **Pinned dock** — hold your most-used apps in a horizontal dock. Long-press to unpin or remove.
- **App folders** — group apps into color-coded folders in the dock. Create, rename, and manage folders from long-press context menus.
- **App drawer** — full-screen scrollable grid of all installed apps. Long-press any app to pin it to the dock, add it to a folder, or create a new folder.
- **Quick settings bar** — brightness slider, rotation lock toggle, Bluetooth toggle, WiFi toggle.
- **Nav dock** — one-tap buttons for Maps, Waze, Spotify, and YouTube.
- **Settings screen** — password-protected settings lock, auto-launch on boot, hide guest mode from switcher, animations toggle, 24h clock format, dashboard glow.
- **Landscape-optimized** — designed for a tablet mounted in landscape orientation.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34
- **Compose BOM:** 2024.02.00
- **Images:** Coil
- **Persistence:** SharedPreferences via JSON serialization

## Building

Open the project in Android Studio, sync Gradle, and run:

```
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Installing

1. Enable **Install from unknown apps** on your tablet (or use Android Studio's Run).
2. Install the APK.
3. Go to **Settings → Apps → Default apps → Home app** and select CarDash.

## Project Structure

```
app/src/main/java/com/cartablet/
├── CarLauncherApp.kt          # Application class
├── MainActivity.kt            # Entry point (singleTask, landscape)
├── data/
│   ├── Profile.kt             # Profile + Folder data classes (JSON serialization)
│   ├── ProfileManager.kt      # CRUD for profiles and folders
│   └── SettingsManager.kt     # App settings persistence
└── ui/
    ├── HomeScreen.kt          # Main launcher UI (dock, drawer, quick settings, profiles)
    ├── SettingsScreen.kt      # Settings with password lock
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```
