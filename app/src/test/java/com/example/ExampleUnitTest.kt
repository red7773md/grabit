package com.example

import com.example.data.util.MediaFileManager
import com.example.model.MediaType
import com.example.model.Platform
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testPlatformDetectionForAllServices() {
    assertEquals(Platform.YOUTUBE, Platform.detectFromUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals(Platform.YOUTUBE, Platform.detectFromUrl("https://youtu.be/dQw4w9WgXcQ"))
    assertEquals(Platform.YOUTUBE, Platform.detectFromUrl("https://www.youtube.com/shorts/5O_R1_kZg28"))
    assertEquals(Platform.YT_MUSIC, Platform.detectFromUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals(Platform.FACEBOOK, Platform.detectFromUrl("https://www.facebook.com/watch?v=10153231379946729"))
    assertEquals(Platform.FACEBOOK, Platform.detectFromUrl("https://fb.watch/xyz123/"))
    assertEquals(Platform.TWITTER_X, Platform.detectFromUrl("https://x.com/SpaceX/status/1780775317769941195"))
    assertEquals(Platform.TWITTER_X, Platform.detectFromUrl("https://twitter.com/NASA/status/123456789"))
    assertEquals(Platform.INSTAGRAM, Platform.detectFromUrl("https://www.instagram.com/reel/C3x9qYVrfmX/"))
    assertEquals(Platform.INSTAGRAM, Platform.detectFromUrl("https://www.instagram.com/p/C3x9qYVrfmX/"))
    assertEquals(Platform.TIKTOK, Platform.detectFromUrl("https://www.tiktok.com/@scout2015/video/6718335390845095173"))
    assertEquals(Platform.REDDIT, Platform.detectFromUrl("https://www.reddit.com/r/videos/comments/64293c/popular_video/"))
    assertEquals(Platform.PINTEREST, Platform.detectFromUrl("https://www.pinterest.com/pin/1234567890/"))
    assertEquals(Platform.SOUNDCLOUD, Platform.detectFromUrl("https://soundcloud.com/octobersveryown/drake-gods-plan"))
    assertEquals(Platform.VIMEO, Platform.detectFromUrl("https://vimeo.com/76979871"))
    assertEquals(Platform.OTHER, Platform.detectFromUrl("https://example.com/videos/nature.mp4"))
  }

  @Test
  fun testYouTubeVideoIdExtraction() {
    assertEquals("dQw4w9WgXcQ", MediaFileManager.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", MediaFileManager.extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ"))
    assertEquals("dQw4w9WgXcQ", MediaFileManager.extractYouTubeVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals("5O_R1_kZg28", MediaFileManager.extractYouTubeVideoId("https://www.youtube.com/shorts/5O_R1_kZg28"))
  }

  @Test
  fun testDirectMediaDetection() {
    assertTrue(MediaFileManager.isDirectMediaUrl("https://example.com/sample.mp4"))
    assertTrue(MediaFileManager.isDirectMediaUrl("https://example.com/audio/song.mp3"))
    assertTrue(MediaFileManager.isDirectMediaUrl("https://example.com/photo.jpg"))
    assertTrue(MediaFileManager.isDirectAudioUrl("https://example.com/track.mp3"))
    assertFalse(MediaFileManager.isDirectMediaUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
  }

  @Test
  fun testUrlNormalization() {
    assertEquals("https://example.com/file.mp4", MediaFileManager.normalizeUrl("Check this out: https://example.com/file.mp4 from friend"))
    assertEquals("https://youtube.com", MediaFileManager.normalizeUrl("youtube.com"))
  }
}

