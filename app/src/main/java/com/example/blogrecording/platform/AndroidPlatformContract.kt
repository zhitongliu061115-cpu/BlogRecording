package com.example.blogrecording.platform

internal object AndroidPlatformContract {
    const val PACKAGE_NAME = "com.example.blogrecording"
    const val APPLICATION_ID = "com.example.blogrecording"
    const val NAMESPACE = "com.example.blogrecording"
    const val MAIN_ACTIVITY = ".MainActivity"
    const val CAPTURE_FOREGROUND_SERVICE = ".service.CaptureForegroundService"
    const val FOREGROUND_SERVICE_TYPES = "microphone|mediaProjection"
    const val CAPTURE_CHANNEL_ID = "capture_foreground"
    const val CAPTURE_NOTIFICATION_ID = 1001
    const val EXTRA_FOREGROUND_SERVICE_TYPE = "foreground_service_type"
    const val EXTRA_PODCAST_TITLE = "podcast_title"
    const val EXTRA_CAPTURE_SOURCE = "capture_source"
    const val EXTRA_RECORDING_STATE = "recording_state"
    const val EXTRA_ACTIVE_SESSION_ID = "active_session_id"
    const val ACTION_PAUSE_CAPTURE = "com.example.blogrecording.action.PAUSE_CAPTURE"
    const val ACTION_RESUME_CAPTURE = "com.example.blogrecording.action.RESUME_CAPTURE"
    const val ACTION_FINISH_CAPTURE = "com.example.blogrecording.action.FINISH_CAPTURE"

    val REQUIRED_PERMISSIONS = setOf(
        "android.permission.RECORD_AUDIO",
        "android.permission.INTERNET",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION",
        "android.permission.CAPTURE_AUDIO_OUTPUT",
        "android.permission.CAPTURE_MEDIA_OUTPUT"
    )
}
