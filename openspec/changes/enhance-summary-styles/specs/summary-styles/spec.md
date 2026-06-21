## Requirement: 即时风格选择

### Scenario: 点击"开始总结"弹出风格选择
Given 用户在首页看到一张可总结的播客卡片
When 用户点击"开始总结"
Then 弹出风格选择对话框，显示 6 种风格选项
And 每种风格显示名称和简短描述

### Scenario: 选择风格后开始总结
Given 风格选择对话框已弹出
When 用户点击某个风格
Then 关闭对话框，以该风格开始生成总结
And 处理状态显示"正在总结"

### Scenario: 取消风格选择
Given 风格选择对话框已弹出
When 用户点击取消或对话框外部
Then 关闭对话框，不触发总结

---

## Requirement: 风格 Prompt 差异化

### Scenario: 快速概览风格
Given 用户选择"快速概览"
When DeepSeek 生成总结
Then Prompt 要求输出一段约150字的轻松概括

### Scenario: 要点列表风格
Given 用户选择"要点列表"
When DeepSeek 生成总结
Then Prompt 要求 Markdown 分级列表格式，约500字

### Scenario: 深度复盘风格
Given 用户选择"深度复盘"
When DeepSeek 生成总结
Then Prompt 要求多章节严谨分析，不限制篇幅

### Scenario: 时间线笔记风格
Given 用户选择"时间线笔记"
When DeepSeek 生成总结
Then Prompt 要求按时间顺序列出条目，带时间戳

### Scenario: 行动清单风格
Given 用户选择"行动清单"
When DeepSeek 生成总结
Then Prompt 要求只输出可执行的待办项

### Scenario: 金句收录风格
Given 用户选择"金句收录"
When DeepSeek 生成总结
Then Prompt 要求摘录原文金句并附语境说明

---

## Requirement: 旧数据兼容

### Scenario: 旧 BRIEF 风格自动映射
Given 用户之前设置风格为 BRIEF
When 启动 App 读取设置
Then 风格自动映射为 QUICK_OVERVIEW

### Scenario: 旧 DEEP_RECAP 风格自动映射
Given 用户之前设置风格为 DEEP_RECAP
When 启动 App 读取设置
Then 风格自动映射为 DEEP_ANALYSIS
