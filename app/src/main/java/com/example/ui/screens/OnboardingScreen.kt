package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String,
    val accentColors: List<Color>
)

@Composable
fun OnboardingScreen(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slides = listOf(
        OnboardingSlide(
            title = "Grab Any Social Media",
            subtitle = "Download videos, reels, stories, music, and photos directly from YouTube, Instagram, TikTok, Facebook, Twitter/X, Reddit, Pinterest, and Snapchat.",
            icon = Icons.Default.CloudDownload,
            badge = "8+ PLATFORMS",
            accentColors = listOf(GrabItCoral, GrabItCoralLight, Color(0xFFFF7675))
        ),
        OnboardingSlide(
            title = "Videos, Music & Photos",
            subtitle = "Choose exactly what you want: crystal-clear 1080p/4K Video, rich 320kbps MP3 Audio / Music soundtracks, or Original High-Res Photos.",
            icon = Icons.Default.Audiotrack,
            badge = "ALL FORMATS",
            accentColors = listOf(GrabItIndigo, GrabItIndigoLight, Color(0xFF6C5CE7))
        ),
        OnboardingSlide(
            title = "Auto-Organized Folders",
            subtitle = "Share any link to GrabIt or paste directly. Media is automatically sorted into clean local folders on your device ready for offline enjoyment.",
            icon = Icons.Default.FolderSpecial,
            badge = "SMART STORAGE",
            accentColors = listOf(GrabItTeal, GrabItTealLight, Color(0xFF00CEC9))
        )
    )

    val currentSlide = slides[currentPage.coerceIn(0, slides.size - 1)]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentPage < slides.size - 1) {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text(
                            text = "Skip",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Center Visual & Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Feature Hero Badge & Graphic
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .background(Brush.linearGradient(currentSlide.accentColors)),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer decorative rings
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentSlide.icon,
                            contentDescription = currentSlide.title,
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                // Badge Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = currentSlide.accentColors.first().copy(alpha = 0.18f)
                ) {
                    Text(
                        text = currentSlide.badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = currentSlide.accentColors.first(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Titles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = currentSlide.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = currentSlide.subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom Navigation & Indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.indices.forEach { index ->
                        val isSelected = index == currentPage
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) currentSlide.accentColors.first()
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                // Next / Get Started Button
                Button(
                    onClick = {
                        if (currentPage < slides.size - 1) {
                            onPageChange(currentPage + 1)
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_action_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentSlide.accentColors.first()
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentPage < slides.size - 1) "Next" else "Get Started with GrabIt",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
