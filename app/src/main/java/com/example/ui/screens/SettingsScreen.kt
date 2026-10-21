package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.DeviceStorageInfo
import com.example.model.MediaType
import com.example.ui.theme.GrabItCoral
import com.example.ui.theme.GrabItIndigo
import com.example.ui.theme.GrabItTeal

private val TabularNumStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun SettingsScreen(
    settings: AppSettings,
    deviceStorageInfo: DeviceStorageInfo,
    isDarkTheme: Boolean,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onChangeFolderClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onClearTempCache: () -> Unit,
    onClearAllHistory: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onOpenAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showQualityDialogFor by remember { mutableStateOf<MediaType?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- 1. STORAGE & FILE LOCATION ---
        item {
            SettingsSectionCard(
                title = "Storage & File Location",
                icon = Icons.Default.Folder,
                iconTint = GrabItIndigo
            ) {
                // Download Folder Path Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable(onClick = onChangeFolderClick)
                        .padding(12.dp)
                        .testTag("change_download_folder_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download Folder",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = settings.downloadFolderPath,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GrabItCoral.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrabItCoral,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Live Device Storage Usage Bar (StatFs)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Device Storage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${deviceStorageInfo.formattedFreeDevice} free of ${deviceStorageInfo.formattedTotalDevice}",
                            style = TabularNumStyle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GrabItTeal
                        )
                    }
                    LinearProgressIndicator(
                        progress = { deviceStorageInfo.deviceUsedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GrabItIndigo,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GrabIt Vault (${deviceStorageInfo.downloadedFilesCount} files): ${deviceStorageInfo.formattedUsedByGrabIt}",
                            style = TabularNumStyle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Temp cache: ${deviceStorageInfo.formattedTempCache}",
                            style = TabularNumStyle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Smart Folders Toggle
                SettingsSwitchRow(
                    title = "Auto-Organize Smart Folders",
                    subtitle = "Group files into Platform / MediaType subfolders",
                    checked = settings.organizeByFolders,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(organizeByFolders = checked) }
                    },
                    testTag = "smart_folders_switch"
                )
            }
        }

        // --- 2. DEFAULT FORMAT & QUALITY ---
        item {
            SettingsSectionCard(
                title = "Default Quality & Formats",
                icon = Icons.Default.HighQuality,
                iconTint = GrabItCoral
            ) {
                SettingsValueRow(
                    title = "Default Video Quality",
                    subtitle = "Preferred resolution for video links",
                    value = settings.defaultVideoQuality,
                    onClick = { showQualityDialogFor = MediaType.VIDEO },
                    testTag = "default_video_quality_row"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsValueRow(
                    title = "Default Audio Format",
                    subtitle = "Preferred bitrate and codec for music",
                    value = settings.defaultAudioFormat,
                    onClick = { showQualityDialogFor = MediaType.AUDIO },
                    testTag = "default_audio_format_row"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsValueRow(
                    title = "Default Photo Format",
                    subtitle = "Resolution for images and galleries",
                    value = settings.defaultPhotoFormat,
                    onClick = { showQualityDialogFor = MediaType.PHOTO },
                    testTag = "default_photo_format_row"
                )
            }
        }

        // --- 3. NETWORK & DOWNLOAD ENGINE ---
        item {
            SettingsSectionCard(
                title = "Network & Performance",
                icon = Icons.Default.Speed,
                iconTint = GrabItTeal
            ) {
                SettingsSwitchRow(
                    title = "Download Over Wi-Fi Only",
                    subtitle = "Prevent mobile data usage for media downloads",
                    checked = settings.wifiOnly,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(wifiOnly = checked) }
                    },
                    testTag = "wifi_only_switch"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsSwitchRow(
                    title = "HTTP Range Resume Support",
                    subtitle = "Resume paused or interrupted downloads seamlessly",
                    checked = settings.fastMultiThreaded,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(fastMultiThreaded = checked) }
                    },
                    testTag = "multithread_switch"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Concurrent Downloads Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Max Concurrent Downloads",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Simultaneous active downloads in queue",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = GrabItIndigo.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${settings.maxConcurrentDownloads}",
                                style = TabularNumStyle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrabItIndigo,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            val isSelected = settings.maxConcurrentDownloads == count
                            OutlinedButton(
                                onClick = {
                                    onUpdateSettings { it.copy(maxConcurrentDownloads = count) }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) GrabItIndigo else Color.Transparent,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("$count", style = TabularNumStyle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. NOTIFICATIONS & APPEARANCE ---
        item {
            SettingsSectionCard(
                title = "Notifications & Appearance",
                icon = Icons.Default.NotificationsActive,
                iconTint = Color(0xFFFFA502)
            ) {
                SettingsSwitchRow(
                    title = "Completion Alerts",
                    subtitle = "Show confirmation banner when media finishes saving",
                    checked = settings.notificationsCompletion,
                    onCheckedChange = { checked ->
                        onUpdateSettings { it.copy(notificationsCompletion = checked) }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                SettingsSwitchRow(
                    title = "Dark Mode Theme",
                    subtitle = "High-contrast obsidian dark mode interface",
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleTheme() },
                    testTag = "dark_mode_switch"
                )
            }
        }

        // --- 5. DATA MANAGEMENT & ABOUT ---
        item {
            SettingsSectionCard(
                title = "Maintenance & App Guide",
                icon = Icons.Default.Storage,
                iconTint = MaterialTheme.colorScheme.error
            ) {
                // Supported Platforms & URLs Directory Navigation Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenAboutClick)
                        .testTag("open_about_supported_urls_row"),
                    color = GrabItIndigo.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GrabItIndigo.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = GrabItIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Supported Platforms & URLs",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "YouTube, YT Music, TikTok, X, FB, Insta, Reddit & more",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = GrabItIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearTempCache,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_temp_cache_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Cache", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onReplayOnboarding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("replay_onboarding_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("App Guide", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_all_history_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Downloads & History", fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "GrabIt v2.5.0 • Direct HTTP Stream & Metadata Engine • Android Share Sheet Enabled",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Quality Picker Dialog
    showQualityDialogFor?.let { mediaType ->
        AlertDialog(
            onDismissRequest = { showQualityDialogFor = null },
            title = {
                Text(
                    text = "Select Default ${mediaType.shortLabel} Quality",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mediaType.defaultQualityOptions.forEach { option ->
                        val currentSelected = when (mediaType) {
                            MediaType.VIDEO -> settings.defaultVideoQuality == option
                            MediaType.AUDIO -> settings.defaultAudioFormat == option
                            MediaType.PHOTO -> settings.defaultPhotoFormat == option
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onUpdateSettings { current ->
                                        when (mediaType) {
                                            MediaType.VIDEO -> current.copy(defaultVideoQuality = option)
                                            MediaType.AUDIO -> current.copy(defaultAudioFormat = option)
                                            MediaType.PHOTO -> current.copy(defaultPhotoFormat = option)
                                        }
                                    }
                                    showQualityDialogFor = null
                                },
                            color = if (currentSelected) GrabItCoral.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 14.sp,
                                    fontWeight = if (currentSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (currentSelected) GrabItCoral else MaterialTheme.colorScheme.onSurface
                                )
                                if (currentSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GrabItCoral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialogFor = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear All History Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Delete All Downloads?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all downloaded files (${deviceStorageInfo.formattedUsedByGrabIt}) and clear your download history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GrabItCoral
            )
        )
    }
}

@Composable
private fun SettingsValueRow(
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrabItCoral
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = GrabItCoral,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
