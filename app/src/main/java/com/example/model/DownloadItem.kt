package com.example.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

data class DownloadItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val platform: Platform,
    val mediaType: MediaType = MediaType.VIDEO,
    val quality: String = "1080p FHD",
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val progress: Float = 0f, // 0.0 to 1.0
    val downloadSpeed: String = "0 KB/s",
    val timeRemaining: String = "",
    val localPath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val duration: String? = null,
    val author: String = "",
    val thumbnailSeed: Int = 1
) {
    val formattedFileSize: String
        get() = formatBytes(fileSize)

    val formattedDownloadedSize: String
        get() = formatBytes(downloadedBytes)

    val progressPercent: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
