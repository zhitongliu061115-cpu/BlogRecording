# Podcast Recap Local ASR

本项目是一个 Android Kotlin + Jetpack Compose 的端侧播客转写与复盘 App 骨架。

## 当前落地内容

- Compose 页面：首页、设置页、历史页、详情页、首次隐私风险弹窗。
- 本地设置与记录存储：DataStore 版本，后续可替换为 Room。
- API Key 存储：Android Keystore + AES/GCM 加密后保存在私有 SharedPreferences。
- 音频采集骨架：麦克风 `AudioRecord`、MediaProjection 内录 `AudioRecord`、Foreground Service 通知。
- 本地处理接口：VAD、SenseVoice ASR、speaker diarization 均已分层，保留 sherpa-onnx 接入点。
- DeepSeek 总结：OkHttp 调用 `https://api.deepseek.com/chat/completions`，支持转写分块、局部摘要和最终合并。

## 重要限制

- 当前代码不内置 sherpa-onnx AAR 和模型文件。
- `SenseVoiceRecognizer` 仍会返回明确的 sherpa-onnx 未接入错误，不会伪造转写结果。
- `SherpaVadDetector` 目前只有基于音量的 fallback，用于打通状态；接入 Silero VAD 后应替换。
- `SpeakerDiarizationEngine` 目前是可替换占位实现；不要用于真实说话人识别质量评估。
- 不上传原始音频、PCM、音频片段、声纹向量或 speaker embedding。

## 建议目录

```text
app/src/main/java/com/example/blogrecording/
  audio/
  asr/
  common/
  data/
  diarization/
  security/
  service/
  summary/
  ui/
  vad/
```

## 模型文件

模型不再让用户手动填写路径。开发者应把 sherpa-onnx 模型文件放进 APK assets：

- `app/src/main/assets/models/sensevoice/model.int8.onnx`
- `app/src/main/assets/models/sensevoice/tokens.txt`
- `app/src/main/assets/models/vad/silero_vad.onnx`
- `app/src/main/assets/models/diarization/segmentation.onnx`
- `app/src/main/assets/models/diarization/embedding.onnx`

首次启动时，App 会自动把 assets 内模型复制到私有目录 `files/bundled_models/`，设置页只显示加载状态，不提供用户路径输入。

Gradle 已加入 `verifyBundledModels`：缺少上述真实模型文件时，`assembleDebug` 会失败，避免生成空模型 APK。

后续接入 sherpa-onnx Android 时，应把 native/AAR 依赖加到 Gradle，并在 `SenseVoiceRecognizer`、`SherpaVadDetector`、`SpeakerDiarizationEngine` 中替换 TODO。

## 测试

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```
