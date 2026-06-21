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
    data object LocalMediaImportBlocked : AppError()
    data object LocalMediaUnsupported : AppError()
    data object LocalMediaTooLarge : AppError()
    data object LocalMediaNoAudioTrack : AppError()
    data object LocalMediaReadFailed : AppError()
    data class LocalMediaDecodeFailed(val reason: String) : AppError()
    data object UrlImportInvalidUrl : AppError()
    data object UrlImportUnsupported : AppError()
    data object UrlImportPrivateOrBlocked : AppError()
    data object UrlImportTimeout : AppError()
    data object UrlImportUnauthorized : AppError()
    data object UrlImportRateLimited : AppError()
    data class UrlImportHttpFailed(val statusCode: Int) : AppError()
    data object UrlImportEmptyResponse : AppError()
    data object UrlImportTooLarge : AppError()
    data object UrlImportRedirectBlocked : AppError()
    data object UrlImportDownloadFailed : AppError()
    data object ExportEmptyContent : AppError()
    data object ExportWriteFailed : AppError()
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
        AppError.LocalMediaImportBlocked -> "请先暂停当前录音，再导入本地音视频"
        AppError.LocalMediaUnsupported -> "暂不支持该音视频格式"
        AppError.LocalMediaTooLarge -> "文件过大，请先导入较短的音视频"
        AppError.LocalMediaNoAudioTrack -> "未找到可用音轨"
        AppError.LocalMediaReadFailed -> "无法读取所选文件，请重新选择"
        is AppError.LocalMediaDecodeFailed -> "音视频解码失败：$reason"
        AppError.UrlImportInvalidUrl -> "请输入有效的 HTTP/HTTPS 链接"
        AppError.UrlImportUnsupported -> "暂不支持该链接，请使用小宇宙单期链接、直链音视频或 RSS enclosure"
        AppError.UrlImportPrivateOrBlocked -> "该内容可能需要登录或不可公开下载，当前版本不支持导入"
        AppError.UrlImportTimeout -> "链接请求超时，请稍后重试或检查网络"
        AppError.UrlImportUnauthorized -> "链接需要授权，当前版本不支持登录导入"
        AppError.UrlImportRateLimited -> "对方服务请求频率受限，请稍后重试"
        is AppError.UrlImportHttpFailed -> "链接请求失败，HTTP 状态码 $statusCode"
        AppError.UrlImportEmptyResponse -> "链接没有返回可导入的媒体内容"
        AppError.UrlImportTooLarge -> "远程媒体过大，请先使用较短的音视频"
        AppError.UrlImportRedirectBlocked -> "链接跳转到不支持的位置"
        AppError.UrlImportDownloadFailed -> "下载远程媒体失败，请稍后重试"
        AppError.ExportEmptyContent -> "当前会话没有可导出的内容"
        AppError.ExportWriteFailed -> "导出文件写入失败，请重试"
        is AppError.RecordingPipelineFailed -> "录音识别流水线异常：$reason"
        is AppError.NetworkFailed -> "网络请求失败：$reason"
        is AppError.Unknown -> "未知错误：$reason"
    }
}
