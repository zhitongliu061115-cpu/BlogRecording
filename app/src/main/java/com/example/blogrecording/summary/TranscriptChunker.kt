package com.example.blogrecording.summary

class TranscriptChunker(
    private val maxCharsPerChunk: Int = 12_000
) {
    fun chunk(transcript: String): List<String> {
        val normalized = transcript.trim()
        if (normalized.isBlank()) return emptyList()
        if (normalized.length <= maxCharsPerChunk) return listOf(normalized)

        val chunks = mutableListOf<String>()
        val paragraphs = normalized.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
        val current = StringBuilder()
        for (paragraph in paragraphs) {
            if (paragraph.length > maxCharsPerChunk) {
                flush(current, chunks)
                chunks += paragraph.chunked(maxCharsPerChunk)
                continue
            }
            if (current.length + paragraph.length + 2 > maxCharsPerChunk) {
                flush(current, chunks)
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(paragraph)
        }
        flush(current, chunks)
        return chunks
    }

    private fun flush(buffer: StringBuilder, chunks: MutableList<String>) {
        val value = buffer.toString().trim()
        if (value.isNotBlank()) chunks += value
        buffer.clear()
    }
}
