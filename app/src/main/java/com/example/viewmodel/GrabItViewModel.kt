package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GrabItUiState(
    val activeDownloads: List<DownloadItem> = emptyList(),
    val recentCompletions: List<DownloadItem> = emptyList(),
    val historyItems: List<DownloadItem> = emptyList(),
    val searchQuery: String = "",
    val selectedPlatformFilter: Platform? = null,
    val selectedMediaTypeFilter: MediaType? = null,
    val isGridLayout: Boolean = false,
    val inputUrl: String = "",
    val detectedPlatform: Platform? = null,
    val selectedInputMediaType: MediaType = MediaType.VIDEO,
    val settings: AppSettings = AppSettings(),
    val showShareConfirmation: Boolean = false,
    val shareConfirmationItem: DownloadItem? = null,
    val showProgressModal: Boolean = false,
    val activeDetailItem: DownloadItem? = null,
    val showFolderPickerDialog: Boolean = false,
    val hasSeenOnboarding: Boolean = true, // default completed or can replay
    val currentOnboardingPage: Int = 0,
    val isDarkTheme: Boolean = true,
    val snackbarMessage: String? = null
)

class GrabItViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GrabItUiState())
    val uiState: StateFlow<GrabItUiState> = _uiState.asStateFlow()

    // Map of download jobs for active progression simulation
    private val downloadJobs = mutableMapOf<String, Job>()

    init {
        loadInitialSampleData()
    }

    private fun loadInitialSampleData() {
        val sampleCompletions = listOf(
            DownloadItem(
                id = "comp_1",
                title = "Neon Tokyo Night Walk in 4K HDR",
                sourceUrl = "https://youtube.com/watch?v=sample1",
                platform = Platform.YOUTUBE,
                mediaType = MediaType.VIDEO,
                quality = "1080p FHD",
                fileSize = 142 * 1024 * 1024L,
                downloadedBytes = 142 * 1024 * 1024L,
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                downloadSpeed = "0 KB/s",
                timeRemaining = "Finished",
                localPath = "GrabIt/YouTube/Videos/tokyo_night_walk.mp4",
                duration = "14:20",
                author = "@CityWalks",
                thumbnailSeed = 0
            ),
            DownloadItem(
                id = "comp_2",
                title = "Cyberpunk Synthwave Chill Session",
                sourceUrl = "https://youtube.com/watch?v=music1",
                platform = Platform.YOUTUBE,
                mediaType = MediaType.AUDIO,
                quality = "320 kbps MP3",
                fileSize = 12 * 1024 * 1024L,
                downloadedBytes = 12 * 1024 * 1024L,
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                downloadSpeed = "0 KB/s",
                timeRemaining = "Finished",
                localPath = "GrabIt/YouTube/Audio/synthwave_chill.mp3",
                duration = "4:55",
                author = "@RetroWave",
                thumbnailSeed = 1
            ),
            DownloadItem(
                id = "comp_3",
                title = "Summer Golden Hour Sunset Reel",
                sourceUrl = "https://instagram.com/reel/sample2",
                platform = Platform.INSTAGRAM,
                mediaType = MediaType.VIDEO,
                quality = "1080p FHD",
                fileSize = 38 * 1024 * 1024L,
                downloadedBytes = 38 * 1024 * 1024L,
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                downloadSpeed = "0 KB/s",
                timeRemaining = "Finished",
                localPath = "GrabIt/Instagram/Videos/sunset_golden_hour.mp4",
                duration = "0:45",
                author = "@travel_nature",
                thumbnailSeed = 2
            ),
            DownloadItem(
                id = "comp_4",
                title = "Minimalist Architectural Photography",
                sourceUrl = "https://pinterest.com/pin/sample4",
                platform = Platform.PINTEREST,
                mediaType = MediaType.PHOTO,
                quality = "Original Quality",
                fileSize = 6 * 1024 * 1024L,
                downloadedBytes = 6 * 1024 * 1024L,
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                downloadSpeed = "0 KB/s",
                timeRemaining = "Finished",
                localPath = "GrabIt/Pinterest/Photos/minimal_architecture.jpg",
                duration = null,
                author = "@arch_digest",
                thumbnailSeed = 3
            ),
            DownloadItem(
                id = "comp_5",
                title = "Epic Street Dance Freestyle Viral",
                sourceUrl = "https://tiktok.com/@dancer/video/sample5",
                platform = Platform.TIKTOK,
                mediaType = MediaType.VIDEO,
                quality = "1080p FHD",
                fileSize = 24 * 1024 * 1024L,
                downloadedBytes = 24 * 1024 * 1024L,
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                downloadSpeed = "0 KB/s",
                timeRemaining = "Finished",
                localPath = "GrabIt/TikTok/Videos/street_dance.mp4",
                duration = "0:30",
                author = "@dance_groove",
                thumbnailSeed = 4
            ),
            DownloadItem(
                id = "comp_6",
                title = "Deep Space Galaxy High-Res Wallpapers",
                sourceUrl = "https://reddit.com/r/astrophotography/sample6",
                platform = Platform.REDDIT,
                mediaType = MediaType.PHOTO,
                quality = "Original Quality",
                fileSize = 9 * 1024 * 1024L,
                downloadedBytes = 9 * 1024 * 1024L,
                status = DownloadStatus.COMPLETED,
                progress = 1.0f,
                downloadSpeed = "0 KB/s",
                timeRemaining = "Finished",
                localPath = "GrabIt/Reddit/Photos/deep_space_galaxy.jpg",
                duration = null,
                author = "u/Stargazer",
                thumbnailSeed = 5
            )
        )

        val sampleActive = listOf(
            DownloadItem(
                id = "act_1",
                title = "Ultimate Lo-Fi Hip Hop Beats to Relax / Study",
                sourceUrl = "https://youtube.com/watch?v=lofi_study",
                platform = Platform.YOUTUBE,
                mediaType = MediaType.AUDIO,
                quality = "320 kbps MP3",
                fileSize = 48 * 1024 * 1024L,
                downloadedBytes = 26 * 1024 * 1024L,
                status = DownloadStatus.DOWNLOADING,
                progress = 0.54f,
                downloadSpeed = "4.8 MB/s",
                timeRemaining = "5s remaining",
                localPath = "GrabIt/YouTube/Audio/lofi_study.mp3",
                duration = "18:40",
                author = "@ChilledCow",
                thumbnailSeed = 6
            ),
            DownloadItem(
                id = "act_2",
                title = "Behind the Scenes: Film Cinema Drone Shots",
                sourceUrl = "https://instagram.com/reel/cinema_drone",
                platform = Platform.INSTAGRAM,
                mediaType = MediaType.VIDEO,
                quality = "1080p FHD",
                fileSize = 85 * 1024 * 1024L,
                downloadedBytes = 32 * 1024 * 1024L,
                status = DownloadStatus.DOWNLOADING,
                progress = 0.38f,
                downloadSpeed = "5.2 MB/s",
                timeRemaining = "11s remaining",
                localPath = "GrabIt/Instagram/Videos/cinema_drone.mp4",
                duration = "1:15",
                author = "@filmmakers_hub",
                thumbnailSeed = 7
            )
        )

        _uiState.update {
            it.copy(
                activeDownloads = sampleActive,
                recentCompletions = sampleCompletions.take(4),
                historyItems = sampleCompletions
            )
        }

        // Start background progression for active downloads
        sampleActive.forEach { startDownloadProgress(it.id) }
    }

    // URL input handling
    fun onUrlInputChanged(newUrl: String) {
        val detected = Platform.detectFromUrl(newUrl)
        _uiState.update {
            it.copy(
                inputUrl = newUrl,
                detectedPlatform = if (newUrl.isNotBlank()) detected else null
            )
        }
    }

    fun onInputMediaTypeChanged(mediaType: MediaType) {
        _uiState.update { it.copy(selectedInputMediaType = mediaType) }
    }

    fun pasteFromClipboard(text: String) {
        onUrlInputChanged(text)
    }

    fun clearInputUrl() {
        _uiState.update { it.copy(inputUrl = "", detectedPlatform = null) }
    }

    fun openShareConfirmationFromInput() {
        val url = _uiState.value.inputUrl.trim()
        if (url.isEmpty()) {
            showSnackbar("Please enter or paste a valid link first.")
            return
        }
        val platform = _uiState.value.detectedPlatform ?: Platform.detectFromUrl(url)
        val mediaType = _uiState.value.selectedInputMediaType
        val sampleTitle = generateSampleTitle(platform, mediaType, url)

        val candidate = DownloadItem(
            id = UUID.randomUUID().toString(),
            title = sampleTitle,
            sourceUrl = url,
            platform = platform,
            mediaType = mediaType,
            quality = when (mediaType) {
                MediaType.VIDEO -> _uiState.value.settings.defaultVideoQuality
                MediaType.AUDIO -> _uiState.value.settings.defaultAudioFormat
                MediaType.PHOTO -> _uiState.value.settings.defaultPhotoFormat
            },
            fileSize = when (mediaType) {
                MediaType.VIDEO -> 42 * 1024 * 1024L
                MediaType.AUDIO -> 9 * 1024 * 1024L
                MediaType.PHOTO -> 4 * 1024 * 1024L
            },
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED,
            progress = 0f,
            downloadSpeed = "Starting...",
            timeRemaining = "Calculating...",
            localPath = "GrabIt/${platform.subfolder}/${mediaType.subfolder}/",
            author = platform.defaultAuthor,
            thumbnailSeed = (0..9).random()
        )

        _uiState.update {
            it.copy(
                showShareConfirmation = true,
                shareConfirmationItem = candidate
            )
        }
    }

    fun openShareConfirmationWithUrl(sharedUrl: String) {
        onUrlInputChanged(sharedUrl)
        openShareConfirmationFromInput()
    }

    fun updateShareConfirmationItem(
        mediaType: MediaType,
        quality: String
    ) {
        _uiState.update { state ->
            val current = state.shareConfirmationItem ?: return@update state
            val newSize = when (mediaType) {
                MediaType.VIDEO -> if (quality.contains("4K")) 120 * 1024 * 1024L else 45 * 1024 * 1024L
                MediaType.AUDIO -> 8 * 1024 * 1024L
                MediaType.PHOTO -> 3 * 1024 * 1024L
            }
            state.copy(
                shareConfirmationItem = current.copy(
                    mediaType = mediaType,
                    quality = quality,
                    fileSize = newSize,
                    localPath = "GrabIt/${current.platform.subfolder}/${mediaType.subfolder}/"
                )
            )
        }
    }

    fun dismissShareConfirmation() {
        _uiState.update { it.copy(showShareConfirmation = false, shareConfirmationItem = null) }
    }

    fun confirmDownloadFromSheet() {
        val item = _uiState.value.shareConfirmationItem ?: return
        val newItem = item.copy(
            status = DownloadStatus.DOWNLOADING,
            downloadSpeed = "3.6 MB/s",
            timeRemaining = "12s left"
        )

        _uiState.update { state ->
            state.copy(
                activeDownloads = listOf(newItem) + state.activeDownloads,
                showShareConfirmation = false,
                shareConfirmationItem = null,
                inputUrl = "",
                detectedPlatform = null
            )
        }

        showSnackbar("Download started: ${newItem.title.take(24)}...")
        startDownloadProgress(newItem.id)
    }

    private fun startDownloadProgress(itemId: String) {
        downloadJobs[itemId]?.cancel()
        val job = viewModelScope.launch {
            while (true) {
                delay(800)
                var completedItem: DownloadItem? = null

                _uiState.update { state ->
                    val updatedActive = state.activeDownloads.map { download ->
                        if (download.id == itemId && download.status == DownloadStatus.DOWNLOADING) {
                            val newProg = (download.progress + 0.08f).coerceAtMost(1f)
                            val newBytes = (newProg * download.fileSize).toLong()
                            val speedMB = (3.5 + (0..20).random() / 10.0)
                            val remainingSec = ((1f - newProg) * (download.fileSize / (speedMB * 1024 * 1024))).toInt().coerceAtLeast(1)

                            if (newProg >= 1f) {
                                val finished = download.copy(
                                    progress = 1f,
                                    downloadedBytes = download.fileSize,
                                    status = DownloadStatus.COMPLETED,
                                    downloadSpeed = "0 KB/s",
                                    timeRemaining = "Finished"
                                )
                                completedItem = finished
                                finished
                            } else {
                                download.copy(
                                    progress = newProg,
                                    downloadedBytes = newBytes,
                                    downloadSpeed = String.format("%.1f MB/s", speedMB),
                                    timeRemaining = "${remainingSec}s left"
                                )
                            }
                        } else {
                            download
                        }
                    }

                    if (completedItem != null) {
                        val item = completedItem!!
                        val remainingActive = updatedActive.filter { it.id != itemId }
                        val updatedRecent = (listOf(item) + state.recentCompletions).distinctBy { it.id }.take(6)
                        val updatedHistory = (listOf(item) + state.historyItems).distinctBy { it.id }

                        // Also update activeDetailItem if open in modal
                        val newDetail = if (state.activeDetailItem?.id == itemId) item else state.activeDetailItem

                        state.copy(
                            activeDownloads = remainingActive,
                            recentCompletions = updatedRecent,
                            historyItems = updatedHistory,
                            activeDetailItem = newDetail
                        )
                    } else {
                        val currentDetail = state.activeDetailItem
                        val newDetail = if (currentDetail?.id == itemId) {
                            updatedActive.firstOrNull { it.id == itemId } ?: currentDetail
                        } else currentDetail

                        state.copy(
                            activeDownloads = updatedActive,
                            activeDetailItem = newDetail
                        )
                    }
                }

                if (completedItem != null) {
                    showSnackbar("Downloaded: ${completedItem?.title?.take(24)} (${completedItem?.quality})")
                    break
                }
            }
        }
        downloadJobs[itemId] = job
    }

    fun pauseDownload(id: String) {
        downloadJobs[id]?.cancel()
        _uiState.update { state ->
            val updated = state.activeDownloads.map {
                if (it.id == id) it.copy(status = DownloadStatus.PAUSED, downloadSpeed = "Paused") else it
            }
            state.copy(
                activeDownloads = updated,
                activeDetailItem = if (state.activeDetailItem?.id == id) {
                    updated.firstOrNull { it.id == id } ?: state.activeDetailItem
                } else state.activeDetailItem
            )
        }
    }

    fun resumeDownload(id: String) {
        _uiState.update { state ->
            val updated = state.activeDownloads.map {
                if (it.id == id) it.copy(status = DownloadStatus.DOWNLOADING, downloadSpeed = "Resuming...") else it
            }
            state.copy(
                activeDownloads = updated,
                activeDetailItem = if (state.activeDetailItem?.id == id) {
                    updated.firstOrNull { it.id == id } ?: state.activeDetailItem
                } else state.activeDetailItem
            )
        }
        startDownloadProgress(id)
    }

    fun cancelDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        _uiState.update { state ->
            state.copy(
                activeDownloads = state.activeDownloads.filter { it.id != id },
                showProgressModal = if (state.activeDetailItem?.id == id) false else state.showProgressModal,
                activeDetailItem = if (state.activeDetailItem?.id == id) null else state.activeDetailItem
            )
        }
        showSnackbar("Download cancelled")
    }

    fun deleteHistoryItem(id: String) {
        _uiState.update { state ->
            state.copy(
                historyItems = state.historyItems.filter { it.id != id },
                recentCompletions = state.recentCompletions.filter { it.id != id }
            )
        }
        showSnackbar("Deleted from GrabIt history")
    }

    // Modal Details
    fun openProgressModal(item: DownloadItem) {
        _uiState.update {
            it.copy(
                showProgressModal = true,
                activeDetailItem = item
            )
        }
    }

    fun dismissProgressModal() {
        _uiState.update {
            it.copy(
                showProgressModal = false,
                activeDetailItem = null
            )
        }
    }

    // History Filters
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setPlatformFilter(platform: Platform?) {
        _uiState.update { it.copy(selectedPlatformFilter = platform) }
    }

    fun setMediaTypeFilter(mediaType: MediaType?) {
        _uiState.update { it.copy(selectedMediaTypeFilter = mediaType) }
    }

    fun toggleHistoryLayout() {
        _uiState.update { it.copy(isGridLayout = !it.isGridLayout) }
    }

    // Settings
    fun updateSettings(newSettings: AppSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        showSnackbar("Settings saved")
    }

    fun setDownloadFolderPath(newPath: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(downloadFolderPath = newPath),
                showFolderPickerDialog = false
            )
        }
        showSnackbar("Download folder updated to: $newPath")
    }

    fun showFolderPicker(show: Boolean) {
        _uiState.update { it.copy(showFolderPickerDialog = show) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    // Onboarding
    fun setOnboardingPage(page: Int) {
        _uiState.update { it.copy(currentOnboardingPage = page) }
    }

    fun completeOnboarding() {
        _uiState.update { it.copy(hasSeenOnboarding = true) }
    }

    fun replayOnboarding() {
        _uiState.update { it.copy(hasSeenOnboarding = false, currentOnboardingPage = 0) }
    }

    // Snackbar
    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun generateSampleTitle(platform: Platform, mediaType: MediaType, url: String): String {
        return when (platform) {
            Platform.YOUTUBE -> when (mediaType) {
                MediaType.VIDEO -> "Full HD Documentaries • Earth From Above (1080p)"
                MediaType.AUDIO -> "Lo-Fi Beats to Chill & Study • Chillhop Sessions"
                MediaType.PHOTO -> "Community Post Gallery Photo"
            }
            Platform.INSTAGRAM -> when (mediaType) {
                MediaType.VIDEO -> "Cinematic Drone Sunset Reel by @traveler"
                MediaType.AUDIO -> "Trending Reel Original Audio Soundtrack"
                MediaType.PHOTO -> "Portrait Photography Collection (High-Res)"
            }
            Platform.TIKTOK -> when (mediaType) {
                MediaType.VIDEO -> "Viral Dance Challenge 2026 Compilation"
                MediaType.AUDIO -> "Viral Sound Effect & Beat Drop (320kbps)"
                MediaType.PHOTO -> "Photo Mode Carousel Album"
            }
            Platform.TWITTER_X -> when (mediaType) {
                MediaType.VIDEO -> "Breaking Tech Keynote Demo Clip"
                MediaType.AUDIO -> "X Space Recorded Discussion (Audio)"
                MediaType.PHOTO -> "Concept Art Wallpaper (Full Res)"
            }
            Platform.REDDIT -> when (mediaType) {
                MediaType.VIDEO -> "r/interestingasfuck • Slow Motion Lightning"
                MediaType.AUDIO -> "r/musicians • Acoustic Original Melody"
                MediaType.PHOTO -> "r/EarthPorn • Alpine Lake Reflection"
            }
            Platform.PINTEREST -> when (mediaType) {
                MediaType.VIDEO -> "DIY Modern Minimalist Studio Decor Idea"
                MediaType.AUDIO -> "Calm Aesthetic Meditation Music"
                MediaType.PHOTO -> "Aesthetic Moodboard Inspiration Pin"
            }
            Platform.SNAPCHAT -> when (mediaType) {
                MediaType.VIDEO -> "Snapchat Spotlight Viral Lens Video"
                MediaType.AUDIO -> "Spotlight Sound Beat"
                MediaType.PHOTO -> "Snap Story Memories Export"
            }
            Platform.FACEBOOK -> when (mediaType) {
                MediaType.VIDEO -> "Master Craftsman Woodworking Watch Video"
                MediaType.AUDIO -> "Podcast Interview Snippet Audio"
                MediaType.PHOTO -> "High Resolution Event Photo"
            }
            Platform.OTHER -> "Shared Web Media Asset"
        }
    }
}
