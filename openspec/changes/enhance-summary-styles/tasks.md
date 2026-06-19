# Tasks

- [ ] 更新 `Models.kt` SummaryStyle 枚举为 6 种，添加风格描述字段
- [ ] 在 `SettingsStore.kt` 读取时做旧值映射（BRIEF→QUICK_OVERVIEW 等）
- [ ] 重写 `SummaryPromptBuilder.kt`，每种风格独立 Prompt 模板
- [ ] `SummaryRepository.kt` / `SessionSummaryUseCase.kt` 接受可选的风格覆写参数
- [ ] `AppUiState.kt` 新增 `summaryStylePicker` 弹窗状态
- [ ] `AppViewModel.kt` 新增 `requestSummaryStylePick` / `startSummaryWithStyle` / `dismissStylePicker`
- [ ] `HomeScreen.kt` 新增风格选择弹窗 UI
- [ ] `SettingsScreen.kt` 更新风格下拉标签
- [ ] 补充单元测试：旧值映射、Prompt 模板差异、风格选择流程
- [ ] 编译通过（`assembleDebug`）
- [ ] 单元测试通过（`testDebugUnitTest`）
- [ ] 真机验证：风格弹窗显示 6 种、选择后正常生成总结
