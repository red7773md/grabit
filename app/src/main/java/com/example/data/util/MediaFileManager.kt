package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.DeviceStorageInfo
import com.example.model.DownloadItem
import com.example.model.LinkMetadata
import com.example.model.MediaType
import com.example.model.Platform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.max

data class StreamProgressUpdate(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progress: Float,
    val speedLabel: String,
    val etaLabel: String
)

/** A link could not be turned into a downloadable file. The message is meant to be shown to the user. */
class MediaResolutionException(message: String) : IllegalStateException(message)

object MediaFileManager {

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun normalizeUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""
        // Extract first http/https URL if the user pasted share text with surrounding words
        val urlRegex = Regex("(https?://[^\\s\"'<>]+)", RegexOption.IGNORE_CASE)
        val match = urlRegex.find(trimmed)
        if (match != null) {
            return match.value
        }
        return if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true) &&
            trimmed.contains(".") && !trimmed.contains(" ")
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    fun isValidHttpUrl(input: String): Boolean {
        val normalized = normalizeUrl(input)
        return try {
            val uri = URI(normalized)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank() && uri.host.contains(".")
        } catch (_: Exception) {
            false
        }
    }

    fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun getRootGrabItDirectory(context: Context): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Downloads")
        val dir = File(root, "GrabIt")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getDownloadDirectory(
        context: Context,
        platformFolder: String,
        mediaFolder: String,
        organizeByFolders: Boolean = true
    ): File {
        val grabItRoot = getRootGrabItDirectory(context)
        val dir = if (organizeByFolders) {
            File(grabItRoot, "$platformFolder/$mediaFolder")
        } else {
            grabItRoot
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Extracts YouTube / YouTube Music video ID from any format (watch?v=, youtu.be, shorts, embed).
     */
    fun extractYouTubeVideoId(url: String): String? {
        val lower = url.lowercase()
        if (!lower.contains("youtube.com") && !lower.contains("youtu.be")) return null
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path.orEmpty()
            when {
                host.contains("youtu.be") -> {
                    path.removePrefix("/").substringBefore("?").ifBlank { null }
                }
                path.contains("/shorts/") -> {
                    path.substringAfter("/shorts/").substringBefore("?").substringBefore("/").ifBlank { null }
                }
                path.contains("/embed/") -> {
                    path.substringAfter("/embed/").substringBefore("?").substringBefore("/").ifBlank { null }
                }
                else -> {
                    uri.query?.split("&")
                        ?.firstOrNull { it.startsWith("v=") }
                        ?.substringAfter("v=")
                        ?.substringBefore("&")
                        ?.ifBlank { null }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Performs real HTTP inspection on the user-supplied URL:
     * 1. Detects platform (YouTube, YouTube Music, TikTok, Facebook, Twitter, Instagram, etc.).
     * 2. Checks platform-specific APIs (Tikwm for TikTok, YouTube videoId/oEmbed, Vimeo, SoundCloud).
     * 3. Parses OpenGraph / Twitter Card HTML tags.
     * 4. Resolves direct media stream and high-res thumbnail/cover URLs.
     */
    suspend fun inspectLinkMetadata(
        rawUrl: String,
        preferredMediaType: MediaType
    ): LinkMetadata = withContext(Dispatchers.IO) {
        val url = normalizeUrl(rawUrl)
        val platform = Platform.detectFromUrl(url)
        val fallbackTitle = deriveTitleFromUrl(url, platform)
        val fallbackAuthor = deriveAuthorFromUrl(url, platform)

        var resolvedTitle = fallbackTitle
        var resolvedAuthor = fallbackAuthor
        var thumbnailUrl = ""
        var resolvedMediaUrl = url
        var detectedType = inferMediaTypeFromUrl(url, preferredMediaType)
        var contentLength = 0L
        var mimeType = ""
        var isDirectStream = false

        // Special Provider 1: TikTok via Tikwm API for direct 1080p MP4 & audio streams
        if (platform == Platform.TIKTOK) {
            fetchTikTokMetadata(url)?.let { tikInfo ->
                if (tikInfo.title.isNotBlank()) resolvedTitle = tikInfo.title
                if (tikInfo.author.isNotBlank()) resolvedAuthor = tikInfo.author
                if (tikInfo.coverUrl.isNotBlank()) thumbnailUrl = tikInfo.coverUrl
                if (tikInfo.playUrl.isNotBlank()) {
                    resolvedMediaUrl = if (preferredMediaType == MediaType.AUDIO && tikInfo.musicUrl.isNotBlank()) {
                        tikInfo.musicUrl
                    } else {
                        tikInfo.playUrl
                    }
                    isDirectStream = true
                    detectedType = preferredMediaType
                }
            }
        }

        // Special Provider 2: YouTube & YouTube Music thumbnail resolution
        val ytVideoId = extractYouTubeVideoId(url)
        if (ytVideoId != null) {
            thumbnailUrl = "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
        }

        // Step 1: Query oEmbed metadata for YouTube, TikTok, Reddit, Vimeo, SoundCloud
        fetchOEmbedMetadata(url, platform)?.let { oembed ->
            if (oembed.title.isNotBlank()) resolvedTitle = oembed.title
            if (oembed.author.isNotBlank()) resolvedAuthor = oembed.author
            if (oembed.thumbnailUrl.isNotBlank()) thumbnailUrl = oembed.thumbnailUrl
        }

        // Step 2: Inspect via HTTP GET (for direct media header or OpenGraph parsing)
        if (!isDirectStream) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/*,video/*,audio/*,*/*;q=0.8")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val finalUrl = response.request.url.toString()
                    val contentTypeHeader = response.header("Content-Type")?.lowercase() ?: ""
                    val contentLengthHeader = response.header("Content-Length")?.toLongOrNull() ?: 0L
                    val disposition = response.header("Content-Disposition") ?: ""

                    mimeType = contentTypeHeader.substringBefore(";").trim()

                    if (contentTypeHeader.startsWith("video/") ||
                        contentTypeHeader.startsWith("audio/") ||
                        contentTypeHeader.startsWith("image/") ||
                        contentTypeHeader.startsWith("application/octet-stream") ||
                        isDirectMediaExtension(finalUrl)
                    ) {
                        isDirectStream = true
                        resolvedMediaUrl = finalUrl
                        contentLength = max(0L, contentLengthHeader)
                        detectedType = when {
                            contentTypeHeader.startsWith("video/") -> MediaType.VIDEO
                            contentTypeHeader.startsWith("audio/") -> MediaType.AUDIO
                            contentTypeHeader.startsWith("image/") -> MediaType.PHOTO
                            else -> inferMediaTypeFromUrl(finalUrl, preferredMediaType)
                        }
                        if (detectedType == MediaType.PHOTO && thumbnailUrl.isBlank()) {
                            thumbnailUrl = finalUrl
                        }
                        extractFilenameFromDisposition(disposition)?.let { fileName ->
                            resolvedTitle = fileName.substringBeforeLast(".")
                        }
                    } else if (contentTypeHeader.contains("text/html") || contentTypeHeader.contains("application/xhtml")) {
                        // Read up to 120 KB of HTML to extract OpenGraph / meta tags
                        val htmlSnippet = readBoundedText(response.body?.byteStream(), 120 * 1024)
                        val parsed = parseHtmlMediaMetadata(htmlSnippet, finalUrl)

                        if (parsed.title.isNotBlank() && resolvedTitle == fallbackTitle) {
                            resolvedTitle = parsed.title
                        }
                        if (parsed.author.isNotBlank() && resolvedAuthor == fallbackAuthor) {
                            resolvedAuthor = parsed.author
                        }
                        if (parsed.imageUrl.isNotBlank() && thumbnailUrl.isBlank()) {
                            thumbnailUrl = parsed.imageUrl
                        }

                        val candidateStream = when (preferredMediaType) {
                            MediaType.PHOTO -> parsed.imageUrl.ifBlank { thumbnailUrl }
                            MediaType.AUDIO -> parsed.audioUrl.ifBlank { parsed.videoUrl }
                            MediaType.VIDEO -> parsed.videoUrl
                        }

                        // The download step checks the real Content-Type, so no file extension is required here.
                        if (candidateStream.isNotBlank()) {
                            resolvedMediaUrl = candidateStream
                            isDirectStream = isDirectMediaExtension(candidateStream)
                        }
                    }
                }
            } catch (_: Exception) {
                // Network timeout or bot gate; keep derived metadata
            }
        }

        // Step 3: Check content length via HEAD if direct stream
        val targetProbeUrl = if (preferredMediaType == MediaType.PHOTO && thumbnailUrl.isNotBlank()) {
            thumbnailUrl
        } else {
            resolvedMediaUrl
        }

        if (contentLength <= 0L && isDirectMediaUrl(targetProbeUrl)) {
            try {
                val headReq = Request.Builder()
                    .url(targetProbeUrl)
                    .head()
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .build()
                httpClient.newCall(headReq).execute().use { headResp ->
                    val len = headResp.header("Content-Length")?.toLongOrNull() ?: 0L
                    if (len > 0L) contentLength = len
                    val headType = headResp.header("Content-Type")?.lowercase() ?: ""
                    if (headType.startsWith("video/") || headType.startsWith("audio/") || headType.startsWith("image/")) {
                        isDirectStream = true
                        mimeType = headType.substringBefore(";").trim()
                    }
                }
            } catch (_: Exception) {}
        }

        LinkMetadata(
            originalUrl = url,
            resolvedMediaUrl = targetProbeUrl.ifBlank { url },
            thumbnailUrl = thumbnailUrl,
            title = resolvedTitle,
            author = resolvedAuthor,
            platform = platform,
            detectedMediaType = detectedType,
            contentLength = contentLength,
            mimeType = mimeType,
            isDirectStreamAvailable = isDirectStream
        )
    }

    /**
     * Downloads the media file for [item] into the GrabIt folder.
     *
     * Candidate links are tried in order: the resolved media URL first, then the link the user gave.
     * A candidate is saved only when the server returns media of the selected type. Web pages and
     * mismatched media are rejected, so GrabIt never saves a placeholder or demo file in their place.
     * If no direct file is found (and the type is video or audio), the link is handed to yt-dlp.
     *
     * @throws MediaResolutionException with a user-facing message when no usable file is found.
     */
    suspend fun streamDownloadMediaFile(
        context: Context,
        item: DownloadItem,
        organizeByFolders: Boolean,
        onProgress: suspend (StreamProgressUpdate) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val targetDir = getDownloadDirectory(
            context = context,
            platformFolder = item.platform.subfolder,
            mediaFolder = item.mediaType.subfolder,
            organizeByFolders = organizeByFolders
        )

        val sanitizedTitle = item.title
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .take(40)
            .trim('_')
            .ifEmpty { "grabit_${item.id.take(8)}" }
        val baseName = "${sanitizedTitle}_${item.id.take(6)}"
        val partFile = File(targetDir, "$baseName.part")

        try {
            // 1. Direct file links, and media that a page points to with a <video>, <audio> or og: tag.
            val saved = downloadFirstMatchingCandidate(
                candidates = buildDownloadCandidates(item),
                partFile = partFile,
                item = item,
                onProgress = onProgress
            )

            val finalFile = File(targetDir, baseName + extensionFor(saved.contentType, saved.url, item.mediaType))
            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }
            finalFile
        } catch (directError: MediaResolutionException) {
            // 2. Pages that don't expose a plain file (YouTube, Instagram, and so on): let yt-dlp handle them.
            if (item.mediaType == MediaType.PHOTO) throw directError
            downloadWithYtDlp(context, item, targetDir, baseName, onProgress)
        }
    }

    /** Netscape-format cookies file that yt-dlp can use for signed-in requests. Kept in app-private storage. */
    fun youTubeCookieFile(context: Context): File = File(context.noBackupFilesDir, "youtube-cookies.txt")

    fun hasYouTubeCookies(context: Context): Boolean =
        youTubeCookieFile(context).let { it.exists() && it.length() > 0L }

    fun deleteYouTubeCookies(context: Context): Boolean = youTubeCookieFile(context).delete()

    /**
     * Validates and stores a Netscape-format cookies file exported from the owner's own browser.
     * Returns an error message, or null when the file was saved.
     */
    fun saveYouTubeCookies(context: Context, text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "The selected file is empty."
        if (trimmed.length > 2_000_000) return "The selected file is too large to be a cookies file."

        // Netscape lines are tab-separated. #HttpOnly_ lines are real cookies, so they count too.
        val hasYouTubeCookie = trimmed.lineSequence().any { line ->
            val isCookieLine = !line.startsWith("#") || line.startsWith("#HttpOnly_")
            isCookieLine && line.contains("youtube.com") && line.count { it == '\t' } >= 6
        }
        if (!hasYouTubeCookie) {
            return "No YouTube cookies were found in that file. Export cookies for youtube.com in Netscape format and try again."
        }

        youTubeCookieFile(context).writeText(trimmed + "\n")
        return null
    }

    @Volatile
    private var ytDlpReady = false

    private fun ensureYtDlpReady(context: Context) {
        if (ytDlpReady) return
        synchronized(this) {
            if (ytDlpReady) return
            try {
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
                ytDlpReady = true
            } catch (e: Exception) {
                throw MediaResolutionException("yt-dlp couldn't start on this device: ${e.message ?: "unknown error"}")
            }
        }
    }

    /**
     * Downloads through yt-dlp. It writes `<baseName>.<ext>` into [targetDir], and it resumes partial
     * downloads by itself. Progress comes from a callback on a reader thread, so it is forwarded through a
     * channel to the coroutine. Pausing or cancelling the download kills the yt-dlp process.
     */
    private suspend fun downloadWithYtDlp(
        context: Context,
        item: DownloadItem,
        targetDir: File,
        baseName: String,
        onProgress: suspend (StreamProgressUpdate) -> Unit
    ): File {
        ensureYtDlpReady(context)

        val cookies = youTubeCookieFile(context).takeIf { it.exists() && it.length() > 0L }
        val request = buildYtDlpRequest(item, File(targetDir, "$baseName.%(ext)s").absolutePath, cookies)

        val outerJob: Job? = currentCoroutineContext()[Job]
        val updates = Channel<StreamProgressUpdate>(Channel.CONFLATED)

        coroutineScope {
            launch {
                for (update in updates) onProgress(update)
            }
            try {
                withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(
                        request,
                        item.id,
                        callback = { percent, etaSeconds, line ->
                            if (outerJob?.isActive == false) {
                                YoutubeDL.getInstance().destroyProcessById(item.id)
                            }
                            val speed = Regex("""at\s+(\S+/s)""").find(line)?.groupValues?.get(1) ?: "Downloading"
                            val total = item.fileSize
                            updates.trySend(
                                StreamProgressUpdate(
                                    downloadedBytes = if (total > 0L) (total * percent / 100f).toLong() else 0L,
                                    totalBytes = total,
                                    progress = (percent / 100f).coerceIn(0.01f, 0.99f),
                                    speedLabel = speed,
                                    etaLabel = if (etaSeconds > 0L) formatEta(etaSeconds) else "Streaming..."
                                )
                            )
                        }
                    )
                }
            } catch (_: YoutubeDL.CanceledException) {
                throw CancellationException("Download cancelled")
            } catch (e: YoutubeDLException) {
                if (outerJob?.isActive == false) throw CancellationException("Download cancelled")
                throw MediaResolutionException(friendlyYtDlpError(e.message))
            } finally {
                updates.close()
            }
        }

        val pattern = Regex("^${Regex.escape(baseName)}\\.(mp4|mkv|webm|mp3|m4a|flac|ogg|opus|wav|aac)$")
        return targetDir.listFiles()
            ?.filter { pattern.matches(it.name) }
            ?.maxByOrNull { it.lastModified() }
            ?: throw MediaResolutionException("yt-dlp finished, but the output file wasn't found.")
    }

    private fun buildYtDlpRequest(item: DownloadItem, outputTemplate: String, cookies: File?): YoutubeDLRequest {
        val request = YoutubeDLRequest(item.sourceUrl)
            .addOption("--no-playlist")
            .addOption("-o", outputTemplate)
        cookies?.let { request.addOption("--cookies", it.absolutePath) }

        when (item.mediaType) {
            MediaType.VIDEO -> {
                val height = ytDlpHeight(item.quality)
                val limit = if (height != null) "[height<=$height]" else ""
                // Prefer MP4/M4A streams so they can be merged without re-encoding.
                request.addOption("-f", "bv*[ext=mp4]$limit+ba[ext=m4a]/bv*$limit+ba/b$limit/best")
                request.addOption("--merge-output-format", "mp4")
            }
            MediaType.AUDIO -> {
                val (format, quality) = ytDlpAudioFormat(item.quality)
                request.addOption("-x")
                request.addOption("--audio-format", format)
                request.addOption("--audio-quality", quality)
            }
            MediaType.PHOTO -> throw MediaResolutionException(
                "Photos can't be downloaded from this link. Choose Video or Audio."
            )
        }
        return request
    }

    private fun ytDlpHeight(quality: String): Int? {
        Regex("(\\d{3,4})p", RegexOption.IGNORE_CASE).find(quality)?.let {
            return it.groupValues[1].toInt()
        }
        return if (quality.contains("4K", ignoreCase = true)) 2160 else null
    }

    private fun ytDlpAudioFormat(quality: String): Pair<String, String> = when {
        quality.contains("FLAC", ignoreCase = true) -> "flac" to "0"
        quality.contains("M4A", ignoreCase = true) -> "m4a" to "256K"
        quality.contains("128") -> "mp3" to "128K"
        else -> "mp3" to "320K"
    }

    private fun friendlyYtDlpError(raw: String?): String {
        val text = raw.orEmpty()
        return when {
            text.contains("Sign in to confirm", ignoreCase = true) ||
                text.contains("not a bot", ignoreCase = true) ->
                "YouTube asked for sign-in verification. Add a YouTube cookies file in Settings and try again."
            text.contains("Private video", ignoreCase = true) -> "This video is private."
            text.contains("Video unavailable", ignoreCase = true) -> "This video is unavailable."
            text.contains("Unsupported URL", ignoreCase = true) -> "yt-dlp doesn't support this link."
            else -> text.lineSequence()
                .map { it.trim() }
                .lastOrNull { it.startsWith("ERROR", ignoreCase = true) }
                ?.take(240)
                ?: "yt-dlp couldn't download this link."
        }
    }

    private fun buildDownloadCandidates(item: DownloadItem): List<String> {
        val isYouTube = item.platform == Platform.YOUTUBE || item.platform == Platform.YT_MUSIC
        return buildList {
            add(item.resolvedMediaUrl)
            // A YouTube thumbnail is only a card image, never the photo the user asked for.
            if (item.mediaType == MediaType.PHOTO && !isYouTube) add(item.thumbnailUrl)
            add(item.sourceUrl)
        }
            .filter { it.startsWith("http", ignoreCase = true) }
            .map { normalizeUrl(it) }
            .distinct()
    }

    private sealed interface CandidateResult {
        data class Saved(val contentType: String) : CandidateResult
        data class Rejected(val reason: String) : CandidateResult
    }

    private class SavedCandidate(val url: String, val contentType: String)

    private suspend fun downloadFirstMatchingCandidate(
        candidates: List<String>,
        partFile: File,
        item: DownloadItem,
        onProgress: suspend (StreamProgressUpdate) -> Unit
    ): SavedCandidate {
        var lastReason = noFileFoundMessage(item.mediaType)
        for (candidate in candidates) {
            when (val result = downloadCandidate(candidate, partFile, item, onProgress)) {
                is CandidateResult.Saved -> return SavedCandidate(candidate, result.contentType)
                is CandidateResult.Rejected -> {
                    lastReason = result.reason
                    if (partFile.exists()) partFile.delete()
                }
            }
        }
        throw MediaResolutionException(lastReason)
    }

    private fun noFileFoundMessage(mediaType: MediaType): String =
        "GrabIt couldn't find a downloadable ${mediaType.label.lowercase()} file at this link. " +
            "Paste a link that points directly to the file."

    /**
     * Streams one candidate URL into [partFile]. Resumes with an HTTP Range request after network
     * drops, and returns [CandidateResult.Rejected] (instead of throwing) when this URL is unusable.
     */
    private suspend fun downloadCandidate(
        url: String,
        partFile: File,
        item: DownloadItem,
        onProgress: suspend (StreamProgressUpdate) -> Unit
    ): CandidateResult {
        var networkFailures = 0
        while (true) {
            val existingBytes = if (partFile.exists()) partFile.length() else 0L
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "*/*")
            if (existingBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingBytes-")
            }

            try {
                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        return CandidateResult.Rejected(
                            "The server refused this link (HTTP ${response.code}). " +
                                "The file may be private or no longer available."
                        )
                    }

                    val contentType = response.header("Content-Type")
                        .orEmpty()
                        .lowercase()
                        .substringBefore(";")
                        .trim()
                    mismatchReason(contentType, url, item.mediaType)?.let { reason ->
                        return CandidateResult.Rejected(reason)
                    }

                    val body = response.body
                        ?: return CandidateResult.Rejected("The server sent no data for this link.")
                    val isResumed = response.code == 206 && existingBytes > 0L
                    val contentLength = body.contentLength()
                    val totalBytes = when {
                        isResumed && contentLength > 0 -> existingBytes + contentLength
                        contentLength > 0 -> contentLength
                        item.fileSize > 0 -> item.fileSize
                        else -> -1L
                    }

                    val buffer = ByteArray(8 * 1024)
                    var bytesDownloaded = if (isResumed) existingBytes else 0L
                    var lastEmitTimeMs = System.currentTimeMillis()
                    var bytesAtLastEmit = bytesDownloaded

                    FileOutputStream(partFile, isResumed).use { output ->
                        body.byteStream().use { input ->
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read == -1) break

                                output.write(buffer, 0, read)
                                bytesDownloaded += read

                                val now = System.currentTimeMillis()
                                val elapsedSinceLastEmit = now - lastEmitTimeMs
                                if (elapsedSinceLastEmit >= 200L) {
                                    val deltaBytes = bytesDownloaded - bytesAtLastEmit
                                    val bytesPerSec = (deltaBytes * 1000L) / elapsedSinceLastEmit
                                    val progressFraction = if (totalBytes > 0) {
                                        (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0.01f, 0.99f)
                                    } else {
                                        0.15f + (bytesDownloaded.toFloat() / (5 * 1024 * 1024f)).coerceAtMost(0.8f)
                                    }
                                    val etaStr = if (totalBytes > bytesDownloaded && bytesPerSec > 0) {
                                        formatEta(((totalBytes - bytesDownloaded) / bytesPerSec).coerceAtLeast(1L))
                                    } else {
                                        "Streaming..."
                                    }

                                    onProgress(
                                        StreamProgressUpdate(
                                            downloadedBytes = bytesDownloaded,
                                            totalBytes = if (totalBytes > 0) totalBytes else bytesDownloaded,
                                            progress = progressFraction,
                                            speedLabel = "${DownloadItem.formatBytes(bytesPerSec)}/s",
                                            etaLabel = etaStr
                                        )
                                    )

                                    lastEmitTimeMs = now
                                    bytesAtLastEmit = bytesDownloaded
                                }
                            }
                        }
                        output.flush()
                    }

                    if (partFile.length() == 0L) {
                        return CandidateResult.Rejected("The server sent an empty file for this link.")
                    }
                    return CandidateResult.Saved(contentType)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: IOException) {
                networkFailures++
                if (networkFailures >= 3) {
                    return CandidateResult.Rejected(
                        "Network error while downloading (${e.message ?: "connection lost"}). " +
                            "Check your connection and try again."
                    )
                }
                delay(2_000L * networkFailures)
            }
        }
    }

    /**
     * Computes real device storage statistics via Android StatFs and actual GrabIt folder files.
     */
    fun getDeviceStorageInfo(context: Context): DeviceStorageInfo {
        return try {
            val grabItRoot = getRootGrabItDirectory(context)
            val statFs = StatFs(grabItRoot.absolutePath)
            val totalDeviceBytes = statFs.totalBytes
            val freeDeviceBytes = statFs.availableBytes
            val usedDeviceBytes = (totalDeviceBytes - freeDeviceBytes).coerceAtLeast(0L)

            var grabItBytes = 0L
            var partBytes = 0L
            var completedCount = 0

            grabItRoot.walkTopDown().forEach { file ->
                if (file.isFile) {
                    if (file.name.endsWith(".part")) {
                        partBytes += file.length()
                    } else {
                        grabItBytes += file.length()
                        completedCount++
                    }
                }
            }

            context.cacheDir?.walkTopDown()?.forEach { file ->
                if (file.isFile) {
                    partBytes += file.length()
                }
            }

            DeviceStorageInfo(
                totalDeviceBytes = totalDeviceBytes,
                freeDeviceBytes = freeDeviceBytes,
                usedDeviceBytes = usedDeviceBytes,
                usedByGrabItBytes = grabItBytes,
                tempCacheBytes = partBytes,
                downloadedFilesCount = completedCount
            )
        } catch (_: Exception) {
            DeviceStorageInfo()
        }
    }

    fun clearTempCache(context: Context): Long {
        var freedBytes = 0L
        try {
            val grabItRoot = getRootGrabItDirectory(context)
            grabItRoot.walkTopDown().forEach { file ->
                if (file.isFile && file.name.endsWith(".part")) {
                    val len = file.length()
                    if (file.delete()) {
                        freedBytes += len
                    }
                }
            }
            context.cacheDir?.listFiles()?.forEach { child ->
                val len = child.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (child.deleteRecursively()) {
                    freedBytes += len
                }
            }
        } catch (_: Exception) {}
        return freedBytes
    }

    fun deletePartialFileForItem(context: Context, item: DownloadItem, organizeByFolders: Boolean) {
        try {
            val targetDir = getDownloadDirectory(
                context = context,
                platformFolder = item.platform.subfolder,
                mediaFolder = item.mediaType.subfolder,
                organizeByFolders = organizeByFolders
            )
            targetDir.listFiles()?.forEach { file ->
                if (file.name.contains(item.id.take(6)) && file.name.endsWith(".part")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun deleteFile(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else true
        } catch (_: Exception) {
            false
        }
    }

    fun openFileWithExternalApp(context: Context, filePath: String, mediaType: MediaType) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Media file does not exist on disk.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val mimeType = when (mediaType) {
                MediaType.VIDEO -> "video/*"
                MediaType.AUDIO -> "audio/*"
                MediaType.PHOTO -> "image/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this media: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareFileWithExternalApp(context: Context, filePath: String, title: String, mediaType: MediaType) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Media file does not exist on disk.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val mimeType = when (mediaType) {
                MediaType.VIDEO -> "video/*"
                MediaType.AUDIO -> "audio/*"
                MediaType.PHOTO -> "image/*"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share media via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareMultipleFilesWithExternalApp(context: Context, items: List<DownloadItem>) {
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>()
        items.forEach { item ->
            if (item.realFilePath.isNotBlank()) {
                val file = File(item.realFilePath)
                if (file.exists()) {
                    try {
                        uris.add(FileProvider.getUriForFile(context, authority, file))
                    } catch (_: Exception) {}
                }
            }
        }

        if (uris.isEmpty()) {
            val joinedUrls = items.joinToString("\n") { "${it.title}: ${it.sourceUrl}" }
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, joinedUrls)
            }
            context.startActivity(Intent.createChooser(textIntent, "Share ${items.size} links").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        }

        try {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ${uris.size} files").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to batch share files: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // --- Helper extraction functions ---

    private data class TikTokMediaInfo(
        val title: String,
        val author: String,
        val playUrl: String,
        val coverUrl: String,
        val musicUrl: String
    )

    private fun fetchTikTokMetadata(url: String): TikTokMediaInfo? {
        return try {
            val formBody = FormBody.Builder()
                .add("url", url)
                .build()
            val req = Request.Builder()
                .url("https://tikwm.com/api/")
                .post(formBody)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optInt("code", -1) != 0) return null
                val data = json.optJSONObject("data") ?: return null
                TikTokMediaInfo(
                    title = data.optString("title", "").trim(),
                    author = data.optJSONObject("author")?.optString("nickname", "")?.trim()
                        ?: data.optString("author", "").trim(),
                    playUrl = data.optString("play", "").trim(),
                    coverUrl = data.optString("cover", "").trim(),
                    musicUrl = data.optString("music", "").trim()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class OEmbedInfo(
        val title: String,
        val author: String,
        val thumbnailUrl: String
    )

    private fun fetchOEmbedMetadata(url: String, platform: Platform): OEmbedInfo? {
        val encoded = try {
            URLEncoder.encode(url, "UTF-8")
        } catch (_: Exception) {
            return null
        }

        val oembedEndpoint = when (platform) {
            Platform.YOUTUBE, Platform.YT_MUSIC -> "https://www.youtube.com/oembed?url=$encoded&format=json"
            Platform.TIKTOK -> "https://www.tiktok.com/oembed?url=$encoded"
            Platform.REDDIT -> "https://www.reddit.com/oembed?url=$encoded"
            Platform.VIMEO -> "https://vimeo.com/api/oembed.json?url=$encoded"
            Platform.SOUNDCLOUD -> "https://soundcloud.com/oembed?url=$encoded&format=json"
            else -> null
        } ?: return null

        return try {
            val req = Request.Builder()
                .url(oembedEndpoint)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val jsonText = resp.body?.string() ?: return null
                val obj = JSONObject(jsonText)
                OEmbedInfo(
                    title = obj.optString("title", "").trim(),
                    author = obj.optString("author_name", "").trim(),
                    thumbnailUrl = obj.optString("thumbnail_url", "").trim()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class HtmlParsedMeta(
        val title: String = "",
        val author: String = "",
        val imageUrl: String = "",
        val videoUrl: String = "",
        val audioUrl: String = ""
    )

    private fun parseHtmlMediaMetadata(html: String, baseUrl: String): HtmlParsedMeta {
        if (html.isBlank()) return HtmlParsedMeta()

        fun findMetaContent(propertyNames: List<String>): String {
            for (prop in propertyNames) {
                val pattern1 = Regex(
                    """<meta[^>]+(?:property|name)\s*=\s*["']${Regex.escape(prop)}["'][^>]+content\s*=\s*["']([^"']+)["']""",
                    RegexOption.IGNORE_CASE
                )
                pattern1.find(html)?.groupValues?.getOrNull(1)?.let {
                    return decodeHtmlEntities(it.trim())
                }
                val pattern2 = Regex(
                    """<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+(?:property|name)\s*=\s*["']${Regex.escape(prop)}["']""",
                    RegexOption.IGNORE_CASE
                )
                pattern2.find(html)?.groupValues?.getOrNull(1)?.let {
                    return decodeHtmlEntities(it.trim())
                }
            }
            return ""
        }

        val ogTitle = findMetaContent(listOf("og:title", "twitter:title")).ifBlank {
            Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it.trim()) } ?: ""
        }
        val ogAuthor = findMetaContent(listOf("og:site_name", "author", "twitter:creator"))
        val ogImage = resolveRelativeUrl(baseUrl, findMetaContent(listOf("og:image:secure_url", "og:image", "twitter:image")))
        val ogVideo = resolveRelativeUrl(
            baseUrl,
            findMetaContent(listOf("og:video:secure_url", "og:video:url", "og:video", "twitter:player:stream")).ifBlank {
                Regex("""<(?:video|source)[^>]+src\s*=\s*["']([^"']+\.(?:mp4|webm|mov)[^"']*)["']""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it.trim()) } ?: ""
            }
        )
        val ogAudio = resolveRelativeUrl(
            baseUrl,
            findMetaContent(listOf("og:audio:secure_url", "og:audio:url", "og:audio")).ifBlank {
                Regex("""<(?:audio|source)[^>]+src\s*=\s*["']([^"']+\.(?:mp3|m4a|wav|ogg)[^"']*)["']""", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)?.let { decodeHtmlEntities(it.trim()) } ?: ""
            }
        )

        return HtmlParsedMeta(
            title = ogTitle,
            author = ogAuthor,
            imageUrl = ogImage,
            videoUrl = ogVideo,
            audioUrl = ogAudio
        )
    }

    private fun resolveRelativeUrl(baseUrl: String, candidate: String): String {
        if (candidate.isBlank()) return ""
        return try {
            URI(baseUrl).resolve(candidate).toString()
        } catch (_: Exception) {
            candidate
        }
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun readBoundedText(stream: java.io.InputStream?, maxBytes: Int): String {
        if (stream == null) return ""
        return try {
            val buffer = ByteArray(4096)
            val out = java.io.ByteArrayOutputStream()
            var total = 0
            while (total < maxBytes) {
                val toRead = minOf(buffer.size, maxBytes - total)
                val read = stream.read(buffer, 0, toRead)
                if (read == -1) break
                out.write(buffer, 0, read)
                total += read
            }
            out.toString("UTF-8")
        } catch (_: Exception) {
            ""
        }
    }

    fun isDirectMediaUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return listOf(
            ".mp4", ".webm", ".mkv", ".mov", ".m4v",
            ".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac",
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
        ).any { path.endsWith(it) }
    }

    fun isDirectAudioUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return listOf(".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac").any { path.endsWith(it) }
    }

    private fun isDirectMediaExtension(url: String): Boolean = isDirectMediaUrl(url)

    private fun inferMediaTypeFromUrl(url: String, fallback: MediaType): MediaType {
        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return when {
            listOf(".mp4", ".webm", ".mkv", ".mov", ".m4v").any { path.endsWith(it) } -> MediaType.VIDEO
            listOf(".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac").any { path.endsWith(it) } -> MediaType.AUDIO
            listOf(".jpg", ".jpeg", ".png", ".webp", ".gif").any { path.endsWith(it) } -> MediaType.PHOTO
            else -> fallback
        }
    }

    private fun mimePrefixFor(mediaType: MediaType): String = when (mediaType) {
        MediaType.VIDEO -> "video/"
        MediaType.AUDIO -> "audio/"
        MediaType.PHOTO -> "image/"
    }

    private fun extensionsFor(mediaType: MediaType): List<String> = when (mediaType) {
        MediaType.VIDEO -> listOf(".mp4", ".webm", ".mkv", ".mov", ".m4v")
        MediaType.AUDIO -> listOf(".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac")
        MediaType.PHOTO -> listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
    }

    private fun hasExtension(url: String, extensions: List<String>): Boolean {
        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return extensions.any { path.endsWith(it) }
    }

    /**
     * Returns null when the response is the media type the user selected. Otherwise returns a
     * user-facing reason the candidate was rejected.
     */
    private fun mismatchReason(contentType: String, url: String, mediaType: MediaType): String? {
        val typeName = mediaType.label.lowercase()
        if (contentType.startsWith(mimePrefixFor(mediaType))) return null

        if (contentType == "text/html" || contentType == "application/xhtml+xml") {
            return "This link opens a web page, not a $typeName file. " +
                "GrabIt can only save links that point directly to a file."
        }

        val otherType = MediaType.values().firstOrNull {
            it != mediaType && contentType.startsWith(mimePrefixFor(it))
        }
        if (otherType != null) {
            return "This link is a ${otherType.label.lowercase()} file. " +
                "Switch the type to ${otherType.shortLabel} and try again."
        }

        // Servers often send a generic type for media files. Accept it only when the URL names a matching file.
        val isGenericType = contentType.isEmpty() ||
            contentType.startsWith("application/") ||
            contentType.startsWith("binary/")
        if (isGenericType && hasExtension(url, extensionsFor(mediaType))) return null

        return "The server didn't identify this link as a $typeName file."
    }

    /** Chooses the saved file extension from the real Content-Type, then from the URL, then the type default. */
    private fun extensionFor(contentType: String, url: String, mediaType: MediaType): String {
        val fromMime = when (contentType) {
            "video/mp4" -> ".mp4"
            "video/webm" -> ".webm"
            "video/quicktime" -> ".mov"
            "video/x-matroska" -> ".mkv"
            "video/x-m4v" -> ".m4v"
            "audio/mpeg", "audio/mp3" -> ".mp3"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> ".m4a"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/flac", "audio/x-flac" -> ".flac"
            "audio/ogg" -> ".ogg"
            "audio/aac" -> ".aac"
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            else -> null
        }
        if (fromMime != null) return fromMime

        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return extensionsFor(mediaType).firstOrNull { path.endsWith(it) }
            ?: ".${mediaType.defaultExtension}"
    }

    private fun extractFilenameFromDisposition(disposition: String): String? {
        if (disposition.isBlank()) return null
        val match = Regex("""filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?""", RegexOption.IGNORE_CASE).find(disposition)
        val raw = match?.groupValues?.getOrNull(1)?.trim() ?: return null
        return try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (_: Exception) {
            raw
        }
    }

    fun deriveTitleFromUrl(url: String, platform: Platform): String {
        return try {
            val uri = URI(url)
            val pathSegments = uri.path?.split("/")?.filter { it.isNotBlank() }.orEmpty()
            val lastSegment = pathSegments.lastOrNull() ?: ""
            val cleanSegment = URLDecoder.decode(lastSegment, "UTF-8")
                .substringBeforeLast(".")
                .replace(Regex("[-_+]"), " ")
                .trim()

            if (cleanSegment.length >= 3 && !cleanSegment.equals("watch", ignoreCase = true)) {
                cleanSegment.replaceFirstChar { it.uppercase() }
            } else {
                val queryV = uri.query?.split("&")?.firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
                if (!queryV.isNullOrBlank()) {
                    "${platform.displayName} Media ($queryV)"
                } else {
                    "${platform.displayName} Media (${uri.host ?: "Web"})"
                }
            }
        } catch (_: Exception) {
            "${platform.displayName} Media"
        }
    }

    private fun deriveAuthorFromUrl(url: String, platform: Platform): String {
        return try {
            val uri = URI(url)
            val segments = uri.path?.split("/")?.filter { it.isNotBlank() }.orEmpty()
            val handle = segments.firstOrNull { it.startsWith("@") }
            when {
                handle != null -> handle
                !uri.host.isNullOrBlank() -> uri.host.removePrefix("www.")
                else -> platform.displayName
            }
        } catch (_: Exception) {
            platform.displayName
        }
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m left"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s left"
            else -> "${seconds}s left"
        }
    }
}
