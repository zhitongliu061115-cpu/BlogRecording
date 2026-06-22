# 贡献指南 (Contributing to EchoMind)

🎉 首先，非常感谢你对 **BlogRecording** 感兴趣并愿意贡献你的力量！

本项目秉持 **Community over Code** 的理念。无论你是来修复错别字、补充文档、完善测试用例，还是提交核心功能代码，我们都非常欢迎。

在开始之前，请确保你已经阅读并同意遵守我们的 [行为准则 (Code of Conduct)](./CODE_OF_CONDUCT.md)。

## 1. 我们的核心开发范式：SDD (规格驱动开发)

与传统的敏捷开发不同，本项目采用 **SDD (Spec-Driven Development, 规格驱动开发)** 模式。我们认为，规格即文档、即测试依据、即社区贡献入口。

如果你想为项目增加新特性，请遵循以下闭环：
1. **先写规格 (Spec)：** 在 `specs/` 目录下用自然语言和验收标准描述该特性（“做什么、为什么、怎样算完成”）。
2. **开发实现：** 基于规格编写代码，或驱动 AI 生成初步实现。
3. **评测与反馈：** 通过我们内置的 `Harness` 评测脚手架自动验证质量，形成完整的闭环。

## 2. 如何开始贡献？

### 寻找任务
如果你是第一次参与贡献，建议你查看 Issue 列表中带有 `good first issue` 或 `help wanted` 标签的任务。这些任务通常边界清晰，非常适合作为新手的破冰之旅。

### 本地开发环境搭建
1. **Fork 本仓库** 到你的个人 GitHub 账号下。
2. **克隆 (Clone)** 你 Fork 的仓库到本地：
```bash
   git clone https://github.com/zhitongliu061115-cpu/BlogRecording.git
   cd BlogRecording
