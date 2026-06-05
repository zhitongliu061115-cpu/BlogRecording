package com.example.blogrecording.asr

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.vad.VadSegment
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SenseVoiceRecognizer(
    private val modelPath: String
) {
    private val initMutex = Mutex()
    @Volatile
    private var recognizer: OfflineRecognizer? = null

    fun validateModel(): AppResult<Unit> {
        val modelDir = File(modelPath)
        if (
            modelPath.isBlank() ||
            !modelDir.exists() ||
            !File(modelDir, MODEL_FILE).exists() ||
            !File(modelDir, TOKENS_FILE).exists()
        ) {
            return AppResult.Failure(AppError.SenseVoiceModelMissing)
        }
        return AppResult.Success(Unit)
    }

    suspend fun recognize(segment: VadSegment): AppResult<AsrResult> {
        val modelCheck = validateModel()
        if (modelCheck is AppResult.Failure) return modelCheck

        return try {
            val offlineRecognizer = getRecognizer()
            val stream = offlineRecognizer.createStream()
            stream.acceptWaveform(segment.samples.toFloatSamples(), segment.sampleRate)
            offlineRecognizer.decode(stream)
            val result = offlineRecognizer.getResult(stream)
            stream.release()
            AppResult.Success(
                AsrResult(
                    text = result.text.trim(),
                    language = result.lang.takeIf { it.isNotBlank() },
                    confidence = null,
                    isFinal = true
                )
            )
        } catch (error: Throwable) {
            AppResult.Failure(AppError.SenseVoiceInitFailed(error.message ?: error.javaClass.simpleName))
        }
    }

    private suspend fun getRecognizer(): OfflineRecognizer = initMutex.withLock {
        recognizer ?: OfflineRecognizer(
            assetManager = null,
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = FEATURE_DIM),
                modelConfig = OfflineModelConfig(
                    senseVoice = OfflineSenseVoiceModelConfig(
                        model = File(modelPath, MODEL_FILE).absolutePath
                    ),
                    tokens = File(modelPath, TOKENS_FILE).absolutePath,
                    numThreads = NUM_THREADS,
                    provider = PROVIDER
                )
            )
        ).also { recognizer = it }
    }

    private fun ShortArray.toFloatSamples(): FloatArray {
        return FloatArray(size) { index -> this[index] / PCM_16BIT_SCALE }
    }

    private companion object {
        const val MODEL_FILE = "model.int8.onnx"
        const val TOKENS_FILE = "tokens.txt"
        const val SAMPLE_RATE = 16_000
        const val FEATURE_DIM = 80
        const val NUM_THREADS = 2
        const val PROVIDER = "cpu"
        const val PCM_16BIT_SCALE = 32768.0f
    }
}
