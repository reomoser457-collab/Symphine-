package com.symphine.gamespace.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.OperationStatus
import com.symphine.gamespace.core.model.UiSettingsState
import com.symphine.gamespace.games.ActiveManualSession
import com.symphine.gamespace.shizuku.ShizukuStatusState
import com.symphine.gamespace.ui.components.InstalledAppIcon
import com.symphine.gamespace.ui.components.KeyValueRow
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.components.StatusBadge
import com.symphine.gamespace.ui.components.TelemetryMeterBar
import com.symphine.gamespace.ui.state.HardwareDashboardState
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults

private data class QuickActionItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    hardware: HardwareDashboardState,
    games: List<DetectedGame>,
    activeProfileId: String,
    activeSession: ActiveManualSession?,
    shizuku: ShizukuStatusState,
    uiSettings: UiSettingsState,
    isMonitoring: Boolean,
    onRefreshAll: () -> Unit,
    onScanGames: () -> Unit,
    onOpenGameSpace: () -> Unit,
    onOpenGameDetail: (String) -> Unit,
    onOpenPerformance: (Int) -> Unit,
    onOpenMonitoring: (Int) -> Unit,
    onOpenSettings: (Int) -> Unit,
    onLaunchGame: (DetectedGame) -> Unit,
    onStopSession: () -> Unit
) {
    val colors = LocalSymphineColors.current

    val quickActions = listOf(
        QuickActionItem("scan", "Scan Games", Icons.Rounded.Search, onScanGames),
        QuickActionItem("library", "Game Library", Icons.Rounded.SportsEsports, onOpenGameSpace),
        QuickActionItem("display", "Display Center", Icons.Rounded.DisplaySettings) { onOpenPerformance(1) },
        QuickActionItem("perf", "Performance", Icons.Rounded.Speed) { onOpenPerformance(0) },
        QuickActionItem("monitor", "Monitoring", Icons.Rounded.MonitorHeart) { onOpenMonitoring(0) },
        QuickActionItem("shizuku", "Shizuku", Icons.Rounded.Security) { onOpenPerformance(3) },
        QuickActionItem("diag", "Diagnostics", Icons.Rounded.Tune) { onOpenMonitoring(3) },
        QuickActionItem("backup", "Backup", Icons.Rounded.Backup) { onOpenSettings(1) }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(colors.cornerRadius))
                    .border(1.dp, colors.purpleAccent.copy(alpha = 0.35f), RoundedCornerShape(colors.cornerRadius))
            ) {
                if (!uiSettings.customBannerUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(uiSettings.customBannerUri),
                        contentDescription = "Custom Symphine Hero Banner",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = uiSettings.bannerZoom,
                                scaleY = uiSettings.bannerZoom,
                                translationX = uiSettings.bannerOffsetX,
                                translationY = uiSettings.bannerOffsetY,
                                rotationZ = uiSettings.bannerRotationDeg
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_symphine_hero_1795128764931),
                        contentDescription = "Symphine Liquid Glass Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x8808090D),
                                    Color(0xCC08090D),
                                    Color(0xF208090D)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SYMPHINE GAME SPACE",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.softSilver,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Native Gaming & Diagnostics • v1.0.0 by Reo Moser",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.lavenderGray
                            )
                        }
                        IconButton(
                            onClick = onRefreshAll,
                            modifier = Modifier.testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh telemetry and games",
                                tint = colors.purpleAccent
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.purpleAccent.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.purpleAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PROFILE: ${activeProfileId.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        StatusBadge(
                            status = when {
                                shizuku.isOperational -> OperationStatus.VERIFIED
                                shizuku.isBinderAlive -> OperationStatus.REQUIRES_PERMISSION
                                else -> OperationStatus.REQUIRES_SHIZUKU
                            }
                        )
                    }
                }
            }
        }

        // Active Session Banner (if user launched/started a game session)
        if (activeSession != null) {
            item {
                LiquidGlassCard(accentColor = SymphineDefaults.StatusEmerald) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ACTIVE SESSION TRACKING",
                                style = MaterialTheme.typography.labelSmall,
                                color = SymphineDefaults.StatusEmerald
                            )
                            Text(
                                activeSession.gameName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.softSilver
                            )
                            Text(
                                "Profile: ${activeSession.profileId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
                        Button(
                            onClick = onStopSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SymphineDefaults.StatusEmerald,
                                contentColor = Color(0xFF08090D)
                            )
                        ) {
                            Text("Save Session")
                        }
                    }
                }
            }
        }

        // Quick Actions Horizontal Grid (2 rows of 4 or horizontal carousel)
        item {
            SectionHeader(
                title = "Quick Actions",
                subtitle = "Verified navigation & system tools",
                icon = Icons.Rounded.Bolt
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickActions.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { action ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surfaceElevated.copy(alpha = 0.8f))
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .clickable(onClick = action.onClick)
                                    .testTag("quick_action_${action.id}")
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.label,
                                        tint = colors.purpleAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.softSilver,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Game Library Spotlight
        item {
            LiquidGlassCard {
                SectionHeader(
                    title = "Game Library (${games.size})",
                    subtitle = "Tap any game to open its 15-section Detail Center",
                    icon = Icons.Rounded.SportsEsports,
                    trailing = {
                        Text(
                            text = "Open All",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.purpleAccent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onOpenGameSpace)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (games.isEmpty()) {
                    Text(
                        "No games detected yet. Tap Scan Games or add any installed app inside Game Space.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(games.take(8), key = { it.packageName }) { game ->
                            Column(
                                modifier = Modifier
                                    .width(126.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.background.copy(alpha = 0.65f))
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .clickable { onOpenGameDetail(game.packageName) }
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                InstalledAppIcon(
                                    packageName = game.packageName,
                                    contentDescription = game.displayTitle,
                                    size = 52.dp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = game.displayTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.softSilver,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = game.customization.assignedProfileId.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.purpleAccent,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = colors.purpleAccent.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLaunchGame(game) }
                                ) {
                                    Text(
                                        text = "Launch",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.secondaryAccent,
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (hardware.isLoading) {
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = colors.purpleAccent)
                        Spacer(Modifier.width(12.dp))
                        Text("Reading genuine Android hardware telemetry...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            // Device Overview Card
            hardware.device?.let { dev ->
                item {
                    LiquidGlassCard {
                        SectionHeader(
                            title = "${dev.manufacturer} ${dev.model}",
                            subtitle = "Android ${dev.androidVersion} (API ${dev.apiLevel}) • ${dev.oemBrand.displayName}",
                            icon = Icons.Rounded.PhoneAndroid
                        )
                        Spacer(Modifier.height(10.dp))
                        KeyValueRow("SoC / Board", "${dev.socModel} (${dev.hardwareBoard})")
                        KeyValueRow("CPU Architecture", "${hardware.cpu?.architecture ?: dev.primaryAbi} • ${hardware.cpu?.coreCount ?: "?"} Cores")
                        KeyValueRow("GPU Renderer", hardware.graphics?.gpuRenderer ?: "Querying...")
                        KeyValueRow("Build ID", dev.buildId)
                    }
                }
            }

            // Battery & Thermal Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidGlassCard(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BatteryChargingFull, "Battery", tint = colors.purpleAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Battery", style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${hardware.battery?.percentage ?: "--"}%",
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.softSilver
                        )
                        Text(
                            text = hardware.battery?.let {
                                "${it.statusLabel} • ${it.temperatureCelsius?.let { t -> String.format("%.1f°C", t) } ?: "Temp N/A"}"
                            } ?: "Unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }

                    LiquidGlassCard(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Thermostat, "Thermal", tint = colors.secondaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thermal", style = MaterialTheme.typography.titleMedium, color = colors.softSilver)
                        }
                        Spacer(Modifier.height(8.dp))
                        val thermal = hardware.thermal
                        Text(
                            text = thermal?.statusLabel?.substringBefore(" ") ?: "Unknown",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (thermal?.isThrottling == true) SymphineDefaults.StatusAmber else SymphineDefaults.StatusEmerald,
                            maxLines = 1
                        )
                        Text(
                            text = if (isMonitoring) "Live monitoring active" else "Tap Monitoring for history",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                }
            }

            // Memory & Storage Card
            item {
                LiquidGlassCard {
                    SectionHeader(
                        title = "Memory & Storage",
                        subtitle = "Real Android ActivityManager & StatFs readings",
                        icon = Icons.Rounded.Memory
                    )
                    Spacer(Modifier.height(10.dp))
                    hardware.memory?.let { mem ->
                        val totalMb = (mem.totalRamBytes / (1024f * 1024f)).coerceAtLeast(1f)
                        val usedMb = (mem.usedRamBytes / (1024f * 1024f)).coerceAtLeast(0f)
                        val availGb = mem.availableRamBytes / (1024f * 1024f * 1024f)
                        val totalGb = mem.totalRamBytes / (1024f * 1024f * 1024f)
                        TelemetryMeterBar(
                            label = "RAM Usage",
                            valueText = "${String.format("%.2f", availGb)} GB free / ${String.format("%.1f", totalGb)} GB",
                            progress = usedMb / totalMb,
                            accent = colors.purpleAccent
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    hardware.storage?.let { st ->
                        val totalGb = (st.totalBytes / (1024f * 1024f * 1024f)).coerceAtLeast(0.1f)
                        val usedGb = (st.usedBytes / (1024f * 1024f * 1024f)).coerceAtLeast(0f)
                        val freeGb = st.availableBytes / (1024f * 1024f * 1024f)
                        TelemetryMeterBar(
                            label = "Internal Storage",
                            valueText = "${String.format("%.1f", freeGb)} GB free / ${String.format("%.1f", totalGb)} GB",
                            progress = usedGb / totalGb,
                            accent = colors.secondaryAccent
                        )
                    }
                }
            }

            // Display & Recommendations Summary
            hardware.display?.let { disp ->
                item {
                    LiquidGlassCard(onClick = { onOpenPerformance(1) }) {
                        SectionHeader(
                            title = "Display & Refresh Modes",
                            subtitle = "${disp.widthPx}×${disp.heightPx} • ${disp.densityDpi} DPI • ${disp.currentRefreshRateHz.toInt()} Hz active",
                            icon = Icons.Rounded.DisplaySettings
                        )
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow(
                            "Supported Modes",
                            disp.supportedRefreshRates.joinToString(" / ") { "${it.toInt()} Hz" }
                        )
                        KeyValueRow(
                            "HDR / Wide Color",
                            "HDR=${disp.isHdrSupported} • WCG=${disp.isWideColorGamut}"
                        )
                    }
                }
            }

            hardware.recommendations.firstOrNull()?.let { rec ->
                item {
                    LiquidGlassCard(onClick = { onOpenPerformance(0) }) {
                        SectionHeader(
                            title = "Offline Recommendation",
                            subtitle = rec.title,
                            icon = Icons.Rounded.AutoAwesome,
                            trailing = { StatusBadge(rec.compatibilityStatus) }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(rec.reason, style = MaterialTheme.typography.bodyMedium, color = colors.lavenderGray)
                    }
                }
            }
        }
    }
}
