package com.example.blogrecording.audio

import com.example.blogrecording.vad.VadSegment

data class PcmChunk(
    val sequence: Int,
    val samples: ShortArray,
    val sampleRate: Int,
    val channelCount: Int,
    val startMs: Long,
    val endMs: Long
) {
    fun toVadSegment(): VadSegment {
        return VadSegment(
            samples = samples,
            sampleRate = sampleRate,
            startMs = startMs,
            endMs = endMs,
            pcmSampleCount = samples.size,
            confidence = null
        )
    }

    fun toVadSegments(maxDurationMs: Long): List<VadSegment> {
        if (samples.isEmpty()) return emptyList()
        val samplesPerSecond = sampleRate.toLong() * channelCount.coerceAtLeast(1)
        if (samplesPerSecond <= 0L) return listOf(toVadSegment())
        val maxSampleCount = ((samplesPerSecond * maxDurationMs.coerceAtLeast(1_000L)) / 1000L)
            .coerceIn(1L, Int.MAX_VALUE.toLong())
            .toInt()
        val segments = mutableListOf<VadSegment>()
        var offset = 0
        while (offset < samples.size) {
            val count = minOf(maxSampleCount, samples.size - offset)
            val segmentStartMs = startMs + samplesToMs(offset.toLong(), samplesPerSecond)
            val segmentEndMs = startMs + samplesToMs((offset + count).toLong(), samplesPerSecond)
            segments += VadSegment(
                samples = samples.copyOfRange(offset, offset + count),
                sampleRate = sampleRate,
                startMs = segmentStartMs,
                endMs = segmentEndMs,
                pcmSampleCount = count,
                confidence = null
            )
            offset += count
        }
        return segments
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PcmChunk
        return sequence == other.sequence &&
            samples.contentEquals(other.samples) &&
            sampleRate == other.sampleRate &&
            channelCount == other.channelCount &&
            startMs == other.startMs &&
            endMs == other.endMs
    }

    override fun hashCode(): Int {
        var result = sequence
        result = 31 * result + samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + startMs.hashCode()
        result = 31 * result + endMs.hashCode()
        return result
    }

    private fun samplesToMs(sampleCount: Long, samplesPerSecond: Long): Long {
        return (sampleCount * 1000L) / samplesPerSecond
    }
}

class PcmChunker(
    targetDurationMs: Long
) {
    private val targetDurationMs = targetDurationMs.coerceAtLeast(MIN_TARGET_DURATION_MS)
    private var sampleRate: Int = 0
    private var channelCount: Int = 1
    private var targetSampleCount: Int = 0
    private var bufferedSamples = ShortArray(0)
    private var bufferedSampleCount = 0
    private var emittedSampleCount = 0L
    private var nextSequence = 1

    val currentDurationMs: Long
        get() = if (sampleRate <= 0) 0L else samplesToMs(bufferedSampleCount.toLong())

    fun offer(stream: PcmAudioStream): List<PcmChunk> {
        if (stream.samples.isEmpty()) return emptyList()
        ensureFormat(stream)
        append(stream.samples)

        val chunks = mutableListOf<PcmChunk>()
        while (bufferedSampleCount >= targetSampleCount) {
            chunks += emitChunk(targetSampleCount)
        }
        return chunks
    }

    fun flush(): PcmChunk? {
        if (bufferedSampleCount == 0 || sampleRate <= 0) return null
        return emitChunk(bufferedSampleCount)
    }

    private fun ensureFormat(stream: PcmAudioStream) {
        if (sampleRate == 0) {
            sampleRate = stream.sampleRate
            channelCount = stream.channelCount.coerceAtLeast(1)
            targetSampleCount = msToSamples(targetDurationMs).coerceAtLeast(1)
            ensureCapacity(targetSampleCount)
        }
    }

    private fun append(samples: ShortArray) {
        ensureCapacity(bufferedSampleCount + samples.size)
        samples.copyInto(bufferedSamples, destinationOffset = bufferedSampleCount)
        bufferedSampleCount += samples.size
    }

    private fun emitChunk(sampleCount: Int): PcmChunk {
        val chunkSamples = bufferedSamples.copyOfRange(0, sampleCount)
        val startSample = emittedSampleCount
        val endSample = emittedSampleCount + sampleCount
        val chunk = PcmChunk(
            sequence = nextSequence++,
            samples = chunkSamples,
            sampleRate = sampleRate,
            channelCount = channelCount,
            startMs = samplesToMs(startSample),
            endMs = samplesToMs(endSample)
        )

        val remaining = bufferedSampleCount - sampleCount
        if (remaining > 0) {
            bufferedSamples.copyInto(bufferedSamples, destinationOffset = 0, startIndex = sampleCount, endIndex = bufferedSampleCount)
        }
        bufferedSampleCount = remaining
        emittedSampleCount = endSample
        return chunk
    }

    private fun ensureCapacity(required: Int) {
        if (bufferedSamples.size >= required) return
        var newSize = bufferedSamples.size.coerceAtLeast(1024)
        while (newSize < required) {
            newSize *= 2
        }
        bufferedSamples = bufferedSamples.copyOf(newSize)
    }

    private fun msToSamples(ms: Long): Int {
        return ((sampleRate.toLong() * channelCount * ms) / 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun samplesToMs(sampleCount: Long): Long {
        val samplesPerSecond = sampleRate.toLong() * channelCount
        if (samplesPerSecond <= 0L) return 0L
        return (sampleCount * 1000L) / samplesPerSecond
    }

    private companion object {
        const val MIN_TARGET_DURATION_MS = 1_000L
    }
}
