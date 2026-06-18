package com.example.blogrecording.data

internal object BundledModelContract {
    const val MODEL_ROOT_DIR = "bundled_models"
    const val SENSEVOICE_DIR = "sensevoice"
    const val VAD_DIR = "vad"
    const val DIARIZATION_DIR = "diarization"
    const val SENSEVOICE_ASSET_DIR = "models/sensevoice"
    const val VAD_ASSET_DIR = "models/vad"
    const val DIARIZATION_ASSET_DIR = "models/diarization"

    val SENSEVOICE_REQUIRED_FILES = listOf("model.int8.onnx", "tokens.txt")
    val VAD_REQUIRED_FILES = listOf("silero_vad.onnx")
    val DIARIZATION_REQUIRED_FILES = listOf("segmentation.onnx", "embedding.onnx")

    val REQUIRED_ASSET_PATHS = listOf(
        "src/main/assets/$SENSEVOICE_ASSET_DIR/model.int8.onnx",
        "src/main/assets/$SENSEVOICE_ASSET_DIR/tokens.txt",
        "src/main/assets/$VAD_ASSET_DIR/silero_vad.onnx",
        "src/main/assets/$DIARIZATION_ASSET_DIR/segmentation.onnx",
        "src/main/assets/$DIARIZATION_ASSET_DIR/embedding.onnx"
    )
}
