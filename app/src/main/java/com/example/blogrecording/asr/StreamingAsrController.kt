package com.example.blogrecording.asr

import com.example.blogrecording.common.AppResult
import com.example.blogrecording.vad.VadSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StreamingAsrController(
    private val recognizer: SenseVoiceRecognizer
) {
    fun transcribe(segments: Flow<AppResult<VadSegment>>): Flow<AppResult<Pair<VadSegment, AsrResult>>> = flow {
        segments.collect { result ->
            when (result) {
                is AppResult.Failure -> emit(result)
                is AppResult.Success -> {
                    when (val asr = recognizer.recognize(result.value)) {
                        is AppResult.Failure -> emit(asr)
                        is AppResult.Success -> emit(AppResult.Success(result.value to asr.value))
                    }
                }
            }
        }
    }
}
