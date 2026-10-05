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
        "https://pipedapi.tokhmi.xyz",
        "https://pipedapi.moomoo.me",
        "https://pipedapi.synclick.org",
        "https://pipedapi.palvelintila.fi",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.privacy.com.de"
    )

    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.tux.pizza",
        "https://invidious.nerdvpn.de",
        "https://invidious.flokinet.to",
        "https://invidious.drgns.space",
        "https://vid.puffyan.us"
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
                    AppLog.d("YouTubeStreamResolver: Resolved videoId=$videoId from Piped ($instance)")
                    return@withContext Result.success(streamUrl)
                }
            }.onFailure { e ->
                AppLog.d("YouTubeStreamResolver: Piped instance $instance failed for videoId=$videoId (${e.message})")
            }
        }

        for (instance in INVIDIOUS_INSTANCES) {
            runCatching {
                val streamUrl = fetchAudioUrlFromInvidious(instance, videoId)
                if (!streamUrl.isNullOrBlank()) {
                    cache[videoId] = streamUrl
                    AppLog.d("YouTubeStreamResolver: Resolved videoId=$videoId from Invidious ($instance)")
                    return@withContext Result.success(streamUrl)
                }
            }.onFailure { e ->
                AppLog.d("YouTubeStreamResolver: Invidious instance $instance failed for videoId=$videoId (${e.message})")
            }
        }

        AppLog.e("YouTubeStreamResolver: All Piped and Invidious instances failed for videoId=$videoId.")
        Result.failure(IOException("Unable to resolve direct audio stream for video $videoId"))
    }

    private fun fetchAudioUrlFromPiped(baseUrl: String, videoId: String): String? {
        val urlString = "$baseUrl/streams/$videoId"
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
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

    private fun fetchAudioUrlFromInvidious(baseUrl: String, videoId: String): String? {
        val urlString = "$baseUrl/api/v1/videos/$videoId"
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.connect()

        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            throw IOException("Invidious instance $baseUrl returned HTTP $code")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(responseText)
        val adaptiveFormats = json.optJSONArray("adaptiveFormats") ?: return null

        if (adaptiveFormats.length() == 0) return null

        var selectedUrl: String? = null
        for (i in 0 until adaptiveFormats.length()) {
            val format = adaptiveFormats.optJSONObject(i) ?: continue
            val streamUrl = format.optString("url")
            val type = format.optString("type", "")

            if (streamUrl.isNotBlank() && type.startsWith("audio/")) {
                if (type.contains("audio/mp4") || type.contains("m4a")) {
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
