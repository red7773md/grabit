package com.example.model

enum class MediaType(
    val label: String,
    val shortLabel: String,
    val subfolder: String,
    val defaultExtension: String,
    val defaultQualityOptions: List<String>
) {
    VIDEO(
        label = "Video",
        shortLabel = "Video",
        subfolder = "Videos",
        defaultExtension = "mp4",
        defaultQualityOptions = listOf("1080p FHD", "720p HD", "480p SD", "4K Ultra")
    ),
    AUDIO(
        label = "Music / Audio",
        shortLabel = "Audio",
        subfolder = "Audio",
        defaultExtension = "mp3",
        defaultQualityOptions = listOf("320 kbps MP3", "256 kbps M4A", "128 kbps MP3", "FLAC Lossless")
    ),
    PHOTO(
        label = "Photo / Image",
        shortLabel = "Photo",
        subfolder = "Photos",
        defaultExtension = "jpg",
        defaultQualityOptions = listOf("Original Quality", "High Res JPG", "Lossless PNG")
    )
}
