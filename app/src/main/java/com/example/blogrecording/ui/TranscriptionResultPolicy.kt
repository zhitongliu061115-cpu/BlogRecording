package com.example.blogrecording.ui

internal object TranscriptionResultPolicy {
    private val weakOneTokenResults = setOf(
        "我",
        "嗯",
        "啊",
        "呃",
        "哦",
        "额",
        "唔",
        "you",
        "i",
        "um",
        "uh",
        "ah",
        "oh"
    )

    fun shouldPersist(text: String): Boolean {
        val normalized = text
            .trim()
            .trim('.', '。', ',', '，', '!', '！', '?', '？', ';', '；', ':', '：', ' ', '\n', '\t')
            .lowercase()
        if (normalized.isBlank()) return false
        if (normalized in weakOneTokenResults) return false
        return normalized.length >= MIN_NORMALIZED_LENGTH
    }

    private const val MIN_NORMALIZED_LENGTH = 2
}
