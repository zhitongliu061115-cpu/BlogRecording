# BlogRecording 项目介绍与 GitHub 上传规范

## 一、项目简要介绍

BlogRecording 是一个 Android Kotlin + Jetpack Compose 项目，定位为本地播客/录音转写与复盘 App。应用支持麦克风录音和系统内录音，录音过程通过前台服务保持稳定运行，并将音频分片送入本地语音处理流程。

当前项目的核心能力包括：

- 录音采集：支持麦克风 `AudioRecord` 和基于 `MediaProjection` 的系统内录音。
- 本地转写：通过 sherpa-onnx 接入 SenseVoice 模型进行离线 ASR。
- 语音处理：包含 PCM 分片、重采样、静音检测、VAD 与说话人分离相关模块。
- 记录管理：使用 DataStore 保存录音会话、转写片段、说话人信息、摘要和状态恢复信息。
- AI 总结：通过 OkHttp 调用 DeepSeek Chat Completions API，对转写文本进行摘要生成。
- 安全存储：DeepSeek API Key 使用 Android Keystore + AES/GCM 加密后保存到私有 SharedPreferences。
- Compose UI：包含首页、历史页、详情页、设置页和隐私提示弹窗。
- 构建校验：`verifyBundledModels` 会在构建前校验必需模型文件是否存在，避免产出缺少模型的 APK。

技术栈概览：

- 语言与框架：Kotlin、Jetpack Compose、Material 3
- 构建系统：Gradle Kotlin DSL、Android Gradle Plugin
- 数据存储：AndroidX DataStore Preferences
- 并发模型：Kotlin Coroutines、Flow
- 网络：OkHttp
- 语音模型：sherpa-onnx、SenseVoice、Silero VAD、speaker diarization 模型
- 测试：JUnit、AndroidX Test、Compose UI Test

当前仓库包含真实模型文件和 sherpa-onnx AAR，因此 GitHub 上传时必须明确大文件和许可证策略。若项目转为公开仓库，建议优先确认模型和 AAR 的分发许可，并考虑将模型文件改为 Release 附件、私有制品库或 Git LFS 管理。

## 二、GitHub 上传规范

### 1. 上传前基础原则

- 不上传密钥、Token、证书、签名文件、个人路径、账号信息和真实用户数据。
- 不上传原始录音、PCM、音频片段、说话人 embedding、调试日志和崩溃日志。
- 不上传本地构建产物，如 `build/`、`.gradle/`、`.cxx/`、`.externalNativeBuild/`、`captures/`。
- 不上传开发者本机配置，如 `local.properties`、IDE workspace 缓存。
- 大于 10 MB 的模型、AAR、APK、视频、音频、数据集必须走 Git LFS、Release 附件或企业制品库。
- 上传前必须确认第三方模型、AAR、字体、图片和音频资源的许可证允许仓库分发。

### 2. 推荐仓库结构

```text
BlogRecording/
  app/                         Android 应用模块
  app/src/main/java/...        Kotlin 源码
  app/src/main/res/...         Android 资源
  app/src/main/assets/models/  离线模型资源，建议 Git LFS 或外部分发
  app/src/test/...             本地单元测试
  app/src/androidTest/...      仪器测试和 UI 测试
  gradle/                      Gradle Wrapper 与版本目录
  docs/                        项目文档、规范、设计说明
  openspec/                    需求变更与规格文档
  README.md                    项目入口说明
```

### 3. 必须提交的文件

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/libs.versions.toml`
- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/`
- `app/src/test/`
- `app/src/androidTest/`
- `.gitignore`
- `.gitattributes`
- `README.md`
- `docs/`

### 4. 禁止提交的文件

```gitignore
local.properties
*.jks
*.keystore
*.p12
*.pem
*.key
*.mobileprovision
*.apk
*.aab
*.ap_
*.hprof
*.log
*.wav
*.mp3
*.m4a
*.pcm
*.flac
*.aac
captures/
build/
.gradle/
.idea/workspace.xml
.idea/caches/
.externalNativeBuild/
.cxx/
```

说明：当前 `.gitignore` 已忽略 `local.properties`、`.gradle/`、`build/`、`captures/` 等常见本地文件。若后续加入签名配置、录音导出、崩溃日志或产物输出目录，需要同步补充忽略规则。

### 5. 大文件与 Git LFS 规范

当前仓库存在以下大文件类型：

- `app/libs/*.aar`
- `app/src/main/assets/models/**/*.onnx`
- `app/src/main/assets/models/**/tokens.txt`

建议在 `.gitattributes` 中统一管理：

```gitattributes
*.onnx filter=lfs diff=lfs merge=lfs -text
*.aar filter=lfs diff=lfs merge=lfs -text
*.apk filter=lfs diff=lfs merge=lfs -text
*.aab filter=lfs diff=lfs merge=lfs -text
```

企业级建议：

- 公开仓库：模型文件优先不要直接放主仓库，建议放 GitHub Releases、对象存储或模型仓库，并在 README 中说明下载方式。
- 私有仓库：可使用 Git LFS，但必须评估仓库克隆速度、LFS 流量、CI 缓存和权限控制。
- 模型更新：每次更新模型必须记录来源、版本、校验值、许可证和兼容的 App 版本。
- 禁止把测试录音、用户录音和个人语料作为模型或样例数据上传。

### 6. 分支管理规范

推荐分支：

- `main`：稳定分支，只允许通过 PR 合并，可随时发布。
- `develop`：集成分支，用于日常开发联调。
- `feature/<scope>-<short-name>`：功能分支。
- `fix/<scope>-<short-name>`：缺陷修复分支。
- `release/<version>`：发布准备分支。
- `hotfix/<version-or-issue>`：线上紧急修复分支。

示例：

```bash
git checkout -b feature/audio-recording-pipeline
git checkout -b fix/deepseek-error-handling
git checkout -b release/1.1.0
```

保护规则：

- `main` 禁止直接 push。
- `main` 合并前必须至少 1 人 Review。
- 合并前必须通过单元测试和构建检查。
- 对安全、存储、录音、ASR、摘要等核心行为的修改必须补充或更新测试。

### 7. 提交信息规范

使用 Conventional Commits：

```text
<type>(<scope>): <subject>
```

常用类型：

- `feat`：新增功能
- `fix`：修复缺陷
- `test`：新增或修改测试
- `docs`：文档变更
- `refactor`：重构，不改变外部行为
- `perf`：性能优化
- `build`：构建系统或依赖变更
- `ci`：CI/CD 变更
- `chore`：杂项维护

示例：

```bash
git commit -m "feat(asr): add SenseVoice offline recognition"
git commit -m "fix(summary): handle empty DeepSeek response"
git commit -m "test(audio): cover PCM chunk boundary cases"
git commit -m "docs(github): add upload standard"
```

提交要求：

- 一个提交只做一类事情。
- 不提交临时调试代码、无关格式化和 IDE 自动改动。
- 行为变化必须尽量补测试。
- 涉及安全或隐私时，提交说明要写清风险和验证方式，但不能写入密钥内容。

### 8. Pull Request 规范

PR 标题使用提交信息风格：

```text
feat(summary): support configurable summary language
```

PR 描述必须包含：

```markdown
## 变更说明
- 

## 影响范围
- 

## 测试结果
- [ ] ./gradlew.bat testDebugUnitTest
- [ ] ./gradlew.bat assembleDebug
- [ ] 必要时完成真机/模拟器验证

## 风险与回滚
- 

## 隐私与安全
- [ ] 未提交 API Key、签名文件、用户录音、日志或个人路径
- [ ] 新增权限、网络请求或数据存储已说明原因
- [ ] 大文件和第三方资源许可证已确认
```

合并要求：

- 所有讨论已解决。
- CI 通过。
- 与目标分支无冲突。
- Review 通过。
- 变更涉及用户隐私、权限、录音、网络、模型文件时，必须有明确测试记录。

### 9. 上传前检查清单

在执行 `git push` 前建议依次检查：

```powershell
git status --short
git diff --check
git diff --stat
git diff --cached --stat
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

检查重点：

- `local.properties` 未被提交。
- 没有 API Key、Token、证书、签名文件。
- 没有真实录音、PCM、音频片段、用户数据。
- 没有 `build/`、`.gradle/`、IDE 缓存。
- 大文件已按 Git LFS 或制品库策略处理。
- README 能说明项目用途、构建方式、权限说明、模型来源和运行前置条件。
- 行为变化已经补充对应测试或说明无法测试的原因。

本次分析中尝试执行过：

```powershell
.\gradlew.bat testDebugUnitTest
```

但当前环境需要下载 Gradle 9.1.0，网络被沙箱拦截，测试未能完成。正式上传前应在具备网络和 Android SDK 的开发环境中重新执行。

### 10. 安全与隐私规范

- API Key 只能由用户在 App 内输入，并由 Android Keystore 加密保存。
- 不允许在源码、README、测试、截图、Issue 或 PR 评论中出现真实 API Key。
- 不上传真实用户录音、转写文本、摘要内容和说话人识别数据。
- 日志中不得打印完整请求头、Authorization、API 响应中的敏感字段。
- 新增网络接口时必须说明请求域名、用途、超时策略、错误处理和隐私影响。
- 新增 Android 权限时必须说明使用场景，并在 UI 或隐私说明中给出用户可理解的解释。

### 11. 发布规范

版本号建议遵循语义化版本：

```text
MAJOR.MINOR.PATCH
```

示例：

- `1.0.0`：首个稳定版本
- `1.1.0`：新增功能，兼容旧版本
- `1.1.1`：缺陷修复
- `2.0.0`：包含不兼容变更

发布前必须确认：

- `versionCode` 递增。
- `versionName` 与 Release Tag 一致。
- 构建产物来自干净工作区。
- 测试和构建通过。
- Release Notes 包含新增功能、修复项、已知问题和模型/依赖变更。
- APK/AAB、模型、AAR、Release 附件的许可证和分发权限已确认。

### 12. 推荐 GitHub 仓库设置

- 开启 branch protection。
- 开启 required pull request review。
- 开启 required status checks。
- 开启 secret scanning。
- 开启 Dependabot alerts。
- 开启 GitHub Actions 权限最小化。
- 对 Release、模型文件和制品库设置最小可见范围。
- 使用 CODEOWNERS 管理核心目录 Review 责任。

推荐 CODEOWNERS 示例：

```text
/app/src/main/java/com/example/blogrecording/audio/       @team-audio
/app/src/main/java/com/example/blogrecording/asr/         @team-ml
/app/src/main/java/com/example/blogrecording/summary/     @team-ai
/app/src/main/java/com/example/blogrecording/security/    @team-security
/app/src/main/assets/models/                              @team-ml @team-security
```

