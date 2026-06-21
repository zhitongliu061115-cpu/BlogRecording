package com.example.blogrecording.importing

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import com.example.blogrecording.audio.PcmAudioStream
import com.example.blogrecording.audio.PcmResampler
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

data class LocalMediaImportSource(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long?
)

data class DecodedLocalMedia(
    val streams: List<PcmAudioStream>,
    val durationMs: Long?,
    val sampleRate: Int,
    val channelCount: Int
)

class LocalMediaImporter(
    private val context: Context,
    private val targetSampleRate: Int = TARGET_SAMPLE_RATE
) {
    private val resampler = PcmResampler()

    fun readSource(uri: Uri): AppResult<LocalMediaImportSource> {
        val resolver = context.contentResolver
        val type = resolver.getType(uri)
        var displayName: String? = null
        var sizeBytes: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        val safeName = LocalMediaImportPolicy.sanitizeDisplayName(displayName ?: uri.lastPathSegment)
        return when (val validation = LocalMediaImportPolicy.validate(safeName, type, sizeBytes)) {
            is AppResult.Success -> AppResult.Success(
                LocalMediaImportSource(
                    uri = uri,
                    displayName = validation.value.displayName,
                    mimeType = validation.value.mimeType,
                    sizeBytes = validation.value.sizeBytes
                )
            )
            is AppResult.Failure -> validation
        }
    }

    fun decode(uri: Uri): Flow<AppResult<DecodedLocalMedia>> = flow {
        val cached = when (val copy = copyToPrivateCache(uri)) {
            is AppResult.Success -> copy.value
            is AppResult.Failure -> {
                emit(copy)
                return@flow
            }
        }
        try {
            emit(decodeCachedFile(cached))
        } finally {
            cached.delete()
        }
    }

    private fun copyToPrivateCache(uri: Uri): AppResult<File> {
        return try {
            val dir = File(context.cacheDir, "local_media_imports").also { it.mkdirs() }
            val file = File.createTempFile("import-", ".media", dir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return AppResult.Failure(AppError.LocalMediaReadFailed)
            AppResult.Success(file)
        } catch (_: SecurityException) {
            AppResult.Failure(AppError.LocalMediaReadFailed)
        } catch (_: Throwable) {
            AppResult.Failure(AppError.LocalMediaReadFailed)
        }
    }

    private fun decodeCachedFile(file: File): AppResult<DecodedLocalMedia> {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = findAudioTrack(extractor)
                ?: return AppResult.Failure(AppError.LocalMediaNoAudioTrack)
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: return AppResult.Failure(AppError.LocalMediaUnsupported)
            val codec = MediaCodec.createDecoderByType(mime)
            decodeSelectedTrack(extractor, codec, format)
        } catch (error: AppErrorMarker) {
            AppResult.Failure(error.error)
        } catch (error: Throwable) {
            AppResult.Failure(AppError.LocalMediaDecodeFailed(error.javaClass.simpleName))
        } finally {
            extractor.release()
        }
    }

    private fun decodeSelectedTrack(
        extractor: MediaExtractor,
        codec: MediaCodec,
        inputFormat: MediaFormat
    ): AppResult<DecodedLocalMedia> {
        val outputStreams = mutableListOf<PcmAudioStream>()
        var outputSampleRate = inputFormat.optionalInt(MediaFormat.KEY_SAMPLE_RATE) ?: targetSampleRate
        var outputChannelCount = inputFormat.optionalInt(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
        val durationMs = inputFormat.optionalLong(MediaFormat.KEY_DURATION)?.let { it / 1000L }
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEnd = false
        var sawOutputEnd = false
        var outputSampleCount = 0L
        codec.configure(inputFormat, null, null, 0)
        codec.start()
        try {
            while (!sawOutputEnd) {
                if (!sawInputEnd) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val sampleSize = inputBuffer?.let { buffer ->
                            buffer.clear()
                            extractor.readSampleData(buffer, 0)
                        } ?: -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEnd = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime.coerceAtLeast(0L),
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        outputSampleRate = outputFormat.optionalInt(MediaFormat.KEY_SAMPLE_RATE) ?: outputSampleRate
                        outputChannelCount = outputFormat.optionalInt(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannelCount
                    }
                    else -> if (outputIndex >= 0) {
                        val buffer = codec.getOutputBuffer(outputIndex)
                        if (bufferInfo.size > 0 && buffer != null) {
                            val samples = buffer.toPcm16Samples(bufferInfo.offset, bufferInfo.size)
                            if (samples.isNotEmpty()) {
                                val timestampMs = samplesToMs(outputSampleCount, outputSampleRate, outputChannelCount)
                                val stream = PcmAudioStream(
                                    samples = samples,
                                    sampleRate = outputSampleRate,
                                    channelCount = outputChannelCount,
                                    timestampMs = timestampMs
                                ).toMono()
                                val normalized = resampler.resample(stream, targetSampleRate)
                                outputStreams += normalized
                                outputSampleCount += samples.size
                            }
                        }
                        sawOutputEnd = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
        }
        if (outputStreams.isEmpty()) {
            return AppResult.Failure(AppError.LocalMediaNoAudioTrack)
        }
        return AppResult.Success(
            DecodedLocalMedia(
                streams = outputStreams,
                durationMs = durationMs ?: inferDuration(outputStreams),
                sampleRate = targetSampleRate,
                channelCount = 1
            )
        )
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return index
        }
        return null
    }

    private fun ByteBuffer.toPcm16Samples(offset: Int, size: Int): ShortArray {
        val duplicate = duplicate()
        duplicate.position(offset)
        duplicate.limit(offset + size)
        val samples = ShortArray(size / 2)
        var index = 0
        while (duplicate.remaining() >= 2 && index < samples.size) {
            val low = duplicate.get().toInt() and 0xff
            val high = duplicate.get().toInt()
            samples[index++] = ((high shl 8) or low).toShort()
        }
        return if (index == samples.size) samples else samples.copyOf(index)
    }

    private fun PcmAudioStream.toMono(): PcmAudioStream {
        if (channelCount <= 1 || samples.isEmpty()) return copy(channelCount = 1)
        val frameCount = samples.size / channelCount
        val mono = ShortArray(frameCount)
        for (frame in 0 until frameCount) {
            var sum = 0
            for (channel in 0 until channelCount) {
                sum += samples[frame * channelCount + channel].toInt()
            }
            mono[frame] = (sum.toFloat() / channelCount)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return copy(samples = mono, channelCount = 1)
    }

    private fun MediaFormat.optionalInt(key: String): Int? {
        return if (containsKey(key)) getInteger(key) else null
    }

    private fun MediaFormat.optionalLong(key: String): Long? {
        return if (containsKey(key)) getLong(key) else null
    }

    private fun inferDuration(streams: List<PcmAudioStream>): Long {
        val totalSamples = streams.sumOf { it.samples.size.toLong() }
        return samplesToMs(totalSamples, targetSampleRate, 1)
    }

    private fun samplesToMs(sampleCount: Long, sampleRate: Int, channelCount: Int): Long {
        val samplesPerSecond = sampleRate.toLong() * channelCount.coerceAtLeast(1)
        if (samplesPerSecond <= 0L) return 0L
        return (sampleCount * 1000L) / samplesPerSecond
    }

    private class AppErrorMarker(val error: AppError) : RuntimeException()

    private companion object {
        const val TARGET_SAMPLE_RATE = 16_000
        const val TIMEOUT_US = 10_000L
    }
}
