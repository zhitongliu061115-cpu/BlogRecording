package com.example.blogrecording.data

import org.json.JSONArray
import org.json.JSONObject

internal object PodcastSessionJsonCodec {
    fun encodeSession(session: PodcastSession): JSONObject {
        return JSONObject()
            .put("id", session.id)
            .put("title", session.title)
            .put("createdAt", session.createdAt)
            .put("updatedAt", session.updatedAt)
            .put("sourceType", session.sourceType?.name)
            .put("status", session.status.name)
            .put("activeSegmentId", session.activeSegmentId)
            .put("lastCompletedSegmentId", session.lastCompletedSegmentId)
            .put("transcript", session.transcript)
            .put("summary", session.summary?.let(::encodeSummary))
            .put("summaryStyle", session.summaryStyle.name)
            .put("summaryLanguage", session.summaryLanguage.name)
            .put("summaryModelName", session.summaryModelName)
            .put("asrModelName", session.asrModelName)
            .put("vadModelName", session.vadModelName)
            .put("diarizationModelName", session.diarizationModelName)
            .put("detectedSpeakerCount", session.detectedSpeakerCount)
            .put("recordingSegmentCount", session.recordingSegmentCount)
            .put("transcriptSegmentCount", session.transcriptSegmentCount)
            .put("errorMessage", session.errorMessage)
            .put("legacyRecordingSessionId", session.legacyRecordingSessionId)
            .put("importedContent", session.importedContent?.let(::encodeImportedContent))
            .put("tagGeneration", encodeTagGeneration(session.tagGeneration))
    }

    fun decodeSession(json: JSONObject): PodcastSession {
        return PodcastSession(
            id = json.getString("id"),
            title = json.getString("title"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            sourceType = json.optString("sourceType").toNullableEnum<AudioSourceType>(),
            status = PodcastSessionStatus.valueOf(json.getString("status")),
            activeSegmentId = json.nullableString("activeSegmentId"),
            lastCompletedSegmentId = json.nullableString("lastCompletedSegmentId"),
            transcript = json.optString("transcript"),
            summary = json.optJSONObject("summary")?.let(::decodeSummary),
            summaryStyle = json.optString("summaryStyle", SummaryStyle.POINTS_QUOTES_ACTIONS.name)
                .toEnumOrDefault(SummaryStyle.POINTS_QUOTES_ACTIONS),
            summaryLanguage = json.optString("summaryLanguage", SummaryLanguage.CHINESE.name)
                .toEnumOrDefault(SummaryLanguage.CHINESE),
            summaryModelName = json.optString("summaryModelName", "deepseek-chat"),
            asrModelName = json.optString("asrModelName"),
            vadModelName = json.optString("vadModelName"),
            diarizationModelName = json.optString("diarizationModelName"),
            detectedSpeakerCount = json.optInt("detectedSpeakerCount"),
            recordingSegmentCount = json.optInt("recordingSegmentCount"),
            transcriptSegmentCount = json.optInt("transcriptSegmentCount"),
            errorMessage = json.nullableString("errorMessage"),
            legacyRecordingSessionId = json.nullableString("legacyRecordingSessionId"),
            importedContent = json.optJSONObject("importedContent")?.let(::decodeImportedContent),
            tagGeneration = json.optJSONObject("tagGeneration")?.let(::decodeTagGeneration)
                ?: SessionTagGeneration.empty()
        )
    }

    fun encodeImportedContent(metadata: ImportedContentMetadata): JSONObject {
        return JSONObject()
            .put("kind", metadata.kind.name)
            .put("displayName", metadata.displayName)
            .put("mimeType", metadata.mimeType)
            .put("sizeBytes", metadata.sizeBytes)
            .put("durationMs", metadata.durationMs)
            .put("status", metadata.status.name)
            .put("errorMessage", metadata.errorMessage)
            .put("importedAt", metadata.importedAt)
            .put("updatedAt", metadata.updatedAt)
            .put("sourceUrl", metadata.sourceUrl)
            .put("sourceHost", metadata.sourceHost)
    }

    fun decodeImportedContent(json: JSONObject): ImportedContentMetadata {
        return ImportedContentMetadata(
            kind = json.optString("kind", ImportedContentKind.LOCAL_MEDIA.name)
                .toEnumOrDefault(ImportedContentKind.LOCAL_MEDIA),
            displayName = json.optString("displayName", "Imported media"),
            mimeType = json.nullableString("mimeType"),
            sizeBytes = json.nullableLong("sizeBytes"),
            durationMs = json.nullableLong("durationMs"),
            status = json.optString("status", ImportedContentStatus.COMPLETED.name)
                .toEnumOrDefault(ImportedContentStatus.COMPLETED),
            errorMessage = json.nullableString("errorMessage"),
            importedAt = json.optLong("importedAt", json.optLong("updatedAt")),
            updatedAt = json.optLong("updatedAt", json.optLong("importedAt")),
            sourceUrl = json.nullableString("sourceUrl"),
            sourceHost = json.nullableString("sourceHost")
        )
    }

    fun encodeSummary(summary: SessionSummary): JSONObject {
        return JSONObject()
            .put("text", summary.text)
            .put("status", summary.status.name)
            .put("modelName", summary.modelName)
            .put("generatedAt", summary.generatedAt)
            .put("updatedAt", summary.updatedAt)
            .put("errorMessage", summary.errorMessage)
            .put("structured", summary.structured?.let(::encodeStructuredSummary))
    }

    fun decodeSummary(json: JSONObject): SessionSummary {
        return SessionSummary(
            text = json.optString("text"),
            status = SummaryStatus.valueOf(json.getString("status")),
            modelName = json.optString("modelName", "deepseek-chat"),
            generatedAt = json.nullableLong("generatedAt"),
            updatedAt = json.getLong("updatedAt"),
            errorMessage = json.nullableString("errorMessage"),
            structured = json.optJSONObject("structured")?.let(::decodeStructuredSummary)
        )
    }

    fun encodeStructuredSummary(summary: StructuredSummary): JSONObject {
        return JSONObject()
            .put("overview", summary.overview)
            .put("keyPoints", JSONArray(summary.keyPoints))
            .put("actionItems", JSONArray(summary.actionItems))
            .put("openQuestions", JSONArray(summary.openQuestions))
            .put("quoteCandidates", JSONArray(summary.quoteCandidates))
            .put("timelineChapters", encodeTimelineChapters(summary.timelineChapters))
            .put("parseStatus", summary.parseStatus.name)
    }

    fun decodeStructuredSummary(json: JSONObject): StructuredSummary {
        return StructuredSummary(
            overview = json.optString("overview"),
            keyPoints = json.stringList("keyPoints"),
            actionItems = json.stringList("actionItems"),
            openQuestions = json.stringList("openQuestions"),
            quoteCandidates = json.stringList("quoteCandidates"),
            timelineChapters = json.optJSONArray("timelineChapters")?.let(::decodeTimelineChapters).orEmpty(),
            parseStatus = json.optString("parseStatus", StructuredSummaryParseStatus.STRUCTURED.name)
                .toEnumOrDefault(StructuredSummaryParseStatus.STRUCTURED)
        )
    }

    fun encodeTimelineChapters(chapters: List<TimelineChapter>): JSONArray {
        return JSONArray(chapters.map(::encodeTimelineChapter))
    }

    fun decodeTimelineChapters(array: JSONArray): List<TimelineChapter> {
        return List(array.length()) { index ->
            decodeTimelineChapter(array.getJSONObject(index))
        }
    }

    fun encodeTimelineChapter(chapter: TimelineChapter): JSONObject {
        return JSONObject()
            .put("title", chapter.title)
            .put("startMs", chapter.startMs)
            .put("endMs", chapter.endMs)
            .put("keyPoints", JSONArray(chapter.keyPoints))
            .put("sourceStartMs", chapter.sourceStartMs)
            .put("sourceEndMs", chapter.sourceEndMs)
    }

    fun decodeTimelineChapter(json: JSONObject): TimelineChapter {
        return TimelineChapter(
            title = json.optString("title"),
            startMs = json.nullableLong("startMs"),
            endMs = json.nullableLong("endMs"),
            keyPoints = json.stringList("keyPoints"),
            sourceStartMs = json.nullableLong("sourceStartMs"),
            sourceEndMs = json.nullableLong("sourceEndMs")
        )
    }

    fun encodeTagGeneration(tagGeneration: SessionTagGeneration): JSONObject {
        return JSONObject()
            .put("tags", JSONArray(tagGeneration.tags.map(::encodeGeneratedTag)))
            .put("status", tagGeneration.status.name)
            .put("generatedAt", tagGeneration.generatedAt)
            .put("updatedAt", tagGeneration.updatedAt)
            .put("errorMessage", tagGeneration.errorMessage)
    }

    fun decodeTagGeneration(json: JSONObject): SessionTagGeneration {
        return SessionTagGeneration(
            tags = json.optJSONArray("tags")?.let(::decodeGeneratedTags).orEmpty(),
            status = json.optString("status", TagGenerationStatus.NOT_READY.name)
                .toEnumOrDefault(TagGenerationStatus.NOT_READY),
            generatedAt = json.nullableLong("generatedAt"),
            updatedAt = json.optLong("updatedAt"),
            errorMessage = json.nullableString("errorMessage")
        )
    }

    fun encodeGeneratedTag(tag: GeneratedTag): JSONObject {
        return JSONObject()
            .put("text", tag.text)
            .put("normalizedKey", tag.normalizedKey)
            .put("order", tag.order)
            .put("source", tag.source.name)
            .put("generatedAt", tag.generatedAt)
            .put("status", tag.status.name)
    }

    fun decodeGeneratedTags(array: JSONArray): List<GeneratedTag> {
        return List(array.length()) { index ->
            decodeGeneratedTag(array.getJSONObject(index))
        }.sortedBy { it.order }
    }

    fun decodeGeneratedTag(json: JSONObject): GeneratedTag {
        return GeneratedTag(
            text = json.optString("text"),
            normalizedKey = json.optString("normalizedKey"),
            order = json.optInt("order"),
            source = json.optString("source", GeneratedTagSource.STRUCTURED_SUMMARY.name)
                .toEnumOrDefault(GeneratedTagSource.STRUCTURED_SUMMARY),
            generatedAt = json.optLong("generatedAt"),
            status = json.optString("status", TagGenerationStatus.GENERATED.name)
                .toEnumOrDefault(TagGenerationStatus.GENERATED)
        )
    }

    fun encodeRecordingSegments(segments: List<RecordingSegment>): JSONArray {
        return JSONArray(segments.map(::encodeRecordingSegment))
    }

    fun decodeRecordingSegments(array: JSONArray): List<RecordingSegment> {
        return List(array.length()) { index ->
            decodeRecordingSegment(array.getJSONObject(index))
        }.sortedBy { it.index }
    }

    fun encodeRecordingSegment(segment: RecordingSegment): JSONObject {
        return JSONObject()
            .put("id", segment.id)
            .put("sessionId", segment.sessionId)
            .put("index", segment.index)
            .put("sourceType", segment.sourceType.name)
            .put("status", segment.status.name)
            .put("startedAt", segment.startedAt)
            .put("endedAt", segment.endedAt)
            .put("durationMs", segment.durationMs)
            .put("pcmFilePath", segment.pcmFilePath)
            .put("audioFilePath", segment.audioFilePath)
            .put("sampleRate", segment.sampleRate)
            .put("channelCount", segment.channelCount)
            .put("transcriptSegmentIds", JSONArray(segment.transcriptSegmentIds))
            .put("errorMessage", segment.errorMessage)
            .put("createdAt", segment.createdAt)
            .put("updatedAt", segment.updatedAt)
    }

    fun decodeRecordingSegment(json: JSONObject): RecordingSegment {
        val transcriptIds = json.optJSONArray("transcriptSegmentIds") ?: JSONArray()
        return RecordingSegment(
            id = json.getString("id"),
            sessionId = json.getString("sessionId"),
            index = json.getInt("index"),
            sourceType = AudioSourceType.valueOf(json.getString("sourceType")),
            status = RecordingSegmentStatus.valueOf(json.getString("status")),
            startedAt = json.getLong("startedAt"),
            endedAt = json.nullableLong("endedAt"),
            durationMs = json.optLong("durationMs"),
            pcmFilePath = json.nullableString("pcmFilePath"),
            audioFilePath = json.nullableString("audioFilePath"),
            sampleRate = json.nullableInt("sampleRate"),
            channelCount = json.nullableInt("channelCount"),
            transcriptSegmentIds = List(transcriptIds.length()) { index -> transcriptIds.getString(index) },
            errorMessage = json.nullableString("errorMessage"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt")
        )
    }

    private fun JSONObject.nullableString(name: String): String? {
        return optString(name).takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JSONObject.nullableLong(name: String): Long? {
        return optLong(name).takeUnless { isNull(name) }
    }

    private fun JSONObject.nullableInt(name: String): Int? {
        return optInt(name).takeUnless { isNull(name) }
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "null" }
    }

    private inline fun <reified T : Enum<T>> String.toNullableEnum(): T? {
        if (isBlank() || this == "null") return null
        return enumValues<T>().firstOrNull { it.name == this }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
        return enumValues<T>().firstOrNull { it.name == this } ?: default
    }
}
