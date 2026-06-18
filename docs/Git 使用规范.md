# Git 使用规范

这份规范用于团队日常开发和上传 GitHub。目标是：分支清楚、提交可读、上传前不带敏感信息和无关文件。

## 1. 分支设计

- `main`：稳定分支，只放可发布代码。不要直接在 `main` 上开发。
- `develop`：日常集成分支，功能开发完成后先合并到这里。
- `feature/功能名`：新功能分支，例如 `feature/录音历史`。
- `fix/问题名`：普通问题修复分支，例如 `fix/摘要失败提示`。
- `hotfix/问题名`：线上紧急修复分支，从 `main` 拉出。
- `release/版本号`：发布准备分支，例如 `release/1.1.0`。

基本原则：

- 一个分支只做一件事。
- 分支名要能看懂，不要用 `test`、`temp`、`new` 这类模糊名字。
- `main` 和 `develop` 建议开启保护，代码通过 PR 合并。

## 2. 日常 Git 流程

### 开发新功能

```bash
git checkout develop
git pull origin develop
git checkout -b feature/功能名
```

开发完成后：

```bash
git status --short
git add 修改的文件
git commit -m "功能：新增某某功能"
git push origin feature/功能名
```

然后在 GitHub 上创建 PR，目标分支选择 `develop`。

### 修复普通问题

```bash
git checkout develop
git pull origin develop
git checkout -b fix/问题名
```

修复、测试、提交后推送：

```bash
git add 修改的文件
git commit -m "修复：说明修复了什么问题"
git push origin fix/问题名
```

然后创建 PR 合并到 `develop`。

### 发布版本

从 `develop` 拉出发布分支：

```bash
git checkout develop
git pull origin develop
git checkout -b release/版本号
```

在 `release/版本号` 上只做发布相关修改，例如版本号、发布说明、小问题修复。

验证通过后：

```bash
git checkout main
git pull origin main
git merge release/版本号
git tag v版本号
git push origin main
git push origin v版本号
```

发布分支的修改也要同步回 `develop`：

```bash
git checkout develop
git pull origin develop
git merge release/版本号
git push origin develop
```

### 紧急修复

紧急修复从 `main` 拉分支：

```bash
git checkout main
git pull origin main
git checkout -b hotfix/问题名
```

修复完成后合并回 `main` 并打 tag，再同步回 `develop`。

## 3. 提交信息规范

提交信息使用中文，格式如下：

```text
类型：简短说明
```

常用类型：

- `功能`：新增功能。
- `修复`：修复问题。
- `文档`：修改文档。
- `测试`：新增或修改测试。
- `重构`：整理代码结构，但不改变功能。
- `构建`：修改 Gradle、依赖、脚本等构建内容。
- `配置`：修改 Git、IDE、CI 等配置。

示例：

```bash
git commit -m "功能：新增录音历史页面"
git commit -m "修复：处理 DeepSeek 返回为空的问题"
git commit -m "文档：精简 Git 使用规范"
git commit -m "测试：补充 PCM 分片边界用例"
```

提交要求：

- 一次提交只做一类事情。
- 不要把调试代码、无关格式化、临时文件一起提交。
- 改了行为尽量补测试。
- 不要在提交信息里写密钥、账号、Token 或其他敏感内容。

## 4. 不要提交这些内容

不要提交密钥和本机配置：

```gitignore
local.properties
*.jks
*.keystore
*.p12
*.pem
*.key
```

不要提交构建产物和缓存：

```gitignore
build/
.gradle/
.externalNativeBuild/
.cxx/
captures/
*.apk
*.aab
*.ap_
```

不要提交用户数据和日志：

```gitignore
*.log
*.hprof
*.wav
*.mp3
*.m4a
*.pcm
*.flac
*.aac
```

特别注意：

- 不要上传 API Key、Token、证书、签名文件。
- 不要上传真实用户录音、转写文本、摘要内容。
- 不要上传个人路径、账号信息、调试日志。

## 5. 大文件规则

模型、AAR、APK、AAB 这类大文件不要直接当普通文件提交。

建议规则：

```gitattributes
*.onnx filter=lfs diff=lfs merge=lfs -text
*.aar filter=lfs diff=lfs merge=lfs -text
*.apk filter=lfs diff=lfs merge=lfs -text
*.aab filter=lfs diff=lfs merge=lfs -text
```

使用建议：

- 私有仓库可以用 Git LFS 管理模型和 AAR。
- 公开仓库建议把模型放到 GitHub Release、对象存储或模型仓库。
- 更新模型时记录来源、版本、许可证和适配的 App 版本。
- 上传第三方模型或库之前，先确认许可证允许分发。

## 6. 上传前检查

每次 `git push` 前先检查：

```powershell
git status --short
git diff --check
git diff --stat
```

如果已经暂存文件，再检查：

```powershell
git diff --cached --stat
```

Android 项目建议在上传前执行：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

上传前确认：

- 没有提交 `local.properties`。
- 没有提交 API Key、Token、证书、签名文件。
- 没有提交真实录音、用户数据、日志。
- 没有提交 `build/`、`.gradle/`、IDE 缓存。
- 大文件已经按 Git LFS、Release 或制品库规则处理。
- 行为变化已经补测试，或者说明了无法测试的原因。

## 7. PR 合并要求

创建 PR 时说明：

- 改了什么。
- 为什么要改。
- 怎么测试的。
- 有没有风险。

合并前确认：

- PR 目标分支正确。
- 没有冲突。
- 测试和构建通过。
- 至少一名成员 Review。
- 涉及权限、录音、网络、模型、密钥存储的修改，要重点说明安全和隐私影响。
