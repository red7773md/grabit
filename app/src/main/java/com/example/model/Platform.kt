package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class Platform(
    val displayName: String,
    val brandColor: Color,
    val textColor: Color = Color.White,
    val domainKeywords: List<String>,
    val defaultAuthor: String,
    val subfolder: String,
    val exampleUrl: String,
    val supportedUrlPatterns: List<String>,
    val supportedTypes: List<MediaType> = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
    val description: String = ""
) {
    YT_MUSIC(
        displayName = "YouTube Music",
        brandColor = Color(0xFFFF0000),
        textColor = Color.White,
        domainKeywords = listOf("music.youtube.com"),
        defaultAuthor = "YouTube Music Artist",
        subfolder = "YouTube_Music",
        exampleUrl = "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
        supportedUrlPatterns = listOf(
            "music.youtube.com/watch?v={id}",
            "music.youtube.com/playlist?list={id}"
        ),
        supportedTypes = listOf(MediaType.AUDIO, MediaType.VIDEO, MediaType.PHOTO),
        description = "High-fidelity audio tracks, video clips, and full-resolution album artwork."
    ),
    YOUTUBE(
        displayName = "YouTube",
        brandColor = PlatformYouTube,
        textColor = Color.White,
        domainKeywords = listOf("youtube.com", "youtu.be"),
        defaultAuthor = "@YouTubeCreator",
        subfolder = "YouTube",
        exampleUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        supportedUrlPatterns = listOf(
            "youtube.com/watch?v={id}",
            "youtu.be/{id}",
            "youtube.com/shorts/{id}",
            "youtube.com/embed/{id}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Standard videos, Shorts, audio extractions, and 1080p MaxRes thumbnails."
    ),
    FACEBOOK(
        displayName = "Facebook",
        brandColor = PlatformFacebook,
        textColor = Color.White,
        domainKeywords = listOf("facebook.com", "fb.watch", "fb.com"),
        defaultAuthor = "Facebook Watch",
        subfolder = "Facebook",
        exampleUrl = "https://www.facebook.com/watch?v=10153231379946729",
        supportedUrlPatterns = listOf(
            "facebook.com/watch?v={id}",
            "fb.watch/{id}",
            "facebook.com/reel/{id}",
            "facebook.com/{user}/videos/{id}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Public Watch clips, Reels, Facebook video posts, and photo posts."
    ),
    TWITTER_X(
        displayName = "Twitter / X",
        brandColor = Color(0xFF14171A),
        textColor = PlatformTwitterX,
        domainKeywords = listOf("twitter.com", "x.com", "t.co"),
        defaultAuthor = "@x_creator",
        subfolder = "Twitter_X",
        exampleUrl = "https://x.com/SpaceX/status/1780775317769941195",
        supportedUrlPatterns = listOf(
            "x.com/{user}/status/{id}",
            "twitter.com/{user}/status/{id}",
            "t.co/{shortId}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Embedded post videos, animated GIFs, audio clips, and original photos."
    ),
    INSTAGRAM(
        displayName = "Instagram",
        brandColor = PlatformInstagram,
        textColor = Color.White,
        domainKeywords = listOf("instagram.com", "instagr.am"),
        defaultAuthor = "@insta.creator",
        subfolder = "Instagram",
        exampleUrl = "https://www.instagram.com/reel/C3x9qYVrfmX/",
        supportedUrlPatterns = listOf(
            "instagram.com/reel/{id}",
            "instagram.com/p/{id}",
            "instagram.com/tv/{id}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Public Reels, video posts, carousel photos, and cover images."
    ),
    TIKTOK(
        displayName = "TikTok",
        brandColor = Color(0xFF010101),
        textColor = Color(0xFF00F2FE),
        domainKeywords = listOf("tiktok.com", "vm.tiktok.com"),
        defaultAuthor = "@tiktok_star",
        subfolder = "TikTok",
        exampleUrl = "https://www.tiktok.com/@scout2015/video/6718335390845095173",
        supportedUrlPatterns = listOf(
            "tiktok.com/@{user}/video/{id}",
            "vm.tiktok.com/{shortId}",
            "tiktok.com/t/{shortId}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Direct watermark-free MP4 video downloads, sound audio tracks, and cover art."
    ),
    REDDIT(
        displayName = "Reddit",
        brandColor = PlatformReddit,
        textColor = Color.White,
        domainKeywords = listOf("reddit.com", "redd.it", "v.redd.it"),
        defaultAuthor = "r/videos",
        subfolder = "Reddit",
        exampleUrl = "https://www.reddit.com/r/videos/comments/64293c/popular_video/",
        supportedUrlPatterns = listOf(
            "reddit.com/r/{sub}/comments/{id}/...",
            "redd.it/{id}",
            "v.redd.it/{id}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Reddit-hosted videos (v.redd.it), GIF animations, and image posts."
    ),
    PINTEREST(
        displayName = "Pinterest",
        brandColor = PlatformPinterest,
        textColor = Color.White,
        domainKeywords = listOf("pinterest.com", "pin.it"),
        defaultAuthor = "Pinterest Pin",
        subfolder = "Pinterest",
        exampleUrl = "https://www.pinterest.com/pin/123456789012345678/",
        supportedUrlPatterns = listOf(
            "pinterest.com/pin/{id}",
            "pin.it/{shortId}"
        ),
        supportedTypes = listOf(MediaType.PHOTO, MediaType.VIDEO),
        description = "Original full-resolution Pinterest pins, video pins, and idea pins."
    ),
    SOUNDCLOUD(
        displayName = "SoundCloud",
        brandColor = PlatformSoundCloud,
        textColor = Color.White,
        domainKeywords = listOf("soundcloud.com", "on.soundcloud.com", "snd.sc"),
        defaultAuthor = "SoundCloud Artist",
        subfolder = "SoundCloud",
        exampleUrl = "https://soundcloud.com/octobersveryown/drake-gods-plan",
        supportedUrlPatterns = listOf(
            "soundcloud.com/{artist}/{track}",
            "on.soundcloud.com/{shortId}"
        ),
        supportedTypes = listOf(MediaType.AUDIO, MediaType.PHOTO),
        description = "Full audio tracks, podcast episodes, and high-res album artwork."
    ),
    VIMEO(
        displayName = "Vimeo",
        brandColor = Color(0xFF1AB7EA),
        textColor = Color.White,
        domainKeywords = listOf("vimeo.com"),
        defaultAuthor = "Vimeo Creator",
        subfolder = "Vimeo",
        exampleUrl = "https://vimeo.com/76979871",
        supportedUrlPatterns = listOf(
            "vimeo.com/{id}",
            "player.vimeo.com/video/{id}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Cinematic HD video streams, progressive MP4 files, and poster stills."
    ),
    SNAPCHAT(
        displayName = "Snapchat",
        brandColor = PlatformSnapchat,
        textColor = PlatformSnapchatText,
        domainKeywords = listOf("snapchat.com", "t.snapchat.com"),
        defaultAuthor = "Snapchat Spotlight",
        subfolder = "Snapchat",
        exampleUrl = "https://www.snapchat.com/spotlight/W7_EDxX0RIm-46W0_o8i9AAAg",
        supportedUrlPatterns = listOf(
            "snapchat.com/spotlight/{id}",
            "t.snapchat.com/{shortId}"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.PHOTO),
        description = "Public Spotlight videos and Story media."
    ),
    OTHER(
        displayName = "Direct Web Media",
        brandColor = GrabItIndigo,
        textColor = Color.White,
        domainKeywords = emptyList(),
        defaultAuthor = "Web Media",
        subfolder = "Other",
        exampleUrl = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
        supportedUrlPatterns = listOf(
            "https://.../*.mp4",
            "https://.../*.mp3",
            "https://.../*.m4a",
            "https://.../*.webm",
            "https://.../*.jpg",
            "https://.../*.png"
        ),
        supportedTypes = listOf(MediaType.VIDEO, MediaType.AUDIO, MediaType.PHOTO),
        description = "Any direct media link over HTTP/HTTPS from servers, CDNs, podcasts, or cloud storage."
    );

    companion object {
        fun detectFromUrl(url: String): Platform {
            val lower = url.lowercase().trim()
            if (lower.isEmpty()) return OTHER

            val host = try {
                val normalized = if (lower.startsWith("http://") || lower.startsWith("https://")) {
                    lower
                } else {
                    "https://$lower"
                }
                java.net.URI(normalized).host?.lowercase().orEmpty()
            } catch (_: Exception) {
                ""
            }

            // Check YT_MUSIC first before YOUTUBE since domain ends with youtube.com
            if (host == "music.youtube.com" || lower.contains("music.youtube.com")) {
                return YT_MUSIC
            }

            if (host.isNotBlank()) {
                entries.firstOrNull { platform ->
                    platform != OTHER && platform.domainKeywords.any { keyword ->
                        host == keyword || host.endsWith(".$keyword")
                    }
                }?.let { return it }
            }

            return entries.firstOrNull { platform ->
                platform != OTHER && platform.domainKeywords.any { keyword ->
                    lower.contains("://$keyword/") || lower.contains(".$keyword/")
                }
            } ?: OTHER
        }
    }
}
