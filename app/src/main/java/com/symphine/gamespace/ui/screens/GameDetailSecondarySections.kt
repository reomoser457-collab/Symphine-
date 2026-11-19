package com.symphine.gamespace.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.symphine.gamespace.core.model.CustomProfileDefinition
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.GameCardStyle
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.SettingRegistryEntry
import com.symphine.gamespace.games.ActiveManualSession
import com.symphine.gamespace.shizuku.ShizukuStatusState
import com.symphine.gamespace.ui.components.KeyValueRow
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.LiveTelemetrySparkline
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.components.StatusBadge
import com.symphine.gamespace.ui.state.HardwareDashboardState
import com.symphine.gamespace.ui.state.TelemetrySample
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults
import com.symphine.gamespace.ui.theme.parseHexColorOrNull

@Composable
fun GameDetailSecondarySection(
    sectionIndex: Int,
    game: DetectedGame,
    accent: Color,
    hardware: HardwareDashboardState,
    shizuku: ShizukuStatusState,
    profiles: List<CustomProfileDefinition>,
    customGroups: Set<String>,
    whitelistedPackages: Set<String>,
    settingsRegistry: List<SettingRegistryEntry>,
    activeSession: ActiveManualSession?,
    isMonitoring: Boolean,
    telemetrySamples: List<TelemetrySample>,
    overlayRunning: Boolean,
    sectionSearch: String,
    onSectionSearchChange: (String) -> Unit,
    onUpdateCustomization: (String, (GameCustomization) -> GameCustomization) -> Unit,
    onResetCustomization: (String) -> Unit,
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
    val context = LocalContext.current

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                onUpdateCustomization(game.packageName) { it.copy(customBannerUri = uri.toString()) }
            }
        }
    )

    when (sectionIndex) {
        7 -> { // 8. Gaming Tools
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Gaming Tools & Quick Panel",
                    subtitle = "Supported Android overlay, audio, display & session utilities",
                    icon = Icons.Rounded.Build
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Floating Gaming Telemetry Overlay", style = MaterialTheme.typography.titleMedium)
                        Text("Shows session timer, RAM, battery, thermal & Hz over games.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                    }
                    Switch(
                        checked = overlayRunning,
                        onCheckedChange = { onToggleOverlay(it, game.customization.assignedProfileId) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-start overlay when launched via Symphine", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = game.customization.overlayEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization(game.packageName) { it.copy(overlayEnabled = enabled) }
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Media Volume") }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Brightness") }
                }
                Spacer(Modifier.height(8.dp))
                if (activeSession?.packageName == game.packageName) {
                    Button(
                        onClick = onStopManualSession,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SymphineDefaults.StatusEmerald, contentColor = Color(0xFF08090D))
                    ) { Text("Stop & Save Manual Session") }
                } else {
                    Button(
                        onClick = { onStartManualSession(game.packageName, game.displayTitle, game.customization.assignedProfileId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D))
                    ) { Text("Start Manual Session Timer") }
                }
            }
        }

        8 -> { // 9. Background
            val isWhitelisted = game.packageName in whitelistedPackages
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Background & Protection",
                    subtitle = "Safe whitelist & Android standby visibility (never kills foreground games)",
                    icon = Icons.Rounded.Layers
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Symphine Protected Whitelist", style = MaterialTheme.typography.titleMedium)
                        Text("Marks this package as protected in Background Diagnostics.", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                    }
                    Switch(checked = isWhitelisted, onCheckedChange = { onToggleWhitelist(game.packageName) })
                }
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Target SDK", "API ${game.targetSdkVersion}")
                KeyValueRow("Whitelist Status", if (isWhitelisted) "Protected" else "Standard")
            }
        }

        9 -> { // 10. Per-Game Customization Studio
            var customLabel by remember(game.customization.customLabel) { mutableStateOf(game.customization.customLabel ?: "") }
            var accentHex by remember(game.customization.customAccentHex) { mutableStateOf(game.customization.customAccentHex ?: "#A78BFA") }
            var notes by remember(game.customization.notes) { mutableStateOf(game.customization.notes) }
            var tagsInput by remember(game.customization.tags) { mutableStateOf(game.customization.tags.joinToString(", ")) }

            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Per-Game Customization Studio",
                    subtitle = "All cosmetic changes stay local and never touch game APK assets",
                    icon = Icons.Rounded.Palette
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("Custom Display Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = accentHex,
                    onValueChange = { accentHex = it },
                    label = { Text("Custom Accent Hex (e.g. #A78BFA)") },
                    isError = parseHexColorOrNull(accentHex) == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Game Notes (sensitivity, layout, graphics notes)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text("Card Style", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameCardStyle.entries.forEach { st ->
                        OutlinedButton(
                            onClick = { onUpdateCustomization(game.packageName) { it.copy(cardStyle = st) } },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, if (game.customization.cardStyle == st) accent else colors.border),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(st.label.substringBefore(" "), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Assign Group: ${game.customization.groupName}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    customGroups.take(4).forEach { grp ->
                        OutlinedButton(
                            onClick = { onUpdateCustomization(game.packageName) { it.copy(groupName = grp) } },
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                        ) { Text(grp, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pick Banner") }
                    Button(
                        onClick = {
                            val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val validHex = accentHex.takeIf { parseHexColorOrNull(it) != null }
                            onUpdateCustomization(game.packageName) {
                                it.copy(
                                    customLabel = customLabel.trim().ifBlank { null },
                                    customAccentHex = validHex,
                                    notes = notes,
                                    tags = parsedTags
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D))
                    ) { Text("Save Studio") }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onResetCustomization(game.packageName) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset Local Customization") }
            }
        }

        10 -> { // 11. Monitoring
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Per-Game Monitoring",
                    subtitle = "Lifecycle-safe telemetry sampling",
                    icon = Icons.Rounded.MonitorHeart
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleMonitoring,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D))
                    ) { Text(if (isMonitoring) "Stop Monitoring" else "Start Monitoring") }
                    OutlinedButton(onClick = onResetMonitoring, modifier = Modifier.weight(1f)) {
                        Text("Reset Graph")
                    }
                }
                Spacer(Modifier.height(10.dp))
                LiveTelemetrySparkline(
                    values = telemetrySamples.map { it.ramUsedPercent },
                    minValue = 0f,
                    maxValue = 100f,
                    lineColor = accent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        11 -> { // 12. Compatibility
            val compat = hardware.oemCompatibility
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Game & Device Compatibility",
                    subtitle = compat?.let { "${it.manufacturer} ${it.model} • API ${it.apiLevel}" } ?: "",
                    icon = Icons.Rounded.Shield
                )
                Spacer(Modifier.height(10.dp))
                compat?.features?.forEach { feat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(feat.name, style = MaterialTheme.typography.bodyMedium, color = colors.softSilver)
                            Text(feat.detail, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        }
                        StatusBadge(feat.status)
                    }
                }
            }
        }

        12 -> { // 13. Profile
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Per-Game Profile Engine",
                    subtitle = "Assigned: ${game.customization.assignedProfileId.uppercase()}",
                    icon = Icons.Rounded.Tune
                )
                Spacer(Modifier.height(10.dp))
                profiles.forEach { prof ->
                    val isAssigned = game.customization.assignedProfileId == prof.id
                    OutlinedButton(
                        onClick = {
                            onUpdateCustomization(game.packageName) { it.copy(assignedProfileId = prof.id) }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        border = BorderStroke(1.dp, if (isAssigned) accent else colors.border)
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isAssigned) "✓ ${prof.name} (Active)" else prof.name,
                                color = if (isAssigned) accent else colors.softSilver
                            )
                            Text(prof.description, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onCreateProfile(
                            "${game.displayTitle} Preset",
                            "Cloned from ${game.displayTitle}",
                            GameProfileType.CUSTOM_TEMPLATE,
                            game.customization.customAccentHex ?: "#A78BFA",
                            game.customization.preferredGameMode,
                            game.customization.preferredRefreshRateHz,
                            game.customization.preferredAnimationScale,
                            game.customization.angleGraphicsMode,
                            game.customization.angleLodOffset
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D))
                ) { Text("Clone Current Settings as Reusable Template") }
            }
        }

        13 -> { // 14. Advanced Settings
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Advanced Settings Registry",
                    subtitle = "Searchable system & game configuration records",
                    icon = Icons.Rounded.Settings
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sectionSearch,
                    onValueChange = onSectionSearchChange,
                    label = { Text("Filter settings...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                settingsRegistry.filter {
                    sectionSearch.isBlank() ||
                        it.name.contains(sectionSearch, ignoreCase = true) ||
                        it.key.contains(sectionSearch, ignoreCase = true)
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
                        Text(entry.description, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOpenAndroidAppInfo(game.packageName) }, modifier = Modifier.weight(1f)) {
                        Text("Android App Info")
                    }
                    OutlinedButton(onClick = { onRemoveFromLibrary(game.packageName) }, modifier = Modifier.weight(1f)) {
                        Text("Remove Game", color = SymphineDefaults.StatusRose)
                    }
                }
            }
        }

        14 -> { // 15. Backup
            LiquidGlassCard(accentColor = accent) {
                SectionHeader(
                    title = "Game Configuration Backup",
                    subtitle = "Restore atomic system snapshots or share game profile summary",
                    icon = Icons.Rounded.Backup
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onAtomicRestoreAll,
                    enabled = shizuku.isOperational,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF08090D))
                ) {
                    Text(if (shizuku.isOperational) "Atomic Restore Original System Settings" else "Atomic Restore (Requires Shizuku)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val summary = buildString {
                            appendLine("SYMPHINE GAME CONFIG: ${game.displayTitle} (${game.packageName})")
                            appendLine("Profile: ${game.customization.assignedProfileId}")
                            appendLine("ANGLE: ${game.customization.angleGraphicsMode} (LOD ${game.customization.angleLodOffset})")
                            appendLine("Preferred Hz: ${game.customization.preferredRefreshRateHz ?: "Default"}")
                            appendLine("Preferred DPI: ${game.customization.preferredDpi ?: "Default"}")
                            appendLine("Notes: ${game.customization.notes}")
                        }
                        onShareGameConfig(summary)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share Per-Game Config Summary") }
            }
        }
    }
}
