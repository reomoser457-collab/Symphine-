package com.symphine.gamespace.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.symphine.gamespace.core.model.CustomProfileDefinition
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.OperationRecord
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.games.ScannedAppInfo
import com.symphine.gamespace.shizuku.ShizukuStatusState
import com.symphine.gamespace.ui.components.InstalledAppIcon
import com.symphine.gamespace.ui.components.KeyValueRow
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.components.StatusBadge
import com.symphine.gamespace.ui.state.HardwareDashboardState
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults

private val perfTabs = listOf(
    "Profiles",
    "Display",
    "Graphics & ANGLE",
    "Shizuku",
    "System Tuning",
    "Trace & Background"
)

@Composable
fun PerformanceScreen(
    initialTab: Int,
    hardware: HardwareDashboardState,
    games: List<DetectedGame>,
    allApps: List<ScannedAppInfo>,
    profiles: List<CustomProfileDefinition>,
    activeProfileId: String,
    shizuku: ShizukuStatusState,
    snapshotCount: Int,
    operationHistory: List<OperationRecord>,
    whitelistedPackages: Set<String>,
    diagnosticsProfile: String,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: (String, String, GameProfileType, String, String?, Float?, Float?, String, Int) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onRequestRefreshRate: (Float?) -> Unit,
    onRequestAnimationScale: (Float) -> Unit,
    onRequestBackgroundFlags: () -> Unit,
    onRequestDpi: (Int?) -> Unit,
    onRequestDownscale: (Int?) -> Unit,
    onRequestAngleOp: (String, String, String, Int, Boolean) -> Unit,
    onRequestArtCompile: (String, String, String) -> Unit,
    onRequestGameMode: (String, String, String) -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onReconnectShizuku: () -> Unit,
    onRefreshShizuku: () -> Unit,
    onAtomicRestoreAll: () -> Unit,
    onToggleWhitelist: (String) -> Unit,
    onSetDiagnosticsProfile: (String) -> Unit
) {
    val colors = LocalSymphineColors.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, perfTabs.lastIndex)) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var selectedPkg by remember(games) { mutableStateOf(games.firstOrNull()?.packageName ?: "com.dts.freefireth") }
    var selectedName by remember(games) { mutableStateOf(games.firstOrNull()?.displayTitle ?: "Free Fire") }
    var selectedLod by remember { mutableIntStateOf(2) }
    var registryQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 12.dp)
    ) {
        Text(
            text = "Performance & System Center",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.softSilver,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Verified Android controls, ReoSherly V5.2 utilities & Shizuku",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(perfTabs) { idx, title ->
                val selected = selectedTab == idx
                Surface(
                    shape = CircleShape,
                    color = if (selected) colors.purpleAccent.copy(alpha = 0.22f) else colors.surfaceElevated,
                    border = BorderStroke(1.dp, if (selected) colors.purpleAccent else colors.border),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { selectedTab = idx }
                        .testTag("perf_tab_${title.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) colors.purpleAccent else colors.lavenderGray,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> { // Profiles & Recommendation Engine
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Offline Recommendation Engine",
                                subtitle = "Tailored from real RAM, Thermal, Display & Shizuku state",
                                icon = Icons.Rounded.AutoAwesome
                            )
                            Spacer(Modifier.height(10.dp))
                            hardware.recommendations.forEach { rec ->
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                    color = colors.background.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(rec.title, style = MaterialTheme.typography.titleMedium, color = colors.softSilver, modifier = Modifier.weight(1f))
                                            StatusBadge(rec.compatibilityStatus)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(rec.reason, style = MaterialTheme.typography.bodySmall, color = colors.lavenderGray)
                                        if (rec.supportedChanges.isNotEmpty()) {
                                            Spacer(Modifier.height(6.dp))
                                            rec.supportedChanges.forEach { ch ->
                                                Text("• $ch", style = MaterialTheme.typography.bodySmall, color = SymphineDefaults.StatusEmerald)
                                            }
                                        }
                                        if (rec.warnings.isNotEmpty()) {
                                            rec.warnings.forEach { w ->
                                                Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = SymphineDefaults.StatusAmber)
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { onSelectProfile(rec.profileType.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                        ) {
                                            Text("Activate ${rec.profileType.displayName}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Profile Engine (${profiles.size})",
                                subtitle = "Active: ${activeProfileId.uppercase()}",
                                icon = Icons.Rounded.Speed,
                                trailing = {
                                    OutlinedButton(onClick = { showCreateProfileDialog = true }) {
                                        Text("+ New")
                                    }
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                            profiles.forEach { prof ->
                                val active = activeProfileId == prof.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                        .background(if (active) colors.purpleAccent.copy(alpha = 0.14f) else colors.background.copy(alpha = 0.55f))
                                        .border(1.dp, if (active) colors.purpleAccent else colors.border, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                        .clickable { onSelectProfile(prof.id) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = if (active) "✓ ${prof.name} (Active)" else prof.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (active) colors.purpleAccent else colors.softSilver
                                        )
                                        Text(prof.description, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                    }
                                    if (prof.isUserCreated) {
                                        IconButton(onClick = { onDeleteProfile(prof.id) }) {
                                            Icon(Icons.Rounded.Delete, "Delete profile", tint = SymphineDefaults.StatusRose)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> { // Display Center
                    val disp = hardware.display
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Display & Refresh Rate Center",
                                subtitle = "Options generated strictly from hardware Display.Mode",
                                icon = Icons.Rounded.DisplaySettings
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("Physical Resolution", disp?.let { "${it.widthPx} × ${it.heightPx}" } ?: "Unknown")
                            KeyValueRow("Aspect Ratio", disp?.aspectRatio ?: "Unknown")
                            KeyValueRow("Current Density", disp?.let { "${it.densityDpi} DPI (${it.densityScale}x)" } ?: "Unknown")
                            KeyValueRow("Current Refresh Rate", disp?.let { "${it.currentRefreshRateHz} Hz" } ?: "Unknown")
                            KeyValueRow("Peak / Min Setting", "peak=${disp?.peakRefreshSetting ?: "default"} | min=${disp?.minRefreshSetting ?: "default"}")

                            Spacer(Modifier.height(10.dp))
                            Text("Verified Device Refresh Modes", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(disp?.supportedRefreshRates ?: listOf(60f)) { hz ->
                                    Button(
                                        onClick = { onRequestRefreshRate(hz) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent.copy(alpha = 0.22f), contentColor = colors.purpleAccent)
                                    ) { Text("${hz.toInt()} Hz") }
                                }
                                item {
                                    OutlinedButton(onClick = { onRequestRefreshRate(null) }) {
                                        Text("Reset ROM Default")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        var dpiField by remember(disp?.densityDpi) { mutableStateOf((disp?.densityDpi ?: 430).toString()) }
                        LiquidGlassCard {
                            SectionHeader(
                                title = "DPI & Proportional Screen Downscale",
                                subtitle = "ReoSherly V5.2 wm density & proportional wm size with atomic backup",
                                icon = Icons.Rounded.Tune
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = dpiField,
                                    onValueChange = { dpiField = it.filter { c -> c.isDigit() } },
                                    label = { Text("Custom DPI (120–1000)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    val v = dpiField.toIntOrNull()
                                    if (v != null && v in 120..1000) onRequestDpi(v)
                                }) { Text("Preview DPI") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onRequestDpi(430) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = colors.purpleAccent)
                                ) { Text("Preset 430 DPI") }
                                OutlinedButton(
                                    onClick = { onRequestDpi(null) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reset DPI") }
                            }

                            Spacer(Modifier.height(14.dp))
                            Text("Proportional Screen Downscale (70% – 100%)", style = MaterialTheme.typography.titleMedium)
                            Text("Scales both width×height and DPI proportionally to keep UI crisp.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(75, 80, 85, 90, 95).forEach { pct ->
                                    OutlinedButton(
                                        onClick = { onRequestDownscale(pct) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) { Text("$pct%", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRequestDownscale(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Reset Resolution & Density to Physical") }
                        }
                    }
                }

                2 -> { // Graphics & ANGLE + ART + GameMode + Animation
                    val gfx = hardware.graphics

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "GPU & ANGLE Backend Diagnostics",
                                subtitle = "Real EGL / OpenGL ES / Vulkan / ANGLE inspection",
                                icon = Icons.Rounded.Brush
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("GPU Renderer", gfx?.gpuRenderer ?: "Loading...")
                            KeyValueRow("Vendor", gfx?.gpuVendor ?: "Unknown")
                            KeyValueRow("OpenGL ES", gfx?.openGlEsVersion ?: "Unknown")
                            KeyValueRow("Vulkan", if (gfx?.vulkanSupported == true) gfx.vulkanVersion ?: "Supported" else "Unsupported")
                            KeyValueRow("ANGLE Package", gfx?.anglePackageName ?: "Unknown")
                            KeyValueRow("ANGLE Target Pkgs", gfx?.angleTargetPackages ?: "None")
                            KeyValueRow("ANGLE Driver Vals", gfx?.angleDriverValues ?: "None")
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "ReoSherly V5.2 Graphics, ART & Game Mode",
                                subtitle = "Select target game for ANGLE, ART Compiler, or Android Game Mode",
                                icon = Icons.Rounded.Speed
                            )
                            Spacer(Modifier.height(10.dp))
                            if (games.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(games, key = { it.packageName }) { g ->
                                        val isTarget = g.packageName == selectedPkg
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isTarget) colors.purpleAccent.copy(alpha = 0.24f) else colors.background,
                                            border = BorderStroke(1.dp, if (isTarget) colors.purpleAccent else colors.border),
                                            modifier = Modifier.clickable {
                                                selectedPkg = g.packageName
                                                selectedName = g.displayTitle
                                            }
                                        ) {
                                            Text(
                                                g.displayTitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isTarget) colors.purpleAccent else colors.softSilver,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                            Text("Target: $selectedName ($selectedPkg)", style = MaterialTheme.typography.labelMedium, color = colors.purpleAccent)
                            Spacer(Modifier.height(8.dp))
                            Text("LOD Offset: $selectedLod", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..4).forEach { lod ->
                                    OutlinedButton(
                                        onClick = { selectedLod = lod },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, if (selectedLod == lod) colors.purpleAccent else colors.border)
                                    ) { Text("LOD $lod") }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onRequestAngleOp(selectedPkg, selectedName, "smooth", selectedLod, false) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                ) { Text("Smooth ANGLE") }
                                Button(
                                    onClick = { onRequestAngleOp(selectedPkg, selectedName, "pixel", selectedLod, false) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = colors.purpleAccent)
                                ) { Text("Pixel ANGLE") }
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { onRequestAngleOp(selectedPkg, selectedName, "reset", selectedLod, false) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Reset ANGLE Driver") }

                            Spacer(Modifier.height(12.dp))
                            Text("ART Bytecode Compiler", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("speed-profile", "speed").forEach { mode ->
                                    Button(
                                        onClick = { onRequestArtCompile(selectedPkg, selectedName, mode) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated, contentColor = colors.softSilver)
                                    ) { Text("Compile ($mode)", style = MaterialTheme.typography.labelSmall) }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("Android Game Mode (API 31+) & Animation Scale", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("performance", "standard", "battery").forEach { mode ->
                                    OutlinedButton(
                                        onClick = { onRequestGameMode(selectedPkg, selectedName, mode) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) { Text(mode, style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0f, 0.25f, 0.5f, 1.0f).forEach { sc ->
                                    OutlinedButton(
                                        onClick = { onRequestAnimationScale(sc) },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) { Text("Anim ${sc}x", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                    }
                }

                3 -> { // Shizuku Integration Center
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Shizuku Integration Center",
                                subtitle = "Optional privileged shell bridge (UID 2000 / Root). Core features never require Shizuku.",
                                icon = Icons.Rounded.Security,
                                trailing = {
                                    StatusBadge(
                                        when {
                                            shizuku.isOperational -> OperationStatus.VERIFIED
                                            shizuku.isBinderAlive -> OperationStatus.REQUIRES_PERMISSION
                                            else -> OperationStatus.REQUIRES_SHIZUKU
                                        }
                                    )
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("Shizuku Installed", if (shizuku.isShizukuAppInstalled) "Yes" else "Not Installed")
                            KeyValueRow("Connection Phase", shizuku.phase.label)
                            KeyValueRow("Binder Alive", shizuku.isBinderAlive.toString())
                            KeyValueRow("Permission Granted", shizuku.isPermissionGranted.toString())
                            KeyValueRow("UserService UID", shizuku.serviceUid?.let { if (it == 0) "0 (Root)" else "$it (Shell)" } ?: "Not Bound")
                            KeyValueRow("Shizuku API Version", shizuku.shizukuVersion?.toString() ?: "N/A")
                            KeyValueRow("Recorded Atomic Backups", "$snapshotCount snapshot(s)")

                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onRequestShizukuPermission,
                                    modifier = Modifier.weight(1f).testTag("request_shizuku_permission_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                ) { Text("Request Access") }
                                OutlinedButton(
                                    onClick = onReconnectShizuku,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reconnect") }
                                OutlinedButton(onClick = onRefreshShizuku) { Text("Refresh") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onAtomicRestoreAll,
                                enabled = shizuku.isOperational,
                                modifier = Modifier.fillMaxWidth().testTag("atomic_restore_all_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SymphineDefaults.StatusEmerald, contentColor = Color(0xFF08090D))
                            ) {
                                Icon(Icons.Rounded.Restore, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Atomic Full Restore ($snapshotCount Snapshots)")
                            }

                            if (!shizuku.lastError.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Last Error: ${shizuku.lastError}", style = MaterialTheme.typography.bodySmall, color = SymphineDefaults.StatusRose)
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Shizuku Setup Guide (Wireless Debugging)",
                                subtitle = "Shizuku provides ADB Shell (UID 2000) privileges — it is NOT root access."
                            )
                            Spacer(Modifier.height(8.dp))
                            val steps = listOf(
                                "1. Install Shizuku from Google Play or GitHub (rikka.app).",
                                "2. Enable Developer Options > Wireless Debugging on Android 11+ (or connect via PC ADB on Android 10).",
                                "3. Open Shizuku > Pairing > Pair device with pairing code, then tap Start.",
                                "4. Confirm Shizuku displays 'Shizuku is running'.",
                                "5. Return to SYMPHINE GAME SPACE > Performance > Shizuku Center.",
                                "6. Tap 'Request Access' and select 'Allow all the time'.",
                                "7. Wait for UserService UID 2000 to show Connected.",
                                "8. All changes record atomic backups first and can be restored anytime."
                            )
                            steps.forEach { step ->
                                Text(step, style = MaterialTheme.typography.bodySmall, color = colors.lavenderGray, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }

                4 -> { // System Tuning & Registry + History
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "System Tuning & Background Flags",
                                subtitle = "Documented Settings.Global flags from ReoSherly V5.2",
                                icon = Icons.Rounded.Tune
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onRequestBackgroundFlags,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                            ) { Text("Preview & Apply Background / Power Flags") }
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = registryQuery,
                                onValueChange = { registryQuery = it },
                                label = { Text("Search Settings Registry...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            hardware.settingsRegistry.filter {
                                registryQuery.isBlank() ||
                                    it.name.contains(registryQuery, ignoreCase = true) ||
                                    it.key.contains(registryQuery, ignoreCase = true) ||
                                    it.category.contains(registryQuery, ignoreCase = true)
                            }.forEach { entry ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(entry.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                        StatusBadge(entry.supportedStatus)
                                    }
                                    Text("${entry.namespace}/${entry.key} • Current: ${entry.currentValue ?: "null"}", style = MaterialTheme.typography.labelSmall, color = colors.purpleAccent)
                                    Text("${entry.androidCompatibility} • ${entry.riskLevel.label}", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Operation Verification Log (${operationHistory.size})",
                                subtitle = "Real read-back verification outcomes",
                                icon = Icons.Rounded.History
                            )
                            Spacer(Modifier.height(8.dp))
                            if (operationHistory.isEmpty()) {
                                Text("No system operations executed yet.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            } else {
                                operationHistory.take(15).forEach { op ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(op.operationName, style = MaterialTheme.typography.bodyMedium)
                                            Text(op.message, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                        }
                                        StatusBadge(op.status)
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> { // Trace & Background Diagnostics
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Modular Trace & Logging Diagnostics",
                                subtitle = "Active Profile: $diagnosticsProfile (Safe sampling, no telemetry tampering)",
                                icon = Icons.Rounded.Tune
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Default", "Lite", "Custom").forEach { prof ->
                                    OutlinedButton(
                                        onClick = { onSetDiagnosticsProfile(prof) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, if (diagnosticsProfile == prof) colors.purpleAccent else colors.border)
                                    ) { Text(prof) }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Validates frame timing, memory pressure, battery state, and operation verification without experimental kernel overrides.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Background App Inventory & Whitelist",
                                subtitle = "User-controlled protection list (Symphine never force-stops system or foreground apps)",
                                icon = Icons.Rounded.Layers
                            )
                            Spacer(Modifier.height(8.dp))
                            allApps.filter { !it.isSystemApp }.take(25).forEach { app ->
                                val whitelisted = app.packageName in whitelistedPackages
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InstalledAppIcon(app.packageName, app.appName, size = 34.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(app.appName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                        Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = colors.mutedText, maxLines = 1)
                                    }
                                    Switch(
                                        checked = whitelisted,
                                        onCheckedChange = { onToggleWhitelist(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateProfileDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Create Custom Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile Name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onCreateProfile(name, desc, GameProfileType.CUSTOM, "#A78BFA", "performance", null, 0.5f, "none", 2)
                    }
                    showCreateProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) { Text("Cancel") }
            }
        )
    }
}
