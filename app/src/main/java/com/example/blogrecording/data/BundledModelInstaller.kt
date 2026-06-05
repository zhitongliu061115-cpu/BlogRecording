package com.example.blogrecording.data

import android.content.Context
import java.io.File

class BundledModelInstaller(private val context: Context) {
    private val modelRoot: File = File(context.filesDir, MODEL_ROOT_DIR)
    val paths: BundledModelPaths
        get() = BundledModelPaths(
            sherpaModelRootPath = modelRoot.absolutePath,
            senseVoiceModelPath = File(modelRoot, SENSEVOICE_DIR).absolutePath,
            vadModelPath = File(modelRoot, VAD_DIR).absolutePath,
            diarizationModelPath = File(modelRoot, DIARIZATION_DIR).absolutePath
        )

    fun installIfBundled(): BundledModelPaths {
        copyRequiredAssets(SENSEVOICE_ASSET_DIR, File(modelRoot, SENSEVOICE_DIR), SENSEVOICE_REQUIRED_FILES)
        copyRequiredAssets(VAD_ASSET_DIR, File(modelRoot, VAD_DIR), VAD_REQUIRED_FILES)
        copyRequiredAssets(DIARIZATION_ASSET_DIR, File(modelRoot, DIARIZATION_DIR), DIARIZATION_REQUIRED_FILES)
        return paths
    }

    fun status(): ModelStatus {
        val installed = paths
        return ModelStatus(
            senseVoice = statusForFiles(installed.senseVoiceModelPath, SENSEVOICE_REQUIRED_FILES),
            vad = statusForFiles(installed.vadModelPath, VAD_REQUIRED_FILES),
            diarization = statusForFiles(installed.diarizationModelPath, DIARIZATION_REQUIRED_FILES)
        )
    }

    private fun copyRequiredAssets(assetDir: String, targetDir: File, requiredFiles: List<String>) {
        targetDir.mkdirs()
        requiredFiles.forEach { fileName ->
            val target = File(targetDir, fileName)
            if (target.exists() && target.length() > 0L) return@forEach
            context.assets.open("$assetDir/$fileName").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun statusForFiles(path: String, requiredFiles: List<String>): ModelLoadStatus {
        val dir = File(path)
        return if (requiredFiles.all { fileName -> File(dir, fileName).let { it.exists() && it.length() > 0L } }) {
            ModelLoadStatus.LOADED
        } else {
            ModelLoadStatus.MISSING
        }
    }

    companion object {
        const val MODEL_ROOT_DIR = "bundled_models"
        private const val SENSEVOICE_DIR = "sensevoice"
        private const val VAD_DIR = "vad"
        private const val DIARIZATION_DIR = "diarization"
        private const val SENSEVOICE_ASSET_DIR = "models/sensevoice"
        private const val VAD_ASSET_DIR = "models/vad"
        private const val DIARIZATION_ASSET_DIR = "models/diarization"
        private val SENSEVOICE_REQUIRED_FILES = listOf("model.int8.onnx", "tokens.txt")
        private val VAD_REQUIRED_FILES = listOf("silero_vad.onnx")
        private val DIARIZATION_REQUIRED_FILES = listOf("segmentation.onnx", "embedding.onnx")
    }
}

data class BundledModelPaths(
    val sherpaModelRootPath: String,
    val senseVoiceModelPath: String,
    val vadModelPath: String,
    val diarizationModelPath: String
)
