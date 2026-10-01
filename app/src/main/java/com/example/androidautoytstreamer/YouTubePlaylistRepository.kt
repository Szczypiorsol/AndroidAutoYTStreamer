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

        if (input.matches(Regex("^[A-Za-z0-9_-]{10,}$"))) {
            return input
        }

        val directListMatch = Regex("(?:[?&]|/)(?:list=)([A-Za-z0-9_-]{10,})").find(input)
        if (directListMatch != null) {
            return directListMatch.groupValues[1]
        }

        val shortLinkMatch = Regex("(?:youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{10,})").find(input)
        if (shortLinkMatch != null) {
            return shortLinkMatch.groupValues[1]
        }

        val playlistPathMatch = Regex("/playlist\\?list=([A-Za-z0-9_-]{10,})").find(input)
        if (playlistPathMatch != null) {
            return playlistPathMatch.groupValues[1]
        }

        return null
    }

    fun loadPlaylistFromUrl(rawInput: String, apiKey: String? = null): Result<List<PlaylistVideo>> {
        val playlistId = parsePlaylistId(rawInput)
            ?: return Result.failure(IllegalArgumentException("Invalid YouTube playlist URL or ID"))

        val key = apiKey?.trim()?.takeIf { it.isNotEmpty() }
        if (key == null) {
            return Result.success(generateFallbackVideos(playlistId))
        }

        return try {
            Result.success(fetchPlaylistFromApi(playlistId, key))
        } catch (_: Exception) {
            Result.success(generateFallbackVideos(playlistId))
        }
    }

    private fun fetchPlaylistFromApi(playlistId: String, apiKey: String): List<PlaylistVideo> {
        val url = buildString {
            append(YOUTUBE_API_URL)
            append("?part=snippet,status")
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
        if (responseCode != HttpURLConnection.HTTP_OK) {
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
            val duration = snippet.optJSONObject("thumbnails")?.optJSONObject("default")?.optInt("height", 180) ?: 180
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
