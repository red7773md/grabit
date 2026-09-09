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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DownloadItem
import com.example.model.MediaType

@Composable
fun MediaThumbnail(
    item: DownloadItem,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 75.dp,
    showDuration: Boolean = true,
    showPlatformBadge: Boolean = true
) {
    // Generate distinct gradient backgrounds based on seed and media type
    val gradients = when (item.mediaType) {
        MediaType.VIDEO -> listOf(
            listOf(Color(0xFF2C3E50), Color(0xFF000000)),
            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
            listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
        )
        MediaType.AUDIO -> listOf(
            listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            listOf(Color(0xFFF12711), Color(0xFFF5AF19)),
            listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)),
            listOf(Color(0xFF200122), Color(0xFF6F0000))
        )
        MediaType.PHOTO -> listOf(
            listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
            listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),
            listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
            listOf(Color(0xFF5614B0), Color(0xFFDBD65C))
        )
    }

    val selectedColors = gradients[item.thumbnailSeed % gradients.size]

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(selectedColors))
    ) {
        // Decorative geometric shapes/waves inside thumbnail
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Diagonal ambient lines
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, h * 0.3f),
                end = Offset(w, h * 0.7f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(w * 0.2f, 0f),
                end = Offset(w * 0.8f, h),
                strokeWidth = 3.dp.toPx()
            )

            // Accent circle glow
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = w * 0.28f,
                center = Offset(w * 0.75f, h * 0.35f)
            )
        }

        // Center Media Icon Indicator
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
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
                color = item.platform.brandColor
            ) {
                Text(
                    text = item.platform.displayName.take(8),
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
                MediaType.VIDEO -> item.duration ?: "0:30"
                MediaType.AUDIO -> item.duration ?: "MP3"
                MediaType.PHOTO -> "HD"
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
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
