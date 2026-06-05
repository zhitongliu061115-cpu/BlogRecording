package com.example.blogrecording.vad

import com.example.blogrecording.audio.PcmAudioStream
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class VadController(
    private val detector: SherpaVadDetector,
    private val minSpeechDurationMs: Long,
    private val minSilenceDurationMs: Long,
    private val maxSpeechDurationMs: Long
) {
    fun process(input: Flow<AppResult<PcmAudioStream>>): Flow<AppResult<VadSegment>> = flow {
        val samples = mutableListOf<Short>()
        var segmentStartMs = 0L
        var lastSpeechMs = 0L
        var inSpeech = false

        input.collect { result ->
            when (result) {
                is AppResult.Failure -> emit(result)
                is AppResult.Success -> {
                    val stream = result.value
                    when (val speech = detector.accept(stream)) {
                        is AppResult.Failure -> emit(speech)
                        is AppResult.Success -> {
                            if (speech.value) {
                                if (!inSpeech) {
                                    inSpeech = true
                                    segmentStartMs = stream.timestampMs
                                    samples.clear()
                                }
                                lastSpeechMs = stream.timestampMs
                                samples += stream.samples.toList()
                            } else if (inSpeech) {
                                val silenceMs = stream.timestampMs - lastSpeechMs
                                val durationMs = stream.timestampMs - segmentStartMs
                                if (silenceMs >= minSilenceDurationMs || durationMs >= maxSpeechDurationMs) {
                                    if (durationMs >= minSpeechDurationMs && samples.isNotEmpty()) {
                                        emit(
                                            AppResult.Success(
                                                VadSegment(
                                                    samples = samples.toShortArray(),
                                                    sampleRate = stream.sampleRate,
                                                    startMs = segmentStartMs,
                                                    endMs = stream.timestampMs,
                                                    pcmSampleCount = samples.size,
                                                    confidence = null
                                                )
                                            )
                                        )
                                    }
                                    inSpeech = false
                                    samples.clear()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
