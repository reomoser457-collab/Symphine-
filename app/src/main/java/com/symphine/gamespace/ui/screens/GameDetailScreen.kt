package com.symphine.gamespace.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.symphine.gamespace.core.model.CustomProfileDefinition
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.SettingRegistryEntry
import com.symphine.gamespace.games.ActiveManualSession
import com.symphine.gamespace.shizuku.ShizukuStatusState
import com.symphine.gamespace.ui.components.InstalledAppIcon
import com.symphine.gamespace.ui.components.KeyValueRow
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.components.StatusBadge
import com.symphine.gamespace.ui.components.TelemetryMeterBar
import com.symphine.gamespace.ui.state.HardwareDashboardState
import com.symphine.gamespace.ui.state.TelemetrySample
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults
import com.symphine.gamespace.ui.theme.parseHexColorOrNull

private val gameDetailSections = listOf(
    "Overview",
    "Performance",
    "Display",
    "Graphics",
    "Memory",
    "Battery",
    "Thermal",
    "Gaming Tools",
    "Background",
    "Customization",
    "Monitoring",
    "Compatibility",
    "Profile",
    "Advanced Settings",
    "Backup"
)

@Composable
fun GameDetailScreen(
    game: DetectedGame?,
    hardware: HardwareDashboardState,
    shizuku: ShizukuStatusState,
    profiles: List<CustomProfileDefinition>,
    customGroups: Set<String>,
    whitelistedPackages: Set<String>,
    operationHistory: List<OperationRecord>,
    settingsRegistry: List<SettingRegistryEntry>,
    activeSession: ActiveManualSession?,
    isMonitoring: Boolean,
    telemetrySamples: List<TelemetrySample>,
    overlayRunning: Boolean,
    onBack: () -> Unit,
    onLaunchGame: (DetectedGame) -> Unit,
    onUpdateCustomization: (String, (GameCustomization) -> GameCustomization) -> Unit,
    onResetCustomization: (String) -> Unit,
    onRequestAngleOp: (String, String, String, Int, Boolean) -> Unit,
    onRequestArtCompile: (String, String, String) -> Unit,
    onRequestGameMode: (String, String, String) -> Unit,
    onRequestRefreshRate: (Float?) -> Unit,
    onRequestAnimationScale: (Float) -> Unit,
    onRequestDpi: (Int?) -> Unit,
    onRequestDownscale: (Int?) -> Unit,
    onToggleOverlay: (Boolean, String) -> Unit,
    onToggleMonitoring: () -> Unit,
    onResetMonitoring: () -> Unit,
    onToggleWhitelist: (String) -> Unit,
    onStartManualSession: (String, String, String) -> Unit,
    onStopManualSession: () -> Unit,
    onOpenAndroidAppInfo: (String) -> Unit,
    onRemoveFromLibrary: (String) -> Unit,
    onAtomicRestoreAll: () -> Unit,
    onCreateProfile: (String, String, GameProfileType, String, String?, Float?, Float?, String, Int) -> Unit,
    onShareGameConfig: (String) -> Unit
) {
    val colors = LocalSymphineColors.current
    if (game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard {
                Text("Game not found in library.", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back to Library") }
            }
        }
        return
    }

    val accent = game.customization.customAccentHex?.let { parseHexColorOrNull(it) } ?: colors.purpleAccent
    var selectedSection by remember { mutableIntStateOf(0) }
    var sectionSearch by remember { mutableStateOf("") }
    var customDpiInput by remember(game.customization.preferredDpi) {
        mutableStateOf((game.customization.preferredDpi ?: hardware.display?.densityDpi ?: 420).toString())
    }
    var downscalePercent by remember(game.customization.preferredDownscalePercent) {
        mutableIntStateOf(game.customization.preferredDownscalePercent ?: 85)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Header with Optional Custom Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 1.dp, color = accent.copy(alpha = 0.25f))
        ) {
            if (!game.customization.customBannerUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(game.customization.customBannerUri),
                    contentDescription = "${game.displayTitle} banner",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xBB08090D), Color(0xEE08090D))
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("game_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.softSilver)
                    }
                    InstalledAppIcon(game.packageName, game.displayTitle, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = game.displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.softSilver,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${game.packageName} • v${game.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = {
                        onUpdateCustomization(game.packageName) { it.copy(isFavorite = !it.isFavorite) }
                    }) {
                        Icon(
                            if (game.customization.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            "Favorite",
                            tint = if (game.customization.isFavorite) accent else colors.mutedText
                        )
                    }
                    IconButton(onClick = {
                        onUpdateCustomization(game.packageName) { it.copy(isPinned = !it.isPinned) }
                    }) {
                        Icon(
                            Icons.Rounded.PushPin,
                            "Pin",
                            tint = if (game.customization.isPinned) accent else colors.mutedText
                        )
                    }
                    Button(
                        onClick = { onLaunchGame(game) },
                        enabled = game.isLaunchable,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("game_detail_launch_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 15-Section Animated Tab Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    itemsIndexed(gameDetailSections) { idx, title ->
                        val selected = selectedSection == idx
                        Surface(
                            shape = CircleShape,
                            color = if (selected) accent.copy(alpha = 0.24f) else colors.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accent else colors.border
                            ),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedSection = idx }
                                .testTag("detail_tab_${title.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = "${idx + 1}. $title",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) accent else colors.lavenderGray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSection) {
                0 -> { // 1. Overview
                    item {
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Game Overview",
                                subtitle = "Classification: ${game.classification.label}",
                                icon = Icons.Rounded.Speed
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("Active Profile", game.customization.assignedProfileId.uppercase(), accent)
                            KeyValueRow("Group / Tags", "${game.customization.groupName} • ${game.customization.tags.joinToString(", ").ifBlank { "None" }}")
                            KeyValueRow("Display Mode", hardware.display?.let { "${it.widthPx}×${it.heightPx} @ ${it.currentRefreshRateHz.toInt()} Hz (${it.densityDpi} DPI)" } ?: "Loading...")
                            KeyValueRow("Thermal Status", hardware.thermal?.statusLabel ?: "Unknown")
                            KeyValueRow("Battery", hardware.battery?.let { "${it.percentage}% • ${it.temperatureCelsius?.let { t -> String.format("%.1f°C", t) } ?: "N/A"}" } ?: "Unknown")
                            KeyValueRow("Available RAM", hardware.memory?.let { "${it.availableRamBytes / (1024 * 1024)} MB" } ?: "Unknown")
                            KeyValueRow("ANGLE Driver", if (game.customization.angleGraphicsMode == "none") "Default" else "${game.customization.angleGraphicsMode.uppercase()} (LOD ${game.customization.angleLodOffset})")
                            KeyValueRow("Monitoring", if (isMonitoring) "Active" else "Idle")
                        }
                    }
                    item {
                        val pkgHistory = operationHistory.filter { it.targetPackage == game.packageName }
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Game Operation History (${pkgHistory.size})",
                                subtitle = "Verified operations & rollback records for this game"
                            )
                            Spacer(Modifier.height(8.dp))
                            if (pkgHistory.isEmpty()) {
                                Text("No system operations recorded for this game yet.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            } else {
                                pkgHistory.take(6).forEach { rec ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(rec.operationName, style = MaterialTheme.typography.bodyMedium, color = colors.softSilver)
                                            Text(rec.message, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                        }
                                        StatusBadge(rec.status)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // 2. Performance
                    item {
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Android Game Mode & ART Compiler",
                                subtitle = "ReoSherly V5.2 & Android 12+ GameManager actions (Verified via Shizuku)",
                                icon = Icons.Rounded.Speed
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("Android Game Mode (API 31+)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("performance", "standard", "battery").forEach { mode ->
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateCustomization(game.packageName) { it.copy(preferredGameMode = mode) }
                                            onRequestGameMode(game.packageName, game.displayTitle, mode)
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, if (game.customization.preferredGameMode == mode) accent else colors.border)
                                    ) {
                                        Text(mode.replaceFirstChar { it.uppercase() }, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("ART Bytecode Compiler (dexopt)", style = MaterialTheme.typography.titleMedium)
                            Text("Pre-compiles game bytecode on-device using cmd package compile.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("speed-profile", "speed", "everything").forEach { mode ->
                                    Button(
                                        onClick = { onRequestArtCompile(game.packageName, game.displayTitle, mode) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = accent),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text(mode, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("UI Animation Scale", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0f, 0.25f, 0.5f, 1.0f).forEach { scale ->
                                    OutlinedButton(
                                        onClick = {
                                            onUpdateCustomization(game.packageName) { it.copy(preferredAnimationScale = scale) }
                                            onRequestAnimationScale(scale)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Text("${scale}x", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> { // 3. Display
                    item {
                        val disp = hardware.display
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Per-Game Display Center",
                                subtitle = "Modes populated strictly from device DisplayManager",
                                icon = Icons.Rounded.DisplaySettings
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("Active Resolution", disp?.let { "${it.widthPx}×${it.heightPx} (${it.aspectRatio})" } ?: "Unknown")
                            KeyValueRow("Current Density", disp?.let { "${it.densityDpi} DPI (${it.densityScale}x)" } ?: "Unknown")
                            KeyValueRow("Current Refresh", disp?.let { "${it.currentRefreshRateHz} Hz" } ?: "Unknown")
                            Spacer(Modifier.height(10.dp))
                            Text("Supported Hardware Refresh Rates", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(disp?.supportedRefreshRates ?: listOf(60f)) { _, hz ->
                                    Button(
                                        onClick = {
                                            onUpdateCustomization(game.packageName) { it.copy(preferredRefreshRateHz = hz) }
                                            onRequestRefreshRate(hz)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f), contentColor = accent)
                                    ) {
                                        Text("${hz.toInt()} Hz")
                                    }
                                }
                                item {
                                    OutlinedButton(onClick = { onRequestRefreshRate(null) }) {
                                        Text("Reset Hz")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(accentColor = accent) {
                            Text("DPI Control & Proportional Downscale (Shizuku)", style = MaterialTheme.typography.titleMedium)
                            Text("Original values are backed up automatically prior to applying.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customDpiInput,
                                    onValueChange = { customDpiInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("DPI (120–1000)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val dpi = customDpiInput.toIntOrNull()
                                    if (dpi != null && dpi in 120..1000) {
                                        onUpdateCustomization(game.packageName) { it.copy(preferredDpi = dpi) }
                                        onRequestDpi(dpi)
                                    }
                                }) { Text("Apply DPI") }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(360, 400, 430, 480).forEach { preset ->
                                    OutlinedButton(
                                        onClick = {
                                            customDpiInput = preset.toString()
                                            onRequestDpi(preset)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) { Text("$preset", style = MaterialTheme.typography.labelSmall) }
                                }
                                OutlinedButton(onClick = { onRequestDpi(null) }) { Text("Reset", style = MaterialTheme.typography.labelSmall) }
                            }

                            Spacer(Modifier.height(14.dp))
                            Text("Proportional Screen Downscale ($downscalePercent%)", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(75, 80, 85, 90, 95).forEach { pct ->
                                    OutlinedButton(
                                        onClick = {
                                            downscalePercent = pct
                                            onUpdateCustomization(game.packageName) { it.copy(preferredDownscalePercent = pct) }
                                            onRequestDownscale(pct)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) { Text("$pct%", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { onRequestDownscale(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Restore Physical Resolution & Density") }
                        }
                    }
                }

                3 -> { // 4. Graphics & ANGLE
                    item {
                        val gfx = hardware.graphics
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Graphics & ANGLE Center",
                                subtitle = "Real GPU/Vulkan diagnostics & ReoSherly V5.2 ANGLE controls",
                                icon = Icons.Rounded.Brush
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("GPU Renderer", gfx?.gpuRenderer ?: "Loading...")
                            KeyValueRow("GPU Vendor", gfx?.gpuVendor ?: "Unknown")
                            KeyValueRow("OpenGL ES", gfx?.openGlEsVersion ?: "Unknown")
                            KeyValueRow("Vulkan", if (gfx?.vulkanSupported == true) gfx.vulkanVersion ?: "Supported" else "Unsupported")
                            KeyValueRow("ANGLE Driver", gfx?.anglePackageName ?: "Unknown")
                            KeyValueRow("Active ANGLE Pkgs", gfx?.angleTargetPackages ?: "None")
                            Spacer(Modifier.height(12.dp))

                            var lod by remember(game.customization.angleLodOffset) { mutableIntStateOf(game.customization.angleLodOffset) }
                            Text("Texture LOD Offset (1–4): $lod", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..4).forEach { l ->
                                    OutlinedButton(
                                        onClick = {
                                            lod = l
                                            onUpdateCustomization(game.packageName) { it.copy(angleLodOffset = l) }
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, if (lod == l) accent else colors.border)
                                    ) { Text("LOD $l") }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onUpdateCustomization(game.packageName) { it.copy(angleGraphicsMode = "smooth", angleLodOffset = lod) }
                                        onRequestAngleOp(game.packageName, game.displayTitle, "smooth", lod, false)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D))
                                ) { Text("Smooth ANGLE") }
                                Button(
                                    onClick = {
                                        onUpdateCustomization(game.packageName) { it.copy(angleGraphicsMode = "pixel", angleLodOffset = lod) }
                                        onRequestAngleOp(game.packageName, game.displayTitle, "pixel", lod, false)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = accent)
                                ) { Text("Pixel ANGLE") }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onUpdateCustomization(game.packageName) { it.copy(angleGraphicsMode = "none") }
                                    onRequestAngleOp(game.packageName, game.displayTitle, "reset", lod, false)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Reset ANGLE Driver Selection") }
                        }
                    }
                }

                4 -> { // 5. Memory
                    item {
                        val mem = hardware.memory
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Memory Diagnostics",
                                subtitle = "Real ActivityManager & /proc/meminfo telemetry (no fake RAM killing)",
                                icon = Icons.Rounded.Memory
                            )
                            Spacer(Modifier.height(10.dp))
                            if (mem != null) {
                                val totalMb = mem.totalRamBytes / (1024L * 1024L)
                                val availMb = mem.availableRamBytes / (1024L * 1024L)
                                val usedMb = mem.usedRamBytes / (1024L * 1024L)
                                TelemetryMeterBar(
                                    label = "System RAM Pressure",
                                    valueText = "$availMb MB free / $totalMb MB",
                                    progress = usedMb.toFloat() / totalMb.coerceAtLeast(1L).toFloat(),
                                    accent = accent
                                )
                                Spacer(Modifier.height(8.dp))
                                KeyValueRow("Used RAM", "$usedMb MB")
                                KeyValueRow("Low Memory Threshold", "${mem.thresholdBytes / (1024 * 1024)} MB")
                                KeyValueRow("Low Memory Flag", mem.isLowMemory.toString())
                                KeyValueRow("Swap Total / Free", mem.swapTotalBytes?.let { "${it / (1024 * 1024)} MB / ${(mem.swapFreeBytes ?: 0L) / (1024 * 1024)} MB" } ?: "Restricted")
                            }
                        }
                    }
                }

                5 -> { // 6. Battery
                    item {
                        val bat = hardware.battery
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Battery Diagnostics",
                                subtitle = "Live Android BatteryManager readings",
                                icon = Icons.Rounded.BatteryChargingFull
                            )
                            Spacer(Modifier.height(10.dp))
                            if (bat != null) {
                                KeyValueRow("Charge Level", "${bat.percentage}%")
                                KeyValueRow("Status / Source", "${bat.statusLabel} (${bat.pluggedSource})")
                                KeyValueRow("Battery Temperature", bat.temperatureCelsius?.let { String.format("%.1f °C", it) } ?: "Unavailable")
                                KeyValueRow("Voltage", bat.voltageMillivolts?.let { "$it mV" } ?: "Unavailable")
                                KeyValueRow("Current", bat.currentMicroamps?.let { "${it / 1000} mA" } ?: "Unavailable")
                                KeyValueRow("Technology", bat.technology ?: "Unknown")
                                KeyValueRow("System Health Flag", bat.healthLabel)
                            }
                        }
                    }
                }

                6 -> { // 7. Thermal
                    item {
                        val th = hardware.thermal
                        LiquidGlassCard(accentColor = accent) {
                            SectionHeader(
                                title = "Thermal Diagnostics",
                                subtitle = th?.sensorAccessNote ?: "",
                                icon = Icons.Rounded.Thermostat
                            )
                            Spacer(Modifier.height(10.dp))
                            if (th != null) {
                                KeyValueRow(
                                    "PowerManager Thermal",
                                    th.statusLabel,
                                    if (th.isThrottling) SymphineDefaults.StatusAmber else SymphineDefaults.StatusEmerald
                                )
                                KeyValueRow("Battery Sensor", th.batteryTempCelsius?.let { String.format("%.1f °C", it) } ?: "Unavailable")
                                if (th.readableZones.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("Readable Thermal Zones", style = MaterialTheme.typography.titleMedium)
                                    th.readableZones.forEach { z ->
                                        KeyValueRow(z.name, String.format("%.1f °C", z.temperatureCelsius))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Symphine never spoofs temperatures or disables Android thermal protection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        GameDetailSecondarySection(
                            sectionIndex = selectedSection,
                            game = game,
                            accent = accent,
                            hardware = hardware,
                            shizuku = shizuku,
                            profiles = profiles,
                            customGroups = customGroups,
                            whitelistedPackages = whitelistedPackages,
                            settingsRegistry = settingsRegistry,
                            activeSession = activeSession,
                            isMonitoring = isMonitoring,
                            telemetrySamples = telemetrySamples,
                            overlayRunning = overlayRunning,
                            sectionSearch = sectionSearch,
                            onSectionSearchChange = { sectionSearch = it },
                            onUpdateCustomization = onUpdateCustomization,
                            onResetCustomization = onResetCustomization,
                            onToggleOverlay = onToggleOverlay,
                            onToggleMonitoring = onToggleMonitoring,
                            onResetMonitoring = onResetMonitoring,
                            onToggleWhitelist = onToggleWhitelist,
                            onStartManualSession = onStartManualSession,
                            onStopManualSession = onStopManualSession,
                            onOpenAndroidAppInfo = onOpenAndroidAppInfo,
                            onRemoveFromLibrary = onRemoveFromLibrary,
                            onAtomicRestoreAll = onAtomicRestoreAll,
                            onCreateProfile = onCreateProfile,
                            onShareGameConfig = onShareGameConfig
                        )
                    }
                }
            }
        }
    }
}
