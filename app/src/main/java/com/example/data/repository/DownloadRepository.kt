package com.example.data.repository

import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.model.DownloadItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DownloadRepository(private val downloadDao: DownloadDao) {

    fun getAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDownloadItem() }
        }
    }

    suspend fun saveDownload(item: DownloadItem) {
        downloadDao.insertOrUpdate(DownloadEntity.fromDownloadItem(item))
    }

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteById(id)
    }

    suspend fun deleteDownloads(ids: List<String>) {
        if (ids.isNotEmpty()) {
            downloadDao.deleteByIds(ids)
        }
    }

    suspend fun clearAll() {
        downloadDao.clearAll()
    }
}
