package com.example.blogrecording.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DetailScreenContractTest {
    @Test
    fun detailScreenShowsHighlightActions() {
        val source = File("src/main/java/com/example/blogrecording/ui/DetailScreen.kt").readText()

        assertTrue(source.contains("高光 / 金句"))
        assertTrue(source.contains("取消收藏"))
        assertTrue(source.contains("收藏"))
        assertTrue(source.contains("onToggleHighlightFavorite"))
    }

    @Test
    fun detailScreenShowsExportActions() {
        val detailSource = File("src/main/java/com/example/blogrecording/ui/DetailScreen.kt").readText()
        val appSource = File("src/main/java/com/example/blogrecording/ui/PodcastRecapApp.kt").readText()

        assertTrue(detailSource.contains("保存导出"))
        assertTrue(detailSource.contains("分享导出"))
        assertTrue(detailSource.contains("Markdown"))
        assertTrue(detailSource.contains("TXT"))
        assertTrue(detailSource.contains("JSON"))
        assertTrue(appSource.contains("onSaveExport"))
        assertTrue(appSource.contains("onShareExport"))
    }

    @Test
    fun detailScreenShowsQaActions() {
        val detailSource = File("src/main/java/com/example/blogrecording/ui/DetailScreen.kt").readText()
        val appSource = File("src/main/java/com/example/blogrecording/ui/PodcastRecapApp.kt").readText()

        assertTrue(detailSource.contains("单期 AI 问答"))
        assertTrue(detailSource.contains("onAskQuestion"))
        assertTrue(detailSource.contains("onRetryQuestion"))
        assertTrue(detailSource.contains("QaMessageStatus.FAILED"))
        assertTrue(appSource.contains("askQuestionForCurrentSession"))
        assertTrue(appSource.contains("retryQaForCurrentSession"))
    }
}
