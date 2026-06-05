package com.example.blogrecording.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledModelAssetsTest {
    @Test
    fun requiredBundledModelAssetsExist() {
        val requiredFiles = listOf(
            "src/main/assets/models/sensevoice/model.int8.onnx",
            "src/main/assets/models/sensevoice/tokens.txt",
            "src/main/assets/models/vad/silero_vad.onnx",
            "src/main/assets/models/diarization/segmentation.onnx",
            "src/main/assets/models/diarization/embedding.onnx"
        )

        requiredFiles.forEach { relativePath ->
            val file = File(relativePath)
            assertTrue("$relativePath must exist", file.exists())
            assertTrue("$relativePath must not be empty", file.length() > 0L)
        }
    }
}
