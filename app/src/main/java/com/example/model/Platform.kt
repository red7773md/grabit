package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class Platform(
    val displayName: String,
    val brandColor: Color,
    val textColor: Color = Color.White,
    val domainKeywords: List<String>,
    val defaultAuthor: String,
    val subfolder: String
) {
    YOUTUBE(
        displayName = "YouTube",
        brandColor = PlatformYouTube,
        textColor = Color.White,
        domainKeywords = listOf("youtube.com", "youtu.be"),
        defaultAuthor = "@YouTubeCreator",
        subfolder = "YouTube"
    ),
    INSTAGRAM(
        displayName = "Instagram",
        brandColor = PlatformInstagram,
        textColor = Color.White,
        domainKeywords = listOf("instagram.com", "instagr.am"),
        defaultAuthor = "@insta.creator",
        subfolder = "Instagram"
    ),
    TIKTOK(
        displayName = "TikTok",
        brandColor = Color(0xFF010101),
        textColor = Color(0xFF00F2FE),
        domainKeywords = listOf("tiktok.com", "vm.tiktok.com"),
        defaultAuthor = "@tiktok_star",
        subfolder = "TikTok"
    ),
    TWITTER_X(
        displayName = "Twitter / X",
        brandColor = Color(0xFF14171A),
        textColor = PlatformTwitterX,
        domainKeywords = listOf("twitter.com", "x.com", "t.co"),
        defaultAuthor = "@x_creator",
        subfolder = "Twitter_X"
    ),
    FACEBOOK(
        displayName = "Facebook",
        brandColor = PlatformFacebook,
        textColor = Color.White,
        domainKeywords = listOf("facebook.com", "fb.watch", "fb.com"),
        defaultAuthor = "Facebook Watch",
        subfolder = "Facebook"
    ),
    REDDIT(
        displayName = "Reddit",
        brandColor = PlatformReddit,
        textColor = Color.White,
        domainKeywords = listOf("reddit.com", "redd.it", "v.redd.it"),
        defaultAuthor = "r/videos",
        subfolder = "Reddit"
    ),
    PINTEREST(
        displayName = "Pinterest",
        brandColor = PlatformPinterest,
        textColor = Color.White,
        domainKeywords = listOf("pinterest.com", "pin.it"),
        defaultAuthor = "Pinterest Pin",
        subfolder = "Pinterest"
    ),
    SNAPCHAT(
        displayName = "Snapchat",
        brandColor = PlatformSnapchat,
        textColor = PlatformSnapchatText,
        domainKeywords = listOf("snapchat.com", "t.snapchat.com"),
        defaultAuthor = "Snapchat Spotlight",
        subfolder = "Snapchat"
    ),
    OTHER(
        displayName = "Web Media",
        brandColor = GrabItIndigo,
        textColor = Color.White,
        domainKeywords = emptyList(),
        defaultAuthor = "Web Media",
        subfolder = "Other"
    );

    companion object {
        fun detectFromUrl(url: String): Platform {
            val lower = url.lowercase().trim()
            if (lower.isEmpty()) return OTHER
            return entries.firstOrNull { platform ->
                platform.domainKeywords.any { keyword -> lower.contains(keyword) }
            } ?: OTHER
        }
    }
}
