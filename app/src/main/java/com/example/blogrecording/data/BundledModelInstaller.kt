package com.example.blogrecording.data

import android.content.Context
import java.io.File

class BundledModelInstaller(private val context: Context) {
    private val modelRoot: File = File(context.filesDir, BundledModelContract.MODEL_ROOT_DIR)
    val paths: BundledModelPaths
        get() = BundledModelPaths(
            sherpaModelRootPath = modelRoot.absolutePath,
            senseVoiceModelPath = File(modelRoot, BundledModelContract.SENSEVOICE_DIR).absolutePath,
            vadModelPath = File(modelRoot, BundledModelContract.VAD_DIR).absolutePath,
            diarizationModelPath = File(modelRoot, BundledModelContract.DIARIZATION_DIR).absolutePath
        )

    fun installIfBundled(): BundledModelPaths {
        copyRequiredAssets(
            BundledModelContract.SENSEVOICE_ASSET_DIR,
            File(modelRoot, BundledModelContract.SENSEVOICE_DIR),
            BundledModelContract.SENSEVOICE_REQUIRED_FILES
        )
        copyRequiredAssets(
            BundledModelContract.VAD_ASSET_DIR,
            File(modelRoot, BundledModelContract.VAD_DIR),
            BundledModelContract.VAD_REQUIRED_FILES
        )
        copyRequiredAssets(
            BundledModelContract.DIARIZATION_ASSET_DIR,
            File(modelRoot, BundledModelContract.DIARIZATION_DIR),
            BundledModelContract.DIARIZATION_REQUIRED_FILES
        )
        return paths
    }

    fun status(): ModelStatus {
        val installed = paths
        return ModelStatus(
            senseVoice = statusForFiles(installed.senseVoiceModelPath, BundledModelContract.SENSEVOICE_REQUIRED_FILES),
            vad = statusForFiles(installed.vadModelPath, BundledModelContract.VAD_REQUIRED_FILES),
            diarization = statusForFiles(
                installed.diarizationModelPath,
                BundledModelContract.DIARIZATION_REQUIRED_FILES
            )
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
}

data class BundledModelPaths(
    val sherpaModelRootPath: String,
    val senseVoiceModelPath: String,
    val vadModelPath: String,
    val diarizationModelPath: String
)
