package com.example.androidautoytstreamer

enum class WatchStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

data class PlaylistVideo(
    val id: String,
    val title: String,
    val durationSeconds: Int = 0,
    val status: WatchStatus = WatchStatus.NOT_STARTED,
    val resumeAtSeconds: Int = 0
)

