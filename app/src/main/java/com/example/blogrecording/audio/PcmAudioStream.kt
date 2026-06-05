package com.example.blogrecording.audio

data class PcmAudioStream(
    val samples: ShortArray,
    val sampleRate: Int,
    val channelCount: Int,
    val timestampMs: Long
) {
    val pcmSampleCount: Int get() = samples.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PcmAudioStream
        return samples.contentEquals(other.samples) &&
            sampleRate == other.sampleRate &&
            channelCount == other.channelCount &&
            timestampMs == other.timestampMs
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + timestampMs.hashCode()
        return result
    }
}
