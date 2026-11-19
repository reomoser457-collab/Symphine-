package com.symphine.gamespace.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import com.symphine.gamespace.core.model.GameSessionEntry
import com.symphine.gamespace.core.model.UiSettingsState
import com.symphine.gamespace.ui.components.KeyValueRow
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.LiveTelemetrySparkline
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.components.TelemetryMeterBar
import com.symphine.gamespace.ui.state.HardwareDashboardState
import com.symphine.gamespace.ui.state.TelemetrySample
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val monitoringTabs = listOf(
    "Live Telemetry",
    "CPU / GPU / RAM",
    "Storage / Battery / Thermal",
    "Sessions & Report"
)

@Composable
fun MonitoringScreen(
    initialTab: Int,
    hardware: HardwareDashboardState,
    uiSettings: UiSettingsState,
    isMonitoring: Boolean,
    samples: List<TelemetrySample>,
    sessions: List<GameSessionEntry>,
    onToggleMonitoring: () -> Unit,
    onResetMonitoring: () -> Unit,
    onSetIntervalMs: (Long) -> Unit,
    onExportCsv: () -> String,
    onClearAppCache: () -> Unit,
    onClearSessions: () -> Unit,
    onGenerateReportText: () -> String,
    onShareText: (String) -> Unit,
    onNotifySnackbar: (String) -> Unit
) {
    val colors = LocalSymphineColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, monitoringTabs.lastIndex)) }
    var showCacheConfirm by remember { mutableStateOf(false) }
    var showClearSessionsConfirm by remember { mutableStateOf(false) }
    var reportPreview by remember { mutableStateOf<String?>(null) }

    val csvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(onExportCsv().toByteArray())
                        }
                    }.onSuccess { onNotifySnackbar("Monitoring CSV exported") }
                        .onFailure { onNotifySnackbar("Export failed: ${it.message}") }
                }
            }
        }
    )

    val reportExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(onGenerateReportText().toByteArray())
                        }
                    }.onSuccess { onNotifySnackbar("Diagnostics report saved") }
                        .onFailure { onNotifySnackbar("Save failed: ${it.message}") }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 12.dp)
    ) {
        Text(
            text = "Hardware & Monitoring Center",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.softSilver,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Real Android telemetry only — never fabricated",
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(monitoringTabs) { idx, title ->
                val selected = selectedTab == idx
                Surface(
                    shape = CircleShape,
                    color = if (selected) colors.purpleAccent.copy(alpha = 0.22f) else colors.surfaceElevated,
                    border = BorderStroke(1.dp, if (selected) colors.purpleAccent else colors.border),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { selectedTab = idx }
                        .testTag("monitoring_tab_$idx")
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
                0 -> { // Live Telemetry
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = if (isMonitoring) "Monitoring Active" else "Monitoring Idle",
                                subtitle = "Lifecycle-safe polling every ${uiSettings.monitoringIntervalMs / 1000}s • ${samples.size} sample(s)",
                                icon = Icons.Rounded.MonitorHeart
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onToggleMonitoring,
                                    modifier = Modifier.weight(1f).testTag("toggle_monitoring_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMonitoring) SymphineDefaults.StatusRose else colors.purpleAccent,
                                        contentColor = Color(0xFF08090D)
                                    )
                                ) {
                                    Icon(if (isMonitoring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isMonitoring) "Stop" else "Start")
                                }
                                OutlinedButton(onClick = onResetMonitoring) { Text("Reset") }
                                OutlinedButton(onClick = { csvExporter.launch("symphine_telemetry.csv") }) { Text("CSV") }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Polling Interval", style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1000L to "1s", 2000L to "2s", 5000L to "5s").forEach { (ms, lbl) ->
                                    OutlinedButton(
                                        onClick = { onSetIntervalMs(ms) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, if (uiSettings.monitoringIntervalMs == ms) colors.purpleAccent else colors.border)
                                    ) { Text(lbl) }
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            Text("RAM Utilization (%)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LiveTelemetrySparkline(
                                values = samples.map { it.ramUsedPercent },
                                minValue = 0f,
                                maxValue = 100f,
                                lineColor = colors.purpleAccent,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        val tempSamples = samples.mapNotNull { it.batteryTempC }
                        LiquidGlassCard {
                            Text("Battery Temperature (°C)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LiveTelemetrySparkline(
                                values = tempSamples,
                                minValue = 20f,
                                maxValue = 55f,
                                lineColor = SymphineDefaults.StatusAmber,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                1 -> { // CPU / GPU / RAM Centers
                    item {
                        val cpu = hardware.cpu
                        LiquidGlassCard {
                            SectionHeader(
                                title = "CPU Center",
                                subtitle = cpu?.let { "${it.modelName} • ${it.coreCount} Cores (${it.architecture})" } ?: "Reading...",
                                icon = Icons.Rounded.Memory
                            )
                            Spacer(Modifier.height(10.dp))
                            if (cpu != null) {
                                KeyValueRow("CPU Utilization", cpu.utilizationPercent?.let { String.format("%.1f%%", it) } ?: cpu.utilizationNote)
                                cpu.coreFrequenciesKhz.forEachIndexed { idx, freq ->
                                    val maxF = cpu.coreMaxFrequenciesKhz.getOrNull(idx)
                                    val gov = cpu.governors.getOrNull(idx) ?: "Restricted"
                                    val freqStr = freq?.let { "${it / 1000} MHz" } ?: "SELinux Restricted"
                                    val maxStr = maxF?.let { "/ ${it / 1000} MHz" } ?: ""
                                    KeyValueRow("Core $idx ($gov)", "$freqStr $maxStr")
                                }
                            }
                        }
                    }

                    item {
                        val gfx = hardware.graphics
                        LiquidGlassCard {
                            SectionHeader(
                                title = "GPU Center",
                                subtitle = gfx?.gpuRenderer ?: "Loading...",
                                icon = Icons.Rounded.MonitorHeart
                            )
                            Spacer(Modifier.height(10.dp))
                            if (gfx != null) {
                                KeyValueRow("GPU Vendor", gfx.gpuVendor)
                                KeyValueRow("OpenGL ES", gfx.openGlEsVersion)
                                KeyValueRow("Vulkan", if (gfx.vulkanSupported) gfx.vulkanVersion ?: "Supported" else "Unsupported")
                                KeyValueRow("GPU Utilization %", "Unsupported on standard Android")
                                Text(gfx.gpuUtilizationNote, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                            }
                        }
                    }

                    item {
                        val mem = hardware.memory
                        LiquidGlassCard {
                            SectionHeader(
                                title = "RAM Center",
                                subtitle = "ActivityManager.MemoryInfo & /proc/meminfo",
                                icon = Icons.Rounded.Memory
                            )
                            Spacer(Modifier.height(10.dp))
                            if (mem != null) {
                                val totalMb = mem.totalRamBytes / (1024L * 1024L)
                                val availMb = mem.availableRamBytes / (1024L * 1024L)
                                val usedMb = mem.usedRamBytes / (1024L * 1024L)
                                TelemetryMeterBar(
                                    label = "Memory Usage",
                                    valueText = "$usedMb MB used / $totalMb MB total",
                                    progress = usedMb.toFloat() / totalMb.coerceAtLeast(1L).toFloat()
                                )
                                Spacer(Modifier.height(8.dp))
                                KeyValueRow("Available RAM", "$availMb MB")
                                KeyValueRow("Low Memory State", mem.isLowMemory.toString())
                                KeyValueRow("Swap Total", mem.swapTotalBytes?.let { "${it / (1024 * 1024)} MB" } ?: "Restricted")
                            }
                        }
                    }
                }

                2 -> { // Storage / Battery / Thermal Centers
                    item {
                        val st = hardware.storage
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Storage Center",
                                subtitle = "Internal /data partition & Symphine app-only cache",
                                icon = Icons.Rounded.Storage
                            )
                            Spacer(Modifier.height(10.dp))
                            if (st != null) {
                                val totalGb = st.totalBytes / (1024f * 1024f * 1024f)
                                val usedGb = st.usedBytes / (1024f * 1024f * 1024f)
                                val freeGb = st.availableBytes / (1024f * 1024f * 1024f)
                                TelemetryMeterBar(
                                    label = "Internal Storage Used",
                                    valueText = "${String.format("%.1f", freeGb)} GB free / ${String.format("%.1f", totalGb)} GB",
                                    progress = usedGb / totalGb.coerceAtLeast(0.1f)
                                )
                                Spacer(Modifier.height(8.dp))
                                KeyValueRow("Symphine Cache", "${st.appCacheBytes / 1024} KB")
                                KeyValueRow("Symphine Data", "${st.appDataBytes / 1024} KB")
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showCacheConfirm = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.CleaningServices, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Clean Symphine App Cache Only")
                                }
                            }
                        }
                    }

                    item {
                        val bat = hardware.battery
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Battery Center",
                                subtitle = "Real BatteryManager telemetry",
                                icon = Icons.Rounded.BatteryChargingFull
                            )
                            Spacer(Modifier.height(10.dp))
                            if (bat != null) {
                                KeyValueRow("Level", "${bat.percentage}%")
                                KeyValueRow("Charging Status", "${bat.statusLabel} (${bat.pluggedSource})")
                                KeyValueRow("Temperature", bat.temperatureCelsius?.let { String.format("%.1f °C", it) } ?: "Unavailable")
                                KeyValueRow("Voltage", bat.voltageMillivolts?.let { "$it mV" } ?: "Unavailable")
                                KeyValueRow("Current", bat.currentMicroamps?.let { "${it / 1000} mA" } ?: "Unavailable")
                            }
                        }
                    }

                    item {
                        val th = hardware.thermal
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Thermal Center",
                                subtitle = th?.sensorAccessNote ?: "",
                                icon = Icons.Rounded.Thermostat
                            )
                            Spacer(Modifier.height(10.dp))
                            if (th != null) {
                                KeyValueRow(
                                    "Thermal Status",
                                    th.statusLabel,
                                    if (th.isThrottling) SymphineDefaults.StatusAmber else SymphineDefaults.StatusEmerald
                                )
                                th.readableZones.forEach { z ->
                                    KeyValueRow(z.name, String.format("%.1f °C", z.temperatureCelsius))
                                }
                            }
                        }
                    }
                }

                3 -> { // Sessions & Performance Report
                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Offline Diagnostics Report",
                                subtitle = "Sanitized hardware & compatibility summary (no personal PII)",
                                icon = Icons.Rounded.Description
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { reportPreview = onGenerateReportText() },
                                    modifier = Modifier.weight(1f).testTag("preview_report_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                                ) { Text("Preview") }
                                OutlinedButton(
                                    onClick = { reportExporter.launch("symphine_diagnostics_report.txt") },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Export .txt") }
                                OutlinedButton(
                                    onClick = { onShareText(onGenerateReportText()) }
                                ) { Icon(Icons.Rounded.Share, "Share report") }
                            }
                            if (reportPreview != null) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    color = colors.background,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(
                                        text = reportPreview!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.softSilver,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LiquidGlassCard {
                            SectionHeader(
                                title = "Game Session History (${sessions.size})",
                                subtitle = "Locally tracked game launches & manual sessions",
                                icon = Icons.Rounded.History,
                                trailing = {
                                    if (sessions.isNotEmpty()) {
                                        TextButton(onClick = { showClearSessionsConfirm = true }) {
                                            Text("Clear", color = SymphineDefaults.StatusRose)
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            if (sessions.isEmpty()) {
                                Text(
                                    "No sessions recorded yet. Launch a game from Game Space or start a manual session timer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.mutedText
                                )
                            } else {
                                val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }
                                sessions.take(20).forEach { s ->
                                    val mins = (s.durationMillis / 60000L)
                                    val secs = ((s.durationMillis % 60000L) / 1000L)
                                    KeyValueRow(
                                        "${s.gameName} (${fmt.format(Date(s.startTimeMillis))})",
                                        "${mins}m ${secs}s • ${s.profileId}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showCacheConfirm = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Clean Symphine App Cache?") },
            text = { Text("This only deletes temporary cache files inside Symphine Space. Your profiles, games, and personal files are untouched.") },
            confirmButton = {
                Button(onClick = {
                    onClearAppCache()
                    showCacheConfirm = false
                }) { Text("Clean Cache") }
            },
            dismissButton = {
                TextButton(onClick = { showCacheConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearSessionsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearSessionsConfirm = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Clear Session History?") },
            text = { Text("All locally saved game session timestamps will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearSessions()
                        showClearSessionsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SymphineDefaults.StatusRose)
                ) { Text("Clear History") }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionsConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
