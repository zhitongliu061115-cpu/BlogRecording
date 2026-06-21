# 增强总结风格 + 即时风格选择

## 背景
当前 4 种总结风格（BRIEF、DEEP_RECAP、TIMELINE_NOTES、POINTS_QUOTES_ACTIONS）发给 DeepSeek 的 Prompt 结构完全相同，只是标签文字不同。风格之间缺乏实质性差异。同时，风格选择仅限设置页，用户每次总结前不能临时切换，使用不便。

## 目标
1. **风格差异化**：6 种风格各有独立的 Prompt 模板，从输出格式、语气、篇幅、内容侧重四个维度拉开差距。
2. **即时选择**：点击"开始总结"时弹出风格选择对话框，用户可以临时选择本次总结的风格，而不必去设置页修改默认值。

## 范围
- 扩充 `SummaryStyle` 枚举为 6 种风格。
- 为每种风格编写独立的 Prompt 模板。
- 首页"开始总结"按钮触发风格选择弹窗。
- 旧数据自动迁移（BRIEF → QUICK_OVERVIEW 等）。

## 不做
- 不改 DeepSeek API 调用方式。
- 不改总结的分块/合并逻辑。
- 不改语言设置（SummaryLanguage 保持不变）。
- 不改变 SettingsScreen 外的其他页面布局。

## 新风格设计

| 枚举值 | 显示名 | 输出格式 | 语气 | 篇幅 | 侧重 |
|---|---|---|---|---|---|
| QUICK_OVERVIEW | 快速概览 | 一段话 | 轻松口语 | ~150字 | 核心主题 |
| BULLET_SUMMARY | 要点列表 | Markdown 分级列表 | 精炼 | ~500字 | 结构化要点 |
| DEEP_ANALYSIS | 深度复盘 | 多章节 | 严谨专业 | 不限 | 全面深度 |
| TIMELINE_NOTES | 时间线笔记 | 带时间戳条目 | 客观中性 | 按内容 | 时间顺序 |
| ACTION_ITEMS | 行动清单 | 待办项列表 | 务实直接 | 精炼 | 纯行动项 |
| GOLDEN_QUOTES | 金句收录 | 引用+语境 | 精选品味 | 按素材 | 金句摘录 |

## 旧数据迁移

| 旧值 | 新值 |
|---|---|
| BRIEF | QUICK_OVERVIEW |
| DEEP_RECAP | DEEP_ANALYSIS |
| POINTS_QUOTES_ACTIONS | BULLET_SUMMARY |
| TIMELINE_NOTES | TIMELINE_NOTES |

## 影响
- `data/Models.kt`：枚举变更
- `data/SettingsStore.kt`：读取设置时做旧值映射
- `summary/SummaryPromptBuilder.kt`：完全重写
- `summary/SummaryRepository.kt`：接受风格覆写参数
- `summary/SessionSummaryUseCase.kt`：接受风格覆写参数
- `ui/state/AppUiState.kt`：新增风格选择弹窗状态
- `ui/HomeScreen.kt`：新增风格选择弹窗
- `ui/AppViewModel.kt`：新增风格选择流程
- `ui/SettingsScreen.kt`：更新下拉标签

## 验收
- 设置页风格下拉显示 6 种风格，标签正确。
- 旧设置数据自动映射到新风格，不会丢失。
- 点击"开始总结"弹出风格选择弹窗，显示 6 种风格。
- 选择风格后开始总结，DeepSeek 返回结果与所选风格匹配。
- 取消选择不触发总结。
- 单元测试和编译通过。
