package com.example.blogrecording.summary

internal object SummaryJsonExtractor {
    fun extractObject(raw: String): String? {
        extractFencedContent(raw)?.let { fenced ->
            findObject(fenced)?.let { return it }
        }
        return findObject(raw)
    }

    private fun extractFencedContent(raw: String): String? {
        val start = raw.indexOf(CODE_FENCE)
        if (start < 0) return null
        val contentStart = start + CODE_FENCE.length
        val end = raw.indexOf(CODE_FENCE, contentStart)
        if (end <= contentStart) return null
        val content = raw.substring(contentStart, end).trim()
        val firstLineEnd = content.indexOf('\n')
        if (firstLineEnd < 0) return content
        val firstLine = content.substring(0, firstLineEnd).trim()
        return if (firstLine.equals("json", ignoreCase = true)) {
            content.substring(firstLineEnd + 1).trim()
        } else {
            content
        }
    }

    private fun findObject(text: String): String? {
        var start = -1
        var depth = 0
        var inString = false
        var escaping = false
        for (index in text.indices) {
            val char = text[index]
            if (start < 0) {
                if (char == '{') {
                    start = index
                    depth = 1
                }
                continue
            }
            if (inString) {
                when {
                    escaping -> escaping = false
                    char == '\\' -> escaping = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) {
                        return text.substring(start, index + 1)
                    }
                }
            }
        }
        return null
    }

    private const val CODE_FENCE = "```"
}
