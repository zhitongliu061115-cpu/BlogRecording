package com.example.blogrecording.vad

import com.example.blogrecording.audio.PcmAudioStream
import com.example.blogrecording.audio.SilenceDetector
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.io.File

class SherpaVadDetector(
    private val modelPath: String,
    private val speechThreshold: Float,
    private val silenceDetector: SilenceDetector = SilenceDetector()
) {
    @Volatile
    private var vad: Vad? = null
    @Volatile
    private var sherpaUnavailableReason: String? = null

    fun validateModel(): AppResult<Unit> {
        if (modelPath.isBlank() || !modelFile().exists()) {
            return AppResult.Failure(AppError.VadModelMissing)
        }
        return AppResult.Success(Unit)
    }

    fun accept(stream: PcmAudioStream): AppResult<Boolean> {
        val modelCheck = validateModel()
        if (modelCheck is AppResult.Failure) return modelCheck

        return when (val detector = getVad()) {
            null -> fallbackAccept(stream)
            else -> runCatching {
                detector.compute(stream.samples.toFloatSamples()) >= speechThreshold
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = {
                    sherpaUnavailableReason = it.message ?: it.javaClass.simpleName
                    fallbackAccept(stream)
                }
            )
        }
    }

    private fun fallbackAccept(stream: PcmAudioStream): AppResult<Boolean> {
        val hasSpeech = !silenceDetector.isSilent(stream)
        val confidence = if (hasSpeech) 0.75f else 0.1f
        return AppResult.Success(confidence >= speechThreshold)
    }

    private fun getVad(): Vad? {
        sherpaUnavailableReason?.let { return null }
        vad?.let { return it }
        return runCatching {
            Vad(
                assetManager = null,
                config = VadModelConfig(
                    sileroVadModelConfig = SileroVadModelConfig(
                        model = modelFile().absolutePath,
                        threshold = speechThreshold,
                        minSilenceDuration = DEFAULT_MIN_SILENCE_SECONDS,
                        minSpeechDuration = DEFAULT_MIN_SPEECH_SECONDS,
                        windowSize = WINDOW_SIZE,
                        maxSpeechDuration = DEFAULT_MAX_SPEECH_SECONDS
                    ),
                    sampleRate = SAMPLE_RATE,
                    numThreads = NUM_THREADS,
                    provider = PROVIDER
                )
            )
        }.fold(
            onSuccess = {
                vad = it
                it
            },
            onFailure = {
                sherpaUnavailableReason = it.message ?: it.javaClass.simpleName
                null
            }
        )
    }

    private fun modelFile(): File {
        val path = File(modelPath)
        return if (path.isDirectory) File(path, MODEL_FILE) else path
    }

    private fun ShortArray.toFloatSamples(): FloatArray {
        return FloatArray(size) { index -> this[index] / PCM_16BIT_SCALE }
    }

    private companion object {
        const val MODEL_FILE = "silero_vad.onnx"
        const val SAMPLE_RATE = 16_000
        const val NUM_THREADS = 1
        const val PROVIDER = "cpu"
        const val WINDOW_SIZE = 512
        const val DEFAULT_MIN_SILENCE_SECONDS = 0.25f
        const val DEFAULT_MIN_SPEECH_SECONDS = 0.25f
        const val DEFAULT_MAX_SPEECH_SECONDS = 30.0f
        const val PCM_16BIT_SCALE = 32768.0f
    }
}
