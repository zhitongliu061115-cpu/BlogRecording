package com.example.blogrecording

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastRecapUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsRecordingEntrypointsAndPrivacyBoundary() {
        acceptPrivacyIfShown()

        composeRule.onNodeWithText("BlogRecording").assertIsDisplayed()
        assertTextExists("系统内录")
        assertTextExists("麦克风录音")
        composeRule.onNodeWithContentDescription("历史").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("设置").assertIsDisplayed()
        assertTextExists("音频留在本机")
    }

    @Test
    fun createdPodcastCardShowsStartAndTranscriptPreview() {
        acceptPrivacyIfShown()

        clickFirstExisting("新建播客", "新建")
        composeRule.waitForIdle()

        assertTextExists("开始")
        assertTextExists("麦克风开始")
        assertTextExists("暂无转写内容")
    }

    @Test
    fun settingsScreenShowsApiKeyAndModelFields() {
        acceptPrivacyIfShown()

        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithText("DeepSeek API Key").assertIsDisplayed()
        composeRule.onNodeWithText("模型随安装包内置，首次启动会自动复制到 App 私有目录，用户不需要手动填写模型路径。").assertExists()
    }

    @Test
    fun historyScreenCanOpenWithExistingOrEmptyData() {
        acceptPrivacyIfShown()

        composeRule.onNodeWithContentDescription("历史").performClick()
        assertTextExists("历史")
        composeRule.onNodeWithContentDescription("返回").assertIsDisplayed()
    }

    private fun acceptPrivacyIfShown() {
        composeRule.waitForIdle()
        val accept = composeRule.onAllNodesWithText("我已了解并继续")
        if (accept.fetchSemanticsNodes().isNotEmpty()) {
            accept[0].performClick()
            composeRule.waitForIdle()
        }
    }

    private fun clickFirstExisting(vararg labels: String) {
        for (label in labels) {
            val nodes = composeRule.onAllNodesWithText(label).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                composeRule.onAllNodesWithText(label)[0].performClick()
                return
            }
        }
        error("None of the expected labels was found: ${labels.joinToString()}")
    }

    private fun assertTextExists(label: String) {
        assertTrue(
            "Expected text not found: $label",
            composeRule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        )
    }
}
