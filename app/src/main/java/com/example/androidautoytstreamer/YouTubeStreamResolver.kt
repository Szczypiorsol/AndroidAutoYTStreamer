package com.example.androidautoytstreamer

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object YouTubeStreamResolver {
    private val cache = ConcurrentHashMap<String, String>()

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.privacydev.net",
        "https://pipedapi.drgns.space",
        "https://pipedapi.mha.fi",
        "https://piped-api.garudalinux.org"
    )

    suspend fun resolveAudioStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        val cached = cache[videoId]
        if (!cached.isNullOrBlank()) {
            AppLog.d("YouTubeStreamResolver: Cache hit for videoId=$videoId")
            return@withContext Result.success(cached)
        }

        AppLog.d("YouTubeStreamResolver: Resolving direct stream URL for videoId=$videoId...")

        for (instance in PIPED_INSTANCES) {
            runCatching {
                val streamUrl = fetchAudioUrlFromPiped(instance, videoId)
                if (!streamUrl.isNullOrBlank()) {
                    cache[videoId] = streamUrl
                    AppLog.d("YouTubeStreamResolver: Successfully resolved videoId=$videoId from $instance")
                    return@withContext Result.success(streamUrl)
                }
            }.onFailure { e ->
                AppLog.d("YouTubeStreamResolver: Instance $instance failed for videoId=$videoId: ${e.message}")
            }
        }

        val fallbackUrl = "https://www.youtube.com/watch?v=$videoId"
        AppLog.e("YouTubeStreamResolver: All instances failed for videoId=$videoId. Using web fallback.")
        Result.failure(IOException("Unable to resolve direct audio stream for video $videoId"))
    }

    private fun fetchAudioUrlFromPiped(baseUrl: String, videoId: String): String? {
        val urlString = "$baseUrl/streams/$videoId"
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.connect()

        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            throw IOException("Piped instance $baseUrl returned HTTP $code")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(responseText)
        val audioStreams = json.optJSONArray("audioStreams") ?: return null

        if (audioStreams.length() == 0) return null

        var selectedUrl: String? = null
        for (i in 0 until audioStreams.length()) {
            val stream = audioStreams.optJSONObject(i) ?: continue
            val streamUrl = stream.optString("url")
            val mimeType = stream.optString("mimeType", "")
            val format = stream.optString("format", "")

            if (streamUrl.isNotBlank()) {
                if (mimeType.contains("audio/mp4") || format.equals("M4A", ignoreCase = true)) {
                    return streamUrl
                }
                if (selectedUrl == null) {
                    selectedUrl = streamUrl
                }
            }
        }

        return selectedUrl
    }

    fun clearCache() {
        cache.clear()
    }
}
