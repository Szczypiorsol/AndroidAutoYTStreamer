package com.example.androidautoytstreamer

import java.util.Locale

class YouTubePlaylistRepository {
    fun parsePlaylistId(rawInput: String): String? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null

        if (input.matches(Regex("^[A-Za-z0-9_-]{10,}$"))) {
            return input
        }

        val urlMatch = Regex("(?:[?&]|/)(?:list=|v=)([A-Za-z0-9_-]{10,})").find(input)
        if (urlMatch != null) {
            return urlMatch.groupValues[1]
        }

        val shortLinkMatch = Regex("youtu\\.be/([A-Za-z0-9_-]{10,})").find(input)
        if (shortLinkMatch != null) {
            return shortLinkMatch.groupValues[1]
        }

        return null
    }

    fun loadPlaylistFromUrl(rawInput: String): Result<List<PlaylistVideo>> {
        val playlistId = parsePlaylistId(rawInput)
            ?: return Result.failure(IllegalArgumentException("Invalid YouTube playlist URL or ID"))

        val generatedVideos = (1..6).map { index ->
            PlaylistVideo(
                id = "${playlistId}_$index",
                title = "YouTube video ${index} from ${playlistId.take(8)}",
                durationSeconds = 180 + index * 45,
                status = if (index == 1) WatchStatus.IN_PROGRESS else WatchStatus.NOT_STARTED,
                resumeAtSeconds = if (index == 1) 90 else 0
            )
        }

        return Result.success(generatedVideos)
    }

    fun placeholderPlaylist(): List<PlaylistVideo> {
        return listOf(
            PlaylistVideo("demo_1", "Demo video 1", 180, WatchStatus.NOT_STARTED),
            PlaylistVideo("demo_2", "Demo video 2", 210, WatchStatus.NOT_STARTED),
            PlaylistVideo("demo_3", "Demo video 3", 240, WatchStatus.COMPLETED)
        )
    }
}
