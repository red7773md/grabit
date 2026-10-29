package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.util.MediaFileManager
import com.example.model.DownloadStatus
import com.example.ui.components.GrabItTopBar
import com.example.ui.theme.GrabItCoral
import com.example.viewmodel.GrabItViewModel

enum class GrabItTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY("History", Icons.Filled.Folder, Icons.Outlined.Folder),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun GrabItMainScreen(
    viewModel: GrabItViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(GrabItTab.HOME) }
    var showAboutScreen by remember { mutableStateOf(false) }

    // Evaluate clipboard when app starts or resumes
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val clipText = clipboardManager.getText()?.text
        viewModel.evaluateClipboardText(clipText)
        viewModel.refreshStorageInfo()
    }

    // Back button returns to HOME tab when on History or Settings
    BackHandler(enabled = (currentTab != GrabItTab.HOME || showAboutScreen) && !uiState.isSelectionMode) {
        if (showAboutScreen) {
            showAboutScreen = false
        } else {
            currentTab = GrabItTab.HOME
        }
    }

    // Show Snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    if (!uiState.hasSeenOnboarding) {
        OnboardingScreen(
            currentPage = uiState.currentOnboardingPage,
            onPageChange = { viewModel.setOnboardingPage(it) },
            onComplete = { viewModel.skipOnboarding() }
        )
        return
    }

    if (showAboutScreen) {
        AboutScreen(
            onNavigateBack = { showAboutScreen = false },
            onTestUrl = { url ->
                showAboutScreen = false
                currentTab = GrabItTab.HOME
                viewModel.onUrlInputChanged(url)
                viewModel.openShareConfirmationFromInput()
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GrabItTopBar(
                title = when (currentTab) {
                    GrabItTab.HOME -> "GrabIt"
                    GrabItTab.HISTORY -> "Downloads Vault"
                    GrabItTab.SETTINGS -> "Settings"
                },
                activeDownloadsCount = uiState.activeDownloads.size,
                isDarkTheme = uiState.isDarkTheme,
                onToggleTheme = { viewModel.toggleDarkTheme() },
                onAboutClick = { showAboutScreen = true },
                onDownloadsClick = {
                    uiState.activeDownloads.firstOrNull()?.let { firstActive ->
                        viewModel.openProgressModal(firstActive)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                GrabItTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        modifier = Modifier.testTag("nav_tab_${tab.name}"),
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == GrabItTab.HISTORY && uiState.historyItems.isNotEmpty()) {
                                        Badge(
                                            containerColor = GrabItCoral,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "${uiState.historyItems.size}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GrabItCoral,
                            selectedTextColor = GrabItCoral,
                            indicatorColor = GrabItCoral.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == GrabItTab.HISTORY && !uiState.isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        currentTab = GrabItTab.HOME
                    },
                    containerColor = GrabItCoral,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_new_download")
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Link", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                GrabItTab.HOME -> {
                    HomeScreen(
                        inputUrl = uiState.inputUrl,
                        inputUrlError = uiState.inputUrlError,
                        isInspectingLink = uiState.isInspectingLink,
                        detectedPlatform = uiState.detectedPlatform,
                        selectedMediaType = uiState.selectedInputMediaType,
                        detectedClipboardUrl = uiState.detectedClipboardUrl,
                        detectedClipboardPlatform = uiState.detectedClipboardPlatform,
                        activeDownloads = uiState.activeDownloads,
                        recentCompletions = uiState.recentCompletions,
                        downloadFolderPath = uiState.settings.downloadFolderPath,
                        deviceStorageInfo = uiState.deviceStorageInfo,
                        isBatchMode = uiState.isBatchMode,
                        onToggleBatchMode = { viewModel.toggleBatchMode() },
                        onStartBatchDownload = { text, type -> viewModel.startBatchDownload(text, type) },
                        onPauseAllDownloads = { viewModel.pauseAllDownloads() },
                        onResumeAllDownloads = { viewModel.resumeAllDownloads() },
                        onClearFailedDownloads = { viewModel.clearFailedDownloads() },
                        onUrlChange = { viewModel.onUrlInputChanged(it) },
                        onMediaTypeChange = { viewModel.onInputMediaTypeChanged(it) },
                        onPasteFromClipboard = { viewModel.pasteFromClipboard(it) },
                        onAcceptClipboardSuggestion = { viewModel.acceptClipboardSuggestion() },
                        onDismissClipboardSuggestion = { viewModel.dismissClipboardSuggestion() },
                        onClearUrl = { viewModel.clearInputUrl() },
                        onGrabMedia = { viewModel.openShareConfirmationFromInput() },
                        onPauseDownload = { viewModel.pauseDownload(it) },
                        onResumeDownload = { viewModel.resumeDownload(it) },
                        onCancelDownload = { viewModel.cancelDownload(it) },
                        onOpenItem = { item ->
                            viewModel.openMediaItem(item)
                        },
                        onShareItem = { item ->
                            if (item.realFilePath.isNotBlank() && java.io.File(item.realFilePath).exists()) {
                                MediaFileManager.shareFileWithExternalApp(
                                    context = context,
                                    filePath = item.realFilePath,
                                    title = item.title,
                                    mediaType = item.mediaType
                                )
                            } else {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                                    putExtra(Intent.EXTRA_TEXT, "Downloaded with GrabIt: ${item.title}\n${item.sourceUrl}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share ${item.title}"))
                            }
                        },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                        onActiveItemClick = { viewModel.openProgressModal(it) },
                        onOpenHistoryClick = { currentTab = GrabItTab.HISTORY },
                        onOpenSettingsClick = { currentTab = GrabItTab.SETTINGS },
                        onOpenAboutClick = { showAboutScreen = true },
                        onSelectTestPlatform = { platform ->
                            viewModel.onUrlInputChanged(platform.exampleUrl)
                        }
                    )
                }

                GrabItTab.HISTORY -> {
                    HistoryScreen(
                        historyItems = uiState.historyItems,
                        searchQuery = uiState.searchQuery,
                        selectedPlatform = uiState.selectedPlatformFilter,
                        selectedMediaType = uiState.selectedMediaTypeFilter,
                        isGridLayout = uiState.isGridLayout,
                        isSelectionMode = uiState.isSelectionMode,
                        selectedIds = uiState.selectedHistoryIds,
                        onSearchChange = { viewModel.onSearchQueryChanged(it) },
                        onPlatformSelect = { viewModel.onPlatformFilterSelected(it) },
                        onMediaTypeSelect = { viewModel.onMediaTypeFilterSelected(it) },
                        onToggleLayout = { viewModel.toggleHistoryLayout() },
                        onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                        onToggleItemSelection = { viewModel.toggleHistoryItemSelection(it) },
                        onSelectAllFiltered = { viewModel.selectAllFilteredHistory(it) },
                        onClearSelection = { viewModel.clearHistorySelection() },
                        onBatchDeleteSelected = { viewModel.deleteSelectedHistoryItems() },
                        onBatchShareSelected = {
                            val selectedItems = viewModel.getSelectedHistoryItems()
                            if (selectedItems.isNotEmpty()) {
                                MediaFileManager.shareMultipleFilesWithExternalApp(context, selectedItems)
                            }
                        },
                        onOpenItem = { item ->
                            viewModel.openMediaItem(item)
                        },
                        onShareItem = { item ->
                            if (item.realFilePath.isNotBlank() && java.io.File(item.realFilePath).exists()) {
                                MediaFileManager.shareFileWithExternalApp(
                                    context = context,
                                    filePath = item.realFilePath,
                                    title = item.title,
                                    mediaType = item.mediaType
                                )
                            } else {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                                    putExtra(Intent.EXTRA_TEXT, "Downloaded with GrabIt: ${item.title}\n${item.sourceUrl}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share ${item.title}"))
                            }
                        },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) }
                    )
                }

                GrabItTab.SETTINGS -> {
                    SettingsScreen(
                        settings = uiState.settings,
                        deviceStorageInfo = uiState.deviceStorageInfo,
                        isDarkTheme = uiState.isDarkTheme,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onChangeFolderClick = { viewModel.openFolderPicker() },
                        onToggleTheme = { viewModel.toggleDarkTheme() },
                        onClearTempCache = { viewModel.clearTempCache() },
                        onClearAllHistory = { viewModel.clearAllHistory() },
                        onReplayOnboarding = { viewModel.replayOnboarding() },
                        onOpenAboutClick = { showAboutScreen = true },
                        hasYouTubeCookies = uiState.hasYouTubeCookies,
                        onImportYouTubeCookies = { viewModel.importYouTubeCookies(it) },
                        onClearYouTubeCookies = { viewModel.clearYouTubeCookies() }
                    )
                }
            }
        }
    }

    // --- OVERLAY MODALS & DIALOGS ---

    // 1. Share / Paste Confirmation Bottom Sheet
    if (uiState.showShareConfirmation && uiState.shareConfirmationItem != null) {
        ShareConfirmationBottomSheet(
            item = uiState.shareConfirmationItem!!,
            downloadFolderPath = uiState.settings.downloadFolderPath,
            onMediaTypeChange = { type, defaultQual ->
                viewModel.updateShareConfirmationItem(type, defaultQual)
            },
            onQualityChange = { quality ->
                viewModel.updateShareConfirmationItem(
                    uiState.shareConfirmationItem!!.mediaType,
                    quality
                )
            },
            onChangeFolderClick = { viewModel.openFolderPicker() },
            onConfirmDownload = { viewModel.confirmDownloadFromSheet() },
            onDismiss = { viewModel.dismissShareConfirmation() }
        )
    }

    // 2. Active Download Progress Detail Modal
    if (uiState.showProgressModal && uiState.activeDetailItem != null) {
        val currentDetail = uiState.activeDetailItem!!
        DownloadProgressModal(
            item = currentDetail,
            downloadFolderPath = uiState.settings.downloadFolderPath,
            onPauseResume = {
                if (currentDetail.status == DownloadStatus.DOWNLOADING) {
                    viewModel.pauseDownload(currentDetail.id)
                } else {
                    viewModel.resumeDownload(currentDetail.id)
                }
            },
            onCancel = { viewModel.cancelDownload(currentDetail.id) },
            onDownloadInBackground = {
                viewModel.dismissProgressModal()
                viewModel.showSnackbar("Continuing download in background queue")
            },
            onOpenMedia = {
                viewModel.openMediaItem(currentDetail)
            },
            onDismiss = { viewModel.dismissProgressModal() }
        )
    }

    // 3. In-App Media Player Modal (Video, Audio, Photo Viewer)
    if (uiState.showMediaPlayerModal && uiState.mediaPlayerItem != null) {
        val playerItem = uiState.mediaPlayerItem!!
        MediaPlayerModal(
            item = playerItem,
            onDismiss = { viewModel.dismissMediaPlayerModal() },
            onShare = { item ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                    putExtra(Intent.EXTRA_TEXT, "Downloaded with GrabIt: ${item.title}\n${item.sourceUrl}")
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share ${item.title}"))
            }
        )
    }

    // 4. Folder Picker Dialog
    if (uiState.showFolderPickerDialog) {
        FolderPickerDialog(
            currentPath = uiState.settings.downloadFolderPath,
            onSelectPath = { viewModel.selectDownloadFolder(it) },
            onDismiss = { viewModel.dismissFolderPicker() }
        )
    }
}
