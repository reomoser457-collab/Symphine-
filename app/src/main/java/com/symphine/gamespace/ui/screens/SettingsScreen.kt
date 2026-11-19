package com.symphine.gamespace.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.symphine.gamespace.core.model.NavAlignment
import com.symphine.gamespace.core.model.NavBarStyle
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.TextScalePreset
import com.symphine.gamespace.core.model.ThemePreset
import com.symphine.gamespace.core.model.UiSettingsState
import com.symphine.gamespace.core.permissions.PermissionManager
import com.symphine.gamespace.ui.components.KeyValueRow
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.components.StatusBadge
import com.symphine.gamespace.ui.state.HardwareDashboardState
import com.symphine.gamespace.ui.theme.JetBrainsMonoFamily
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults
import com.symphine.gamespace.ui.theme.parseHexColorOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    uiSettings: UiSettingsState,
    hardware: HardwareDashboardState,
    snapshotCount: Int,
    onUpdateUiSettings: ((UiSettingsState) -> UiSettingsState) -> Unit,
    onResetUiSettings: () -> Unit,
    onWipeAllData: () -> Unit,
    onGenerateBackupJson: suspend () -> String,
    onValidateBackupImport: (String) -> Unit,
    onExecuteAtomicRestore: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onRequestShizuku: () -> Unit,
    onOpenShizukuTab: () -> Unit,
    onEmitSnackbar: (String) -> Unit
) {
    val colors = LocalSymphineColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionManager = remember(context) { PermissionManager(context) }

    val tabs = listOf("Appearance", "Backup", "Permissions", "Compatibility", "About")
    val safeTab = selectedTab.coerceIn(0, tabs.lastIndex)

    var showWipeConfirm by remember { mutableStateOf(false) }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedUri = runCatching {
                    val dest = File(context.filesDir, "symphine_hero_banner.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    Uri.fromFile(dest).toString() + "?t=${System.currentTimeMillis()}"
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    if (savedUri != null) {
                        onUpdateUiSettings { it.copy(customBannerUri = savedUri) }
                        onEmitSnackbar("Custom Hero Banner updated")
                    } else {
                        onEmitSnackbar("Unable to load selected image")
                    }
                }
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val jsonStr = onGenerateBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output stream")
                }
                withContext(Dispatchers.Main) {
                    result.onSuccess {
                        onEmitSnackbar("Backup exported successfully")
                    }.onFailure {
                        onEmitSnackbar("Backup export failed: ${it.message}")
                    }
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val rawResult = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: error("Unable to read selected file")
                }
                withContext(Dispatchers.Main) {
                    rawResult.onSuccess { raw ->
                        onValidateBackupImport(raw)
                    }.onFailure {
                        onEmitSnackbar("Could not open backup file: ${it.message}")
                    }
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        onEmitSnackbar(
            if (granted) "Notification permission granted"
            else "Notification permission denied — in-app feedback remains active"
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Wipe All Symphine Data?", color = SymphineDefaults.StatusRose) },
            text = {
                Text(
                    "This removes all local Symphine game customizations, custom profiles, session history, operation logs, and appearance settings. Device system settings are NOT touched.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.softSilver
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm = false
                        File(context.filesDir, "symphine_hero_banner.jpg").delete()
                        onWipeAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SymphineDefaults.StatusRose,
                        contentColor = Color(0xFF08090D)
                    )
                ) {
                    Text("Wipe Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel", color = colors.lavenderGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = safeTab,
            containerColor = colors.surface,
            contentColor = colors.purpleAccent,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = safeTab == idx,
                    onClick = { onSelectTab(idx) },
                    modifier = Modifier.testTag("settings_tab_$idx"),
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (safeTab == idx) colors.purpleAccent else colors.mutedText
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (safeTab) {
                0 -> {
                    // APPEARANCE & UI CUSTOMIZATION CENTER
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Theme & Color Palette",
                                subtitle = "Customize Liquid Glass AMOLED Lavender & hex accents",
                                icon = Icons.Rounded.Palette
                            )
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemePreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = uiSettings.themePreset == preset,
                                        onClick = { onUpdateUiSettings { it.copy(themePreset = preset) } },
                                        label = { Text(preset.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.purpleAccent.copy(alpha = 0.22f),
                                            selectedLabelColor = colors.secondaryAccent
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Custom Hex Colors (auto-activates Custom Hex Palette)",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.lavenderGray
                            )
                            Spacer(Modifier.height(8.dp))
                            HexColorInputRow(
                                label = "Primary Accent",
                                value = uiSettings.primaryHex,
                                onApplyHex = { hex ->
                                    onUpdateUiSettings {
                                        it.copy(
                                            themePreset = ThemePreset.CUSTOM,
                                            primaryHex = hex,
                                            accentHex = hex
                                        )
                                    }
                                }
                            )
                            HexColorInputRow(
                                label = "Secondary Accent",
                                value = uiSettings.secondaryHex,
                                onApplyHex = { hex ->
                                    onUpdateUiSettings {
                                        it.copy(themePreset = ThemePreset.CUSTOM, secondaryHex = hex)
                                    }
                                }
                            )
                            HexColorInputRow(
                                label = "Background",
                                value = uiSettings.backgroundHex,
                                onApplyHex = { hex ->
                                    onUpdateUiSettings {
                                        it.copy(themePreset = ThemePreset.CUSTOM, backgroundHex = hex)
                                    }
                                }
                            )
                            HexColorInputRow(
                                label = "Card Surface",
                                value = uiSettings.surfaceHex,
                                onApplyHex = { hex ->
                                    onUpdateUiSettings {
                                        it.copy(themePreset = ThemePreset.CUSTOM, surfaceHex = hex)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Liquid Glass & Geometry",
                                subtitle = "Corner radius, translucency, glow, and typography scale",
                                icon = Icons.Rounded.ColorLens
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Corner Radius: ${uiSettings.cornerRadiusDp} dp",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.softSilver
                            )
                            Slider(
                                value = uiSettings.cornerRadiusDp.toFloat(),
                                onValueChange = { v -> onUpdateUiSettings { it.copy(cornerRadiusDp = v.toInt().coerceIn(0, 32)) } },
                                valueRange = 0f..32f,
                                colors = SliderDefaults.colors(thumbColor = colors.purpleAccent, activeTrackColor = colors.purpleAccent)
                            )

                            Text(
                                "Glass Surface Opacity: ${(uiSettings.glassSurfaceOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.softSilver
                            )
                            Slider(
                                value = uiSettings.glassSurfaceOpacity,
                                onValueChange = { v -> onUpdateUiSettings { it.copy(glassSurfaceOpacity = v.coerceIn(0.45f, 0.98f)) } },
                                valueRange = 0.45f..0.98f,
                                colors = SliderDefaults.colors(thumbColor = colors.purpleAccent, activeTrackColor = colors.purpleAccent)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Glass Accent Border Glow", style = MaterialTheme.typography.bodyMedium, color = colors.softSilver)
                                    Text("Subtle tinted border highlight on cards", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                }
                                Switch(
                                    checked = uiSettings.glassGlowEnabled,
                                    onCheckedChange = { checked -> onUpdateUiSettings { it.copy(glassGlowEnabled = checked) } },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF08090D), checkedTrackColor = colors.purpleAccent)
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Reduced Motion", style = MaterialTheme.typography.bodyMedium, color = colors.softSilver)
                                    Text("Minimizes UI transition animations", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                                }
                                Switch(
                                    checked = uiSettings.reducedMotion,
                                    onCheckedChange = { checked -> onUpdateUiSettings { it.copy(reducedMotion = checked) } },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF08090D), checkedTrackColor = colors.purpleAccent)
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("Typography Scale", style = MaterialTheme.typography.labelMedium, color = colors.lavenderGray)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextScalePreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = uiSettings.textScale == preset,
                                        onClick = { onUpdateUiSettings { it.copy(textScale = preset) } },
                                        label = { Text(preset.label) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Navigation Bar Customization",
                                subtitle = "Style, alignment, height & monitoring interval",
                                icon = Icons.Rounded.DevicesOther
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("Bottom Navigation Style", style = MaterialTheme.typography.labelMedium, color = colors.lavenderGray)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NavBarStyle.entries.forEach { style ->
                                    FilterChip(
                                        selected = uiSettings.navBarStyle == style,
                                        onClick = { onUpdateUiSettings { it.copy(navBarStyle = style) } },
                                        label = { Text(style.label) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("Alignment", style = MaterialTheme.typography.labelMedium, color = colors.lavenderGray)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NavAlignment.entries.forEach { align ->
                                    FilterChip(
                                        selected = uiSettings.navAlignment == align,
                                        onClick = { onUpdateUiSettings { it.copy(navAlignment = align) } },
                                        label = { Text(align.label) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text("Navigation Bar Height: ${uiSettings.navHeightDp} dp", style = MaterialTheme.typography.bodySmall, color = colors.softSilver)
                            Slider(
                                value = uiSettings.navHeightDp.toFloat(),
                                onValueChange = { v -> onUpdateUiSettings { it.copy(navHeightDp = v.toInt().coerceIn(58, 86)) } },
                                valueRange = 58f..86f,
                                colors = SliderDefaults.colors(thumbColor = colors.purpleAccent, activeTrackColor = colors.purpleAccent)
                            )

                            Spacer(Modifier.height(6.dp))
                            Text("Live Monitoring Polling Interval", style = MaterialTheme.typography.labelMedium, color = colors.lavenderGray)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1000L to "1s", 2000L to "2s", 3000L to "3s", 5000L to "5s").forEach { (ms, label) ->
                                    FilterChip(
                                        selected = uiSettings.monitoringIntervalMs == ms,
                                        onClick = { onUpdateUiSettings { it.copy(monitoringIntervalMs = ms) } },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Hero Banner Customization",
                                subtitle = "Zero-permission Photo Picker with zoom, offset & rotation",
                                icon = Icons.Rounded.Image
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        bannerPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("pick_hero_banner_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                ) {
                                    Text("Choose Photo")
                                }
                                OutlinedButton(
                                    onClick = {
                                        File(context.filesDir, "symphine_hero_banner.jpg").delete()
                                        onUpdateUiSettings {
                                            it.copy(
                                                customBannerUri = null,
                                                bannerZoom = 1f,
                                                bannerOffsetX = 0f,
                                                bannerOffsetY = 0f,
                                                bannerRotationDeg = 0f
                                            )
                                        }
                                        onEmitSnackbar("Hero banner reset to Symphine default")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset Banner")
                                }
                            }

                            if (!uiSettings.customBannerUri.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text("Zoom: ${String.format("%.2fx", uiSettings.bannerZoom)}", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = uiSettings.bannerZoom,
                                    onValueChange = { v -> onUpdateUiSettings { it.copy(bannerZoom = v) } },
                                    valueRange = 0.8f..2.2f
                                )
                                Text("Rotation: ${uiSettings.bannerRotationDeg.toInt()}°", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = uiSettings.bannerRotationDeg,
                                    onValueChange = { v -> onUpdateUiSettings { it.copy(bannerRotationDeg = v) } },
                                    valueRange = -30f..30f
                                )
                            }
                        }
                    }

                    item {
                        LiquidGlassCard(accentColor = SymphineDefaults.StatusRose) {
                            SectionHeader(
                                title = "Reset & Data Controls",
                                subtitle = "Restore default Liquid Glass UI or clear local Symphine records",
                                icon = Icons.Rounded.RestartAlt
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onResetUiSettings,
                                    modifier = Modifier.weight(1f).testTag("reset_ui_settings_button")
                                ) {
                                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reset UI")
                                }
                                OutlinedButton(
                                    onClick = { showWipeConfirm = true },
                                    modifier = Modifier.weight(1f).testTag("wipe_all_data_button"),
                                    border = BorderStroke(1.dp, SymphineDefaults.StatusRose.copy(alpha = 0.55f))
                                ) {
                                    Icon(Icons.Rounded.DeleteForever, null, tint = SymphineDefaults.StatusRose, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Wipe Data", color = SymphineDefaults.StatusRose)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // BACKUP & RESTORE
                    item {
                        LiquidGlassCard(accentColor = colors.purpleAccent) {
                            SectionHeader(
                                title = "Backup & Restore Center",
                                subtitle = "Export & validate JSON backups with Storage Access Framework",
                                icon = Icons.Rounded.Backup
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Backups include UI appearance, per-game customizations, custom tags/notes, user profiles, whitelists, and diagnostics settings. No private device secrets or third-party data are included.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.lavenderGray
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                                        exportBackupLauncher.launch("symphine_backup_$stamp.json")
                                    },
                                    modifier = Modifier.weight(1f).testTag("export_backup_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                ) {
                                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export JSON")
                                }
                                OutlinedButton(
                                    onClick = {
                                        importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    },
                                    modifier = Modifier.weight(1f).testTag("import_backup_button")
                                ) {
                                    Icon(Icons.Rounded.UploadFile, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import & Validate")
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard(accentColor = SymphineDefaults.StatusAmber) {
                            SectionHeader(
                                title = "Atomic System Snapshot Restore",
                                subtitle = "$snapshotCount captured original system setting snapshot(s)",
                                icon = Icons.Rounded.Restore
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Before SYMPHINE writes any Shizuku setting (ANGLE driver, Refresh Rate, Animation Scale, Background flags, DPI, Downscale), the exact pre-change value is stored locally so you can revert safely.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.lavenderGray
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = onExecuteAtomicRestore,
                                modifier = Modifier.fillMaxWidth().testTag("atomic_restore_settings_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SymphineDefaults.StatusAmber,
                                    contentColor = Color(0xFF08090D)
                                )
                            ) {
                                Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore All Original System Settings", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                2 -> {
                    // PERMISSIONS CENTER
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Permissions & Least-Privilege Policy",
                                subtitle = "SYMPHINE only requests permissions needed for features you choose",
                                icon = Icons.Rounded.Security,
                                trailing = {
                                    TextButton(onClick = onRefreshPermissions) {
                                        Text("Recheck", color = colors.purpleAccent)
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No INTERNET permission, no broad storage permissions, and zero background telemetry tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SymphineDefaults.StatusEmerald
                            )
                        }
                    }

                    items(hardware.permissions, key = { it.id }) { perm ->
                        LiquidGlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    perm.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.softSilver,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                StatusBadge(perm.status)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Purpose: ${perm.reason}", style = MaterialTheme.typography.bodySmall, color = colors.lavenderGray)
                            Spacer(Modifier.height(4.dp))
                            Text("Recovery: ${perm.recoveryInstructions}", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Text("Fallback: ${perm.fallbackInfo}", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)

                            if (!perm.isGranted) {
                                Spacer(Modifier.height(10.dp))
                                when (perm.id) {
                                    "overlay" -> {
                                        OutlinedButton(
                                            onClick = { runCatching { context.startActivity(permissionManager.createOverlayPermissionIntent()) } },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Open Overlay Permission Settings")
                                        }
                                    }
                                    "notifications" -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                Button(
                                                    onClick = {
                                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                                ) {
                                                    Text("Request Permission")
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { runCatching { context.startActivity(permissionManager.createAppNotificationSettingsIntent()) } },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("App Settings")
                                            }
                                        }
                                    }
                                    "dnd" -> {
                                        OutlinedButton(
                                            onClick = { runCatching { context.startActivity(permissionManager.createDndAccessIntent()) } },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Open Do Not Disturb Access")
                                        }
                                    }
                                    "usage_stats" -> {
                                        OutlinedButton(
                                            onClick = { runCatching { context.startActivity(permissionManager.createUsageAccessIntent()) } },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Open Usage Access Settings")
                                        }
                                    }
                                    "shizuku" -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = onRequestShizuku,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                            ) {
                                                Text("Authorize Shizuku")
                                            }
                                            OutlinedButton(
                                                onClick = onOpenShizukuTab,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Setup Guide")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // OEM COMPATIBILITY MATRIX
                    val compat = hardware.oemCompatibility
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "OEM Compatibility Center",
                                subtitle = if (compat != null) "${compat.manufacturer} ${compat.model} • ${compat.oemBrand.displayName} • API ${compat.apiLevel}" else "Analyzing device...",
                                icon = Icons.Rounded.VerifiedUser
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Every feature is classified honestly against your device ROM, Android API level, and Shizuku authorization state.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                    }

                    if (compat != null) {
                        items(compat.features, key = { it.featureId }) { feat ->
                            LiquidGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(feat.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.purpleAccent)
                                        Text(feat.name, style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    StatusBadge(feat.status)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(feat.detail, style = MaterialTheme.typography.bodySmall, color = colors.lavenderGray)
                            }
                        }
                    }
                }

                4 -> {
                    // ABOUT & CHANGELOG
                    item {
                        LiquidGlassCard(accentColor = colors.purpleAccent) {
                            SectionHeader(
                                title = "SYMPHINE GAME SPACE",
                                subtitle = "Version 1.0.0 • Developer: Reo Moser",
                                icon = Icons.Rounded.Info
                            )
                            Spacer(Modifier.height(10.dp))
                            KeyValueRow("Package ID", "com.symphine.gamespace")
                            KeyValueRow("Developer", "Reo Moser")
                            KeyValueRow("Minimum Android", "Android 10 (API 29)")
                            KeyValueRow("Target Android", "Android 15/16+ (API 35/36)")
                            KeyValueRow("Network Access", "Offline-First (Zero INTERNET permission)")
                            KeyValueRow("Design System", "Symphine Liquid Glass AMOLED Lavender")
                        }
                    }

                    item {
                        LiquidGlassCard {
                            Text("Offline & Privacy Guarantee", style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "SYMPHINE GAME SPACE operates 100% locally on your device. No analytics, no ads, no cloud telemetry, and no unverified shell hacks. Hardware metrics are queried strictly through official Android system services and /proc/stat & /proc/cpuinfo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.lavenderGray
                            )
                        }
                    }

                    item {
                        LiquidGlassCard {
                            Text("Compatible vs Unsupported Features", style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "• Verified without Shizuku: Installed Game Scanner, 15-section Game Library customization, RAM/Storage/Battery/Thermal/Display/GPU inspection, Live Telemetry Charts & CSV Export, Floating Overlay (with overlay permission), JSON Backup/Restore.\n" +
                                "• Requires Shizuku (ADB Wireless Debugging): ANGLE GL driver binding, ART package compilation, Android Game Mode API (Android 12+), system Refresh Rate write, Animation Scale write, Background restriction flags, DPI & Proportional Resolution Downscale with atomic rollback.\n" +
                                "• Not possible without Root / Vendor HAL: Live GPU load %, raw SoC die temperature on locked kernels, debug.angle.* system properties on UID 2000.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                    }

                    item {
                        LiquidGlassCard {
                            Text("Open-Source Libraries", style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                            Spacer(Modifier.height(6.dp))
                            listOf(
                                "Jetpack Compose & Material 3 (Apache 2.0)",
                                "AndroidX Navigation Compose, Room & DataStore (Apache 2.0)",
                                "Kotlinx Coroutines & Kotlinx Serialization (Apache 2.0)",
                                "Coil Compose Image Loading (Apache 2.0)",
                                "Shizuku API & Provider 13.1.5 by RikkaApps (Apache 2.0)"
                            ).forEach { lib ->
                                Text("• $lib", style = MaterialTheme.typography.bodySmall, color = colors.lavenderGray)
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            Text("Changelog — v1.0.0", style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "• Initial release of SYMPHINE GAME SPACE by Reo Moser\n" +
                                "• Symphine Liquid Glass AMOLED Lavender UI + full hex & nav customization\n" +
                                "• Native Kotlin + AIDL Shizuku UserService integrating ANGLE, ART dexopt, Game Mode, Refresh Rate, Animation Scale, DPI, Proportional Downscale & Atomic Rollback\n" +
                                "• Offline Recommendation Engine, 15-section Game Detail Center & Draggable Telemetry Overlay",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.lavenderGray
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Android App Info")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HexColorInputRow(
    label: String,
    value: String,
    onApplyHex: (String) -> Unit
) {
    val colors = LocalSymphineColors.current
    var draft by remember(value) { mutableStateOf(value) }
    val parsedColor = parseHexColorOrNull(draft)
    val isValid = parsedColor != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(parsedColor ?: colors.border)
                .border(1.dp, colors.softSilver.copy(alpha = 0.4f), CircleShape)
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { next ->
                if (next.length <= 9) {
                    draft = next
                    if (parseHexColorOrNull(next) != null) {
                        val normalized = if (next.startsWith("#")) next.uppercase() else "#${next.uppercase()}"
                        onApplyHex(normalized)
                    }
                }
            },
            label = { Text(label) },
            isError = !isValid,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
            modifier = Modifier.weight(1f)
        )
    }
}
