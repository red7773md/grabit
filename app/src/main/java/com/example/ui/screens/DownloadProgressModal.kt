package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.MediaType
import com.example.ui.components.GrabItCircularProgressIndicator
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.GrabItAmber
import com.example.ui.theme.GrabItCoral
import com.example.ui.theme.GrabItIndigo
import com.example.ui.theme.GrabItTeal

private val TabularNumStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun DownloadProgressModal(
    item: DownloadItem,
    downloadFolderPath: String,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onDownloadInBackground: () -> Unit,
    onOpenMedia: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFailed = item.status == DownloadStatus.FAILED
    val isPaused = item.status == DownloadStatus.PAUSED
    val isCompleted = item.status == DownloadStatus.COMPLETED

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("download_progress_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isFailed -> MaterialTheme.colorScheme.error
                                        isPaused -> GrabItAmber
                                        isCompleted -> GrabItTeal
                                        else -> GrabItCoral
                                    }
                                )
                        )
                        Text(
                            text = when (item.status) {
                                DownloadStatus.PAUSED -> "Download Paused"
                                DownloadStatus.QUEUED -> "Queued for Download"
                                DownloadStatus.COMPLETED -> "Download Complete"
                                DownloadStatus.FAILED -> "Download Interrupted"
                                else -> "Streaming Media"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("close_progress_modal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center Circular Animated Gauge
                GrabItCircularProgressIndicator(
                    progress = item.progress,
                    size = 148.dp,
                    strokeWidth = 11.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Title & Platform Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlatformBadge(platform = item.platform)

                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${item.quality} • ${item.mediaType.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (isFailed && !item.errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.errorMessage,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Metrics Grid (Speed, Downloaded/Total, ETA)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricColumn(
                        label = "Speed",
                        value = if (isPaused || isFailed) "0 B/s" else item.downloadSpeed,
                        color = GrabItTeal
                    )
                    VerticalDivider(
                        modifier = Modifier.height(30.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    MetricColumn(
                        label = "Transferred",
                        value = if (item.fileSize > 0L) {
                            "${item.formattedDownloadedSize} / ${item.formattedFileSize}"
                        } else {
                            item.formattedDownloadedSize
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    VerticalDivider(
                        modifier = Modifier.height(30.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    MetricColumn(
                        label = "Status",
                        value = when {
                            isPaused -> "Paused"
                            isFailed -> "Failed"
                            else -> item.timeRemaining.ifEmpty { "Streaming" }
                        },
                        color = if (isFailed) MaterialTheme.colorScheme.error else GrabItIndigo
                    )
                }

                // Destination Folder Path
                Text(
                    text = "Saving to: $downloadFolderPath/${item.platform.subfolder}/${item.mediaType.subfolder}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCompleted) {
                        Button(
                            onClick = onOpenMedia,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("modal_open_media_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GrabItCoral)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.mediaType == MediaType.VIDEO) Icons.Default.PlayArrow else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (item.mediaType == MediaType.VIDEO) "Play Video Now" else "Open Media Now",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("modal_done_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        if (!isFailed) {
                            Button(
                                onClick = onDownloadInBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("download_in_background_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GrabItIndigo)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Continue in Background", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPauseResume,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("modal_pause_resume_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isFailed -> Icons.Default.Refresh
                                        isPaused -> Icons.Default.PlayArrow
                                        else -> Icons.Default.Pause
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isFailed -> "Retry"
                                        isPaused -> "Resume"
                                        else -> "Pause"
                                    },
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("modal_cancel_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TabularNumStyle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
