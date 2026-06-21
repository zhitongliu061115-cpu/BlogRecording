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
            importedContent = json.optJSONObject("importedContent")?.let(::decodeImportedContent)
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
            updatedAt = json.optLong("updatedAt", json.optLong("importedAt"))
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
    }

    fun decodeSummary(json: JSONObject): SessionSummary {
        return SessionSummary(
            text = json.optString("text"),
            status = SummaryStatus.valueOf(json.getString("status")),
            modelName = json.optString("modelName", "deepseek-chat"),
            generatedAt = json.nullableLong("generatedAt"),
            updatedAt = json.getLong("updatedAt"),
            errorMessage = json.nullableString("errorMessage")
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

    private inline fun <reified T : Enum<T>> String.toNullableEnum(): T? {
        if (isBlank() || this == "null") return null
        return enumValues<T>().firstOrNull { it.name == this }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
        return enumValues<T>().firstOrNull { it.name == this } ?: default
    }
}
