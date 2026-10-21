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
            subtitle = "Download videos, reels, music tracks, and photos directly into your private offline vault with live speed telemetry.",
            icon = Icons.Default.CloudDownload,
            badge = "UNIVERSAL VAULT",
            accentColors = listOf(GrabItCoral, GrabItCoralLight, Color(0xFFFF7675))
        ),
        OnboardingSlide(
            title = "Supported Platforms",
            subtitle = "Built-in stream engines for YouTube, YouTube Music, Facebook, Twitter/X, Instagram, TikTok, Reddit, Pinterest, and SoundCloud.",
            icon = Icons.Default.Language,
            badge = "10+ PLATFORMS",
            accentColors = listOf(GrabItIndigo, GrabItIndigoLight, Color(0xFF6C5CE7))
        ),
        OnboardingSlide(
            title = "Videos, Music & Photos",
            subtitle = "Choose your preferred capture format: crystal-clear 1080p Video, rich 320kbps MP3 Audio / Soundtracks, or Original High-Res Photos.",
            icon = Icons.Default.Audiotrack,
            badge = "ALL FORMATS",
            accentColors = listOf(GrabItTeal, GrabItTealLight, Color(0xFF00CEC9))
        ),
        OnboardingSlide(
            title = "Instant Share Sheet & Folders",
            subtitle = "Tap Share in YouTube, Instagram, X, or any browser and pick GrabIt to start downloading instantly, auto-sorted into clean device folders.",
            icon = Icons.Default.FolderSpecial,
            badge = "SMART STORAGE",
            accentColors = listOf(GrabItCoral, GrabItIndigo, Color(0xFFE65100))
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (currentPage == 1) {
                    // Dedicated Platforms Showcase Grid for Slide 1
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "VERIFIED COMPATIBILITY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = GrabItIndigoLight
                            )
                            val showcasePlatforms = listOf(
                                "YouTube" to Color(0xFFFF0000),
                                "YT Music" to Color(0xFFCC0000),
                                "Instagram" to Color(0xFFE1306C),
                                "TikTok" to Color(0xFF00F2FE),
                                "Twitter / X" to Color(0xFF1DA1F2),
                                "Facebook" to Color(0xFF1877F2),
                                "Reddit" to Color(0xFFFF4500),
                                "SoundCloud" to Color(0xFFFF5500)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                showcasePlatforms.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { (name, color) ->
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                color = color.copy(alpha = 0.15f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(color)
                                                    )
                                                    Text(
                                                        text = name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Feature Hero Badge & Graphic
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Brush.linearGradient(currentSlide.accentColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentSlide.icon,
                                contentDescription = currentSlide.title,
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
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
