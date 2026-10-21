package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesManager
import com.example.data.util.MediaFileManager
import com.example.model.Platform
import com.example.viewmodel.GrabItViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GrabIt", appName)
    }

    @Test
    fun `url normalization and validation works for shared text and links`() {
        assertTrue(MediaFileManager.isValidHttpUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(MediaFileManager.isValidHttpUrl("Check this out! https://instagram.com/reel/abc123 more text"))
        assertEquals(
            "https://instagram.com/reel/abc123",
            MediaFileManager.normalizeUrl("Check this out! https://instagram.com/reel/abc123 more text")
        )
        assertFalse(MediaFileManager.isValidHttpUrl("not a url at all"))
    }

    @Test
    fun `platform detection identifies youtube, yt music, facebook, twitter, instagram and more`() {
        assertEquals(
            Platform.YT_MUSIC,
            Platform.detectFromUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            Platform.YOUTUBE,
            Platform.detectFromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
        assertEquals(
            Platform.YOUTUBE,
            Platform.detectFromUrl("https://youtu.be/jNQXAC9IVRw")
        )
        assertEquals(
            Platform.FACEBOOK,
            Platform.detectFromUrl("https://www.facebook.com/watch?v=12345678")
        )
        assertEquals(
            Platform.TWITTER_X,
            Platform.detectFromUrl("https://x.com/SpaceX/status/1780775317769941195")
        )
        assertEquals(
            Platform.INSTAGRAM,
            Platform.detectFromUrl("https://www.instagram.com/reel/C3x9qYVrfmX/")
        )
        assertEquals(
            Platform.TIKTOK,
            Platform.detectFromUrl("https://www.tiktok.com/@scout2015/video/6718335390845095173")
        )
        assertEquals(
            Platform.SOUNDCLOUD,
            Platform.detectFromUrl("https://soundcloud.com/octobersveryown/drake-gods-plan")
        )
        assertEquals(
            Platform.VIMEO,
            Platform.detectFromUrl("https://vimeo.com/76979871")
        )
        assertEquals(
            Platform.OTHER,
            Platform.detectFromUrl("https://commondatastorage.googleapis.com/sample/video.mp4")
        )
    }

    @Test
    fun `every supported platform in about directory has example and supported url patterns`() {
        Platform.entries.forEach { platform ->
            assertTrue(
                "Platform ${platform.name} must have a valid non-empty displayName",
                platform.displayName.isNotBlank()
            )
            assertTrue(
                "Platform ${platform.name} must have an example URL",
                platform.exampleUrl.startsWith("http")
            )
            assertTrue(
                "Platform ${platform.name} must list supported URL patterns",
                platform.supportedUrlPatterns.isNotEmpty()
            )
            assertTrue(
                "Platform ${platform.name} must list supported media types",
                platform.supportedTypes.isNotEmpty()
            )
        }
    }

    @Test
    fun `youtube video id extractor parses watch shorts youtube and embed urls`() {
        assertEquals("dQw4w9WgXcQ", MediaFileManager.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("jNQXAC9IVRw", MediaFileManager.extractYouTubeVideoId("https://youtu.be/jNQXAC9IVRw"))
        assertEquals("sample123", MediaFileManager.extractYouTubeVideoId("https://www.youtube.com/shorts/sample123?feature=share"))
        assertEquals("embed456", MediaFileManager.extractYouTubeVideoId("https://www.youtube.com/embed/embed456"))
        assertEquals("dQw4w9WgXcQ", MediaFileManager.extractYouTubeVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun `preferences manager persists settings and onboarding state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        prefs.setHasSeenOnboarding(true)
        assertTrue(prefs.hasSeenOnboarding())

        val initial = prefs.loadSettings()
        prefs.saveSettings(initial.copy(wifiOnly = true, maxConcurrentDownloads = 5, defaultVideoQuality = "4K Ultra"))

        val reloaded = PreferencesManager(context).loadSettings()
        assertTrue(reloaded.wifiOnly)
        assertEquals(5, reloaded.maxConcurrentDownloads)
        assertEquals("4K Ultra", reloaded.defaultVideoQuality)
    }

    @Test
    fun `viewmodel starts with zero dummy items and detects clipboard links`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        PreferencesManager(app).setHasSeenOnboarding(true)

        val viewModel = GrabItViewModel(app)
        val initialState = viewModel.uiState.value

        assertTrue(initialState.activeDownloads.isEmpty())
        assertTrue(initialState.historyItems.isEmpty())

        viewModel.evaluateClipboardText("https://www.tiktok.com/@creator/video/987654321")
        val afterClip = viewModel.uiState.value
        assertEquals("https://www.tiktok.com/@creator/video/987654321", afterClip.detectedClipboardUrl)
        assertEquals(Platform.TIKTOK, afterClip.detectedClipboardPlatform)

        viewModel.dismissClipboardSuggestion()
        assertNull(viewModel.uiState.value.detectedClipboardUrl)
    }
}
