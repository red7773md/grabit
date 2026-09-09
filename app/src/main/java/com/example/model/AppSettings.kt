package com.example.model

data class AppSettings(
    val downloadFolderPath: String = "/storage/emulated/0/Download/GrabIt",
    val organizeByFolders: Boolean = true,
    val wifiOnly: Boolean = false,
    val defaultVideoQuality: String = "1080p FHD",
    val defaultAudioFormat: String = "320 kbps MP3",
    val defaultPhotoFormat: String = "Original Quality",
    val maxConcurrentDownloads: Int = 3,
    val fastMultiThreaded: Boolean = true,
    val notificationsCompletion: Boolean = true,
    val notificationsProgress: Boolean = true,
    val soundOnFinish: Boolean = false,
    val themeMode: String = "system" // "system", "dark", "light"
)
