package com.example.blogrecording.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRecordingFallbackContractTest {
    @Test
    fun homeKeepsMicrophoneFallbackControlsBesideInternalAudioActions() {
        val source = File("src/main/java/com/example/blogrecording/ui/HomeScreen.kt").readText()

        assertTrue(source.contains("SourceButton(\"系统内录\""))
        assertTrue(source.contains("SourceButton(\"麦克风录音\""))
        assertTrue(source.contains("Text(\"麦克风开始\")"))
        assertTrue(source.contains("Text(\"麦克风续录\")"))
    }

    @Test
    fun homeCardDefaultStartAndResumeStillUseInternalAudioCallbacks() {
        val source = File("src/main/java/com/example/blogrecording/ui/HomeScreen.kt").readText()

        assertTrue(source.contains("onStartRecording = { onStartInternalSession(card.sessionId) }"))
        assertTrue(source.contains("onResumeRecording = { onResumeInternalSession(card.sessionId) }"))
    }
}
