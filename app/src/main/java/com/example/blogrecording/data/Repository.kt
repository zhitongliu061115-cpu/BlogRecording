package com.example.blogrecording.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.recordsDataStore by preferencesDataStore(name = RecordingPersistenceContract.DATASTORE_NAME)

class Repository(private val context: Context) : SessionRepository {
    val sessions: Flow<List<RecordingSessionEntity>> = context.recordsDataStore.data.map { prefs ->
        val order = prefs[Keys.SessionOrder].orEmpty().lines().filter { it.isNotBlank() }
        order.mapNotNull { id ->
            prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(id))]?.let(::decodeSession)
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
        markInterruptedPodcastAndLegacySessions()
        return
        val now = System.currentTimeMillis()
        context.recordsDataStore.edit { prefs ->
            prefs[Keys.SessionOrder].orEmpty()
                .lines()
                .filter { it.isNotBlank() }
                .forEach { id ->
                    val key = stringPreferencesKey(RecordingPersistenceContract.sessionKey(id))
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
            prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(session.id))] = encodeSession(session)
        }
    }

    suspend fun getSession(sessionId: String): RecordingSessionEntity? {
        val prefs = context.recordsDataStore.data.first()
        return prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(sessionId))]?.let(::decodeSession)
    }

    suspend fun deleteSession(sessionId: String) {
        context.recordsDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(RecordingPersistenceContract.sessionKey(sessionId)))
            prefs.remove(stringPreferencesKey(RecordingPersistenceContract.segmentsKey(sessionId)))
            prefs.remove(stringPreferencesKey(RecordingPersistenceContract.speakersKey(sessionId)))
            prefs.remove(stringPreferencesKey(RecordingPersistenceContract.recordingSegmentsKey(sessionId)))
            prefs[Keys.SessionOrder] = prefs[Keys.SessionOrder]
                .orEmpty()
                .lines()
                .filter { it.isNotBlank() && it != sessionId }
                .joinToString(separator = "\n")
        }
    }

    suspend fun saveSegments(sessionId: String, segments: List<TranscriptSegmentEntity>) {
        context.recordsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(RecordingPersistenceContract.segmentsKey(sessionId))] = JSONArray(
                segments.map(::encodeSegment)
            ).toString()
        }
        rebuildSessionTranscript(sessionId)
    }

    suspend fun appendSegment(segment: TranscriptSegmentEntity) {
        val segments = getSegments(segment.sessionId) + segment
        saveSegments(segment.sessionId, segments)
        if (segment.recordingSegmentId != null) {
            attachTranscriptToRecordingSegment(segment)
        }
    }

    suspend fun markSegmentTranscriptionFailed(
        sessionId: String,
        recordingSegmentId: String,
        errorMessage: String
    ): AppResult<RecordingSegment> {
        val detail = getPodcastSessionDetail(sessionId) ?: return missingSession()
        val segment = detail.recordingSegments.firstOrNull { it.id == recordingSegmentId }
            ?: return missingSession()
        val failed = segment.copy(
            status = RecordingSegmentStatus.ERROR,
            errorMessage = errorMessage.takeIf { it.isNotBlank() } ?: "Transcription failed",
            updatedAt = System.currentTimeMillis()
        )
        return updateSegment(failed)
    }

    suspend fun getSegments(sessionId: String): List<TranscriptSegmentEntity> {
        val prefs = context.recordsDataStore.data.first()
        val raw = prefs[stringPreferencesKey(RecordingPersistenceContract.segmentsKey(sessionId))] ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            decodeSegment(array.getJSONObject(index))
        }.sortedBy { it.startMs }
    }

    suspend fun saveSpeakerProfiles(sessionId: String, speakers: List<SpeakerProfileEntity>) {
        context.recordsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(RecordingPersistenceContract.speakersKey(sessionId))] = JSONArray(
                speakers.map(::encodeSpeaker)
            ).toString()
        }
    }

    suspend fun getSpeakerProfiles(sessionId: String): List<SpeakerProfileEntity> {
        val prefs = context.recordsDataStore.data.first()
        val raw = prefs[stringPreferencesKey(RecordingPersistenceContract.speakersKey(sessionId))] ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { index ->
            decodeSpeaker(array.getJSONObject(index))
        }
    }

    suspend fun updateSummary(sessionId: String, summary: String): AppResult<Unit> {
        val podcastSession = getPodcastSession(sessionId)
        if (podcastSession != null) {
            return when (val result = updateSummaryLifecycle(
                sessionId = sessionId,
                status = SummaryStatus.SUMMARIZED,
                modelName = podcastSession.summaryModelName,
                summaryText = summary,
                generatedAt = System.currentTimeMillis()
            )) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Failure -> AppResult.Failure(result.error)
            }
        }
        val session = getSession(sessionId) ?: return AppResult.Failure(
            com.example.blogrecording.common.AppError.Unknown("记录不存在")
        )
        saveSession(session.copy(summary = summary, status = RecordingStatus.COMPLETED, updatedAt = System.currentTimeMillis()))
        return AppResult.Success(Unit)
    }

    override suspend fun createSession(
        title: String?,
        sourceType: AudioSourceType?
    ): PodcastSession {
        val now = System.currentTimeMillis()
        val session = PodcastSession(
            id = UUID.randomUUID().toString(),
            title = title?.takeIf { it.isNotBlank() } ?: "Untitled podcast",
            createdAt = now,
            updatedAt = now,
            sourceType = sourceType,
            status = PodcastSessionStatus.DRAFT,
            activeSegmentId = null,
            lastCompletedSegmentId = null,
            transcript = "",
            summary = null,
            summaryStyle = SummaryStyle.POINTS_QUOTES_ACTIONS,
            summaryLanguage = SummaryLanguage.CHINESE,
            summaryModelName = "deepseek-chat",
            asrModelName = "SenseVoice sherpa-onnx",
            vadModelName = "Silero VAD sherpa-onnx",
            diarizationModelName = "sherpa-onnx speaker diarization",
            detectedSpeakerCount = 0,
            recordingSegmentCount = 0,
            transcriptSegmentCount = 0,
            errorMessage = null,
            legacyRecordingSessionId = null
        )
        savePodcastSession(session)
        saveRecordingSegments(session.id, emptyList())
        return session
    }

    override suspend fun createImportedSession(
        title: String,
        metadata: ImportedContentMetadata
    ): AppResult<PodcastSession> {
        val session = createSession(
            title = title.takeIf { it.isNotBlank() } ?: metadata.displayName,
            sourceType = AudioSourceType.LOCAL_MEDIA
        ).copy(
            status = PodcastSessionStatus.PROCESSING,
            importedContent = metadata,
            updatedAt = metadata.updatedAt
        )
        savePodcastSession(session)
        return AppResult.Success(session)
    }

    override suspend fun updateImportedContent(
        sessionId: String,
        metadata: ImportedContentMetadata,
        status: PodcastSessionStatus?
    ): AppResult<PodcastSession> {
        val detail = getPodcastSessionDetail(sessionId) ?: return missingSession()
        val updated = detail.session.copy(
            status = status ?: detail.session.status,
            activeSegmentId = if (status == PodcastSessionStatus.ERROR) null else detail.session.activeSegmentId,
            importedContent = metadata,
            updatedAt = metadata.updatedAt,
            errorMessage = metadata.errorMessage
        )
        savePodcastSession(updated)
        return AppResult.Success(updated)
    }

    override suspend fun appendImportedSegment(
        sessionId: String,
        startedAt: Long,
        durationMs: Long,
        sampleRate: Int,
        channelCount: Int
    ): AppResult<RecordingSegment> {
        val detail = getPodcastSessionDetail(sessionId) ?: return missingSession()
        val now = System.currentTimeMillis()
        val segment = RecordingSegment(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            index = detail.recordingSegments.size + 1,
            sourceType = AudioSourceType.LOCAL_MEDIA,
            status = RecordingSegmentStatus.COMPLETED,
            startedAt = startedAt,
            endedAt = startedAt + durationMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L),
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = sampleRate,
            channelCount = channelCount,
            transcriptSegmentIds = emptyList(),
            errorMessage = null,
            createdAt = startedAt,
            updatedAt = now
        )
        saveRecordingSegments(sessionId, detail.recordingSegments + segment)
        savePodcastSession(
            detail.session.copy(
                sourceType = AudioSourceType.LOCAL_MEDIA,
                activeSegmentId = null,
                lastCompletedSegmentId = segment.id,
                recordingSegmentCount = detail.recordingSegments.size + 1,
                status = PodcastSessionStatus.PROCESSING,
                updatedAt = now,
                errorMessage = null
            )
        )
        return AppResult.Success(segment)
    }

    override suspend fun renameSession(sessionId: String, title: String): AppResult<PodcastSession> {
        val session = getPodcastSession(sessionId) ?: return missingSession()
        val updated = session.copy(
            title = title.takeIf { it.isNotBlank() } ?: session.title,
            updatedAt = System.currentTimeMillis()
        )
        savePodcastSession(updated)
        return AppResult.Success(updated)
    }

    override suspend fun appendSegment(
        sessionId: String,
        sourceType: AudioSourceType,
        startedAt: Long
    ): AppResult<RecordingSegment> {
        val detail = getPodcastSessionDetail(sessionId) ?: return missingSession()
        val now = System.currentTimeMillis()
        val segment = RecordingSegment(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            index = detail.recordingSegments.size + 1,
            sourceType = sourceType,
            status = RecordingSegmentStatus.RECORDING,
            startedAt = startedAt,
            endedAt = null,
            durationMs = 0L,
            pcmFilePath = null,
            audioFilePath = null,
            sampleRate = null,
            channelCount = null,
            transcriptSegmentIds = emptyList(),
            errorMessage = null,
            createdAt = startedAt,
            updatedAt = startedAt
        )
        saveRecordingSegments(sessionId, detail.recordingSegments + segment)
        savePodcastSession(
            detail.session.copy(
                sourceType = sourceType,
                activeSegmentId = segment.id,
                recordingSegmentCount = detail.recordingSegments.size + 1,
                updatedAt = now,
                errorMessage = null
            )
        )
        return AppResult.Success(segment)
    }

    override suspend fun updateSegment(segment: RecordingSegment): AppResult<RecordingSegment> {
        val detail = getPodcastSessionDetail(segment.sessionId) ?: return missingSession()
        if (detail.recordingSegments.none { it.id == segment.id }) return missingSession()
        val segments = detail.recordingSegments.map {
            if (it.id == segment.id) segment else it
        }
        val lastCompletedSegmentId = segments.lastOrNull {
            it.status == RecordingSegmentStatus.COMPLETED ||
                it.status == RecordingSegmentStatus.PAUSED
        }?.id ?: detail.session.lastCompletedSegmentId
        saveRecordingSegments(segment.sessionId, segments)
        savePodcastSession(
            detail.session.copy(
                recordingSegmentCount = segments.size,
                lastCompletedSegmentId = lastCompletedSegmentId,
                updatedAt = segment.updatedAt
            )
        )
        return AppResult.Success(segment)
    }

    override suspend fun updateStatus(
        sessionId: String,
        status: PodcastSessionStatus,
        errorMessage: String?
    ): AppResult<PodcastSession> {
        val detail = getPodcastSessionDetail(sessionId) ?: return missingSession()
        val activeSegmentId = when (status) {
            PodcastSessionStatus.RECORDING -> detail.session.activeSegmentId
                ?: detail.recordingSegments.lastOrNull { it.status == RecordingSegmentStatus.RECORDING }?.id
            PodcastSessionStatus.DRAFT,
            PodcastSessionStatus.PROCESSING,
            PodcastSessionStatus.SUMMARIZING,
            PodcastSessionStatus.SUMMARIZED -> detail.session.activeSegmentId
            PodcastSessionStatus.PAUSED,
            PodcastSessionStatus.READY_FOR_SUMMARY,
            PodcastSessionStatus.ERROR -> null
        }
        val lastCompletedSegmentId = if (status == PodcastSessionStatus.PAUSED) {
            detail.recordingSegments.lastOrNull {
                it.status == RecordingSegmentStatus.COMPLETED ||
                    it.status == RecordingSegmentStatus.PAUSED
            }?.id ?: detail.session.lastCompletedSegmentId
        } else {
            detail.session.lastCompletedSegmentId
        }
        val updated = detail.session.copy(
            status = status,
            activeSegmentId = activeSegmentId,
            lastCompletedSegmentId = lastCompletedSegmentId,
            updatedAt = System.currentTimeMillis(),
            errorMessage = errorMessage,
            importedContent = detail.session.importedContent?.let {
                if (status == PodcastSessionStatus.ERROR) {
                    it.copy(
                        status = ImportedContentStatus.FAILED,
                        errorMessage = errorMessage,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    it
                }
            }
        )
        savePodcastSession(updated)
        return AppResult.Success(updated)
    }

    override suspend fun updateSummaryLifecycle(
        sessionId: String,
        status: SummaryStatus,
        modelName: String,
        summaryText: String?,
        generatedAt: Long?,
        errorMessage: String?
    ): AppResult<PodcastSession> {
        val detail = getPodcastSessionDetail(sessionId) ?: return missingSession()
        val now = System.currentTimeMillis()
        val existing = detail.session.summary
        val boundedError = errorMessage
            ?.take(MAX_SUMMARY_ERROR_MESSAGE_CHARS)
            ?.takeIf { it.isNotBlank() }
        val summary = SessionSummary(
            text = when (status) {
                SummaryStatus.SUMMARIZED -> summaryText.orEmpty()
                SummaryStatus.NOT_READY,
                SummaryStatus.READY,
                SummaryStatus.SUMMARIZING,
                SummaryStatus.FAILED -> existing?.text.orEmpty()
            },
            status = status,
            modelName = modelName.takeIf { it.isNotBlank() }
                ?: existing?.modelName
                ?: detail.session.summaryModelName,
            generatedAt = when (status) {
                SummaryStatus.SUMMARIZED -> generatedAt ?: now
                SummaryStatus.NOT_READY,
                SummaryStatus.READY,
                SummaryStatus.SUMMARIZING,
                SummaryStatus.FAILED -> existing?.generatedAt
            },
            updatedAt = now,
            errorMessage = if (status == SummaryStatus.FAILED) boundedError else null
        )
        val sessionStatus = when (status) {
            SummaryStatus.NOT_READY -> detail.session.status
            SummaryStatus.READY -> PodcastSessionStatus.READY_FOR_SUMMARY
            SummaryStatus.SUMMARIZING -> PodcastSessionStatus.SUMMARIZING
            SummaryStatus.SUMMARIZED -> PodcastSessionStatus.SUMMARIZED
            SummaryStatus.FAILED -> PodcastSessionStatus.READY_FOR_SUMMARY
        }
        val updated = detail.session.copy(
            status = sessionStatus,
            activeSegmentId = if (sessionStatus == PodcastSessionStatus.RECORDING) {
                detail.session.activeSegmentId
            } else {
                null
            },
            summary = summary,
            summaryModelName = summary.modelName,
            updatedAt = now,
            errorMessage = if (status == SummaryStatus.FAILED) boundedError else null
        )
        savePodcastSession(updated)
        return AppResult.Success(updated)
    }

    override fun observeSessions(): Flow<List<PodcastSession>> {
        return context.recordsDataStore.data.map { prefs ->
            val order = prefs[Keys.SessionOrder].orEmpty().lines().filter { it.isNotBlank() }
            order.mapNotNull { id ->
                prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(id))]
                    ?.let(::decodePodcastSession)
            }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeSessionDetail(sessionId: String): Flow<PodcastSessionDetail?> {
        return context.recordsDataStore.data.map { prefs ->
            readPodcastSessionDetail(prefs, sessionId)
        }
    }

    private suspend fun rebuildSessionTranscript(sessionId: String) {
        val session = getSession(sessionId) ?: return
        val segments = getSegments(sessionId)
        val transcript = segments.joinToString(separator = "\n\n") { it.toTranscriptText() }
        val podcastSession = getPodcastSession(sessionId)
        if (podcastSession != null) {
            savePodcastSession(
                podcastSession.copy(
                    transcript = transcript,
                    transcriptSegmentCount = segments.size,
                    detectedSpeakerCount = segments.map { it.speakerId }.distinct().size,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            saveSession(
                session.copy(
                    transcript = transcript,
                    segmentCount = segments.size,
                    detectedSpeakerCount = segments.map { it.speakerId }.distinct().size,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun markInterruptedPodcastAndLegacySessions() {
        val now = System.currentTimeMillis()
        context.recordsDataStore.edit { prefs ->
            prefs[Keys.SessionOrder].orEmpty()
                .lines()
                .filter { it.isNotBlank() }
                .forEach { id ->
                    val key = stringPreferencesKey(RecordingPersistenceContract.sessionKey(id))
                    val raw = prefs[key] ?: return@forEach
                    if (isPodcastSessionJson(raw)) {
                        val session = decodePodcastSession(raw)
                        if (session.status == PodcastSessionStatus.RECORDING || session.activeSegmentId != null) {
                            val segmentsKey = stringPreferencesKey(
                                RecordingPersistenceContract.recordingSegmentsKey(id)
                            )
                            val segments = prefs[segmentsKey]
                                ?.let { PodcastSessionJsonCodec.decodeRecordingSegments(JSONArray(it)) }
                                .orEmpty()
                            val recovered = PodcastSessionMigration.recoverInterrupted(session, segments)
                            prefs[key] = PodcastSessionJsonCodec.encodeSession(
                                recovered.session.copy(updatedAt = now)
                            ).toString()
                            prefs[segmentsKey] = PodcastSessionJsonCodec.encodeRecordingSegments(
                                recovered.recordingSegments.map { segment ->
                                    if (segment.status == RecordingSegmentStatus.INTERRUPTED) {
                                        segment.copy(updatedAt = now)
                                    } else {
                                        segment
                                    }
                                }
                            ).toString()
                        }
                    } else {
                        val session = decodeLegacySession(raw)
                        if (session.status.isInterruptedOnStartup()) {
                            prefs[key] = encodeSession(
                                session.copy(
                                    status = RecordingStatus.ERROR,
                                    updatedAt = now,
                                    errorMessage = "Previous recording was interrupted. Please start again."
                                )
                            )
                        }
                    }
                }
        }
    }

    private suspend fun getPodcastSession(sessionId: String): PodcastSession? {
        val prefs = context.recordsDataStore.data.first()
        return prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(sessionId))]
            ?.let(::decodePodcastSession)
    }

    private suspend fun getPodcastSessionDetail(sessionId: String): PodcastSessionDetail? {
        val prefs = context.recordsDataStore.data.first()
        return readPodcastSessionDetail(prefs, sessionId)
    }

    private fun readPodcastSessionDetail(
        prefs: Preferences,
        sessionId: String
    ): PodcastSessionDetail? {
        val session = prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(sessionId))]
            ?.let(::decodePodcastSession)
            ?: return null
        val recordingSegments = prefs[stringPreferencesKey(RecordingPersistenceContract.recordingSegmentsKey(sessionId))]
            ?.let { PodcastSessionJsonCodec.decodeRecordingSegments(JSONArray(it)) }
            .orEmpty()
        val transcriptSegments = prefs[stringPreferencesKey(RecordingPersistenceContract.segmentsKey(sessionId))]
            ?.let { raw ->
                val array = JSONArray(raw)
                List(array.length()) { index -> decodeSegment(array.getJSONObject(index)) }
                    .sortedBy { it.startMs }
            }
            .orEmpty()
        val speakerProfiles = prefs[stringPreferencesKey(RecordingPersistenceContract.speakersKey(sessionId))]
            ?.let { raw ->
                val array = JSONArray(raw)
                List(array.length()) { index -> decodeSpeaker(array.getJSONObject(index)) }
            }
            .orEmpty()
        return PodcastSessionDetail(
            session = session,
            recordingSegments = recordingSegments,
            transcriptSegments = transcriptSegments,
            speakerProfiles = speakerProfiles
        )
    }

    private suspend fun savePodcastSession(session: PodcastSession) {
        context.recordsDataStore.edit { prefs ->
            val existing = prefs[Keys.SessionOrder].orEmpty().lines().filter { it.isNotBlank() }
            val order = if (session.id in existing) existing else listOf(session.id) + existing
            prefs[Keys.SessionOrder] = order.joinToString(separator = "\n")
            prefs[stringPreferencesKey(RecordingPersistenceContract.SCHEMA_VERSION_KEY)] =
                RecordingPersistenceContract.CURRENT_SCHEMA_VERSION.toString()
            prefs[stringPreferencesKey(RecordingPersistenceContract.sessionKey(session.id))] =
                PodcastSessionJsonCodec.encodeSession(session).toString()
        }
    }

    private suspend fun saveRecordingSegments(sessionId: String, segments: List<RecordingSegment>) {
        context.recordsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(RecordingPersistenceContract.recordingSegmentsKey(sessionId))] =
                PodcastSessionJsonCodec.encodeRecordingSegments(segments).toString()
        }
    }

    private suspend fun attachTranscriptToRecordingSegment(segment: TranscriptSegmentEntity) {
        val detail = getPodcastSessionDetail(segment.sessionId) ?: return
        val recordingSegments = detail.recordingSegments.map { recordingSegment ->
            if (recordingSegment.id == segment.recordingSegmentId) {
                recordingSegment.copy(
                    transcriptSegmentIds = (recordingSegment.transcriptSegmentIds + segment.id).distinct(),
                    updatedAt = segment.createdAt
                )
            } else {
                recordingSegment
            }
        }
        saveRecordingSegments(segment.sessionId, recordingSegments)
        savePodcastSession(
            detail.session.copy(
                transcriptSegmentCount = detail.transcriptSegments.size + 1,
                updatedAt = segment.createdAt
            )
        )
    }

    private fun <T> missingSession(): AppResult<T> {
        return AppResult.Failure(AppError.Unknown("session missing"))
    }

    private object Keys {
        val SessionOrder = stringPreferencesKey(RecordingPersistenceContract.SESSION_ORDER_KEY)
    }

    private companion object {
        const val MAX_SUMMARY_ERROR_MESSAGE_CHARS = 160
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
    if (isPodcastSessionJson(raw)) {
        return decodePodcastSession(raw).toLegacyRecordingSession()
    }
    return decodeLegacySession(raw)
}

private fun decodeLegacySession(raw: String): RecordingSessionEntity {
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

private fun decodePodcastSession(raw: String): PodcastSession {
    return if (isPodcastSessionJson(raw)) {
        PodcastSessionJsonCodec.decodeSession(JSONObject(raw))
    } else {
        PodcastSessionMigration.fromLegacyRecordingSession(decodeLegacySession(raw))
    }
}

private fun isPodcastSessionJson(raw: String): Boolean {
    val status = JSONObject(raw).optString("status")
    return enumValues<PodcastSessionStatus>().any { it.name == status }
}

private fun PodcastSession.toLegacyRecordingSession(): RecordingSessionEntity {
    return RecordingSessionEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceType = sourceType ?: AudioSourceType.MICROPHONE,
        status = toLegacyRecordingStatus(),
        transcript = transcript,
        summary = summary?.text,
        asrModelName = asrModelName,
        vadModelName = vadModelName,
        diarizationModelName = diarizationModelName,
        summaryModelName = summaryModelName,
        summaryStyle = summaryStyle,
        summaryLanguage = summaryLanguage,
        detectedSpeakerCount = detectedSpeakerCount,
        segmentCount = transcriptSegmentCount,
        errorMessage = errorMessage
    )
}

private fun PodcastSession.toLegacyRecordingStatus(): RecordingStatus {
    return when (status) {
        PodcastSessionStatus.DRAFT -> RecordingStatus.NOT_STARTED
        PodcastSessionStatus.RECORDING -> RecordingStatus.CAPTURING_AUDIO
        PodcastSessionStatus.PROCESSING -> RecordingStatus.TRANSCRIBING
        PodcastSessionStatus.SUMMARIZING -> RecordingStatus.SUMMARIZING
        PodcastSessionStatus.SUMMARIZED -> RecordingStatus.COMPLETED
        PodcastSessionStatus.PAUSED,
        PodcastSessionStatus.READY_FOR_SUMMARY -> RecordingStatus.COMPLETED
        PodcastSessionStatus.ERROR -> RecordingStatus.ERROR
    }
}

private fun encodeSegment(segment: TranscriptSegmentEntity): JSONObject {
    return JSONObject()
        .put("id", segment.id)
        .put("sessionId", segment.sessionId)
        .put("recordingSegmentId", segment.recordingSegmentId)
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
        recordingSegmentId = json.optString("recordingSegmentId").takeIf { it.isNotBlank() && it != "null" },
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
