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
}
