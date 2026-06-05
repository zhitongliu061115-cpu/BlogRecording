package com.example.blogrecording.vad

data class VadSegment(
    val samples: ShortArray,
    val sampleRate: Int,
    val startMs: Long,
    val endMs: Long,
    val pcmSampleCount: Int,
    val confidence: Float?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VadSegment
        return samples.contentEquals(other.samples) &&
            sampleRate == other.sampleRate &&
            startMs == other.startMs &&
            endMs == other.endMs &&
            pcmSampleCount == other.pcmSampleCount &&
            confidence == other.confidence
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + startMs.hashCode()
        result = 31 * result + endMs.hashCode()
        result = 31 * result + pcmSampleCount
        result = 31 * result + (confidence?.hashCode() ?: 0)
        return result
    }
}
