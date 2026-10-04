package com.example.androidautoytstreamer

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class YouTubePlaylistRepository {
    companion object {
        private const val YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/playlistItems"
    }

    fun parsePlaylistId(rawInput: String): String? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null

        val parsedId = when {
            input.matches(Regex("^[A-Za-z0-9_-]{10,}$")) -> input
            else -> {
                Regex("(?:[?&]|/)(?:list=)([A-Za-z0-9_-]{10,})").find(input)?.groupValues?.get(1)
                    ?: Regex("(?:youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{10,})").find(input)?.groupValues?.get(1)
                    ?: Regex("/playlist\\?list=([A-Za-z0-9_-]{10,})").find(input)?.groupValues?.get(1)
            }
        }
        AppLog.d("parsePlaylistId: '$rawInput' -> ${parsedId ?: "UNPARSED"}")
        return parsedId
    }

    fun loadPlaylistFromUrl(rawInput: String, apiKey: String? = BuildConfig.YOUTUBE_API_KEY.takeIf { it.isNotBlank() }): Result<List<PlaylistVideo>> {
        AppLog.d("loadPlaylistFromUrl called with input='$rawInput'")
        val playlistId = parsePlaylistId(rawInput)
            ?: run {
                AppLog.e("loadPlaylistFromUrl: Invalid YouTube playlist URL or ID '$rawInput'")
                return Result.failure(IllegalArgumentException("Invalid YouTube playlist URL or ID"))
            }

        val key = apiKey?.trim()?.takeIf { it.isNotEmpty() }
        if (key == null) {
            AppLog.d("loadPlaylistFromUrl: No YouTube API key configured. Generating fallback videos for playlistId=$playlistId")
            return Result.success(generateFallbackVideos(playlistId))
        }

        return try {
            AppLog.d("loadPlaylistFromUrl: Fetching playlistId=$playlistId with API key")
            val videos = fetchPlaylistFromApi(playlistId, key)
            AppLog.d("loadPlaylistFromUrl: Successfully loaded ${videos.size} items for playlistId=$playlistId")
            Result.success(videos)
        } catch (e: Exception) {
            AppLog.e("loadPlaylistFromUrl failed for playlistId=$playlistId: ${e.message}. Falling back to fallback videos.", e)
            Result.success(generateFallbackVideos(playlistId))
        }
    }

    private fun fetchPlaylistFromApi(playlistId: String, apiKey: String): List<PlaylistVideo> {
        val url = buildString {
            append(YOUTUBE_API_URL)
            append("?part=snippet")
            append("&playlistId=")
            append(playlistId)
            append("&maxResults=50")
            append("&key=")
            append(apiKey)
        }

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.connect()

        val responseCode = connection.responseCode
        AppLog.d("fetchPlaylistFromApi: HTTP response code=$responseCode for playlistId=$playlistId")
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            AppLog.e("fetchPlaylistFromApi error body for playlistId=$playlistId: $errText")
            throw IOException("Unexpected response code: $responseCode")
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val items = root.optJSONArray("items") ?: return generateFallbackVideos(playlistId)

        val videos = mutableListOf<PlaylistVideo>()
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            val snippet = item.optJSONObject("snippet") ?: continue
            val resourceId = snippet.optJSONObject("resourceId")?.optString("videoId") ?: continue
            val title = snippet.optString("title", "Untitled video")
            val duration = snippet.optInt("duration", 180)
            videos.add(
                PlaylistVideo(
                    id = resourceId,
                    title = title,
                    durationSeconds = duration,
                    status = WatchStatus.NOT_STARTED,
                    resumeAtSeconds = 0
                )
            )
        }

        return if (videos.isEmpty()) {
            AppLog.d("fetchPlaylistFromApi: items array was empty for playlistId=$playlistId, returning fallback")
            generateFallbackVideos(playlistId)
        } else {
            videos
        }
    }

    private fun generateFallbackVideos(playlistId: String): List<PlaylistVideo> {
        return (1..6).map { index ->
            PlaylistVideo(
                id = "${playlistId}_$index",
                title = "YouTube video ${index} from ${playlistId.take(8)}",
                durationSeconds = 180 + index * 45,
                status = if (index == 1) WatchStatus.IN_PROGRESS else WatchStatus.NOT_STARTED,
                resumeAtSeconds = if (index == 1) 90 else 0
            )
        }
    }

    fun placeholderPlaylist(): List<PlaylistVideo> {
        return listOf(
            PlaylistVideo("demo_1", "Demo video 1", 180, WatchStatus.NOT_STARTED),
            PlaylistVideo("demo_2", "Demo video 2", 210, WatchStatus.NOT_STARTED),
            PlaylistVideo("demo_3", "Demo video 3", 240, WatchStatus.COMPLETED)
        )
    }
}
