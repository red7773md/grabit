package com.example.model

enum class DownloadStatus {
    INSPECTING,
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
    val resolvedMediaUrl: String = "",
    val thumbnailUrl: String = "",
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
    val realFilePath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val duration: String? = null,
    val author: String = "",
    val thumbnailSeed: Int = 1,
    val errorMessage: String? = null
) {
    val formattedFileSize: String
        get() = if (fileSize > 0L) formatBytes(fileSize) else "Size unknown"

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
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

data class LinkMetadata(
    val originalUrl: String,
    val resolvedMediaUrl: String,
    val thumbnailUrl: String = "",
    val title: String,
    val author: String,
    val platform: Platform,
    val detectedMediaType: MediaType,
    val contentLength: Long = 0L,
    val mimeType: String = "",
    val duration: String? = null,
    val isDirectStreamAvailable: Boolean = true
)

data class DeviceStorageInfo(
    val totalDeviceBytes: Long = 0L,
    val freeDeviceBytes: Long = 0L,
    val usedDeviceBytes: Long = 0L,
    val usedByGrabItBytes: Long = 0L,
    val tempCacheBytes: Long = 0L,
    val downloadedFilesCount: Int = 0
) {
    val formattedTotalDevice: String
        get() = DownloadItem.formatBytes(totalDeviceBytes)

    val formattedFreeDevice: String
        get() = DownloadItem.formatBytes(freeDeviceBytes)

    val formattedUsedByGrabIt: String
        get() = DownloadItem.formatBytes(usedByGrabItBytes)

    val formattedTempCache: String
        get() = DownloadItem.formatBytes(tempCacheBytes)

    val deviceUsedFraction: Float
        get() = if (totalDeviceBytes > 0) {
            (usedDeviceBytes.toFloat() / totalDeviceBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val grabItShareOfUsedFraction: Float
        get() = if (totalDeviceBytes > 0) {
            (usedByGrabItBytes.toFloat() / totalDeviceBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
