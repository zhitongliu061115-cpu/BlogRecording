package com.example.blogrecording.diarization

import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.vad.VadSegment
import com.k2fsa.sherpa.onnx.FastClusteringConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarization
import com.k2fsa.sherpa.onnx.OfflineSpeakerDiarizationConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeakerSegmentationPyannoteModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SpeakerDiarizationEngine(
    private val modelPath: String,
    private val enabled: Boolean,
    private val maxSpeakerCount: Int
) {
    private val initMutex = Mutex()
    @Volatile
    private var diarization: OfflineSpeakerDiarization? = null
    @Volatile
    private var sherpaUnavailableReason: String? = null

    fun validateModel(): AppResult<Unit> {
        if (!enabled) return AppResult.Success(Unit)
        val modelDir = File(modelPath)
        if (
            modelPath.isBlank() ||
            !modelDir.exists() ||
            !File(modelDir, SEGMENTATION_FILE).exists() ||
            !File(modelDir, EMBEDDING_FILE).exists()
        ) {
            return AppResult.Failure(AppError.DiarizationModelMissing)
        }
        return AppResult.Success(Unit)
    }

    suspend fun label(segment: VadSegment): AppResult<SpeakerSegment> {
        val modelCheck = validateModel()
        if (modelCheck is AppResult.Failure) return modelCheck

        if (!enabled) {
            return AppResult.Success(
                SpeakerSegment("unknown", "未知说话人", unstable = true, segment.confidence)
            )
        }

        val sherpaSpeaker = runCatching {
            getDiarization()?.process(segment.samples.toFloatSamples())
                ?.maxByOrNull { it.end - it.start }
                ?.speaker
        }.fold(
            onSuccess = { it },
            onFailure = {
                sherpaUnavailableReason = it.message ?: it.javaClass.simpleName
                null
            }
        )

        val boundedSpeakerCount = maxSpeakerCount.coerceIn(2, 8)
        val speakerIndex = when (sherpaSpeaker) {
            null -> ((segment.startMs / 30_000) % boundedSpeakerCount).toInt() + 1
            else -> (sherpaSpeaker % boundedSpeakerCount) + 1
        }
        val unstable = sherpaSpeaker == null
        return AppResult.Success(
            SpeakerSegment(
                speakerId = "speaker_$speakerIndex",
                displayName = "Speaker $speakerIndex",
                unstable = unstable,
                vadConfidence = segment.confidence
            )
        )
    }

    private suspend fun getDiarization(): OfflineSpeakerDiarization? = initMutex.withLock {
        if (sherpaUnavailableReason != null) return@withLock null
        diarization ?: runCatching {
            OfflineSpeakerDiarization(
                assetManager = null,
                config = OfflineSpeakerDiarizationConfig(
                    segmentation = OfflineSpeakerSegmentationModelConfig(
                        pyannote = OfflineSpeakerSegmentationPyannoteModelConfig(
                            model = File(modelPath, SEGMENTATION_FILE).absolutePath
                        ),
                        numThreads = NUM_THREADS,
                        provider = PROVIDER
                    ),
                    embedding = SpeakerEmbeddingExtractorConfig(
                        model = File(modelPath, EMBEDDING_FILE).absolutePath,
                        numThreads = NUM_THREADS,
                        provider = PROVIDER
                    ),
                    clustering = FastClusteringConfig(
                        numClusters = maxSpeakerCount.coerceIn(2, 8),
                        threshold = CLUSTER_THRESHOLD
                    )
                )
            )
        }.fold(
            onSuccess = {
                diarization = it
                it
            },
            onFailure = {
                sherpaUnavailableReason = it.message ?: it.javaClass.simpleName
                null
            }
        )
    }

    private fun ShortArray.toFloatSamples(): FloatArray {
        return FloatArray(size) { index -> this[index] / PCM_16BIT_SCALE }
    }

    private companion object {
        const val SEGMENTATION_FILE = "segmentation.onnx"
        const val EMBEDDING_FILE = "embedding.onnx"
        const val NUM_THREADS = 1
        const val PROVIDER = "cpu"
        const val CLUSTER_THRESHOLD = 0.5f
        const val PCM_16BIT_SCALE = 32768.0f
    }
}
