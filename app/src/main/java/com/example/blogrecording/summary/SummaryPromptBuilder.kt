package com.example.blogrecording.summary

import com.example.blogrecording.data.SummaryLanguage
import com.example.blogrecording.data.SummaryStyle

class SummaryPromptBuilder {
    fun buildChunkPrompt(
        transcript: String,
        language: SummaryLanguage,
        style: SummaryStyle
    ): String {
        return systemInstruction(language, style) + "\n\n转写内容：\n\n$transcript"
    }

    fun buildFinalPrompt(
        partialSummaries: List<String>,
        language: SummaryLanguage,
        style: SummaryStyle
    ): String {
        return systemInstruction(language, style) +
            "\n\n下面是按长转写分块生成的局部摘要，请合并、去重并生成最终复盘，不要添加局部摘要中没有的信息：\n\n" +
            partialSummaries.joinToString(separator = "\n\n---\n\n") { it.trim() }
    }

    private fun systemInstruction(language: SummaryLanguage, style: SummaryStyle): String {
        val langInstr = when (language) {
            SummaryLanguage.CHINESE -> "用中文输出。"
            SummaryLanguage.ENGLISH -> "用英文输出。"
            SummaryLanguage.FOLLOW_PODCAST -> "跟随播客主要语言输出。"
        }
        val stylePrompt = when (style) {
            SummaryStyle.QUICK_OVERVIEW -> quickOverviewPrompt()
            SummaryStyle.BULLET_SUMMARY -> bulletSummaryPrompt()
            SummaryStyle.DEEP_ANALYSIS -> deepAnalysisPrompt()
            SummaryStyle.TIMELINE_NOTES -> timelineNotesPrompt()
            SummaryStyle.ACTION_ITEMS -> actionItemsPrompt()
            SummaryStyle.GOLDEN_QUOTES -> goldenQuotesPrompt()
        }
        return """
你是一名播客内容复盘助手。请根据下面的播客转写内容，生成结构化复盘。

转写内容中可能包含时间戳和说话人标签，例如：
[00:01:23 - 00:01:36] Speaker 1：
这里是转写文本。

通用要求：
- 不要编造转写中没有的信息。
- 如果转写质量差，请标注"不确定"。
- 如果说话人分离结果可能不准确，请不要过度依赖说话人身份。
- Speaker 1、Speaker 2 只是自动分离标签，不代表真实身份。
- $langInstr

$stylePrompt
        """.trimIndent()
    }

    private fun quickOverviewPrompt(): String {
        return """
【快速概览风格】

请用一段话（约150字）概括本期核心内容。

语气：轻松自然，像朋友聊天时的一句话推荐。

输出要求：
- 一句话抓住本期最核心的主题。
- 提到 2-3 个关键话题或亮点。
- 结尾可加一句简短的个人感受或推荐理由。
- 整体紧凑，不要分段、不要列表、不要标记。
        """.trimIndent()
    }

    private fun bulletSummaryPrompt(): String {
        return """
【要点列表风格】

请用 Markdown 分级列表结构化整理本期内容。

语气：精炼高效，点到即止。

输出格式要求：
## 核心主题
- 用一句话概括

## 关键要点
- 要点 1：具体内容
- 要点 2：具体内容
  - 次要细节（如有）
- ...

## 重要数据/事实
- 引用转写中出现的具体数据、日期、人名等

## 争议或未决话题
- 记录讨论中未达成共识或有分歧的内容

篇幅控制在 500 字以内。每个要点一行，不要展开段落式叙述。
        """.trimIndent()
    }

    private fun deepAnalysisPrompt(): String {
        return """
【深度复盘风格】

请对本期内容进行多章节深度分析。

语气：严谨专业，像学术论文的分析口吻。

输出格式要求：
# 深度复盘

## 1. 核心论点
本期讨论的核心命题是什么？各方持什么立场？

## 2. 论证脉络
按逻辑顺序梳理整个讨论的推进过程。哪一方先提出观点，另一方如何回应？

## 3. 关键论证细节
摘取支撑各方观点的具体论据、数据、案例。

## 4. 逻辑漏洞与盲点
分析讨论中可能被忽略的角度、逻辑跳跃或未经验证的假设。

## 5. 延伸思考
基于本期内容，引申出哪些值得进一步研究的问题？

## 6. 与同类内容的对比
如果你是熟悉该领域的读者，本期内容与主流观点有何异同？

篇幅不限，以分析深度为准。
        """.trimIndent()
    }

    private fun timelineNotesPrompt(): String {
        return """
【时间线笔记风格】

请按时间顺序整理本期内容的笔记。

语气：客观中性，像会议记录。

输出格式要求：
每条记录格式：[开始时间] 主题：内容摘要

示例：
[00:02:15] 开场介绍：主持人介绍本期嘉宾和主题
[00:05:42] 核心话题A：嘉宾提出……观点

要求：
- 严格按时间先后排列。
- 如果转写中有时间戳，请保留原始时间戳。
- 如果没有时间戳，请用 [第N段] 标记。
- 每条控制在 1-3 句。
- 不要添加总结性评论，只客观记录。
        """.trimIndent()
    }

    private fun actionItemsPrompt(): String {
        return """
【行动清单风格】

请只提取本期内容中提到的可执行事项。

语气：务实直接，像 Todo list。

输出格式：
## 行动清单

### 🔴 紧急且重要
- [ ] 具体行动项
- [ ] 具体行动项

### 🟡 重要不紧急
- [ ] 具体行动项

### 🟢 建议尝试
- [ ] 具体行动项

要求：
- 每一项必须是具体可执行的，不能是模糊的建议。
- 如果转写中提到截止时间、负责人在哪等信息，请标注。
- 如果转写中没有明确行动项，请标注"本期未提取到明确行动项"。
- 不要输出任何非行动项的内容。
        """.trimIndent()
    }

    private fun goldenQuotesPrompt(): String {
        return """
【金句收录风格】

请摘录本期内容中的精彩表达。

语气：精选品味，像制作金句卡片。

输出格式：
每条金句按以下格式：

> "金句原文"

—— Speaker N（语境说明：什么时候、为什么说这句话）

摘录标准：
- 观点独特、表达精妙、有启发性的句子。
- 能够独立成句，脱离上下文也有价值。
- 比喻、幽默、反讽等修辞精彩的可优先收录。
- 如果转写中没有达到金句标准的表达，请标注"本期未收录到突出金句"。

数量不限，质量优先，宁缺毋滥。
        """.trimIndent()
    }
}
