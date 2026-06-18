package com.example.blogrecording.platform

import org.junit.Assert.assertEquals
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
}
