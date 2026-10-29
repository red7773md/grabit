package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GrabItDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.DownloadRepository
import com.example.data.util.MediaFileManager
import com.example.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val inputUrlError: String? = null,
    val isInspectingLink: Boolean = false,
    val detectedPlatform: Platform? = null,
    val selectedInputMediaType: MediaType = MediaType.VIDEO,
    val detectedClipboardUrl: String? = null,
    val detectedClipboardPlatform: Platform? = null,
    val isSelectionMode: Boolean = false,
    val selectedHistoryIds: Set<String> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val deviceStorageInfo: DeviceStorageInfo = DeviceStorageInfo(),
    val showShareConfirmation: Boolean = false,
    val shareConfirmationItem: DownloadItem? = null,
    val showProgressModal: Boolean = false,
    val activeDetailItem: DownloadItem? = null,
    val showMediaPlayerModal: Boolean = false,
    val mediaPlayerItem: DownloadItem? = null,
    val showFolderPickerDialog: Boolean = false,
    val hasSeenOnboarding: Boolean = true,
    val currentOnboardingPage: Int = 0,
    val isBatchMode: Boolean = false,
    val isDarkTheme: Boolean = true,
    val hasYouTubeCookies: Boolean = false,
    val snackbarMessage: String? = null
)

class GrabItViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GrabItDatabase.getDatabase(application)
    private val repository = DownloadRepository(database.downloadDao())
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(GrabItUiState())
    val uiState: StateFlow<GrabItUiState> = _uiState.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private var inspectJob: Job? = null

    init {
        val savedSettings = preferencesManager.loadSettings()
        val seenOnboarding = preferencesManager.hasSeenOnboarding()
        val initialDark = savedSettings.themeMode != "light"

        _uiState.update { state ->
            state.copy(
                settings = savedSettings,
                hasSeenOnboarding = seenOnboarding,
                isDarkTheme = initialDark,
                hasYouTubeCookies = MediaFileManager.hasYouTubeCookies(application)
            )
        }

        refreshStorageInfo()

        viewModelScope.launch {
            repository.getAllDownloads().collect { savedItems ->
                _uiState.update { state ->
                    // Remove any selected IDs that no longer exist
                    val validSelected = state.selectedHistoryIds.intersect(savedItems.map { it.id }.toSet())
                    state.copy(
                        historyItems = savedItems,
                        recentCompletions = savedItems.take(6),
                        selectedHistoryIds = validSelected,
                        isSelectionMode = if (validSelected.isEmpty()) false else state.isSelectionMode
                    )
                }
                refreshStorageInfo()
            }
        }
    }

    fun refreshStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = MediaFileManager.getDeviceStorageInfo(getApplication())
            _uiState.update { it.copy(deviceStorageInfo = info) }
        }
    }

    // --- Smart Clipboard Detection ---
    fun evaluateClipboardText(rawClipboard: String?) {
        val text = rawClipboard?.trim().orEmpty()
        if (text.isEmpty() || !MediaFileManager.isValidHttpUrl(text)) {
            return
        }
        val normalized = MediaFileManager.normalizeUrl(text)
        val dismissed = preferencesManager.getLastDismissedClipboardUrl()
        val currentInput = MediaFileManager.normalizeUrl(_uiState.value.inputUrl)
        val alreadyInHistory = _uiState.value.historyItems.any {
            MediaFileManager.normalizeUrl(it.sourceUrl).equals(normalized, ignoreCase = true)
        }
        val alreadyActive = _uiState.value.activeDownloads.any {
            MediaFileManager.normalizeUrl(it.sourceUrl).equals(normalized, ignoreCase = true)
        }

        if (normalized.equals(dismissed, ignoreCase = true) ||
            normalized.equals(currentInput, ignoreCase = true) ||
            alreadyInHistory ||
            alreadyActive
        ) {
            return
        }

        val platform = Platform.detectFromUrl(normalized)
        _uiState.update {
            it.copy(
                detectedClipboardUrl = normalized,
                detectedClipboardPlatform = platform
            )
        }
    }

    fun acceptClipboardSuggestion() {
        val url = _uiState.value.detectedClipboardUrl ?: return
        preferencesManager.setLastDismissedClipboardUrl(url)
        _uiState.update {
            it.copy(
                inputUrl = url,
                inputUrlError = null,
                detectedPlatform = Platform.detectFromUrl(url),
                detectedClipboardUrl = null,
                detectedClipboardPlatform = null
            )
        }
        openShareConfirmationFromInput()
    }

    fun dismissClipboardSuggestion() {
        val url = _uiState.value.detectedClipboardUrl.orEmpty()
        if (url.isNotBlank()) {
            preferencesManager.setLastDismissedClipboardUrl(url)
        }
        _uiState.update {
            it.copy(
                detectedClipboardUrl = null,
                detectedClipboardPlatform = null
            )
        }
    }

    // --- URL Input & Real Link Inspection ---
    fun onUrlInputChanged(newUrl: String) {
        val detected = Platform.detectFromUrl(newUrl)
        _uiState.update {
            it.copy(
                inputUrl = newUrl,
                inputUrlError = null,
                detectedPlatform = if (newUrl.isNotBlank()) detected else null
            )
        }
    }

    fun onInputMediaTypeChanged(mediaType: MediaType) {
        _uiState.update { it.copy(selectedInputMediaType = mediaType) }
    }

    fun pasteFromClipboard(text: String) {
        val normalized = MediaFileManager.normalizeUrl(text)
        onUrlInputChanged(normalized)
        if (normalized == _uiState.value.detectedClipboardUrl) {
            dismissClipboardSuggestion()
        }
    }

    fun clearInputUrl() {
        inspectJob?.cancel()
        _uiState.update {
            it.copy(
                inputUrl = "",
                inputUrlError = null,
                isInspectingLink = false,
                detectedPlatform = null
            )
        }
    }

    fun openShareConfirmationFromInput() {
        val rawUrl = _uiState.value.inputUrl.trim()
        if (rawUrl.isEmpty()) {
            _uiState.update { it.copy(inputUrlError = "Paste or enter an HTTP/HTTPS media link first.") }
            showSnackbar("Please enter or paste a valid link first.")
            return
        }

        if (!MediaFileManager.isValidHttpUrl(rawUrl)) {
            _uiState.update { it.copy(inputUrlError = "Invalid URL format. Include a valid domain (e.g. https://...).") }
            showSnackbar("Please enter a valid HTTP or HTTPS URL.")
            return
        }

        val normalizedUrl = MediaFileManager.normalizeUrl(rawUrl)
        val preferredMediaType = _uiState.value.selectedInputMediaType

        inspectJob?.cancel()
        inspectJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInspectingLink = true,
                    inputUrlError = null
                )
            }

            val metadata = MediaFileManager.inspectLinkMetadata(normalizedUrl, preferredMediaType)
            val effectiveType = metadata.detectedMediaType
            val defaultQuality = when (effectiveType) {
                MediaType.VIDEO -> _uiState.value.settings.defaultVideoQuality
                MediaType.AUDIO -> _uiState.value.settings.defaultAudioFormat
                MediaType.PHOTO -> _uiState.value.settings.defaultPhotoFormat
            }

            val folderSubpath = if (_uiState.value.settings.organizeByFolders) {
                "GrabIt/${metadata.platform.subfolder}/${effectiveType.subfolder}/"
            } else {
                "GrabIt/"
            }

            val candidate = DownloadItem(
                id = UUID.randomUUID().toString(),
                title = metadata.title,
                sourceUrl = normalizedUrl,
                resolvedMediaUrl = metadata.resolvedMediaUrl,
                thumbnailUrl = metadata.thumbnailUrl,
                platform = metadata.platform,
                mediaType = effectiveType,
                quality = defaultQuality,
                fileSize = metadata.contentLength,
                downloadedBytes = 0L,
                status = DownloadStatus.QUEUED,
                progress = 0f,
                downloadSpeed = "Ready",
                timeRemaining = if (metadata.isDirectStreamAvailable) "Direct Stream Ready" else "Web Stream",
                localPath = folderSubpath,
                author = metadata.author,
                thumbnailSeed = (normalizedUrl.hashCode() and 0x7FFFFFFF) % 10
            )

            _uiState.update {
                it.copy(
                    isInspectingLink = false,
                    selectedInputMediaType = effectiveType,
                    showShareConfirmation = true,
                    shareConfirmationItem = candidate
                )
            }
        }
    }

    fun openShareConfirmationWithUrl(sharedUrl: String) {
        val normalized = MediaFileManager.normalizeUrl(sharedUrl)
        onUrlInputChanged(normalized)
        openShareConfirmationFromInput()
    }

    fun updateShareConfirmationItem(
        mediaType: MediaType,
        quality: String
    ) {
        _uiState.update { state ->
            val current = state.shareConfirmationItem ?: return@update state
            val folderSubpath = if (state.settings.organizeByFolders) {
                "GrabIt/${current.platform.subfolder}/${mediaType.subfolder}/"
            } else {
                "GrabIt/"
            }
            val updatedResolvedUrl = when {
                mediaType == MediaType.PHOTO && current.thumbnailUrl.isNotBlank() -> current.thumbnailUrl
                else -> current.resolvedMediaUrl
            }
            state.copy(
                shareConfirmationItem = current.copy(
                    mediaType = mediaType,
                    quality = quality,
                    resolvedMediaUrl = updatedResolvedUrl,
                    localPath = folderSubpath
                )
            )
        }
    }

    fun dismissShareConfirmation() {
        _uiState.update { it.copy(showShareConfirmation = false, shareConfirmationItem = null) }
    }

    fun confirmDownloadFromSheet() {
        val item = _uiState.value.shareConfirmationItem ?: return
        val context = getApplication<Application>()

        if (_uiState.value.settings.wifiOnly && !MediaFileManager.isOnWifi(context)) {
            showSnackbar("Wi-Fi Only mode is enabled in Settings. Connect to Wi-Fi or disable the restriction.")
            return
        }

        val activeCount = _uiState.value.activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
        val maxConcurrent = _uiState.value.settings.maxConcurrentDownloads
        val shouldQueue = activeCount >= maxConcurrent

        val initialStatus = if (shouldQueue) DownloadStatus.QUEUED else DownloadStatus.DOWNLOADING
        val newItem = item.copy(
            status = initialStatus,
            downloadSpeed = if (shouldQueue) "Queued" else "Connecting...",
            timeRemaining = if (shouldQueue) "Waiting for slot" else "Starting...",
            progress = 0.01f,
            errorMessage = null
        )

        _uiState.update { state ->
            state.copy(
                activeDownloads = listOf(newItem) + state.activeDownloads,
                showShareConfirmation = false,
                shareConfirmationItem = null,
                inputUrl = "",
                detectedPlatform = null,
                showProgressModal = true,
                activeDetailItem = newItem
            )
        }

        if (shouldQueue) {
            showSnackbar("Added to queue (${maxConcurrent} max concurrent downloads).")
        } else {
            startStreamDownload(newItem)
        }
    }

    private fun startStreamDownload(initialItem: DownloadItem) {
        val itemId = initialItem.id
        downloadJobs[itemId]?.cancel()

        val job = viewModelScope.launch {
            val context = getApplication<Application>()
            val organizeByFolders = _uiState.value.settings.organizeByFolders

            try {
                val createdFile = MediaFileManager.streamDownloadMediaFile(
                    context = context,
                    item = initialItem,
                    organizeByFolders = organizeByFolders,
                    onProgress = { progressUpdate ->
                        _uiState.update { state ->
                            val updatedActive = state.activeDownloads.map { download ->
                                if (download.id == itemId && download.status == DownloadStatus.DOWNLOADING) {
                                    download.copy(
                                        progress = progressUpdate.progress,
                                        downloadedBytes = progressUpdate.downloadedBytes,
                                        fileSize = progressUpdate.totalBytes,
                                        downloadSpeed = progressUpdate.speedLabel,
                                        timeRemaining = progressUpdate.etaLabel,
                                        errorMessage = null
                                    )
                                } else download
                            }
                            val currentDetail = state.activeDetailItem
                            val updatedDetail = if (currentDetail?.id == itemId) {
                                updatedActive.firstOrNull { it.id == itemId } ?: currentDetail
                            } else currentDetail

                            state.copy(
                                activeDownloads = updatedActive,
                                activeDetailItem = updatedDetail
                            )
                        }
                    }
                )

                val actualFileSize = createdFile.length()
                val relativePath = if (organizeByFolders) {
                    "GrabIt/${initialItem.platform.subfolder}/${initialItem.mediaType.subfolder}/${createdFile.name}"
                } else {
                    "GrabIt/${createdFile.name}"
                }

                val finishedItem = initialItem.copy(
                    progress = 1.0f,
                    downloadedBytes = actualFileSize,
                    fileSize = actualFileSize,
                    status = DownloadStatus.COMPLETED,
                    downloadSpeed = "0 KB/s",
                    timeRemaining = "Completed",
                    realFilePath = createdFile.absolutePath,
                    localPath = relativePath,
                    timestamp = System.currentTimeMillis(),
                    errorMessage = null
                )

                repository.saveDownload(finishedItem)

                _uiState.update { state ->
                    val remainingActive = state.activeDownloads.filter { it.id != itemId }
                    val updatedRecent = (listOf(finishedItem) + state.recentCompletions).distinctBy { it.id }.take(6)
                    val updatedHistory = (listOf(finishedItem) + state.historyItems).distinctBy { it.id }
                    val updatedDetail = if (state.activeDetailItem?.id == itemId) finishedItem else state.activeDetailItem

                    state.copy(
                        activeDownloads = remainingActive,
                        recentCompletions = updatedRecent,
                        historyItems = updatedHistory,
                        activeDetailItem = updatedDetail
                    )
                }

                if (_uiState.value.settings.notificationsCompletion) {
                    showSnackbar("Saved: ${finishedItem.title.take(28)}")
                }
                refreshStorageInfo()
                promoteNextQueuedDownload()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                val reason = e.localizedMessage ?: "Unable to stream media from this link"
                _uiState.update { state ->
                    val updatedActive = state.activeDownloads.map { download ->
                        if (download.id == itemId) {
                            download.copy(
                                status = DownloadStatus.FAILED,
                                downloadSpeed = "Failed",
                                timeRemaining = reason,
                                errorMessage = reason
                            )
                        } else download
                    }
                    val currentDetail = state.activeDetailItem
                    val updatedDetail = if (currentDetail?.id == itemId) {
                        updatedActive.firstOrNull { it.id == itemId } ?: currentDetail
                    } else currentDetail

                    state.copy(
                        activeDownloads = updatedActive,
                        activeDetailItem = updatedDetail
                    )
                }
                showSnackbar("Download failed: $reason")
                promoteNextQueuedDownload()
            }
        }
        downloadJobs[itemId] = job
    }

    private fun promoteNextQueuedDownload() {
        val state = _uiState.value
        val activeCount = state.activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
        if (activeCount >= state.settings.maxConcurrentDownloads) return

        val nextQueued = state.activeDownloads.firstOrNull { it.status == DownloadStatus.QUEUED } ?: return
        val started = nextQueued.copy(
            status = DownloadStatus.DOWNLOADING,
            downloadSpeed = "Connecting...",
            timeRemaining = "Starting..."
        )
        _uiState.update { s ->
            s.copy(
                activeDownloads = s.activeDownloads.map { if (it.id == started.id) started else it },
                activeDetailItem = if (s.activeDetailItem?.id == started.id) started else s.activeDetailItem
            )
        }
        startStreamDownload(started)
    }

    // --- Active Download Controls ---
    fun pauseDownload(id: String) {
        downloadJobs[id]?.cancel()
        _uiState.update { state ->
            val updated = state.activeDownloads.map {
                if (it.id == id) it.copy(status = DownloadStatus.PAUSED, downloadSpeed = "Paused", timeRemaining = "Paused") else it
            }
            val detail = if (state.activeDetailItem?.id == id) {
                updated.firstOrNull { it.id == id }
            } else state.activeDetailItem

            state.copy(activeDownloads = updated, activeDetailItem = detail)
        }
    }

    fun resumeDownload(id: String) {
        val target = _uiState.value.activeDownloads.firstOrNull { it.id == id } ?: return
        val resumed = target.copy(
            status = DownloadStatus.DOWNLOADING,
            downloadSpeed = "Resuming...",
            timeRemaining = "Calculating...",
            errorMessage = null
        )
        _uiState.update { state ->
            val updated = state.activeDownloads.map { if (it.id == id) resumed else it }
            val detail = if (state.activeDetailItem?.id == id) resumed else state.activeDetailItem
            state.copy(activeDownloads = updated, activeDetailItem = detail)
        }
        startStreamDownload(resumed)
    }

    fun cancelDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        val item = _uiState.value.activeDownloads.firstOrNull { it.id == id }
        if (item != null) {
            viewModelScope.launch(Dispatchers.IO) {
                MediaFileManager.deletePartialFileForItem(
                    getApplication(),
                    item,
                    _uiState.value.settings.organizeByFolders
                )
                refreshStorageInfo()
            }
        }
        _uiState.update { state ->
            val closeModal = state.activeDetailItem?.id == id
            state.copy(
                activeDownloads = state.activeDownloads.filter { it.id != id },
                showProgressModal = if (closeModal) false else state.showProgressModal,
                activeDetailItem = if (closeModal) null else state.activeDetailItem
            )
        }
        showSnackbar("Download cancelled")
        promoteNextQueuedDownload()
    }

    fun toggleBatchMode() {
        _uiState.update { it.copy(isBatchMode = !it.isBatchMode) }
    }

    fun pauseAllDownloads() {
        val currentActive = _uiState.value.activeDownloads
        currentActive.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }.forEach {
            downloadJobs[it.id]?.cancel()
        }
        _uiState.update { state ->
            val updated = state.activeDownloads.map {
                if (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED) {
                    it.copy(status = DownloadStatus.PAUSED, downloadSpeed = "Paused", timeRemaining = "Paused")
                } else it
            }
            state.copy(activeDownloads = updated)
        }
        showSnackbar("Paused all active downloads")
    }

    fun resumeAllDownloads() {
        val state = _uiState.value
        val maxConcurrent = state.settings.maxConcurrentDownloads
        var runningCount = state.activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }

        val pausedItems = state.activeDownloads.filter { it.status == DownloadStatus.PAUSED }
        if (pausedItems.isEmpty()) return

        val itemsToStart = mutableListOf<DownloadItem>()
        val itemsToQueue = mutableListOf<DownloadItem>()

        for (item in pausedItems) {
            if (runningCount < maxConcurrent) {
                itemsToStart.add(
                    item.copy(
                        status = DownloadStatus.DOWNLOADING,
                        downloadSpeed = "Resuming...",
                        timeRemaining = "Calculating...",
                        errorMessage = null
                    )
                )
                runningCount++
            } else {
                itemsToQueue.add(
                    item.copy(
                        status = DownloadStatus.QUEUED,
                        downloadSpeed = "Queued",
                        timeRemaining = "Waiting for slot",
                        errorMessage = null
                    )
                )
            }
        }

        val startedMap = (itemsToStart + itemsToQueue).associateBy { it.id }
        _uiState.update { s ->
            s.copy(activeDownloads = s.activeDownloads.map { startedMap[it.id] ?: it })
        }

        for (item in itemsToStart) {
            startStreamDownload(item)
        }
        showSnackbar("Resumed downloads in queue")
    }

    fun clearFailedDownloads() {
        _uiState.update { state ->
            state.copy(activeDownloads = state.activeDownloads.filter { it.status != DownloadStatus.FAILED })
        }
    }

    fun startBatchDownload(rawBatchText: String, preferredMediaType: MediaType) {
        val candidateUrls = rawBatchText.split(Regex("[\\r\\n,;\\s]+"))
            .map { MediaFileManager.normalizeUrl(it) }
            .filter { MediaFileManager.isValidHttpUrl(it) }
            .distinct()

        if (candidateUrls.isEmpty()) {
            _uiState.update { it.copy(inputUrlError = "No valid http/https URLs found in batch input") }
            return
        }

        val context = getApplication<Application>()
        if (_uiState.value.settings.wifiOnly && !MediaFileManager.isOnWifi(context)) {
            showSnackbar("Wi-Fi Only mode enabled. Connect to Wi-Fi to start batch downloads.")
            return
        }

        _uiState.update { it.copy(inputUrl = "", inputUrlError = null, isInspectingLink = true) }
        showSnackbar("Enqueuing batch of ${candidateUrls.size} links...")

        viewModelScope.launch {
            val maxConcurrent = _uiState.value.settings.maxConcurrentDownloads
            val itemsToStart = mutableListOf<DownloadItem>()

            for (url in candidateUrls) {
                val metadata = MediaFileManager.inspectLinkMetadata(url, preferredMediaType)
                val effectiveType = if (metadata.isDirectStreamAvailable) metadata.detectedMediaType else preferredMediaType
                val folderSubpath = if (_uiState.value.settings.organizeByFolders) {
                    "GrabIt/${metadata.platform.subfolder}/${effectiveType.subfolder}/"
                } else {
                    "GrabIt/"
                }

                val currentActiveCount = _uiState.value.activeDownloads.count { it.status == DownloadStatus.DOWNLOADING } + itemsToStart.size
                val shouldQueue = currentActiveCount >= maxConcurrent
                val initialStatus = if (shouldQueue) DownloadStatus.QUEUED else DownloadStatus.DOWNLOADING

                val newItem = DownloadItem(
                    id = UUID.randomUUID().toString(),
                    sourceUrl = metadata.originalUrl,
                    resolvedMediaUrl = metadata.resolvedMediaUrl,
                    thumbnailUrl = metadata.thumbnailUrl,
                    title = metadata.title,
                    platform = metadata.platform,
                    mediaType = effectiveType,
                    quality = "High",
                    fileSize = metadata.contentLength,
                    downloadedBytes = 0L,
                    status = initialStatus,
                    progress = 0.01f,
                    downloadSpeed = if (shouldQueue) "Queued" else "Connecting...",
                    timeRemaining = if (shouldQueue) "Waiting for slot" else "Starting...",
                    localPath = folderSubpath,
                    author = metadata.author,
                    thumbnailSeed = (metadata.originalUrl.hashCode() and 0x7FFFFFFF) % 10
                )

                _uiState.update { state ->
                    state.copy(activeDownloads = state.activeDownloads + newItem)
                }

                if (!shouldQueue) {
                    itemsToStart.add(newItem)
                }
            }

            _uiState.update { it.copy(isInspectingLink = false) }

            for (item in itemsToStart) {
                startStreamDownload(item)
            }
            showSnackbar("Batch queue started (${candidateUrls.size} downloads)")
        }
    }

    fun openProgressModal(item: DownloadItem) {
        _uiState.update { it.copy(showProgressModal = true, activeDetailItem = item) }
    }

    fun dismissProgressModal() {
        _uiState.update { it.copy(showProgressModal = false, activeDetailItem = null) }
    }

    // --- Media Player & File Operations ---
    fun openMediaItem(item: DownloadItem) {
        _uiState.update {
            it.copy(
                showProgressModal = false,
                showMediaPlayerModal = true,
                mediaPlayerItem = item
            )
        }
    }

    fun dismissMediaPlayerModal() {
        _uiState.update {
            it.copy(
                showMediaPlayerModal = false,
                mediaPlayerItem = null
            )
        }
    }

    fun deleteHistoryItem(id: String) {
        val target = _uiState.value.historyItems.firstOrNull { it.id == id }
            ?: _uiState.value.recentCompletions.firstOrNull { it.id == id }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (target != null && target.realFilePath.isNotBlank()) {
                    MediaFileManager.deleteFile(target.realFilePath)
                }
                repository.deleteDownload(id)
            }

            _uiState.update { state ->
                val closePlayer = state.mediaPlayerItem?.id == id
                state.copy(
                    historyItems = state.historyItems.filter { it.id != id },
                    recentCompletions = state.recentCompletions.filter { it.id != id },
                    selectedHistoryIds = state.selectedHistoryIds - id,
                    showMediaPlayerModal = if (closePlayer) false else state.showMediaPlayerModal,
                    mediaPlayerItem = if (closePlayer) null else state.mediaPlayerItem
                )
            }
            refreshStorageInfo()
            showSnackbar("Deleted media file from storage")
        }
    }

    // --- Multi-Select Batch Operations in History ---
    fun toggleSelectionMode(enable: Boolean? = null) {
        _uiState.update { state ->
            val targetMode = enable ?: !state.isSelectionMode
            state.copy(
                isSelectionMode = targetMode,
                selectedHistoryIds = if (!targetMode) emptySet() else state.selectedHistoryIds
            )
        }
    }

    fun toggleHistoryItemSelection(id: String) {
        _uiState.update { state ->
            val updatedSet = if (state.selectedHistoryIds.contains(id)) {
                state.selectedHistoryIds - id
            } else {
                state.selectedHistoryIds + id
            }
            state.copy(
                isSelectionMode = updatedSet.isNotEmpty(),
                selectedHistoryIds = updatedSet
            )
        }
    }

    fun selectAllFilteredHistory(filteredIds: List<String>) {
        _uiState.update { state ->
            val allSelected = filteredIds.isNotEmpty() && state.selectedHistoryIds.containsAll(filteredIds)
            val newSelection = if (allSelected) {
                state.selectedHistoryIds - filteredIds.toSet()
            } else {
                state.selectedHistoryIds + filteredIds
            }
            state.copy(
                isSelectionMode = newSelection.isNotEmpty(),
                selectedHistoryIds = newSelection
            )
        }
    }

    fun clearHistorySelection() {
        _uiState.update {
            it.copy(
                isSelectionMode = false,
                selectedHistoryIds = emptySet()
            )
        }
    }

    fun deleteSelectedHistoryItems() {
        val idsToDelete = _uiState.value.selectedHistoryIds.toList()
        if (idsToDelete.isEmpty()) return

        val itemsToDelete = _uiState.value.historyItems.filter { it.id in idsToDelete }
        val totalFreedBytes = itemsToDelete.sumOf { it.fileSize }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                itemsToDelete.forEach { item ->
                    if (item.realFilePath.isNotBlank()) {
                        MediaFileManager.deleteFile(item.realFilePath)
                    }
                }
                repository.deleteDownloads(idsToDelete)
            }

            _uiState.update { state ->
                state.copy(
                    historyItems = state.historyItems.filterNot { it.id in idsToDelete },
                    recentCompletions = state.recentCompletions.filterNot { it.id in idsToDelete },
                    isSelectionMode = false,
                    selectedHistoryIds = emptySet()
                )
            }
            refreshStorageInfo()
            showSnackbar("Deleted ${idsToDelete.size} files (${DownloadItem.formatBytes(totalFreedBytes)} freed)")
        }
    }

    fun getSelectedHistoryItems(): List<DownloadItem> {
        val selectedIds = _uiState.value.selectedHistoryIds
        return _uiState.value.historyItems.filter { it.id in selectedIds }
    }

    fun clearAllHistory() {
        val allItems = _uiState.value.historyItems
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                allItems.forEach { item ->
                    if (item.realFilePath.isNotBlank()) {
                        MediaFileManager.deleteFile(item.realFilePath)
                    }
                }
                repository.clearAll()
            }
            _uiState.update {
                it.copy(
                    historyItems = emptyList(),
                    recentCompletions = emptyList(),
                    isSelectionMode = false,
                    selectedHistoryIds = emptySet()
                )
            }
            refreshStorageInfo()
            showSnackbar("All download history & files cleared")
        }
    }

    fun clearTempCache() {
        viewModelScope.launch {
            val freed = withContext(Dispatchers.IO) {
                MediaFileManager.clearTempCache(getApplication())
            }
            refreshStorageInfo()
            val formatted = DownloadItem.formatBytes(freed)
            showSnackbar(if (freed > 0L) "Cleared $formatted of temporary cache" else "Cache is already clean (0 B)")
        }
    }

    // --- History Filtering & Layout ---
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onPlatformFilterSelected(platform: Platform?) {
        _uiState.update { state ->
            val next = if (state.selectedPlatformFilter == platform) null else platform
            state.copy(selectedPlatformFilter = next)
        }
    }

    fun onMediaTypeFilterSelected(mediaType: MediaType?) {
        _uiState.update { state ->
            val next = if (state.selectedMediaTypeFilter == mediaType) null else mediaType
            state.copy(selectedMediaTypeFilter = next)
        }
    }

    fun toggleHistoryLayout() {
        _uiState.update { it.copy(isGridLayout = !it.isGridLayout) }
    }

    // --- Persisted Settings ---
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _uiState.update { state ->
            val updated = transform(state.settings)
            preferencesManager.saveSettings(updated)
            val newDark = when (updated.themeMode) {
                "dark" -> true
                "light" -> false
                else -> state.isDarkTheme
            }
            state.copy(
                settings = updated,
                isDarkTheme = newDark
            )
        }
    }

    fun openFolderPicker() {
        _uiState.update { it.copy(showFolderPickerDialog = true) }
    }

    fun dismissFolderPicker() {
        _uiState.update { it.copy(showFolderPickerDialog = false) }
    }

    fun selectDownloadFolder(newPath: String) {
        updateSettings { it.copy(downloadFolderPath = newPath) }
        _uiState.update { it.copy(showFolderPickerDialog = false) }
        showSnackbar("Download path updated: $newPath")
    }

    fun toggleDarkTheme() {
        _uiState.update { state ->
            val nextDark = !state.isDarkTheme
            val nextMode = if (nextDark) "dark" else "light"
            val updatedSettings = state.settings.copy(themeMode = nextMode)
            preferencesManager.saveSettings(updatedSettings)
            state.copy(
                isDarkTheme = nextDark,
                settings = updatedSettings
            )
        }
    }

    // --- Onboarding ---
    fun setOnboardingPage(page: Int) {
        _uiState.update { it.copy(currentOnboardingPage = page.coerceIn(0, 2)) }
    }

    fun nextOnboardingPage() {
        _uiState.update { state ->
            if (state.currentOnboardingPage < 2) {
                state.copy(currentOnboardingPage = state.currentOnboardingPage + 1)
            } else {
                preferencesManager.setHasSeenOnboarding(true)
                state.copy(hasSeenOnboarding = true)
            }
        }
    }

    fun skipOnboarding() {
        preferencesManager.setHasSeenOnboarding(true)
        _uiState.update { it.copy(hasSeenOnboarding = true) }
    }

    fun replayOnboarding() {
        preferencesManager.setHasSeenOnboarding(false)
        _uiState.update {
            it.copy(
                hasSeenOnboarding = false,
                currentOnboardingPage = 0
            )
        }
    }

    // --- Snackbar ---
    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    /** Reads the picked cookies file, validates it, and stores it in app-private storage. */
    fun importYouTubeCookies(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val error = try {
                val text = app.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                if (text == null) {
                    "Couldn't open the selected file."
                } else {
                    MediaFileManager.saveYouTubeCookies(app, text)
                }
            } catch (e: Exception) {
                "Couldn't read the file: ${e.message ?: "unknown error"}"
            }
            _uiState.update { it.copy(hasYouTubeCookies = MediaFileManager.hasYouTubeCookies(app)) }
            showSnackbar(error ?: "YouTube cookies saved.")
        }
    }

    fun clearYouTubeCookies() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            MediaFileManager.deleteYouTubeCookies(app)
            _uiState.update { it.copy(hasYouTubeCookies = false) }
            showSnackbar("YouTube cookies removed.")
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
