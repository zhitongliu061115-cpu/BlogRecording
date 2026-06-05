package com.example.blogrecording.asr

data class AsrResult(
    val text: String,
    val language: String?,
    val confidence: Float?,
    val isFinal: Boolean
)
