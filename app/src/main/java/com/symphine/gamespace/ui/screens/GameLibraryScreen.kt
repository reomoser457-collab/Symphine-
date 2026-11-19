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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.symphine.gamespace.core.model.DetectedGame
import com.symphine.gamespace.core.model.GameCustomization
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.LibraryFilter
import com.symphine.gamespace.core.model.LibrarySortOrder
import com.symphine.gamespace.core.model.LibraryViewMode
import com.symphine.gamespace.games.ScannedAppInfo
import com.symphine.gamespace.ui.components.InstalledAppIcon
import com.symphine.gamespace.ui.components.LiquidGlassCard
import com.symphine.gamespace.ui.components.SectionHeader
import com.symphine.gamespace.ui.state.LibraryUiFilterState
import com.symphine.gamespace.ui.theme.LocalSymphineColors
import com.symphine.gamespace.ui.theme.SymphineDefaults
import com.symphine.gamespace.ui.theme.parseHexColorOrNull

@Composable
fun GameLibraryScreen(
    games: List<DetectedGame>,
    allLaunchableApps: List<ScannedAppInfo>,
    filterState: LibraryUiFilterState,
    customGroups: Set<String>,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onFilterChange: ((LibraryUiFilterState) -> LibraryUiFilterState) -> Unit,
    onToggleBulkPackage: (String) -> Unit,
    onClearBulk: () -> Unit,
    onBulkApply: ((GameCustomization) -> GameCustomization, String) -> Unit,
    onAddAppToLibrary: (String) -> Unit,
    onAddCustomGroup: (String) -> Unit,
    onUpdateGameCustomization: (String, (GameCustomization) -> GameCustomization) -> Unit,
    onLaunchGame: (DetectedGame) -> Unit,
    onOpenGameDetail: (String) -> Unit
) {
    val colors = LocalSymphineColors.current
    var showSortMenu by remember { mutableStateOf(false) }
    var showAddAppDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var showBulkGroupDialog by remember { mutableStateOf(false) }
    var showBulkProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Game Space",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.softSilver
                )
                Text(
                    text = "${games.size} game(s) • Tap card for 15-section Detail Center",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
            Row {
                IconButton(
                    onClick = { showAddAppDialog = true },
                    modifier = Modifier.testTag("add_app_to_library_button")
                ) {
                    Icon(Icons.Rounded.Add, "Add app to library", tint = colors.purpleAccent)
                }
                IconButton(
                    onClick = {
                        onFilterChange {
                            it.copy(bulkSelectionMode = !it.bulkSelectionMode, selectedPackages = emptySet())
                        }
                    },
                    modifier = Modifier.testTag("bulk_select_toggle_button")
                ) {
                    Icon(
                        Icons.Rounded.SelectAll,
                        "Bulk selection mode",
                        tint = if (filterState.bulkSelectionMode) colors.purpleAccent else colors.lavenderGray
                    )
                }
                IconButton(
                    onClick = onRescan,
                    modifier = Modifier.testTag("rescan_games_button")
                ) {
                    Icon(Icons.Rounded.Refresh, "Rescan installed games", tint = colors.lavenderGray)
                }
            }
        }

        if (isScanning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = colors.purpleAccent
            )
        }

        // Search + View Mode + Sort Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = { q -> onFilterChange { it.copy(searchQuery = q) } },
                modifier = Modifier
                    .weight(1f)
                    .testTag("game_search_input"),
                placeholder = { Text("Search name, package, tag...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onFilterChange { it.copy(searchQuery = "") } }) {
                            Icon(Icons.Rounded.Close, "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Cycle view mode
            IconButton(
                onClick = {
                    val next = LibraryViewMode.entries[(filterState.viewMode.ordinal + 1) % LibraryViewMode.entries.size]
                    onFilterChange { it.copy(viewMode = next) }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .testTag("cycle_view_mode_button")
            ) {
                Icon(
                    imageVector = when (filterState.viewMode) {
                        LibraryViewMode.GRID -> Icons.Rounded.GridView
                        LibraryViewMode.LIST -> Icons.Rounded.ViewList
                        LibraryViewMode.COMPACT -> Icons.Rounded.ViewAgenda
                        LibraryViewMode.LARGE_CARDS -> Icons.Rounded.SportsEsports
                    },
                    contentDescription = "View mode: ${filterState.viewMode.label}",
                    tint = colors.purpleAccent
                )
            }

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .testTag("sort_menu_button")
                ) {
                    Icon(Icons.Rounded.Sort, "Sort games", tint = colors.lavenderGray)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    LibrarySortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                onFilterChange { it.copy(sortOrder = order) }
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Filter Chips + Groups
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryFilter.entries.filter { it != LibraryFilter.CUSTOM_GROUP }) { f ->
                FilterChip(
                    selected = filterState.filter == f,
                    onClick = { onFilterChange { it.copy(filter = f) } },
                    label = { Text(f.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.purpleAccent.copy(alpha = 0.25f),
                        selectedLabelColor = colors.secondaryAccent
                    )
                )
            }
            items(customGroups.toList().sorted()) { grp ->
                FilterChip(
                    selected = filterState.filter == LibraryFilter.CUSTOM_GROUP && filterState.selectedGroup == grp,
                    onClick = {
                        onFilterChange { it.copy(filter = LibraryFilter.CUSTOM_GROUP, selectedGroup = grp) }
                    },
                    label = { Text("Group: $grp") }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { showNewGroupDialog = true },
                    label = { Text("+ Group") }
                )
            }
        }

        // Bulk Action Bar
        if (filterState.bulkSelectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(1.dp, colors.purpleAccent.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${filterState.selectedPackages.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.purpleAccent
                        )
                        TextButton(onClick = onClearBulk) {
                            Text("Exit Bulk")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            OutlinedButton(onClick = { onBulkApply({ it.copy(isFavorite = true) }, "Favorite") }) {
                                Text("Favorite")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { onBulkApply({ it.copy(isPinned = true) }, "Pin") }) {
                                Text("Pin")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { onBulkApply({ it.copy(isHidden = !it.isHidden) }, "Toggle Hide") }) {
                                Text("Hide/Unhide")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showBulkGroupDialog = true }) {
                                Text("Assign Group")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { showBulkProfileDialog = true }) {
                                Text("Assign Profile")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Main Game Collection
        if (games.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard {
                    SectionHeader(
                        title = "No Games Match Filter",
                        subtitle = "Try clearing search, changing filter, or tapping + to add any installed app.",
                        icon = Icons.Rounded.SportsEsports
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showAddAppDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purpleAccent, contentColor = Color(0xFF08090D))
                        ) {
                            Text("Add Installed App")
                        }
                        OutlinedButton(onClick = onRescan) {
                            Text("Rescan")
                        }
                    }
                }
            }
        } else {
            when (filterState.viewMode) {
                LibraryViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 108.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(games, key = { it.packageName }) { game ->
                            GameGridCard(
                                game = game,
                                bulkMode = filterState.bulkSelectionMode,
                                isSelected = game.packageName in filterState.selectedPackages,
                                onToggleSelect = { onToggleBulkPackage(game.packageName) },
                                onToggleFavorite = {
                                    onUpdateGameCustomization(game.packageName) { it.copy(isFavorite = !it.isFavorite) }
                                },
                                onLaunch = { onLaunchGame(game) },
                                onOpenDetail = { onOpenGameDetail(game.packageName) }
                            )
                        }
                    }
                }
                LibraryViewMode.LIST, LibraryViewMode.COMPACT, LibraryViewMode.LARGE_CARDS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 108.dp),
                        verticalArrangement = Arrangement.spacedBy(if (filterState.viewMode == LibraryViewMode.COMPACT) 8.dp else 12.dp)
                    ) {
                        items(games, key = { it.packageName }) { game ->
                            GameListRowCard(
                                game = game,
                                viewMode = filterState.viewMode,
                                bulkMode = filterState.bulkSelectionMode,
                                isSelected = game.packageName in filterState.selectedPackages,
                                onToggleSelect = { onToggleBulkPackage(game.packageName) },
                                onToggleFavorite = {
                                    onUpdateGameCustomization(game.packageName) { it.copy(isFavorite = !it.isFavorite) }
                                },
                                onTogglePin = {
                                    onUpdateGameCustomization(game.packageName) { it.copy(isPinned = !it.isPinned) }
                                },
                                onLaunch = { onLaunchGame(game) },
                                onOpenDetail = { onOpenGameDetail(game.packageName) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Any Installed App Dialog
    if (showAddAppDialog) {
        var appQuery by remember { mutableStateOf("") }
        val existingPkgs = remember(games) { games.map { it.packageName }.toSet() }
        val candidates = remember(allLaunchableApps, existingPkgs, appQuery) {
            allLaunchableApps
                .filter { it.packageName !in existingPkgs }
                .filter {
                    appQuery.isBlank() ||
                        it.appName.contains(appQuery, ignoreCase = true) ||
                        it.packageName.contains(appQuery, ignoreCase = true)
                }
        }
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Add App to Game Library", color = colors.softSilver) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    OutlinedTextField(
                        value = appQuery,
                        onValueChange = { appQuery = it },
                        placeholder = { Text("Filter installed apps...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(candidates.take(60), key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .clickable {
                                        onAddAppToLibrary(app.packageName)
                                        showAddAppDialog = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InstalledAppIcon(app.packageName, app.appName, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.appName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = colors.mutedText, maxLines = 1)
                                }
                                Icon(Icons.Rounded.Add, "Add", tint = colors.purpleAccent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAppDialog = false }) { Text("Close") }
            }
        )
    }

    if (showNewGroupDialog) {
        var newGroup by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Create Custom Group") },
            text = {
                OutlinedTextField(
                    value = newGroup,
                    onValueChange = { newGroup = it },
                    label = { Text("Group name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newGroup.isNotBlank()) onAddCustomGroup(newGroup.trim())
                    showNewGroupDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBulkGroupDialog) {
        AlertDialog(
            onDismissRequest = { showBulkGroupDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Assign Group to Selected") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    customGroups.sorted().forEach { grp ->
                        OutlinedButton(
                            onClick = {
                                onBulkApply({ it.copy(groupName = grp) }, "Group '$grp'")
                                showBulkGroupDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(grp) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBulkGroupDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBulkProfileDialog) {
        AlertDialog(
            onDismissRequest = { showBulkProfileDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Assign Profile to Selected") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameProfileType.entries.take(6).forEach { prof ->
                        OutlinedButton(
                            onClick = {
                                onBulkApply({ it.copy(assignedProfileId = prof.id) }, "Profile '${prof.displayName}'")
                                showBulkProfileDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(prof.displayName) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBulkProfileDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GameGridCard(
    game: DetectedGame,
    bulkMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLaunch: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val colors = LocalSymphineColors.current
    val customAccent = game.customization.customAccentHex?.let { parseHexColorOrNull(it) } ?: colors.purpleAccent

    LiquidGlassCard(
        accentColor = customAccent,
        onClick = { if (bulkMode) onToggleSelect() else onOpenDetail() },
        modifier = Modifier.testTag("game_card_${game.packageName}")
    ) {
        if (!game.customization.customBannerUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(game.customization.customBannerUri),
                contentDescription = "${game.displayTitle} banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            InstalledAppIcon(game.packageName, game.displayTitle, size = 48.dp)
            if (bulkMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
            } else {
                Row {
                    if (game.customization.isPinned) {
                        Icon(
                            Icons.Rounded.PushPin,
                            "Pinned",
                            tint = customAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(
                        imageVector = if (game.customization.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (game.customization.isFavorite) customAccent else colors.mutedText,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onToggleFavorite)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
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
            color = customAccent,
            maxLines = 1
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onLaunch,
                enabled = game.isLaunchable,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("launch_game_${game.packageName}"),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = customAccent, contentColor = Color(0xFF08090D))
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Play", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(
                onClick = onOpenDetail,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.background.copy(alpha = 0.7f))
                    .border(1.dp, colors.border, CircleShape)
            ) {
                Icon(Icons.Rounded.Tune, "Game Detail Center", tint = colors.softSilver, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun GameListRowCard(
    game: DetectedGame,
    viewMode: LibraryViewMode,
    bulkMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onLaunch: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val colors = LocalSymphineColors.current
    val customAccent = game.customization.customAccentHex?.let { parseHexColorOrNull(it) } ?: colors.purpleAccent

    LiquidGlassCard(
        accentColor = customAccent,
        onClick = { if (bulkMode) onToggleSelect() else onOpenDetail() },
        modifier = Modifier.testTag("game_row_${game.packageName}")
    ) {
        if (viewMode == LibraryViewMode.LARGE_CARDS && !game.customization.customBannerUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(game.customization.customBannerUri),
                contentDescription = "${game.displayTitle} banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bulkMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = "Select",
                    tint = customAccent,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
            InstalledAppIcon(
                game.packageName,
                game.displayTitle,
                size = if (viewMode == LibraryViewMode.COMPACT) 40.dp else 52.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.softSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (game.customization.isHidden) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.VisibilityOff, "Hidden", tint = colors.mutedText, modifier = Modifier.size(14.dp))
                    }
                }
                if (viewMode != LibraryViewMode.COMPACT) {
                    Text(
                        text = game.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Profile: ${game.customization.assignedProfileId} • ${game.customization.groupName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = customAccent
                )
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    Icons.Rounded.PushPin,
                    "Pin",
                    tint = if (game.customization.isPinned) customAccent else colors.mutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (game.customization.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Favorite",
                    tint = if (game.customization.isFavorite) customAccent else colors.mutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
            Button(
                onClick = onLaunch,
                enabled = game.isLaunchable,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = customAccent, contentColor = Color(0xFF08090D))
            ) {
                Text("Play", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
