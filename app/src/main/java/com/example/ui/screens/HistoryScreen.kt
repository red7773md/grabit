package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadItem
import com.example.model.MediaType
import com.example.model.Platform
import com.example.ui.components.*
import com.example.ui.theme.GrabItCoral
import com.example.ui.theme.GrabItIndigo

private val TabularNumStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun HistoryScreen(
    historyItems: List<DownloadItem>,
    searchQuery: String,
    selectedPlatform: Platform?,
    selectedMediaType: MediaType?,
    isGridLayout: Boolean,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onPlatformSelect: (Platform?) -> Unit,
    onMediaTypeSelect: (MediaType?) -> Unit,
    onToggleLayout: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleItemSelection: (String) -> Unit,
    onSelectAllFiltered: (List<String>) -> Unit,
    onClearSelection: () -> Unit,
    onBatchDeleteSelected: () -> Unit,
    onBatchShareSelected: () -> Unit,
    onOpenItem: (DownloadItem) -> Unit,
    onShareItem: (DownloadItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        onClearSelection()
    }

    val filteredItems = remember(historyItems, searchQuery, selectedPlatform, selectedMediaType) {
        historyItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.author.contains(searchQuery, ignoreCase = true) ||
                    item.platform.displayName.contains(searchQuery, ignoreCase = true)
            val matchesPlatform = selectedPlatform == null || item.platform == selectedPlatform
            val matchesType = selectedMediaType == null || item.mediaType == selectedMediaType
            matchesSearch && matchesPlatform && matchesType
        }
    }

    val totalSize = remember(filteredItems) {
        filteredItems.sumOf { it.fileSize }
    }

    val selectedSize = remember(historyItems, selectedIds) {
        historyItems.filter { it.id in selectedIds }.sumOf { it.fileSize }
    }

    val allFilteredSelected = remember(filteredItems, selectedIds) {
        filteredItems.isNotEmpty() && selectedIds.containsAll(filteredItems.map { it.id })
    }

    if (showBatchDeleteConfirm && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedIds.size} Selected Files?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently remove ${selectedIds.size} media files (${DownloadItem.formatBytes(selectedSize)}) from your device storage and history."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatchDeleteConfirm = false
                        onBatchDeleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_batch_delete_btn")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen_content")
    ) {
        // --- CONTEXTUAL MULTI-SELECT BATCH BAR ---
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = GrabItIndigo.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_selection_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_selection_mode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection mode",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column {
                            Text(
                                text = "${selectedIds.size} selected",
                                style = TabularNumStyle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = DownloadItem.formatBytes(selectedSize),
                                style = TabularNumStyle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { onSelectAllFiltered(filteredItems.map { it.id }) },
                            modifier = Modifier.testTag("select_all_history_btn")
                        ) {
                            Icon(
                                imageVector = if (allFilteredSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allFilteredSelected) "None" else "All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onBatchShareSelected,
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("batch_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share selected",
                                tint = if (selectedIds.isNotEmpty()) GrabItIndigo else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = { showBatchDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("batch_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // --- TOP FILTER & SEARCH HEADER ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("history_search_field"),
                        placeholder = {
                            Text(
                                text = "Search title, creator, or platform...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrabItCoral,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    if (historyItems.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelectionMode) GrabItCoral else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            color = if (isSelectionMode) GrabItCoral.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            IconButton(
                                onClick = onToggleSelectionMode,
                                modifier = Modifier.testTag("toggle_selection_mode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Batch Select Mode",
                                    tint = if (isSelectionMode) GrabItCoral else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Toggle Grid/List Button
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        IconButton(
                            onClick = onToggleLayout,
                            modifier = Modifier.testTag("toggle_history_layout_btn")
                        ) {
                            Icon(
                                imageVector = if (isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Layout",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Media Type Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMediaType == null,
                        onClick = { onMediaTypeSelect(null) },
                        label = { Text("All Types", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GrabItCoral,
                            selectedLabelColor = Color.White
                        )
                    )
                    MediaType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedMediaType == type,
                            onClick = { onMediaTypeSelect(type) },
                            label = { Text(type.shortLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GrabItCoral,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("history_type_filter_${type.name}")
                        )
                    }
                }

                // Platform Filter Chips Scrollable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedPlatform == null,
                        onClick = { onPlatformSelect(null) },
                        label = { Text("All Platforms", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GrabItIndigo,
                            selectedLabelColor = Color.White
                        )
                    )
                    Platform.entries.filter { it != Platform.OTHER }.forEach { platform ->
                        PlatformChip(
                            platform = platform,
                            isSelected = selectedPlatform == platform,
                            onClick = { onPlatformSelect(platform) }
                        )
                    }
                }

                // Summary bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredItems.size} of ${historyItems.size} items",
                        style = TabularNumStyle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total Size: ${DownloadItem.formatBytes(totalSize)}",
                        style = TabularNumStyle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrabItIndigo
                    )
                }
            }
        }

        // --- CONTENT LIST OR GRID ---
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = if (historyItems.isEmpty()) "No Downloads Yet" else "No Matching Downloads",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (historyItems.isEmpty()) {
                            "Media files you download will appear here with search, platform filters, and batch management."
                        } else {
                            "Try adjusting your search or filter chips above."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotEmpty() || selectedPlatform != null || selectedMediaType != null) {
                        Button(
                            onClick = {
                                onSearchChange("")
                                onPlatformSelect(null)
                                onMediaTypeSelect(null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GrabItIndigo)
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            }
        } else {
            if (isGridLayout) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        DownloadGridCard(
                            item = item,
                            onOpen = { onOpenItem(item) },
                            onShare = { onShareItem(item) },
                            onDelete = { onDeleteItem(item.id) },
                            isSelectionMode = isSelectionMode,
                            isSelected = item.id in selectedIds,
                            onToggleSelect = { onToggleItemSelection(item.id) },
                            onLongClick = { onToggleItemSelection(item.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        CompletedDownloadCard(
                            item = item,
                            onOpen = { onOpenItem(item) },
                            onShare = { onShareItem(item) },
                            onDelete = { onDeleteItem(item.id) },
                            isSelectionMode = isSelectionMode,
                            isSelected = item.id in selectedIds,
                            onToggleSelect = { onToggleItemSelection(item.id) },
                            onLongClick = { onToggleItemSelection(item.id) }
                        )
                    }
                }
            }
        }
    }
}
