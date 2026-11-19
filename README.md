# SYMPHINE GAME SPACE

**Native Android Gaming Hub, Per-Game Diagnostics & Verified System Tuning Suite**

- **App Name:** SYMPHINE GAME SPACE (`Symphine Space`)
- **Package ID:** `com.symphine.gamespace`
- **Version:** `1.0.0` (`versionCode = 1`)
- **Developer:** Reo Moser
- **Min SDK:** Android 10 (API 29)
- **Target / Compile SDK:** Android 15 / 16+ (API 35 / 36)
- **Network Access:** **100% Offline** (zero `INTERNET` permission, zero ads, zero cloud telemetry)

---

## Key Features

### 1. Home Dashboard & Live Hardware Diagnostics
- Genuine Android device telemetry read directly from `Build`, `ActivityManager`, `StatFs`, `BatteryManager`, `PowerManager` thermal status, `DisplayManager`, `EGL10`/`GLES20`, `/proc/cpuinfo`, and `/proc/stat`.
- Live hardware meters for RAM, Internal Storage, Battery level & temperature, Thermal throttling status, and Display refresh modes.
- Quick Actions bar for instant scanning, Display Center, Performance tuning, Monitoring, Shizuku Center, Diagnostics Report export, and Backup.

### 2. Game Space Library & 15-Section Game Detail Center
- Automatic detection of installed launchable games (`PackageManager` + `ApplicationInfo.CATEGORY_GAME` + package heuristics) plus manual Add/Hide app controls.
- Grid, List, and Compact views; Favorites, Pinned, Hidden, Recently Played, and Custom Group filters; Bulk selection and multi-game actions.
- Per-game 15-section **Game Detail Center**:
  1. Overview (version, target SDK, classification, install/update timestamps, session timer)
  2. Performance (Android Game Mode, ART compiler, Offline Recommendation Engine)
  3. Display (Supported `Display.Mode` refresh rates, animation/touch scale, DPI, proportional downscale)
  4. Graphics (OpenGL ES/Vulkan inspection + ANGLE Graphics Profile `smooth`/`pixel` + Texture LOD `1–4`)
  5. Memory (Live RAM usage, low-memory threshold, Safe App Cache clearing)
  6. Battery (Percentage, charging source, voltage, real battery temperature, Battery Optimization shortcut)
  7. Thermal (`PowerManager.getCurrentThermalStatus()` real throttling levels & alerts)
  8. Gaming Tools (Draggable Floating Overlay launcher, Gaming DND shortcut, App Info)
  9. Background & Power (Protected Package Whitelist, Shizuku power flags with rollback)
  10. Customization (Custom alias, custom accent hex, banner, favorite/pin, tags, markdown-free notes, group assignment)
  11. Monitoring (Live sparkline, sample rate control, manual session logger)
  12. Compatibility (OEM-specific feature support classification)
  13. Profile Assignment (Balanced, Gaming, Battery Saver, Smart, Recommended, Display Focus, Monitoring Focus, or custom cloned profiles)
  14. Advanced Settings Registry (Inspect namespace, key, original vs current value, compatibility, verification state)
  15. Backup & Atomic Rollback (JSON config export & one-tap snapshot revert)

### 3. ReoSherly Graphics V5.2 — Symphine Native Integration
All validated optimizations from the **ReoSherly Graphics V5.2** workflow are integrated safely into Kotlin + AIDL `ISymphineShizukuService` (strictly using `ProcessBuilder` with allowlisted commands—never running arbitrary user shell strings):
- **ANGLE Graphics Driver Profile (`smooth` / `pixel` + LOD 1–4):** Writes `angle_gl_driver_selection_pkgs` / `angle_gl_driver_selection_values` with pre-change snapshot capture and read-back verification.
- **ART Bytecode Compiler:** Runs `cmd package compile -m speed-profile|speed|everything -f <package>` via Shizuku.
- **Android Game Mode API (Android 12+ / API 31+):** Runs `cmd game mode performance|standard|battery <package>`.
- **Refresh Rate Control:** Strictly populated from `DisplayManager` `supportedModes`; writes `peak_refresh_rate` and `min_refresh_rate` with atomic restore.
- **UI & Touch Animation Scale:** Writes `window_animation_scale`, `transition_animation_scale`, and `animator_duration_scale`.
- **Display Density & Proportional Downscale:** Computes proportional `wm size WxH` and `wm density DPI` (`70%–100%`) with one-tap `wm size reset` / `wm density reset`.
- **Background & Power Flags:** Controlled toggles with `OriginalBackupSnapshotEntity` capture and full **Atomic Rollback**.

### 4. Symphine Liquid Glass Appearance & Backup Center
- **AMOLED Lavender Design System:** `#08090D` OLED background, `#111319` glass cards, `#A78BFA` primary lavender-purple accent, bundled **Space Grotesk**, **Plus Jakarta Sans**, and **JetBrains Mono** typography.
- **Customization Center:** Built-in presets + live hex color editor, corner radius slider (`0–32 dp`), glass opacity, glow toggle, reduced motion, custom Hero Banner via zero-permission Android Photo Picker, and customizable Bottom Navigation bar (`Floating Pill`, `Liquid Capsule`, `Minimal Indicator`, `Material 3 Bar`, plus adaptive `NavigationRail` on tablets/foldables).
- **Backup & Restore:** Export and pre-validate `.json` configuration backups via Android Storage Access Framework (`SAF`).

---

## Architecture

```text
app/src/main/
├── aidl/com/symphine/gamespace/shizuku/ISymphineShizukuService.aidl
├── java/com/example/MainActivity.kt
└── java/com/symphine/gamespace/
    ├── app/SymphineApplication.kt
    ├── backup/BackupEngine.kt
    ├── core/
    │   ├── model/         # Domain models, OperationStatus, UiSettingsState
    │   ├── notifications/ # NotificationHelper (API 29-36 safe)
    │   └── permissions/   # Least-privilege PermissionManager
    ├── data/
    │   ├── database/      # Room v2 (InstalledGameEntity, InstalledGameDao, Customizations, Sessions, Snapshots, Logs, Profiles)
    │   └── datastore/     # Preferences DataStore (UI theme, whitelists, groups)
    ├── device/            # Real hardware telemetry providers (CPU/RAM/Battery/Thermal/GPU/Display)
    ├── diagnostics/       # OEM Compatibility Analyzer & Text Report Generator
    ├── games/             # GameScanner & GameRepository
    ├── overlay/           # Draggable Floating Telemetry Overlay Service
    ├── performance/       # Offline Recommendation Engine & Settings Registry
    ├── shizuku/           # ShizukuManager, UserService (UID 2000), OperationRegistry
    └── ui/
        ├── components/    # LiquidGlassCard, Sparkline, Verification & Backup Dialogs
        ├── navigation/    # Type-safe Navigation Compose & Adaptive Rail/Bar
        ├── screens/       # Home, Library, GameDetail (15 sections), Performance, Monitoring, Settings
        ├── state/         # SymphineMainViewModel (StateFlow + lifecycle-safe polling)
        └── theme/         # SymphineTheme & bundled fonts
```

---

## Android Compatibility & Honest Limitations

| Capability | Requirements | Notes |
|---|---|---|
| Game Scanner & 15-Section Library | None (Android 10+) | Uses `<queries>` launcher intent + `CATEGORY_GAME` |
| RAM, Storage, Battery, Thermal, Display, GPU Info | None (Android 10+) | Real Android SDK + `/proc` readings |
| Live Telemetry Charts & CSV Export | None (Android 10+) | Lifecycle-safe Flow polling + SAF export |
| JSON Backup & Photo Picker Banner | None (Android 10+) | Zero storage permissions needed |
| Floating Gaming Overlay | Display Over Other Apps | Optional; full app works without it |
| Gaming Do Not Disturb Shortcut | DND Policy Access | Optional system access |
| ANGLE, ART Compile, Game Mode, Refresh Rate, Animation Scale, DPI, Downscale | **Shizuku** (Wireless Debugging / ADB) | Every write captures pre-change snapshot + verifies read-back |
| `debug.angle.*` system properties | Root (UID 0) | Gracefully skipped on Shell UID 2000 without crash |
| Live GPU Utilization % / Root-only SoC sensors | Vendor/Kernel privileges | Marked `Unsupported` rather than fabricated |

---

## Permissions Explained

- `SYSTEM_ALERT_WINDOW` (Optional): Floating draggable game telemetry overlay.
- `POST_NOTIFICATIONS` (Optional, API 33+): Overlay foreground service & operation notices.
- `ACCESS_NOTIFICATION_POLICY` (Optional): Gaming DND mode shortcut.
- `PACKAGE_USAGE_STATS` (Optional): Extra foreground check fallback; built-in launcher session tracker works without it.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`: Keeps active user-started overlay alive.
- `VIBRATE`: Subtle UI haptic feedback.
- `moe.shizuku.permission.API_V23` (Optional): Communicates with running Shizuku server.
- **No `INTERNET` permission. No `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*`.**

---

## Shizuku Setup Guide (No Root Required)

1. Install and open the **Shizuku** app on your Android device.
2. Enable **Developer Options** (`Settings -> About phone -> Tap Build Number 7 times`).
3. Enable **USB Debugging** and **Wireless Debugging**.
4. Inside **Shizuku -> Start via Wireless Debugging**, follow the pairing notification prompt and tap **Start**.
5. Open **SYMPHINE GAME SPACE -> Performance -> Shizuku** (or **Settings -> Permissions**).
6. Tap **Authorize Shizuku**, allow access in the popup, and confirm `Verified / Operational`.

---

## Build Instructions

### Desktop / CLI Gradle Build
Requires JDK 17+ and Android SDK (API 35/36) configured via `ANDROID_SDK_ROOT`:

```bash
# Debug APK
gradle :app:assembleDebug

# Release APK (uses project signing config)
gradle :app:assembleRelease

# Run unit tests
gradle :app:testDebugUnitTest
```

Generated debug APK path:
`app/build/outputs/apk/debug/app-debug.apk`

### Mobile / On-Device Build Workflow (AndroidIDE / Termux)
1. Clone or extract the project into AndroidIDE workspace storage.
2. Open the root directory containing `settings.gradle.kts` and `gradle/libs.versions.toml`.
3. Ensure JDK 17+ is selected in AndroidIDE Preferences and `ANDROID_SDK_ROOT` points to your installed SDK platforms/build-tools.
4. Note: This project uses AGP 9.x + KSP (`2.3.5`) + AIDL (`buildFeatures.aidl = true`). Legacy tools such as AIDE (which lack AGP 8+/9+ Kotlin DSL & Compose compiler support) are not supported; use **AndroidIDE** or desktop Android Studio / Gradle CLI.
5. Run the `assembleDebug` task and install `app/build/outputs/apk/debug/app-debug.apk`.

---

## Backup & Restore Format

Backups are saved as versioned JSON (`schemaVersion = 1`) containing:
- `uiSettings` (theme preset, hex colors, corner radius, glass opacity, nav bar settings, text scale)
- `gameCustomizations` (custom aliases, favorite/pin/hidden states, assigned profiles, tags, notes, group names)
- `customProfiles` (user-created profiles)
- `whitelistedPackages`, `customGroups`, `diagnosticsProfile`

Before applying an imported backup, SYMPHINE displays a **Validate & Preview Dialog** showing app version, source device, counts, conflicts, and granular checkboxes.

---

## Troubleshooting

- **Games missing in Library:** Tap the **Rescan** button or tap **+ Add App** to include any launchable app manually.
- **Shizuku says "Permission Required" or "Service Not Running":** Open the Shizuku app first to confirm the daemon is running, then return to SYMPHINE (`ON_RESUME` auto-rebinds).
- **OEM capped refresh rate at 60Hz:** Some Xiaomi (HyperOS Joyose), OPPO/Realme (ColorOS), and Vivo ROMs maintain proprietary per-game refresh whitelists outside `Settings.System`. Check the **Settings -> Compatibility** tab for your ROM's exact behavior.
- **Resolution/DPI looks too small after testing Downscale:** Tap **Reset Screen Size & Density** in Display Center or tap **Atomic Restore All** in Backup / Shizuku Center.

---

## Privacy & License

- **Privacy Policy:** SYMPHINE GAME SPACE collects zero data, makes zero network connections, and stores all preferences, logs, and original-setting snapshots strictly inside private app storage (`Room` + `DataStore`).
- **Developer:** Reo Moser (`com.symphine.gamespace`)
