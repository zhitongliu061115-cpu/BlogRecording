package com.example.blogrecording.audio

class SilenceDetector(
    private val amplitudeThreshold: Int = 120
) {
    fun isSilent(stream: PcmAudioStream): Boolean {
        if (stream.samples.isEmpty()) return true
        val avg = stream.samples.map { kotlin.math.abs(it.toInt()) }.average()
        return avg < amplitudeThreshold
    }
}
