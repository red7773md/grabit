package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.MediaFileManager
import com.example.model.DeviceStorageInfo
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.MediaType
import com.example.model.Platform
import com.example.ui.components.*
import com.example.ui.theme.*

private val TabularNumStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun HomeScreen(
    inputUrl: String,
    inputUrlError: String?,
    isInspectingLink: Boolean,
    detectedPlatform: Platform?,
    selectedMediaType: MediaType,
    detectedClipboardUrl: String?,
    detectedClipboardPlatform: Platform?,
    activeDownloads: List<DownloadItem>,
    recentCompletions: List<DownloadItem>,
    downloadFolderPath: String,
    deviceStorageInfo: DeviceStorageInfo,
    isBatchMode: Boolean = false,
    onToggleBatchMode: () -> Unit = {},
    onStartBatchDownload: (String, MediaType) -> Unit = { _, _ -> },
    onPauseAllDownloads: () -> Unit = {},
    onResumeAllDownloads: () -> Unit = {},
    onClearFailedDownloads: () -> Unit = {},
    onUrlChange: (String) -> Unit,
    onMediaTypeChange: (MediaType) -> Unit,
    onPasteFromClipboard: (String) -> Unit,
    onAcceptClipboardSuggestion: () -> Unit,
    onDismissClipboardSuggestion: () -> Unit,
    onClearUrl: () -> Unit,
    onGrabMedia: () -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onOpenItem: (DownloadItem) -> Unit,
    onShareItem: (DownloadItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onActiveItemClick: (DownloadItem) -> Unit,
    onOpenHistoryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onOpenAboutClick: () -> Unit = {},
    onSelectTestPlatform: (Platform) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 0. SMART CLIPBOARD AUTO-DETECTION BANNER ---
        item {
            AnimatedVisibility(
                visible = !detectedClipboardUrl.isNullOrBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val clipUrl = detectedClipboardUrl.orEmpty()
                val clipPlatform = detectedClipboardPlatform ?: Platform.OTHER

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.2.dp,
                            color = GrabItIndigo.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("smart_clipboard_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GrabItIndigo.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GrabItIndigo.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPasteGo,
                                        contentDescription = null,
                                        tint = GrabItIndigoLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "Copied Link Detected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                PlatformBadge(platform = clipPlatform)
                            }

                            IconButton(
                                onClick = onDismissClipboardSuggestion,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("dismiss_clipboard_banner_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss clipboard suggestion",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = clipUrl,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Button(
                            onClick = onAcceptClipboardSuggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("accept_clipboard_banner_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GrabItIndigo)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Inspect & Download Copied Link",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 1. PASTE LINK INPUT CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val detectedMultipleUrls: List<String> = remember(inputUrl) {
                    if (inputUrl.isBlank()) {
                        emptyList()
                    } else {
                        inputUrl.split(Regex("[\\r\\n,;\\s]+"))
                            .map { MediaFileManager.normalizeUrl(it) }
                            .filter { MediaFileManager.isValidHttpUrl(it) }
                            .distinct()
                    }
                }
                val isEffectivelyBatch: Boolean = isBatchMode || (detectedMultipleUrls.size > 1)

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header & Batch Mode Toggle Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEffectivelyBatch) "Batch Downloader" else "Download Media",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEffectivelyBatch) "Paste multiple links (separated by newlines)" else "Paste a direct video, audio, image, or social link",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Mode Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleBatchMode() }
                                .testTag("batch_mode_toggle_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEffectivelyBatch) Icons.Default.Layers else Icons.Default.Link,
                                    contentDescription = null,
                                    tint = if (isEffectivelyBatch) GrabItCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEffectivelyBatch) "Batch Mode" else "Single",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEffectivelyBatch) GrabItCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- SUPPORTED PLATFORMS QUICK SELECTOR CAROUSEL ---
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Supported Platforms (Tap to try link):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "View All (10+)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrabItIndigo,
                                modifier = Modifier
                                    .clickable(onClick = onOpenAboutClick)
                                    .testTag("home_view_all_platforms_link")
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            val carouselPlatforms = listOf(
                                Platform.YOUTUBE,
                                Platform.YT_MUSIC,
                                Platform.TIKTOK,
                                Platform.INSTAGRAM,
                                Platform.TWITTER_X,
                                Platform.FACEBOOK,
                                Platform.REDDIT,
                                Platform.SOUNDCLOUD
                            )
                            items(carouselPlatforms, key = { it.name }) { platform ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = platform.brandColor.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = platform.brandColor.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectTestPlatform(platform)
                                            onUrlChange(platform.exampleUrl)
                                        }
                                        .testTag("home_platform_chip_${platform.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(platform.brandColor)
                                        )
                                        Text(
                                            text = platform.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input Text Field (Single line or Multi-line if Batch)
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = onUrlChange,
                        enabled = !isInspectingLink,
                        isError = inputUrlError != null,
                        singleLine = !isEffectivelyBatch,
                        minLines = if (isEffectivelyBatch) 3 else 1,
                        maxLines = if (isEffectivelyBatch) 5 else 1,
                        supportingText = if (inputUrlError != null) {
                            {
                                Text(
                                    text = inputUrlError,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else if (isEffectivelyBatch && detectedMultipleUrls.isNotEmpty()) {
                            {
                                Text(
                                    text = "Detected ${detectedMultipleUrls.size} valid links ready for batch download",
                                    fontSize = 11.sp,
                                    color = GrabItTeal,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paste_url_input_field"),
                        placeholder = {
                            Text(
                                text = if (isEffectivelyBatch) "Paste multiple URLs separated by newlines:\nhttps://youtube.com/watch?v=...\nhttps://tiktok.com/@..." else "https://...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isEffectivelyBatch) Icons.Default.Layers else Icons.Default.Link,
                                contentDescription = "Link",
                                tint = if (detectedPlatform != null) detectedPlatform.brandColor else GrabItCoral
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inputUrl.isNotEmpty() && !isInspectingLink) {
                                    IconButton(
                                        onClick = onClearUrl,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("clear_url_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear URL",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text ?: ""
                                        if (clipText.isNotBlank()) {
                                            onPasteFromClipboard(clipText)
                                        }
                                    },
                                    enabled = !isInspectingLink,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("paste_clipboard_btn")
                                    ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste from clipboard",
                                        tint = GrabItIndigo,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrabItCoral,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    // Media Type Selector (Video / Audio / Photo)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred Capture Format:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaType.entries.forEach { type ->
                                val isSelected = selectedMediaType == type
                                val icon = when (type) {
                                    MediaType.VIDEO -> Icons.Default.Videocam
                                    MediaType.AUDIO -> Icons.Default.Audiotrack
                                    MediaType.PHOTO -> Icons.Default.Image
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) GrabItCoral else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !isInspectingLink) { onMediaTypeChange(type) }
                                        .testTag("media_type_tab_${type.name}"),
                                    color = if (isSelected) GrabItCoral.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = type.label,
                                            tint = if (isSelected) GrabItCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = type.shortLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) GrabItCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Primary Inspect & Download CTA Button
                    Button(
                        onClick = {
                            if (isEffectivelyBatch) {
                                onStartBatchDownload(inputUrl, selectedMediaType)
                            } else {
                                onGrabMedia()
                            }
                        },
                        enabled = !isInspectingLink && inputUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("grab_media_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrabItCoral
                        )
                    ) {
                        if (isInspectingLink) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.2.dp
                                )
                                Text(
                                    text = if (isEffectivelyBatch) "Inspecting Batch Streams..." else "Inspecting Link Metadata...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEffectivelyBatch) Icons.Default.Layers else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isEffectivelyBatch) {
                                        val count = if (detectedMultipleUrls.isNotEmpty()) detectedMultipleUrls.size else 1
                                        "Inspect & Enqueue Batch ($count Links)"
                                    } else {
                                        "Inspect & Download ${selectedMediaType.shortLabel}"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Supported Platforms & URL Guide link row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenAboutClick)
                            .padding(vertical = 4.dp)
                            .testTag("home_view_supported_urls_btn"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = GrabItIndigo,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Full Supported URLs Guide & Specifications (YouTube, FB, X...)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GrabItIndigo
                        )
                    }
                }
            }
        }

        // --- 2. STORAGE & DESTINATION TELEMETRY BAR ---
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = onOpenSettingsClick)
                    .testTag("storage_destination_banner"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GrabItIndigo.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = "Storage Path",
                                tint = GrabItIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${deviceStorageInfo.formattedFreeDevice} Free",
                                    style = TabularNumStyle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${deviceStorageInfo.downloadedFilesCount} saved (${deviceStorageInfo.formattedUsedByGrabIt})",
                                    style = TabularNumStyle,
                                    fontSize = 11.sp,
                                    color = GrabItTeal,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = downloadFolderPath,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = "Manage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrabItCoral
                    )
                }
            }
        }

        // --- 3. ACTIVE DOWNLOADS QUEUE ---
        if (activeDownloads.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GrabItCoral)
                        )
                        Text(
                            text = "Active Queue (${activeDownloads.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Tap card for live telemetry",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Batch Queue Controls Bar
            item {
                val downloadingCount = activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
                val pausedCount = activeDownloads.count { it.status == DownloadStatus.PAUSED }
                val queuedCount = activeDownloads.count { it.status == DownloadStatus.QUEUED }
                val failedCount = activeDownloads.count { it.status == DownloadStatus.FAILED }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (downloadingCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GrabItCoral.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$downloadingCount downloading",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrabItCoral,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (pausedCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "$pausedCount paused",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (queuedCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GrabItIndigo.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$queuedCount queued",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrabItIndigo,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Batch Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (downloadingCount > 0 || queuedCount > 0) {
                                TextButton(
                                    onClick = onPauseAllDownloads,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("pause_all_downloads_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause All",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Pause All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (pausedCount > 0) {
                                TextButton(
                                    onClick = onResumeAllDownloads,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("resume_all_downloads_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume All",
                                        tint = GrabItCoral,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Resume All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrabItCoral)
                                }
                            }
                            if (failedCount > 0) {
                                TextButton(
                                    onClick = onClearFailedDownloads,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("clear_failed_downloads_btn")
                                ) {
                                    Text("Clear Failed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            items(activeDownloads, key = { it.id }) { download ->
                ActiveDownloadCard(
                    item = download,
                    onPauseResume = {
                        if (download.status == DownloadStatus.DOWNLOADING) {
                            onPauseDownload(download.id)
                        } else {
                            onResumeDownload(download.id)
                        }
                    },
                    onCancel = { onCancelDownload(download.id) },
                    onClick = { onActiveItemClick(download) }
                )
            }
        }

        // --- 4. RECENT COMPLETIONS SECTION ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Downloads",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (recentCompletions.isNotEmpty()) {
                    TextButton(
                        onClick = onOpenHistoryClick,
                        modifier = Modifier.testTag("see_all_history_btn")
                    ) {
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrabItCoral
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = GrabItCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (recentCompletions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(GrabItIndigo.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = GrabItIndigo,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "Your Vault is Ready",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Paste any direct media link or share a URL from your browser or social apps via Android's Share Sheet to save files offline.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            items(recentCompletions, key = { it.id }) { completed ->
                CompletedDownloadCard(
                    item = completed,
                    onOpen = { onOpenItem(completed) },
                    onShare = { onShareItem(completed) },
                    onDelete = { onDeleteItem(completed.id) }
                )
            }
        }
    }
}
