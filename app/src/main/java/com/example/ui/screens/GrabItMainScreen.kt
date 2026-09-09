package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GrabItTopBar
import com.example.ui.theme.GrabItCoral
import com.example.ui.theme.GrabItIndigo
import com.example.viewmodel.GrabItViewModel

enum class GrabItNavTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun GrabItMainScreen(
    viewModel: GrabItViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(GrabItNavTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Listen for snackbar notifications
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (!state.hasSeenOnboarding) {
        OnboardingScreen(
            currentPage = state.currentOnboardingPage,
            onPageChange = { viewModel.setOnboardingPage(it) },
            onComplete = { viewModel.completeOnboarding() }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GrabItTopBar(
                title = "GrabIt",
                activeDownloadsCount = state.activeDownloads.size,
                isDarkTheme = state.isDarkTheme,
                onToggleTheme = { viewModel.toggleTheme() },
                onDownloadsClick = {
                    currentTab = GrabItNavTab.HOME
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grabit_bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                GrabItNavTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (tab == GrabItNavTab.HOME && state.activeDownloads.isNotEmpty()) {
                                        Badge(containerColor = GrabItCoral) {
                                            Text(state.activeDownloads.size.toString())
                                        }
                                    } else if (tab == GrabItNavTab.HISTORY && state.historyItems.isNotEmpty()) {
                                        Badge(containerColor = GrabItIndigo) {
                                            Text(state.historyItems.size.toString())
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
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GrabItCoral,
                            selectedTextColor = GrabItCoral,
                            indicatorColor = GrabItCoral.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding),
            label = "tabContentAnim"
        ) { tab ->
            when (tab) {
                GrabItNavTab.HOME -> {
                    HomeScreen(
                        inputUrl = state.inputUrl,
                        detectedPlatform = state.detectedPlatform,
                        selectedMediaType = state.selectedInputMediaType,
                        activeDownloads = state.activeDownloads,
                        recentCompletions = state.recentCompletions,
                        downloadFolderPath = state.settings.downloadFolderPath,
                        onUrlChange = { viewModel.onUrlInputChanged(it) },
                        onMediaTypeChange = { viewModel.onInputMediaTypeChanged(it) },
                        onPasteFromClipboard = { viewModel.pasteFromClipboard(it) },
                        onClearUrl = { viewModel.clearInputUrl() },
                        onGrabMedia = { viewModel.openShareConfirmationFromInput() },
                        onPauseDownload = { viewModel.pauseDownload(it) },
                        onResumeDownload = { viewModel.resumeDownload(it) },
                        onCancelDownload = { viewModel.cancelDownload(it) },
                        onOpenItem = { item ->
                            Toast.makeText(context, "Opening ${item.title}", Toast.LENGTH_SHORT).show()
                        },
                        onShareItem = { item ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out this media: ${item.sourceUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Media Link"))
                        },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                        onActiveItemClick = { viewModel.openProgressModal(it) },
                        onOpenHistoryClick = { currentTab = GrabItNavTab.HISTORY },
                        onOpenSettingsClick = { currentTab = GrabItNavTab.SETTINGS }
                    )
                }

                GrabItNavTab.HISTORY -> {
                    HistoryScreen(
                        items = state.historyItems,
                        searchQuery = state.searchQuery,
                        selectedPlatform = state.selectedPlatformFilter,
                        selectedMediaType = state.selectedMediaTypeFilter,
                        isGridLayout = state.isGridLayout,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onPlatformSelect = { viewModel.setPlatformFilter(it) },
                        onMediaTypeSelect = { viewModel.setMediaTypeFilter(it) },
                        onToggleLayout = { viewModel.toggleHistoryLayout() },
                        onOpenItem = { item ->
                            Toast.makeText(context, "Opening ${item.title}", Toast.LENGTH_SHORT).show()
                        },
                        onShareItem = { item ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out this media: ${item.sourceUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Media Link"))
                        },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) }
                    )
                }

                GrabItNavTab.SETTINGS -> {
                    SettingsScreen(
                        settings = state.settings,
                        onUpdateSettings = { viewModel.updateSettings(it) },
                        onShowFolderPicker = { viewModel.showFolderPicker(true) },
                        onReplayOnboarding = { viewModel.replayOnboarding() },
                        onClearCache = {
                            viewModel.showSnackbar("Temporary cache cleared (0 B freed)")
                        }
                    )
                }
            }
        }
    }

    // --- Share Confirmation Bottom Sheet ---
    if (state.showShareConfirmation && state.shareConfirmationItem != null) {
        ShareConfirmationBottomSheet(
            item = state.shareConfirmationItem!!,
            onMediaTypeChange = { mediaType, quality ->
                viewModel.updateShareConfirmationItem(mediaType, quality)
            },
            onConfirmDownload = { viewModel.confirmDownloadFromSheet() },
            onDismiss = { viewModel.dismissShareConfirmation() }
        )
    }

    // --- Download Progress Detailed Modal ---
    if (state.showProgressModal && state.activeDetailItem != null) {
        DownloadProgressModal(
            item = state.activeDetailItem!!,
            onPauseResume = {
                val item = state.activeDetailItem!!
                if (item.status == com.example.model.DownloadStatus.PAUSED) {
                    viewModel.resumeDownload(item.id)
                } else {
                    viewModel.pauseDownload(item.id)
                }
            },
            onCancel = {
                val item = state.activeDetailItem!!
                viewModel.cancelDownload(item.id)
            },
            onDownloadInBackground = {
                viewModel.dismissProgressModal()
                viewModel.showSnackbar("Continuing download in background...")
            },
            onDismiss = { viewModel.dismissProgressModal() }
        )
    }

    // --- Folder Picker Dialog ---
    if (state.showFolderPickerDialog) {
        FolderPickerDialog(
            currentPath = state.settings.downloadFolderPath,
            onSelectPath = { viewModel.setDownloadFolderPath(it) },
            onDismiss = { viewModel.showFolderPicker(false) }
        )
    }
}
