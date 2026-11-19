# Changelog — SYMPHINE GAME SPACE

All notable changes to **SYMPHINE GAME SPACE** (`com.symphine.gamespace`, Developer: Reo Moser) are documented in this file.

## [1.0.0] — Initial Release

### Added
- **Core Platform & Identity**: Configured package `com.symphine.gamespace` (`versionCode = 1`, `versionName = "1.0.0"`), custom adaptive launcher icon on `#0D0E14` Liquid Glass canvas, and bundled **Space Grotesk**, **Plus Jakarta Sans**, and **JetBrains Mono** typography.
- **Symphine Liquid Glass AMOLED Lavender Theme**: Full Material 3 color scheme (`#08090D` OLED background, `#111319` glass surfaces, `#A78BFA` primary accent), live hex color editor, corner radius slider (`0–32 dp`), glass opacity & glow controls, reduced motion, custom Photo Picker Hero Banner, and 4 customizable navigation styles (`Pill`, `Capsule`, `Indicator`, `Material Bar`) plus adaptive tablet `NavigationRail`.
- **Game Library & 15-Section Detail Center**: Automated launcher & `CATEGORY_GAME` scanner, manual app whitelist/addition, Grid/List/Compact layouts, bulk multi-select operations, session timer & history, custom aliases/banners/tags/notes, and full 15-section Game Detail workspace.
- **ReoSherly Graphics V5.2 Native & Shizuku Integration**:
  - AIDL `ISymphineShizukuService` (Shizuku 13.1.5) running strictly allowlisted `ProcessBuilder` commands (no arbitrary shell injection).
  - Pre-execution verification modal showing exact namespaces, keys, risk levels, and rollback instructions.
  - Pre-change `OriginalBackupSnapshotEntity` capture & post-write read-back verification (`Verified`, `Applied`, `Failed`, `Restored`, `Requires Shizuku`, `Unsupported`).
  - Integrated ANGLE Graphics Profile (`smooth`/`pixel` + Texture LOD 1–4), ART Bytecode Compiler (`speed-profile`/`speed`/`everything`), Android Game Mode API (API 31+), Refresh Rate lock from real `Display.Mode` list, Animation Scale presets, Display DPI & Proportional Downscale (`70%–100%`), Background/Power flags, and one-tap **Atomic Rollback**.
- **Genuine Hardware Diagnostics & Live Monitoring**: Real-time `ActivityManager` RAM, `StatFs` storage, `BatteryManager` battery/temperature, `PowerManager` thermal status, `DisplayManager` modes, `GLES20`/`EGL10` GPU/ANGLE inspection, `/proc/stat` CPU estimation, live sparkline telemetry chart, CSV export, and shareable text diagnostics report.
- **Backup, Permissions & OEM Compatibility Centers**: JSON backup export/validation/selective import via Storage Access Framework (`SAF`), least-privilege permission manager with recovery instructions, and honest ROM compatibility matrix.
