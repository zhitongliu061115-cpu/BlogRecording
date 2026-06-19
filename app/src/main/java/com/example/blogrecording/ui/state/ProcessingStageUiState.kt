package com.example.blogrecording.ui.state

enum class ProcessingStage {
    IDLE,
    AUTHORIZING,
    CAPTURING,
    BUFFERING,
    TRANSCRIBING,
    SILENCE,
    PAUSED,
    SUMMARIZING,
    COMPLETED,
    ERROR
}

data class ProcessingStageUiState(
    val stage: ProcessingStage = ProcessingStage.IDLE,
    val title: String = "未开始",
    val message: String = "等待开始录制",
    val progressLabel: String? = null,
    val isActive: Boolean = false,
    val isWarning: Boolean = false
) {
    companion object {
        fun idle(): ProcessingStageUiState = ProcessingStageUiState()

        fun authorizingSystemAudio(): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.AUTHORIZING,
            title = "等待系统内录授权",
            message = "请允许屏幕和系统音频捕获",
            isActive = true
        )

        fun capturing(sourceLabel: String): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.CAPTURING,
            title = "${sourceLabel}录制中",
            message = "正在捕获音频，本机处理",
            isActive = true
        )

        fun buffering(bufferedMs: Long, targetMs: Long): ProcessingStageUiState {
            val bufferedSeconds = (bufferedMs / 1000).coerceAtLeast(0)
            val targetSeconds = (targetMs / 1000).coerceAtLeast(1)
            return ProcessingStageUiState(
                stage = ProcessingStage.BUFFERING,
                title = "音频缓存中",
                message = "录到 ${targetSeconds} 秒后自动转文字",
                progressLabel = "${bufferedSeconds}/${targetSeconds} 秒",
                isActive = true
            )
        }

        fun transcribing(
            chunkSequence: Int,
            segmentIndex: Int? = null,
            segmentCount: Int? = null,
            message: String = "SenseVoice 正在识别本地音频"
        ): ProcessingStageUiState {
            val progress = if (segmentIndex != null && segmentCount != null) {
                "第 ${chunkSequence} 批 ${segmentIndex}/${segmentCount}"
            } else {
                "第 ${chunkSequence} 批"
            }
            return ProcessingStageUiState(
                stage = ProcessingStage.TRANSCRIBING,
                title = "正在转文字",
                message = message,
                progressLabel = progress,
                isActive = true
            )
        }

        fun silentInternalAudio(): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.SILENCE,
            title = "未捕获到可转写声音",
            message = "当前 App 可能不允许系统内录，或正在播放静音",
            isActive = true,
            isWarning = true
        )

        fun paused(): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.PAUSED,
            title = "已暂停",
            message = "可续录、完成或生成总结"
        )

        fun summarizing(): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.SUMMARIZING,
            title = "正在总结",
            message = "仅发送转写文本生成总结",
            isActive = true
        )

        fun completed(message: String): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.COMPLETED,
            title = "已完成",
            message = message
        )

        fun error(message: String): ProcessingStageUiState = ProcessingStageUiState(
            stage = ProcessingStage.ERROR,
            title = "需要处理",
            message = message,
            isWarning = true
        )
    }
}
