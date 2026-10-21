package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppSettings

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        return AppSettings(
            downloadFolderPath = prefs.getString(KEY_FOLDER_PATH, "/storage/emulated/0/Download/GrabIt")
                ?: "/storage/emulated/0/Download/GrabIt",
            organizeByFolders = prefs.getBoolean(KEY_ORGANIZE_FOLDERS, true),
            wifiOnly = prefs.getBoolean(KEY_WIFI_ONLY, false),
            defaultVideoQuality = prefs.getString(KEY_VIDEO_QUALITY, "1080p FHD") ?: "1080p FHD",
            defaultAudioFormat = prefs.getString(KEY_AUDIO_FORMAT, "320 kbps MP3") ?: "320 kbps MP3",
            defaultPhotoFormat = prefs.getString(KEY_PHOTO_FORMAT, "Original Quality") ?: "Original Quality",
            maxConcurrentDownloads = prefs.getInt(KEY_MAX_CONCURRENT, 3),
            fastMultiThreaded = prefs.getBoolean(KEY_FAST_MULTI_THREADED, true),
            notificationsCompletion = prefs.getBoolean(KEY_NOTIFY_COMPLETION, true),
            notificationsProgress = prefs.getBoolean(KEY_NOTIFY_PROGRESS, true),
            soundOnFinish = prefs.getBoolean(KEY_SOUND_FINISH, false),
            themeMode = prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_FOLDER_PATH, settings.downloadFolderPath)
            .putBoolean(KEY_ORGANIZE_FOLDERS, settings.organizeByFolders)
            .putBoolean(KEY_WIFI_ONLY, settings.wifiOnly)
            .putString(KEY_VIDEO_QUALITY, settings.defaultVideoQuality)
            .putString(KEY_AUDIO_FORMAT, settings.defaultAudioFormat)
            .putString(KEY_PHOTO_FORMAT, settings.defaultPhotoFormat)
            .putInt(KEY_MAX_CONCURRENT, settings.maxConcurrentDownloads)
            .putBoolean(KEY_FAST_MULTI_THREADED, settings.fastMultiThreaded)
            .putBoolean(KEY_NOTIFY_COMPLETION, settings.notificationsCompletion)
            .putBoolean(KEY_NOTIFY_PROGRESS, settings.notificationsProgress)
            .putBoolean(KEY_SOUND_FINISH, settings.soundOnFinish)
            .putString(KEY_THEME_MODE, settings.themeMode)
            .apply()
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, seen).apply()
    }

    fun getLastDismissedClipboardUrl(): String {
        return prefs.getString(KEY_LAST_DISMISSED_CLIPBOARD, "") ?: ""
    }

    fun setLastDismissedClipboardUrl(url: String) {
        prefs.edit().putString(KEY_LAST_DISMISSED_CLIPBOARD, url).apply()
    }

    companion object {
        private const val PREFS_NAME = "grabit_user_preferences"
        private const val KEY_FOLDER_PATH = "download_folder_path"
        private const val KEY_ORGANIZE_FOLDERS = "organize_by_folders"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_VIDEO_QUALITY = "default_video_quality"
        private const val KEY_AUDIO_FORMAT = "default_audio_format"
        private const val KEY_PHOTO_FORMAT = "default_photo_format"
        private const val KEY_MAX_CONCURRENT = "max_concurrent_downloads"
        private const val KEY_FAST_MULTI_THREADED = "fast_multi_threaded"
        private const val KEY_NOTIFY_COMPLETION = "notifications_completion"
        private const val KEY_NOTIFY_PROGRESS = "notifications_progress"
        private const val KEY_SOUND_FINISH = "sound_on_finish"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_LAST_DISMISSED_CLIPBOARD = "last_dismissed_clipboard_url"
    }
}
