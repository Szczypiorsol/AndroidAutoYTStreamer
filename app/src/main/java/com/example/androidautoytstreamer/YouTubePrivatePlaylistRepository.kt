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
        val token = authManager.getAccessToken() ?: return Result.failure(IllegalStateException("Google sign-in required"))

        val url = URL("https://www.googleapis.com/youtube/v3/playlists?part=snippet,status&mine=true&maxResults=25")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        return try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
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

            Result.success(playlists)
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Unable to load private YouTube playlists"))
        }
    }
}

data class PlaylistSummary(
    val id: String,
    val title: String,
    val description: String
)

