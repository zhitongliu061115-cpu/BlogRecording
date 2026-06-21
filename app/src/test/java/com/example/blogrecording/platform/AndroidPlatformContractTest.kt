package com.example.blogrecording.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidPlatformContractTest {
    @Test
    fun appIdentityRemainsStable() {
        assertEquals("com.example.blogrecording", AndroidPlatformContract.PACKAGE_NAME)
        assertEquals("com.example.blogrecording", AndroidPlatformContract.APPLICATION_ID)
        assertEquals("com.example.blogrecording", AndroidPlatformContract.NAMESPACE)
    }

    @Test
    fun foregroundServiceContractRemainsStable() {
        assertEquals(".MainActivity", AndroidPlatformContract.MAIN_ACTIVITY)
        assertEquals(".service.CaptureForegroundService", AndroidPlatformContract.CAPTURE_FOREGROUND_SERVICE)
        assertEquals("microphone|mediaProjection", AndroidPlatformContract.FOREGROUND_SERVICE_TYPES)
        assertEquals("capture_foreground", AndroidPlatformContract.CAPTURE_CHANNEL_ID)
        assertEquals(1001, AndroidPlatformContract.CAPTURE_NOTIFICATION_ID)
        assertEquals("foreground_service_type", AndroidPlatformContract.EXTRA_FOREGROUND_SERVICE_TYPE)
        assertEquals("podcast_title", AndroidPlatformContract.EXTRA_PODCAST_TITLE)
        assertEquals("capture_source", AndroidPlatformContract.EXTRA_CAPTURE_SOURCE)
        assertEquals("recording_state", AndroidPlatformContract.EXTRA_RECORDING_STATE)
        assertEquals("active_session_id", AndroidPlatformContract.EXTRA_ACTIVE_SESSION_ID)
        assertEquals("com.example.blogrecording.action.PAUSE_CAPTURE", AndroidPlatformContract.ACTION_PAUSE_CAPTURE)
        assertEquals("com.example.blogrecording.action.RESUME_CAPTURE", AndroidPlatformContract.ACTION_RESUME_CAPTURE)
        assertEquals("com.example.blogrecording.action.FINISH_CAPTURE", AndroidPlatformContract.ACTION_FINISH_CAPTURE)
    }

    @Test
    fun requiredPermissionsRemainDeclaredInContract() {
        assertTrue("android.permission.RECORD_AUDIO" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertTrue("android.permission.INTERNET" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertTrue("android.permission.POST_NOTIFICATIONS" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertTrue("android.permission.FOREGROUND_SERVICE" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertTrue("android.permission.FOREGROUND_SERVICE_MICROPHONE" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertTrue(
            "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" in
                AndroidPlatformContract.REQUIRED_PERMISSIONS
        )
    }

    @Test
    fun internalAudioMvpDoesNotRequirePrivilegedSystemCapturePermissions() {
        assertFalse("android.permission.CAPTURE_AUDIO_OUTPUT" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertFalse("android.permission.CAPTURE_MEDIA_OUTPUT" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
        assertFalse("android.permission.CAPTURE_AUDIO_HOTWORD" in AndroidPlatformContract.REQUIRED_PERMISSIONS)
    }
}
