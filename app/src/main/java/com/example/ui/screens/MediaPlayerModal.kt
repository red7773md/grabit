package com.example.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.MediaFileManager
import com.example.model.DownloadItem
import com.example.model.MediaType
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.GrabItCoral
import com.example.ui.theme.GrabItIndigo
import com.example.ui.theme.GrabItTeal
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MediaPlayerModal(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onShare: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(item.realFilePath) {
        if (item.realFilePath.isNotBlank()) File(item.realFilePath) else null
    }
    val fileExists = file?.exists() == true

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("media_player_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Platform Badge, Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        PlatformBadge(platform = item.platform)
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_media_player_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Media Display Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.mediaType) {
                        MediaType.VIDEO -> {
                            if (fileExists && file != null) {
                                VideoPlayerView(file = file)
                            } else {
                                MediaPlaceholderView(item = item, message = "Video file saved to storage")
                            }
                        }

                        MediaType.AUDIO -> {
                            if (fileExists && file != null) {
                                AudioPlayerView(file = file, item = item)
                            } else {
                                MediaPlaceholderView(item = item, message = "Audio file saved to storage")
                            }
                        }

                        MediaType.PHOTO -> {
                            if (fileExists && file != null) {
                                PhotoViewer(file = file)
                            } else {
                                MediaThumbnail(
                                    item = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // File Details Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.quality} • ${item.formattedFileSize}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GrabItTeal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (fileExists) "✓ Saved On Disk" else "Saved",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrabItTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (item.realFilePath.isNotBlank()) {
                            Text(
                                text = "Path: ${item.realFilePath}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Open in External App (Gallery, VLC, Photos)
                    Button(
                        onClick = {
                            if (item.realFilePath.isNotBlank()) {
                                MediaFileManager.openFileWithExternalApp(
                                    context = context,
                                    filePath = item.realFilePath,
                                    mediaType = item.mediaType
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_external_app_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrabItCoral)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open In Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share File
                    OutlinedButton(
                        onClick = {
                            if (item.realFilePath.isNotBlank() && fileExists) {
                                MediaFileManager.shareFileWithExternalApp(
                                    context = context,
                                    filePath = item.realFilePath,
                                    title = item.title,
                                    mediaType = item.mediaType
                                )
                            } else {
                                onShare(item)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_media_file_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Media", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerView(file: File) {
    var isPlaying by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.fromFile(file))
                    val mediaController = MediaController(ctx)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Play/Pause button if touched
        IconButton(
            onClick = {
                videoViewRef?.let { vv ->
                    if (vv.isPlaying) {
                        vv.pause()
                        isPlaying = false
                    } else {
                        vv.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }
}

@Composable
private fun AudioPlayerView(file: File, item: DownloadItem) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var totalDuration by remember { mutableFloatStateOf(1f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        val player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                totalDuration = duration.toFloat().coerceAtLeast(1f)
                start()
                isPlaying = true
            } catch (_: Exception) {}
        }
        mediaPlayer = player

        onDispose {
            try {
                player.stop()
                player.release()
            } catch (_: Exception) {}
        }
    }

    // Progress updater
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentPosition = it.currentPosition.toFloat()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Audio Disc / Animated Ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(GrabItIndigo.copy(alpha = 0.25f))
                .border(3.dp, GrabItCoral, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = GrabItCoral,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = item.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        Text(
            text = item.author.ifEmpty { "High Quality Audio" },
            fontSize = 13.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Audio slider
        Slider(
            value = (currentPosition / totalDuration).coerceIn(0f, 1f),
            onValueChange = { frac ->
                val newPos = (frac * totalDuration).toInt()
                mediaPlayer?.seekTo(newPos)
                currentPosition = newPos.toFloat()
            },
            colors = SliderDefaults.colors(
                thumbColor = GrabItCoral,
                activeTrackColor = GrabItCoral
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Play / Pause Toggle
        IconButton(
            onClick = {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.pause()
                        isPlaying = false
                    } else {
                        mp.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GrabItCoral)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PhotoViewer(file: File) {
    val bitmap = remember(file) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Downloaded Photo",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        )
    } else {
        Text("Unable to render image bitmap", color = Color.White)
    }
}

@Composable
private fun MediaPlaceholderView(item: DownloadItem, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = when (item.mediaType) {
                MediaType.VIDEO -> Icons.Default.Videocam
                MediaType.AUDIO -> Icons.Default.Audiotrack
                MediaType.PHOTO -> Icons.Default.Photo
            },
            contentDescription = null,
            tint = GrabItCoral,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = item.title,
            color = Color.LightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
