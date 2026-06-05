package com.example.blogrecording.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.recordsDataStore by preferencesDataStore(name = "podcast_recap_records")

class Repository(private val context: Context) {
    val sessions: Flow<List<RecordingSessionEntity>> = context.recordsDataStore.data.map { prefs ->
        val order = prefs[Keys.SessionOrder].orEmpty().lines().filter { it.isNotBlank() }
        order.mapNotNull { id ->
            prefs[stringPreferencesKey(sessionKey(id))]?.let(::decodeSession)
        }.sortedByDescending { it.createdAt }
    }

    suspend fun createSession(sourceType: AudioSourceType, settings: AppSettings): RecordingSessionEntity {
        val now = System.currentTimeMillis()
        val session = RecordingSessionEntity(
            id = UUID.randomUUID().toString(),
            title = "未命名播客记录",
            createdAt = now,
            updatedAt = now,
            sourceType = sourceType,
            status = RecordingStatus.NOT_STARTED,
            transcript = "",
            summary = null,
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            summaryModelName = settings.deepSeekModel,
            summaryStyle = settings.summaryStyle,
            summaryLanguage = settings.summaryLanguage,
            detectedSpeakerCount = 0,
            segmentCount = 0,
            errorMessage = null
        )
        saveSession(session)
        return session
    }

    suspend fun markInterruptedSessions() {
        val now = System.currentTimeMillis()
        context.recordsDataStore.edit { prefs ->
            prefs[Keys.SessionOrder].orEmpty()
                .lines()
                .filter { it.isNotBlank() }
                .forEach { id ->
                    val key = stringPreferencesKey(sessionKey(id))
                    val session = prefs[key]?.let(::decodeSession) ?: return@forEach
                    if (session.status.isInterruptedOnStartup()) {
                        prefs[key] = encodeSession(
                            session.copy(
                                status = RecordingStatus.ERROR,
                                updatedAt = now,
                                errorMessage = "上次录音被中断，请重新开始"
                            )
                        )
                    }
                }
        }
    }

    suspend fun saveSession(session: RecordingSessionEntity) {
        context.recordsDataStore.edit { prefs ->
            val existing = prefs[Keys.SessionOrder].orEmpty().lines().filter { it.isNotBlank() }
            val order = if (session.id in existing) existing else listOf(session.id) + existing
            prefs[Keys.SessionOrder] = order.joinToString(separator = "\n")
            prefs[stringPreferencesKey(sessionKey(session.id))] = encodeSession(session)
        }
    }

    suspend fun getSession(sessionId: String): RecordingSessionEntity? {
        val prefs = context.recordsDataStore.data.first()
        return prefs[stringPreferencesKey(sessionKey(sessionId))]?.let(::decodeSession)
    }

    suspend fun deleteSession(sessionId: String) {
        context.recordsDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(sessionKey(sessionId)))
            prefs.remove(stringPreferencesKey(segmentsKey(sessionId)))
            prefs.remove(stringPreferencesKey(speakersKey(sessionId)))
            prefs[Keys.SessionOrder] = prefs[Keys.SessionOrder]
                .orEmpty()
                .lines()
                .filter { it.isNotBlank() && it != sessionId }
                .joinToString(separator = "\n")
        }
    }

    suspend fun saveSegments(sessionId: String, segments: List<TranscriptSegmentEntity>) {
        context.recordsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(segmentsKey(sessionId))] = JSONArray(
                segments.map(::encodeSegment)
            ).toString()
        }
        rebuildSessionTranscript(sessionId)
    }

    suspend fun appendSegment(segment: TranscriptSegmentEntity) {
        val segments = getSegments(segment.sessionId) + segment
        saveSegments(segment.sessionId, segments)
    }

    suspend fun getSegments(sessionId: String): List<TranscriptSegmentEntity> {
        val prefs = context.recordsDataStore.data.first()
        val raw = prefs[stringPreferencesKey(segmentsKey(sessionId))] ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            decodeSegment(array.getJSONObject(index))
        }.sortedBy { it.startMs }
    }

    suspend fun saveSpeakerProfiles(sessionId: String, speakers: List<SpeakerProfileEntity>) {
        context.recordsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(speakersKey(sessionId))] = JSONArray(
                speakers.map(::encodeSpeaker)
            ).toString()
        }
    }

    suspend fun getSpeakerProfiles(sessionId: String): List<SpeakerProfileEntity> {
        val prefs = context.recordsDataStore.data.first()
        val raw = prefs[stringPreferencesKey(speakersKey(sessionId))] ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            decodeSpeaker(array.getJSONObject(index))
        }
    }

    suspend fun updateSummary(sessionId: String, summary: String): AppResult<Unit> {
        val session = getSession(sessionId) ?: return AppResult.Failure(
            com.example.blogrecording.common.AppError.Unknown("记录不存在")
        )
        saveSession(session.copy(summary = summary, status = RecordingStatus.COMPLETED, updatedAt = System.currentTimeMillis()))
        return AppResult.Success(Unit)
    }

    private suspend fun rebuildSessionTranscript(sessionId: String) {
        val session = getSession(sessionId) ?: return
        val segments = getSegments(sessionId)
        val transcript = segments.joinToString(separator = "\n\n") { it.toTranscriptText() }
        saveSession(
            session.copy(
                transcript = transcript,
                segmentCount = segments.size,
                detectedSpeakerCount = segments.map { it.speakerId }.distinct().size,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private object Keys {
        val SessionOrder = stringPreferencesKey("session_order")
    }
}

fun TranscriptSegmentEntity.toTranscriptText(): String {
    return "[${startMs.formatTimestamp()} - ${endMs.formatTimestamp()}] $speakerDisplayName：\n$text"
}

fun Long.formatTimestamp(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun sessionKey(id: String) = "session_$id"
private fun segmentsKey(sessionId: String) = "segments_$sessionId"
private fun speakersKey(sessionId: String) = "speakers_$sessionId"

private fun encodeSession(session: RecordingSessionEntity): String {
    return JSONObject()
        .put("id", session.id)
        .put("title", session.title)
        .put("createdAt", session.createdAt)
        .put("updatedAt", session.updatedAt)
        .put("sourceType", session.sourceType.name)
        .put("status", session.status.name)
        .put("transcript", session.transcript)
        .put("summary", session.summary)
        .put("asrModelName", session.asrModelName)
        .put("vadModelName", session.vadModelName)
        .put("diarizationModelName", session.diarizationModelName)
        .put("summaryModelName", session.summaryModelName)
        .put("summaryStyle", session.summaryStyle.name)
        .put("summaryLanguage", session.summaryLanguage.name)
        .put("detectedSpeakerCount", session.detectedSpeakerCount)
        .put("segmentCount", session.segmentCount)
        .put("errorMessage", session.errorMessage)
        .toString()
}

private fun decodeSession(raw: String): RecordingSessionEntity {
    val json = JSONObject(raw)
    return RecordingSessionEntity(
        id = json.getString("id"),
        title = json.getString("title"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt"),
        sourceType = AudioSourceType.valueOf(json.getString("sourceType")),
        status = RecordingStatus.valueOf(json.getString("status")),
        transcript = json.optString("transcript"),
        summary = json.optString("summary").takeIf { it.isNotBlank() && it != "null" },
        asrModelName = json.optString("asrModelName"),
        vadModelName = json.optString("vadModelName"),
        diarizationModelName = json.optString("diarizationModelName"),
        summaryModelName = json.optString("summaryModelName", "deepseek-chat"),
        summaryStyle = SummaryStyle.valueOf(json.optString("summaryStyle", SummaryStyle.POINTS_QUOTES_ACTIONS.name)),
        summaryLanguage = SummaryLanguage.valueOf(json.optString("summaryLanguage", SummaryLanguage.CHINESE.name)),
        detectedSpeakerCount = json.optInt("detectedSpeakerCount"),
        segmentCount = json.optInt("segmentCount"),
        errorMessage = json.optString("errorMessage").takeIf { it.isNotBlank() && it != "null" }
    )
}

private fun encodeSegment(segment: TranscriptSegmentEntity): JSONObject {
    return JSONObject()
        .put("id", segment.id)
        .put("sessionId", segment.sessionId)
        .put("startMs", segment.startMs)
        .put("endMs", segment.endMs)
        .put("speakerId", segment.speakerId)
        .put("speakerDisplayName", segment.speakerDisplayName)
        .put("text", segment.text)
        .put("language", segment.language)
        .put("confidence", segment.confidence)
        .put("vadConfidence", segment.vadConfidence)
        .put("isFinal", segment.isFinal)
        .put("createdAt", segment.createdAt)
}

private fun decodeSegment(json: JSONObject): TranscriptSegmentEntity {
    return TranscriptSegmentEntity(
        id = json.getString("id"),
        sessionId = json.getString("sessionId"),
        startMs = json.getLong("startMs"),
        endMs = json.getLong("endMs"),
        speakerId = json.getString("speakerId"),
        speakerDisplayName = json.getString("speakerDisplayName"),
        text = json.getString("text"),
        language = json.optString("language").takeIf { it.isNotBlank() && it != "null" },
        confidence = json.optDouble("confidence", Double.NaN).takeUnless { it.isNaN() }?.toFloat(),
        vadConfidence = json.optDouble("vadConfidence", Double.NaN).takeUnless { it.isNaN() }?.toFloat(),
        isFinal = json.optBoolean("isFinal", true),
        createdAt = json.getLong("createdAt")
    )
}

private fun encodeSpeaker(speaker: SpeakerProfileEntity): JSONObject {
    return JSONObject()
        .put("id", speaker.id)
        .put("sessionId", speaker.sessionId)
        .put("speakerId", speaker.speakerId)
        .put("displayName", speaker.displayName)
        .put("colorIndex", speaker.colorIndex)
        .put("segmentCount", speaker.segmentCount)
        .put("totalSpeechDurationMs", speaker.totalSpeechDurationMs)
        .put("createdAt", speaker.createdAt)
        .put("updatedAt", speaker.updatedAt)
}

private fun decodeSpeaker(json: JSONObject): SpeakerProfileEntity {
    return SpeakerProfileEntity(
        id = json.getString("id"),
        sessionId = json.getString("sessionId"),
        speakerId = json.getString("speakerId"),
        displayName = json.getString("displayName"),
        colorIndex = json.optInt("colorIndex"),
        segmentCount = json.optInt("segmentCount"),
        totalSpeechDurationMs = json.optLong("totalSpeechDurationMs"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt")
    )
}
