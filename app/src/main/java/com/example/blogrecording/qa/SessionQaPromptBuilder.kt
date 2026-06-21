package com.example.blogrecording.qa

class SessionQaPromptBuilder {
    fun build(question: String, context: SessionQaContext): String {
        return """
        You are answering questions about one podcast episode.
        Use only the episode context below.
        If the answer is not present in the episode context, say that the episode content does not provide enough information.
        Do not use web knowledge, other episodes, app logs, private paths, API keys, or hidden memory.
        Keep the answer concise and cite relevant timestamps when available.

        Episode context:
        ${context.text}

        User question:
        ${question.trim()}
        """.trimIndent()
    }
}
