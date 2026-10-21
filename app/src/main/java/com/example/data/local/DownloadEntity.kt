package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.MediaType
import com.example.model.Platform

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val sourceUrl: String,
    val resolvedMediaUrl: String = "",
    val thumbnailUrl: String = "",
    val platformName: String,
    val mediaTypeName: String,
    val quality: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val statusName: String,
    val progress: Float,
    val downloadSpeed: String,
    val timeRemaining: String,
    val localPath: String,
    val realFilePath: String,
    val timestamp: Long,
    val duration: String?,
    val author: String,
    val thumbnailSeed: Int
) {
    fun toDownloadItem(): DownloadItem {
        val platform = try {
            Platform.valueOf(platformName)
        } catch (_: Exception) {
            Platform.OTHER
        }
        val mediaType = try {
            MediaType.valueOf(mediaTypeName)
        } catch (_: Exception) {
            MediaType.VIDEO
        }
        val status = try {
            DownloadStatus.valueOf(statusName)
        } catch (_: Exception) {
            DownloadStatus.COMPLETED
        }

        return DownloadItem(
            id = id,
            title = title,
            sourceUrl = sourceUrl,
            resolvedMediaUrl = resolvedMediaUrl,
            thumbnailUrl = thumbnailUrl,
            platform = platform,
            mediaType = mediaType,
            quality = quality,
            fileSize = fileSize,
            downloadedBytes = downloadedBytes,
            status = status,
            progress = progress,
            downloadSpeed = downloadSpeed,
            timeRemaining = timeRemaining,
            localPath = localPath,
            realFilePath = realFilePath,
            timestamp = timestamp,
            duration = duration,
            author = author,
            thumbnailSeed = thumbnailSeed
        )
    }

    companion object {
        fun fromDownloadItem(item: DownloadItem): DownloadEntity {
            return DownloadEntity(
                id = item.id,
                title = item.title,
                sourceUrl = item.sourceUrl,
                resolvedMediaUrl = item.resolvedMediaUrl,
                thumbnailUrl = item.thumbnailUrl,
                platformName = item.platform.name,
                mediaTypeName = item.mediaType.name,
                quality = item.quality,
                fileSize = item.fileSize,
                downloadedBytes = item.downloadedBytes,
                statusName = item.status.name,
                progress = item.progress,
                downloadSpeed = item.downloadSpeed,
                timeRemaining = item.timeRemaining,
                localPath = item.localPath,
                realFilePath = item.realFilePath,
                timestamp = item.timestamp,
                duration = item.duration,
                author = item.author,
                thumbnailSeed = item.thumbnailSeed
            )
        }
    }
}
