package com.example.blogrecording.audio

class SilenceDetector(
    private val amplitudeThreshold: Int = 120
) {
    fun isSilent(stream: PcmAudioStream): Boolean {
        return averageAmplitude(stream) < amplitudeThreshold
    }

    fun averageAmplitude(stream: PcmAudioStream): Double {
        return averageAmplitude(stream.samples)
    }

    fun averageAmplitude(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0L
        samples.forEach { sample ->
            sum += kotlin.math.abs(sample.toInt())
        }
        return sum.toDouble() / samples.size
    }
}
