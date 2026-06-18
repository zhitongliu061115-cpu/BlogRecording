package com.example.blogrecording.common

import android.util.Log

object Logger {
    private const val TAG = "PodcastRecap"

    fun info(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    fun warn(message: String, throwable: Throwable? = null) {
        runCatching { Log.w(TAG, message, throwable) }
    }

    fun error(message: String, throwable: Throwable? = null) {
        runCatching { Log.e(TAG, message, throwable) }
    }
}
