package com.example.blogrecording.common

sealed class AppError {
    data object RecordAudioPermissionDenied : AppError()
    data object NotificationPermissionDenied : AppError()
    data object MediaProjectionDenied : AppError()
    data object InternalAudioSilent : AppError()
    data object NoSpeechDetected : AppError()
    data object SenseVoiceModelMissing : AppError()
    data object VadModelMissing : AppError()
    data object DiarizationModelMissing : AppError()
    data class SherpaInitFailed(val reason: String) : AppError()
    data class SenseVoiceInitFailed(val reason: String) : AppError()
    data class VadInitFailed(val reason: String) : AppError()
    data class DiarizationInitFailed(val reason: String) : AppError()
    data object SpeakerDiarizationUnstable : AppError()
    data class AudioRecordInitFailed(val reason: String) : AppError()
    data object DeepSeekApiKeyMissing : AppError()
    data object DeepSeekUnauthorized : AppError()
    data object DeepSeekRateLimited : AppError()
    data object TranscriptTooLong : AppError()
    data object ForegroundServiceStartFailed : AppError()
    data object BackgroundKilled : AppError()
    data object DeviceTooSlow : AppError()
    data class RecordingPipelineFailed(val reason: String) : AppError()
    data class NetworkFailed(val reason: String) : AppError()
    data class Unknown(val reason: String) : AppError()
}

fun AppError.toUserMessage(): String {
    return when (this) {
        AppError.RecordAudioPermissionDenied -> "未授予录音权限"
        AppError.NotificationPermissionDenied -> "未授予通知权限，前台录音通知可能无法显示"
        AppError.MediaProjectionDenied -> "未允许屏幕/音频捕获，请重新授权或使用麦克风录音"
        AppError.InternalAudioSilent -> "当前 App 音频无法被系统捕获，请换可捕获音源、切扬声器，或用麦克风录音"
        AppError.NoSpeechDetected -> "长时间未检测到有效语音"
        AppError.SenseVoiceModelMissing -> "SenseVoice 模型未找到"
        AppError.VadModelMissing -> "VAD 模型未找到"
        AppError.DiarizationModelMissing -> "说话人分离模型未找到"
        is AppError.SherpaInitFailed -> "sherpa-onnx 初始化失败：$reason"
        is AppError.SenseVoiceInitFailed -> "SenseVoice 初始化失败：$reason"
        is AppError.VadInitFailed -> "VAD 初始化失败：$reason"
        is AppError.DiarizationInitFailed -> "说话人分离初始化失败：$reason"
        AppError.SpeakerDiarizationUnstable -> "说话人分离结果可能不准确"
        is AppError.AudioRecordInitFailed -> "AudioRecord 初始化失败：$reason"
        AppError.DeepSeekApiKeyMissing -> "DeepSeek API Key 未配置"
        AppError.DeepSeekUnauthorized -> "DeepSeek API 认证失败"
        AppError.DeepSeekRateLimited -> "DeepSeek 请求频率受限"
        AppError.TranscriptTooLong -> "转写文本过长，请缩短或分段总结"
        AppError.ForegroundServiceStartFailed -> "前台服务启动失败"
        AppError.BackgroundKilled -> "App 被系统杀后台"
        AppError.DeviceTooSlow -> "设备性能不足，实时处理跟不上"
        is AppError.RecordingPipelineFailed -> "录音识别流水线异常：$reason"
        is AppError.NetworkFailed -> "网络请求失败：$reason"
        is AppError.Unknown -> "未知错误：$reason"
    }
}
