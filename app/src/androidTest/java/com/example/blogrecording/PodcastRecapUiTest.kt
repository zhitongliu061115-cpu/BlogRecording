package com.example.blogrecording

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PodcastRecapUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstRunPrivacyDialogCanBeAccepted() {
        acceptPrivacyIfShown()

        composeRule.onNodeWithText("Podcast Recap Local ASR").assertIsDisplayed()
        composeRule.onNodeWithText("开始麦克风录音").assertIsDisplayed()
        composeRule.onNodeWithText("隐私边界").assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsApiKeyAndModelFields() {
        acceptPrivacyIfShown()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("DeepSeek API Key").assertIsDisplayed()
        composeRule.onNodeWithText("模型随安装包内置，首次启动会自动复制到 App 私有目录，用户不需要手动填写模型路径。").assertIsDisplayed()
    }

    @Test
    fun historyScreenShowsEmptyState() {
        acceptPrivacyIfShown()

        composeRule.onNodeWithText("查看历史").performClick()
        composeRule.onNodeWithText("历史").assertIsDisplayed()
        composeRule.onNodeWithText("暂无记录。").assertIsDisplayed()
    }

    private fun acceptPrivacyIfShown() {
        composeRule.waitForIdle()
        val accept = composeRule.onAllNodesWithText("我已了解并继续")
        if (accept.fetchSemanticsNodes().isNotEmpty()) {
            accept[0].performClick()
            composeRule.waitForIdle()
        }
    }
}
