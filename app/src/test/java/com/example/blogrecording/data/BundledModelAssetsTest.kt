package com.example.blogrecording.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BundledModelAssetsTest {
    @Test
    fun requiredBundledModelAssetsExist() {
        BundledModelContract.REQUIRED_ASSET_PATHS.forEach { relativePath ->
            val file = File(relativePath)
            assertTrue("$relativePath must exist", file.exists())
            assertTrue("$relativePath must not be empty", file.length() > 0L)
        }
    }

    @Test
    fun modelContractKeepsRequiredAssetNames() {
        assertTrue("model.int8.onnx" in BundledModelContract.SENSEVOICE_REQUIRED_FILES)
        assertTrue("tokens.txt" in BundledModelContract.SENSEVOICE_REQUIRED_FILES)
        assertTrue("silero_vad.onnx" in BundledModelContract.VAD_REQUIRED_FILES)
        assertTrue("segmentation.onnx" in BundledModelContract.DIARIZATION_REQUIRED_FILES)
        assertTrue("embedding.onnx" in BundledModelContract.DIARIZATION_REQUIRED_FILES)
    }
}
