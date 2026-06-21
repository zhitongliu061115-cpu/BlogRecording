package com.example.blogrecording.summary

import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryPromptBuilderTest {
    @Test
    fun promptKeepsPrivacyAndAccuracyConstraints() {
        val prompt = SummaryPromptBuilder().buildChunkPrompt(
            transcript = "[00:00:01 - 00:00:03] Speaker 1：内容",
            language = SummaryLanguage.CHINESE,
            style = SummaryStyle.BULLET_SUMMARY
        )

        assertTrue(prompt.contains("不要编造"))
        assertTrue(prompt.contains("Speaker 1、Speaker 2 只是自动分离标签"))
        assertTrue(prompt.contains("用中文输出"))
        assertTrue(prompt.contains("[00:00:01 - 00:00:03] Speaker 1"))
    }
    @Test
    fun promptRequestsStructuredJsonFields() {
        val prompt = SummaryPromptBuilder().buildChunkPrompt(
            transcript = "content",
            language = SummaryLanguage.CHINESE,
            style = SummaryStyle.BULLET_SUMMARY
        )

        assertTrue(prompt.contains("要点列表"))
        assertTrue(prompt.contains("Markdown 分级列表"))
        assertTrue(prompt.contains("核心主题"))
        assertTrue(prompt.contains("关键要点"))
        assertTrue(prompt.contains("重要数据/事实"))
        assertTrue(prompt.contains("争议或未决话题"))
        assertTrue(prompt.contains("500 字"))
    }
}
