package com.example.blogrecording.audio

class PcmResampler {
    fun resample(input: PcmAudioStream, targetSampleRate: Int): PcmAudioStream {
        if (input.sampleRate == targetSampleRate) return input
        if (input.samples.isEmpty()) return input.copy(sampleRate = targetSampleRate)

        val ratio = targetSampleRate.toDouble() / input.sampleRate.toDouble()
        val outputSize = (input.samples.size * ratio).toInt().coerceAtLeast(1)
        val output = ShortArray(outputSize)
        for (i in output.indices) {
            val srcIndex = (i / ratio).toInt().coerceIn(0, input.samples.lastIndex)
            output[i] = input.samples[srcIndex]
        }
        return input.copy(samples = output, sampleRate = targetSampleRate)
    }
}
