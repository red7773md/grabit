package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadItem
import com.example.model.MediaType
import com.example.model.Platform
import com.example.ui.components.CompletedDownloadCard
import com.example.ui.components.DownloadGridCard
import com.example.ui.components.PlatformChip
import com.example.ui.theme.GrabItCoral
import com.example.ui.theme.GrabItIndigo

@Composable
fun HistoryScreen(
    items: List<DownloadItem>,
    searchQuery: String,
    selectedPlatform: Platform?,
    selectedMediaType: MediaType?,
    isGridLayout: Boolean,
    onSearchChange: (String) -> Unit,
    onPlatformSelect: (Platform?) -> Unit,
    onMediaTypeSelect: (MediaType?) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenItem: (DownloadItem) -> Unit,
    onShareItem: (DownloadItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter the items according to search, platform, and media type
    val filteredItems = remember(items, searchQuery, selectedPlatform, selectedMediaType) {
        items.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.platform.displayName.contains(searchQuery, ignoreCase = true) ||
                    item.author.contains(searchQuery, ignoreCase = true)

            val matchesPlatform = selectedPlatform == null || item.platform == selectedPlatform
            val matchesMediaType = selectedMediaType == null || item.mediaType == selectedMediaType

            matchesQuery && matchesPlatform && matchesMediaType
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("history_screen_content")
    ) {
        // --- 1. SEARCH BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_field"),
                placeholder = {
                    Text(
                        text = "Search downloaded files, platforms, or creators...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GrabItCoral,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )
        }

        // --- 2. MEDIA TYPE TABS (All, Videos, Music, Photos) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeTabs = listOf(
                Pair("All", null),
                Pair("Videos", MediaType.VIDEO),
                Pair("Music", MediaType.AUDIO),
                Pair("Photos", MediaType.PHOTO)
            )

            typeTabs.forEach { (label, type) ->
                val isSelected = selectedMediaType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onMediaTypeSelect(type) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        when (type) {
                            MediaType.VIDEO -> Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp))
                            MediaType.AUDIO -> Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(14.dp))
                            MediaType.PHOTO -> Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                            null -> Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrabItCoral.copy(alpha = 0.18f),
                        selectedLabelColor = GrabItCoral,
                        selectedLeadingIconColor = GrabItCoral
                    ),
                    modifier = Modifier.testTag("media_filter_chip_$label")
                )
            }
        }

        // --- 3. PLATFORM FILTER CHIPS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All Platforms" chip
            FilterChip(
                selected = selectedPlatform == null,
                onClick = { onPlatformSelect(null) },
                label = { Text("All Platforms", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GrabItIndigo.copy(alpha = 0.18f),
                    selectedLabelColor = GrabItIndigo
                )
            )

            // Individual Platform Chips
            Platform.entries.filter { it != Platform.OTHER }.forEach { platform ->
                val count = items.count { it.platform == platform }
                PlatformChip(
                    platform = platform,
                    isSelected = selectedPlatform == platform,
                    onClick = {
                        onPlatformSelect(if (selectedPlatform == platform) null else platform)
                    },
                    count = if (count > 0) count else null,
                    modifier = Modifier.testTag("platform_chip_${platform.name}")
                )
            }
        }

        // --- 4. RESULTS HEADER & GRID/LIST TOGGLE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredItems.size} ${if (filteredItems.size == 1) "item" else "items"} saved",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleLayout,
                    modifier = Modifier.size(36.dp).testTag("toggle_history_layout_btn")
                ) {
                    Icon(
                        imageVector = if (isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridLayout) "Switch to List View" else "Switch to Grid View",
                        tint = GrabItIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 5. GRID OR LIST VIEW ---
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
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "No Downloads Found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try adjusting your filters or search terms, or paste a new link on Home.",
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
                            onDelete = { onDeleteItem(item.id) }
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
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}
