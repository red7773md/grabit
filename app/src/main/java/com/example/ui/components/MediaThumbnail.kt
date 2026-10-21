package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DownloadItem
import com.example.model.MediaType
import java.io.File

@Composable
fun MediaThumbnail(
    item: DownloadItem,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 75.dp,
    showDuration: Boolean = true,
    showPlatformBadge: Boolean = true
) {
    val gradients = when (item.mediaType) {
        MediaType.VIDEO -> listOf(
            listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
            listOf(Color(0xFF4C1D95), Color(0xFF1E1B4B)),
            listOf(Color(0xFF881337), Color(0xFF4C0519)),
            listOf(Color(0xFF1F1C2C), Color(0xFF312E81))
        )
        MediaType.AUDIO -> listOf(
            listOf(Color(0xFF065F46), Color(0xFF064E3B)),
            listOf(Color(0xFF9A3412), Color(0xFF431407)),
            listOf(Color(0xFF5B21B6), Color(0xFF2E1065)),
            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
        MediaType.PHOTO -> listOf(
            listOf(Color(0xFF0369A1), Color(0xFF0C4A6E)),
            listOf(Color(0xFF9D174D), Color(0xFF500724)),
            listOf(Color(0xFFB45309), Color(0xFF451A03)),
            listOf(Color(0xFF3730A3), Color(0xFF1E1B4B))
        )
    }

    val selectedColors = gradients[(item.thumbnailSeed and 0x7FFFFFFF) % gradients.size]
    val localPhotoFile = if (item.mediaType == MediaType.PHOTO && item.realFilePath.isNotBlank()) {
        File(item.realFilePath).takeIf { it.exists() }
    } else null
    val imageModel: Any? = localPhotoFile ?: item.thumbnailUrl.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(selectedColors))
    ) {
        // Ambient geometric canvas backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, h * 0.3f),
                end = Offset(w, h * 0.7f),
                strokeWidth = 2.dp.toPx()
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = w * 0.28f,
                center = Offset(w * 0.75f, h * 0.35f)
            )
        }

        // Real network thumbnail or local photo file via Coil
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dark scrim so overlay icons stay readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
            )
        }

        // Center Media Icon Indicator
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            when (item.mediaType) {
                MediaType.VIDEO -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                MediaType.AUDIO -> Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Audio",
                    tint = Color(0xFF2ED573),
                    modifier = Modifier.size(16.dp)
                )
                MediaType.PHOTO -> Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Photo",
                    tint = Color(0xFF70A1FF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Top Left Platform Badge
        if (showPlatformBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp),
                shape = RoundedCornerShape(4.dp),
                color = item.platform.brandColor.copy(alpha = 0.92f)
            ) {
                Text(
                    text = item.platform.displayName.take(9),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = item.platform.textColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Bottom Right Duration or Format Tag
        if (showDuration) {
            val label = when (item.mediaType) {
                MediaType.VIDEO -> item.duration ?: "VIDEO"
                MediaType.AUDIO -> item.duration ?: "AUDIO"
                MediaType.PHOTO -> "IMG"
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
