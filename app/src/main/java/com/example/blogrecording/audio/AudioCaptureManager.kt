package com.example.blogrecording.audio

import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow

interface AudioCaptureManager {
    fun start(): Flow<AppResult<PcmAudioStream>>
    fun stop()
}
