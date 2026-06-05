package com.example.blogrecording.summary

import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle

class SummaryPromptBuilder {
    fun buildChunkPrompt(
        transcript: String,
        language: SummaryLanguage,
        style: SummaryStyle
    ): String {
        return basePrompt(language, style) + "\n\n转写内容：\n\n$transcript"
    }

    fun buildFinalPrompt(partialSummaries: List<String>, language: SummaryLanguage, style: SummaryStyle): String {
        return basePrompt(language, style) +
            "\n\n下面是按长转写分块生成的局部摘要，请合并、去重并生成最终复盘，不要添加局部摘要中没有的信息：\n\n" +
            partialSummaries.joinToString(separator = "\n\n---\n\n")
    }

    private fun basePrompt(language: SummaryLanguage, style: SummaryStyle): String {
        val languageInstruction = when (language) {
            SummaryLanguage.CHINESE -> "用中文输出。"
            SummaryLanguage.ENGLISH -> "用英文输出。"
            SummaryLanguage.FOLLOW_PODCAST -> "跟随播客主要语言输出。"
        }
        val styleInstruction = when (style) {
            SummaryStyle.BRIEF -> "风格：简洁摘要。"
            SummaryStyle.DEEP_RECAP -> "风格：深度复盘。"
            SummaryStyle.TIMELINE_NOTES -> "风格：时间线笔记。"
            SummaryStyle.POINTS_QUOTES_ACTIONS -> "风格：要点 + 金句 + 行动项。"
        }
        return """
            你是一名播客内容复盘助手。请根据下面的播客转写内容，生成结构化复盘。

            转写内容中可能包含时间戳和说话人标签，例如：
            [00:01:23 - 00:01:36] Speaker 1：
            这里是转写文本。

            请输出：

            1. 一句话概括
            2. 核心主题
            3. 关键观点
            4. 重要细节
            5. 按说话人整理的观点
            6. 值得收藏的表达或金句
            7. 对听众有用的行动项
            8. 可能值得继续思考的问题
            9. 3 个适合作为笔记标题的候选标题

            要求：

            - 不要编造转写中没有的信息。
            - 如果转写质量差，请标注“不确定”。
            - 如果说话人分离结果可能不准确，请不要过度依赖说话人身份。
            - Speaker 1、Speaker 2 只是自动分离标签，不代表真实身份。
            - 保持清晰、有层次。
            - $languageInstruction
            - $styleInstruction
        """.trimIndent()
    }
}
