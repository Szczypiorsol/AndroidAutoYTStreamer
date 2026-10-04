package com.example.androidautoytstreamer

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class YouTubePrivatePlaylistRepository(
    private val context: Context,
    private val authManager: GoogleAuthManager
) {
    fun loadPlaylists(): Result<List<PlaylistSummary>> {
        AppLog.initialize(context)
        AppLog.d("Loading private playlists from YouTube API")
        val token = authManager.getAccessToken()
        if (token == null) {
            AppLog.e("loadPlaylists failed: Google access token is null")
            return Result.failure(IllegalStateException("Google sign-in required"))
        }

        val url = URL("https://www.googleapis.com/youtube/v3/playlists?part=snippet,status&mine=true&maxResults=25")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        return try {
            val code = connection.responseCode
            AppLog.d("loadPlaylists HTTP response code=$code")
            if (code != HttpURLConnection.HTTP_OK) {
                val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                AppLog.e("loadPlaylists HTTP error $code body: $errBody")
                return Result.failure(IOException("YouTube API call failed: $code"))
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(response)
            val items = root.optJSONArray("items") ?: return Result.success(emptyList())

            val playlists = mutableListOf<PlaylistSummary>()
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val snippet = item.optJSONObject("snippet") ?: continue
                val id = item.optString("id")
                val title = snippet.optString("title", "Untitled playlist")
                val description = snippet.optString("description", "")
                playlists.add(PlaylistSummary(id, title, description))
            }

            AppLog.d("Loaded private playlists count=${playlists.size}")
            Result.success(playlists)
        } catch (exception: Exception) {
            AppLog.e("Unable to load private YouTube playlists", exception)
            Result.failure(IllegalStateException("Unable to load private YouTube playlists"))
        }
    }

    fun loadPlaylistItems(playlistId: String): Result<List<PlaylistVideo>> {
        AppLog.initialize(context)
        AppLog.d("Loading items for playlistId=$playlistId")
        val token = authManager.getAccessToken()
        if (token == null) {
            AppLog.e("loadPlaylistItems failed for playlistId=$playlistId: Google access token is null")
            return Result.failure(IllegalStateException("Google sign-in required"))
        }

        val url = URL(
            "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,status&playlistId=$playlistId&maxResults=50"
        )
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        return try {
            val code = connection.responseCode
            AppLog.d("loadPlaylistItems HTTP response code=$code for playlistId=$playlistId")
            if (code != HttpURLConnection.HTTP_OK) {
                val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                AppLog.e("loadPlaylistItems HTTP error $code body for playlistId=$playlistId: $errBody")
                return Result.failure(IOException("YouTube playlist items call failed: $code"))
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(response)
            val items = root.optJSONArray("items") ?: return Result.success(emptyList())

            val videos = mutableListOf<PlaylistVideo>()
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val snippet = item.optJSONObject("snippet") ?: continue
                val resourceId = snippet.optJSONObject("resourceId")?.optString("videoId") ?: continue
                val title = snippet.optString("title", "Untitled video")
                val duration = 180 + index * 30
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

            AppLog.d("Loaded playlist items count=${videos.size} for playlistId=$playlistId")
            Result.success(videos)
        } catch (exception: Exception) {
            AppLog.e("Unable to load private YouTube playlist items for playlistId=$playlistId", exception)
            Result.failure(IllegalStateException("Unable to load private YouTube playlist items"))
        }
    }
}

data class PlaylistSummary(
    val id: String,
    val title: String,
    val description: String
)
