package com.example.androidautoytstreamer

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val TAG = "AndroidAutoYT"
    private const val LOG_FILE_NAME = "app-log.txt"
    private val lock = Any()

    @Volatile
    private var logFile: File? = null

    fun initialize(context: Context) {
        initialize(context.filesDir)
    }

    fun initialize(filesDir: File) {
        synchronized(lock) {
            if (logFile == null) {
                logFile = File(filesDir, LOG_FILE_NAME)
            }
        }
    }

    fun d(message: String) {
        runCatching { Log.d(TAG, message) }
        append("D", message, null)
    }

    fun e(message: String, throwable: Throwable? = null) {
        runCatching { Log.e(TAG, message, throwable) }
        append("E", message, throwable)
    }

    fun filePath(): String? = logFile?.absolutePath

    private fun append(level: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = buildString {
            append(timestamp)
            append(' ')
            append(level)
            append(' ')
            append(message)
            if (throwable != null) {
                append(" :: ")
                append(throwable::class.java.simpleName)
                append(": ")
                append(throwable.message ?: "")
            }
        }

        synchronized(lock) {
            runCatching {
                file.appendText(line + System.lineSeparator())
            }
        }
    }
}
