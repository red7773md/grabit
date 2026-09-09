package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.Platform
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onShowFolderPicker: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    var videoQualityExpanded by remember { mutableStateOf(false) }
    var audioQualityExpanded by remember { mutableStateOf(false) }
    var photoQualityExpanded by remember { mutableStateOf(false) }

    val videoQualities = listOf("1080p FHD (Recommended)", "720p HD", "480p SD", "4K Ultra HD")
    val audioQualities = listOf("320 kbps MP3 (Best)", "256 kbps M4A", "128 kbps MP3", "FLAC Lossless")
    val photoQualities = listOf("Original Quality", "High Res JPG", "Compressed WebP")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen_content"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. STORAGE & FOLDERS ---
        SettingsSection(title = "Storage & Folders", icon = Icons.Default.Folder) {
            // Folder Path Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowFolderPicker)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Folder Path",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = settings.downloadFolderPath,
                        fontSize = 12.sp,
                        color = GrabItIndigo,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onShowFolderPicker,
                    modifier = Modifier.testTag("change_folder_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change folder",
                        tint = GrabItIndigo
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Sub-folders Toggle
            SettingsSwitchItem(
                title = "Auto-Organize Subfolders",
                subtitle = "Save into platform & media folders (e.g., GrabIt/Instagram/Videos/)",
                checked = settings.organizeByFolders,
                onCheckedChange = { onUpdateSettings(settings.copy(organizeByFolders = it)) }
            )
        }

        // --- 2. DEFAULT MEDIA QUALITY ---
        SettingsSection(title = "Default Quality & Formats", icon = Icons.Default.HighQuality) {
            // Video Quality Dropdown
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Default Video Quality",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = videoQualityExpanded,
                    onExpandedChange = { videoQualityExpanded = !videoQualityExpanded }
                ) {
                    OutlinedTextField(
                        value = settings.defaultVideoQuality,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoQualityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = videoQualityExpanded,
                        onDismissRequest = { videoQualityExpanded = false }
                    ) {
                        videoQualities.forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(quality) },
                                onClick = {
                                    onUpdateSettings(settings.copy(defaultVideoQuality = quality))
                                    videoQualityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Audio Format Dropdown
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Default Music / Audio Format",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = audioQualityExpanded,
                    onExpandedChange = { audioQualityExpanded = !audioQualityExpanded }
                ) {
                    OutlinedTextField(
                        value = settings.defaultAudioFormat,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioQualityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = audioQualityExpanded,
                        onDismissRequest = { audioQualityExpanded = false }
                    ) {
                        audioQualities.forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(quality) },
                                onClick = {
                                    onUpdateSettings(settings.copy(defaultAudioFormat = quality))
                                    audioQualityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Photo Format Dropdown
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Default Photo Format",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = photoQualityExpanded,
                    onExpandedChange = { photoQualityExpanded = !photoQualityExpanded }
                ) {
                    OutlinedTextField(
                        value = settings.defaultPhotoFormat,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = photoQualityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = photoQualityExpanded,
                        onDismissRequest = { photoQualityExpanded = false }
                    ) {
                        photoQualities.forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(quality) },
                                onClick = {
                                    onUpdateSettings(settings.copy(defaultPhotoFormat = quality))
                                    photoQualityExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 3. NETWORK & PERFORMANCE ---
        SettingsSection(title = "Network & Download Rules", icon = Icons.Default.NetworkCheck) {
            // Wi-Fi Only Toggle
            SettingsSwitchItem(
                title = "Download over Wi-Fi only",
                subtitle = "Prevent downloading over cellular data",
                checked = settings.wifiOnly,
                onCheckedChange = { onUpdateSettings(settings.copy(wifiOnly = it)) },
                testTag = "wifi_only_switch"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Max Concurrent Downloads
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Max Concurrent Downloads",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Number of active download threads",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (settings.maxConcurrentDownloads > 1) {
                                onUpdateSettings(settings.copy(maxConcurrentDownloads = settings.maxConcurrentDownloads - 1))
                            }
                        },
                        enabled = settings.maxConcurrentDownloads > 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = settings.maxConcurrentDownloads.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrabItCoral
                    )

                    IconButton(
                        onClick = {
                            if (settings.maxConcurrentDownloads < 5) {
                                onUpdateSettings(settings.copy(maxConcurrentDownloads = settings.maxConcurrentDownloads + 1))
                            }
                        },
                        enabled = settings.maxConcurrentDownloads < 5,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Multi-thread Turbo
            SettingsSwitchItem(
                title = "Multi-Threaded Turbo Download",
                subtitle = "Splits large videos into parts for up to 3x faster speed",
                checked = settings.fastMultiThreaded,
                onCheckedChange = { onUpdateSettings(settings.copy(fastMultiThreaded = it)) }
            )
        }

        // --- 4. NOTIFICATIONS ---
        SettingsSection(title = "Notifications & Alerts", icon = Icons.Default.Notifications) {
            SettingsSwitchItem(
                title = "Download Completion Alert",
                subtitle = "Notify when a video, song, or photo is saved",
                checked = settings.notificationsCompletion,
                onCheckedChange = { onUpdateSettings(settings.copy(notificationsCompletion = it)) },
                testTag = "notifications_toggle"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            SettingsSwitchItem(
                title = "Live Progress Notification",
                subtitle = "Show real-time speed and progress in notification shade",
                checked = settings.notificationsProgress,
                onCheckedChange = { onUpdateSettings(settings.copy(notificationsProgress = it)) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            SettingsSwitchItem(
                title = "Sound on Completion",
                subtitle = "Play short chime when download completes",
                checked = settings.soundOnFinish,
                onCheckedChange = { onUpdateSettings(settings.copy(soundOnFinish = it)) }
            )
        }

        // --- 5. STORAGE BREAKDOWN & CACHE ---
        SettingsSection(title = "Storage Library", icon = Icons.Default.PieChart) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "GrabIt Library: 231.0 MB used",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Multi-colored storage bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    Box(modifier = Modifier.weight(0.65f).fillMaxHeight().background(GrabItCoral))
                    Box(modifier = Modifier.weight(0.20f).fillMaxHeight().background(GrabItIndigo))
                    Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(GrabItTeal))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GrabItCoral))
                        Text("Videos 180MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GrabItIndigo))
                        Text("Music 38MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GrabItTeal))
                        Text("Photos 13MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Temporary Cache (0 B)")
                }
            }
        }

        // --- 6. ABOUT & ONBOARDING REPLAY ---
        SettingsSection(title = "App Info & Help", icon = Icons.Default.Info) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReplayOnboarding)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = GrabItIndigo)
                    Text(
                        text = "Replay Intro & Feature Guide",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Supported Media Platforms:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Platform.entries.filter { it != Platform.OTHER }.take(5).forEach {
                        PlatformBadge(platform = it)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Platform.entries.filter { it != Platform.OTHER }.drop(5).forEach {
                        PlatformBadge(platform = it)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GrabIt Media Downloader v1.0.0 • Clean Material 3",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GrabItCoral,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            content = content
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GrabItCoral
            ),
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
