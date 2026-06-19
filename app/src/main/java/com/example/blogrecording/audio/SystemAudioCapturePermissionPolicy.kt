package com.example.blogrecording.audio

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal object SystemAudioCapturePermissionPolicy {
    const val CAPTURE_AUDIO_OUTPUT = "android.permission.CAPTURE_AUDIO_OUTPUT"
    const val CAPTURE_MEDIA_OUTPUT = "android.permission.CAPTURE_MEDIA_OUTPUT"

    val privilegedPermissions = setOf(
        CAPTURE_AUDIO_OUTPUT,
        CAPTURE_MEDIA_OUTPUT
    )

    fun hasPrivilegedCapturePermission(context: Context): Boolean {
        return hasPrivilegedCapturePermission { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPrivilegedCapturePermission(isGranted: (String) -> Boolean): Boolean {
        return privilegedPermissions.any(isGranted)
    }
}
