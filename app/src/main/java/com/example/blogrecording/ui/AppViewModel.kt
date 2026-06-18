package com.example.blogrecording.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blogrecording.asr.SenseVoiceRecognizer
import com.example.blogrecording.asr.TranscriptAssembler
import com.example.blogrecording.audio.AudioCaptureManager
import com.example.blogrecording.audio.InternalAudioCaptureManager
import com.example.blogrecording.audio.MicAudioCaptureManager
import com.example.blogrecording.audio.PcmChunk
import com.example.blogrecording.audio.PcmChunker
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.BundledModelInstaller
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.Repository
import com.example.blogrecording.data.SettingsStore
import com.example.blogrecording.data.TranscriptSegmentEntity
import com.example.blogrecording.data.isInterruptedOnStartup
import com.example.blogrecording.diarization.SpeakerDiarizationEngine
import com.example.blogrecording.diarization.SpeakerSegment
import com.example.blogrecording.diarization.SpeakerProfileManager
import com.example.blogrecording.security.ApiKeyStore
import com.example.blogrecording.service.CaptureForegroundService
import com.example.blogrecording.summary.DeepSeekSummaryClient
import com.example.blogrecording.summary.SummaryRepository
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.vad.VadSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val repository = Repository(application)
    private val apiKeyStore = ApiKeyStore(application)
    private val bundledModelInstaller = BundledModelInstaller(application)
    private val summaryRepository = SummaryRepository(DeepSeekSummaryClient())
    private val transcriptAssembler = TranscriptAssembler()
    private val speakerProfileManager = SpeakerProfileManager()

    private val mutableState = MutableStateFlow(AppUiState(hasApiKey = apiKeyStore.hasApiKey()))
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    private var summaryJob: Job? = null
    private var captureJob: Job? = null
    private var activeCaptureManager: AudioCaptureManager? = null
    private val captureDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val recognitionDispatcher = Dispatchers.Default.limitedParallelism(1)

    init {
        viewModelScope.launch {
            repository.markInterruptedSessions()
            val modelPaths = bundledModelInstaller.installIfBundled()
            settingsStore.updateModelPaths(modelPaths)
            combine(settingsStore.settings, repository.sessions) { settings, sessions ->
                settings to sessions
            }.collect { (settings, sessions) ->
                val selectedId = mutableState.value.selectedSessionId
                val selected = selectedId?.let { id -> sessions.firstOrNull { it.id == id } }
                mutableState.value = mutableState.value.copy(
                    settings = settings,
                    sessions = sessions,
                    currentSession = selected ?: mutableState.value.currentSession?.takeUnless {
                        it.status.isInterruptedOnStartup()
                    },
                    hasApiKey = apiKeyStore.hasApiKey(),
                    modelStatus = bundledModelInstaller.status()
                )
            }
        }
    }

    fun navigate(screen: AppScreen) {
        mutableState.value = mutableState.value.copy(currentScreen = screen, error = null)
    }

    fun openDetail(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            val segments = repository.getSegments(sessionId)
            mutableState.value = mutableState.value.copy(
                currentScreen = AppScreen.DETAIL,
                selectedSessionId = sessionId,
                currentSession = session,
                currentSegments = segments,
                error = null
            )
        }
    }

    fun acceptPrivacy() {
        viewModelScope.launch {
            settingsStore.acceptPrivacyNotice()
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            val paths = bundledModelInstaller.paths
            settingsStore.updateSettings(
                settings.copy(
                    sherpaModelRootPath = paths.sherpaModelRootPath,
                    senseVoiceModelPath = paths.senseVoiceModelPath,
                    vadModelPath = paths.vadModelPath,
                    diarizationModelPath = paths.diarizationModelPath
                )
            )
            mutableState.value = mutableState.value.copy(modelStatus = bundledModelInstaller.status())
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            when (val result = apiKeyStore.saveApiKey(apiKey)) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(hasApiKey = true, error = null)
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(error = result.error)
            }
        }
    }

    fun deleteApiKey() {
        apiKeyStore.deleteApiKey()
        mutableState.value = mutableState.value.copy(hasApiKey = false)
    }

    fun onRecordAudioPermissionDenied() {
        val denied = RecordingLifecyclePolicy.deniedPermissionState(AppError.RecordAudioPermissionDenied)
        mutableState.value = mutableState.value.copy(
            recordingStatus = denied.recordingStatus,
            error = denied.error
        )
    }

    fun onNotificationPermissionDenied() {
        val denied = RecordingLifecyclePolicy.deniedPermissionState(AppError.NotificationPermissionDenied)
        mutableState.value = mutableState.value.copy(
            recordingStatus = denied.recordingStatus,
            error = denied.error
        )
    }

    fun onMediaProjectionDenied() {
        viewModelScope.launch {
            val session = repository.createSession(AudioSourceType.INTERNAL_AUDIO, mutableState.value.settings)
            val failed = session.copy(
                status = RecordingStatus.ERROR,
                errorMessage = "需要完成系统内录授权"
            )
            repository.saveSession(failed)
            mutableState.value = mutableState.value.copy(
                currentSession = failed,
                selectedSessionId = failed.id,
                recordingStatus = RecordingStatus.ERROR,
                audioSourceType = AudioSourceType.INTERNAL_AUDIO,
                vadLabel = "需要 MediaProjection 授权",
                error = AppError.MediaProjectionDenied
            )
        }
    }

    fun startMicrophoneRecording() {
        viewModelScope.launch {
            val manager = MicAudioCaptureManager(getApplication())
            startRecording(AudioSourceType.MICROPHONE, manager, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        }
    }

    fun startInternalRecording(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val serviceError = startForegroundServiceSafely(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            if (serviceError != null) {
                val session = repository.createSession(AudioSourceType.INTERNAL_AUDIO, mutableState.value.settings)
                val failed = session.copy(status = RecordingStatus.ERROR, errorMessage = serviceError.toString())
                repository.saveSession(failed)
                mutableState.value = mutableState.value.copy(
                    currentSession = failed,
                    selectedSessionId = failed.id,
                    recordingStatus = RecordingStatus.ERROR,
                    audioSourceType = AudioSourceType.INTERNAL_AUDIO,
                    error = serviceError
                )
                return@launch
            }
            val projection = try {
                context.getSystemService(MediaProjectionManager::class.java).getMediaProjection(resultCode, data)
            } catch (error: SecurityException) {
                val session = repository.createSession(AudioSourceType.INTERNAL_AUDIO, mutableState.value.settings)
                val failed = session.copy(status = RecordingStatus.ERROR, errorMessage = error.message)
                repository.saveSession(failed)
                mutableState.value = mutableState.value.copy(
                    currentSession = failed,
                    selectedSessionId = failed.id,
                    recordingStatus = RecordingStatus.ERROR,
                    audioSourceType = AudioSourceType.INTERNAL_AUDIO,
                    error = AppError.MediaProjectionDenied
                )
                context.stopService(Intent(context, CaptureForegroundService::class.java))
                return@launch
            }
            startRecording(
                sourceType = AudioSourceType.INTERNAL_AUDIO,
                captureManager = InternalAudioCaptureManager(projection),
                foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
                foregroundServiceAlreadyStarted = true
            )
        }
    }

    fun stopRecording() {
        val job = captureJob
        val manager = activeCaptureManager
        if (job == null && manager == null) return
        updateStopRequestedState()
        manager?.stop()
        activeCaptureManager = null
        captureJob = null
        val session = mutableState.value.currentSession
        viewModelScope.launch {
            try {
                job?.join()
                val latest = session?.id?.let { repository.getSession(it) } ?: session
                if (latest != null && latest.status != RecordingStatus.ERROR) {
                    val completed = latest.copy(status = RecordingStatus.COMPLETED, updatedAt = System.currentTimeMillis())
                    repository.saveSession(completed)
                    updateRecordingState(
                        currentSession = completed,
                        recordingStatus = RecordingStatus.COMPLETED,
                        vadLabel = "已停止，可生成总结",
                        error = null
                    )
                }
            } finally {
                stopForegroundService()
            }
        }
    }

    fun generateSummaryForCurrent() {
        val session = mutableState.value.currentSession ?: return
        generateSummary(session)
    }

    fun generateSummary(session: RecordingSessionEntity) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isGeneratingSummary = true, recordingStatus = RecordingStatus.SUMMARIZING, error = null)
            val apiKey = when (val result = apiKeyStore.readApiKey()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> {
                    mutableState.value = mutableState.value.copy(isGeneratingSummary = false, error = result.error)
                    return@launch
                }
            }
            if (session.transcript.isBlank()) {
                mutableState.value = mutableState.value.copy(isGeneratingSummary = false, error = AppError.Unknown("转写为空"))
                return@launch
            }
            when (val summary = summaryRepository.generateSummary(apiKey, session.transcript, mutableState.value.settings)) {
                is AppResult.Success -> {
                    repository.updateSummary(session.id, summary.value)
                    val updated = repository.getSession(session.id)
                    mutableState.value = mutableState.value.copy(
                        isGeneratingSummary = false,
                        recordingStatus = RecordingStatus.COMPLETED,
                        currentSession = updated,
                        error = null
                    )
                }
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(
                    isGeneratingSummary = false,
                    recordingStatus = RecordingStatus.ERROR,
                    error = summary.error
                )
            }
        }
    }

    fun deleteCurrentSession() {
        val sessionId = mutableState.value.currentSession?.id ?: return
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            mutableState.value = mutableState.value.copy(
                currentScreen = AppScreen.HISTORY,
                currentSession = null,
                currentSegments = emptyList(),
                selectedSessionId = null
            )
        }
    }

    private suspend fun startRecording(
        sourceType: AudioSourceType,
        captureManager: AudioCaptureManager,
        foregroundServiceType: Int,
        foregroundServiceAlreadyStarted: Boolean = false
    ) {
        captureJob?.cancel()
        activeCaptureManager?.stop()
        captureJob = null
        activeCaptureManager = null

        val settings = mutableState.value.settings
        val gateError = modelGateError(settings)
        val session = repository.createSession(sourceType, settings)
        if (gateError != null) {
            val failed = session.copy(status = RecordingStatus.ERROR, errorMessage = gateError.toString())
            repository.saveSession(failed)
            mutableState.value = mutableState.value.copy(
                currentSession = failed,
                selectedSessionId = failed.id,
                recordingStatus = RecordingStatus.ERROR,
                audioSourceType = sourceType,
                error = gateError
            )
            return
        }

        val serviceError = if (foregroundServiceAlreadyStarted) null else startForegroundServiceSafely(foregroundServiceType)
        if (serviceError != null) {
            val failed = session.copy(status = RecordingStatus.ERROR, errorMessage = serviceError.toString())
            repository.saveSession(failed)
            mutableState.value = mutableState.value.copy(
                currentSession = failed,
                selectedSessionId = failed.id,
                recordingStatus = RecordingStatus.ERROR,
                audioSourceType = sourceType,
                error = serviceError
            )
            return
        }

        activeCaptureManager = captureManager
        val capturing = session.copy(status = RecordingStatus.CAPTURING_AUDIO)
        repository.saveSession(capturing)
        mutableState.value = mutableState.value.copy(
            currentSession = capturing,
            selectedSessionId = capturing.id,
            currentSegments = emptyList(),
            recordingStatus = RecordingStatus.CAPTURING_AUDIO,
            audioSourceType = sourceType,
            vadLabel = "录音中，按 ${settings.transcriptionChunkDurationMs / 1000} 秒分片异步转写",
            error = null
        )
        captureJob = viewModelScope.launch(captureDispatcher) {
            runCatching {
                runRecordingPipeline(capturing, settings, captureManager)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                stopForegroundService()
                handleCaptureFailure(
                    sessionId = capturing.id,
                    error = AppError.RecordingPipelineFailed(error.message ?: error.javaClass.simpleName)
                )
            }.onSuccess {
                activeCaptureManager = null
            }
        }
    }

    private suspend fun runRecordingPipeline(
        session: RecordingSessionEntity,
        settings: AppSettings,
        captureManager: AudioCaptureManager
    ) = coroutineScope {
        val chunkDurationMs = settings.transcriptionChunkDurationMs.coerceIn(10_000L, 600_000L)
        val chunker = PcmChunker(chunkDurationMs)
        val chunks = Channel<PcmChunk>(capacity = 2)
        val recognizer = SenseVoiceRecognizer(settings.senseVoiceModelPath)
        val diarization = SpeakerDiarizationEngine(
            modelPath = settings.diarizationModelPath,
            enabled = settings.enableSpeakerDiarization,
            maxSpeakerCount = settings.maxSpeakerCount
        )

        val processor = launch(recognitionDispatcher) {
            for (chunk in chunks) {
                processChunk(
                    session = session,
                    chunk = chunk,
                    recognizer = recognizer,
                    diarization = diarization
                )
            }
        }

        try {
            captureManager.start().collect { captureResult ->
                when (captureResult) {
                    is AppResult.Failure -> handleCaptureFailure(session.id, captureResult.error)
                    is AppResult.Success -> {
                        val readyChunks = chunker.offer(captureResult.value)
                        readyChunks.forEach { chunk -> sendChunk(chunks, chunk) }
                        updateRecordingState(
                            recordingStatus = RecordingStatus.CAPTURING_AUDIO,
                            vadLabel = "录音中，已缓存 ${chunker.currentDurationMs / 1000} 秒，按 ${chunkDurationMs / 1000} 秒分片转写"
                        )
                    }
                }
            }
        } finally {
            val partial = chunker.flush()
            if (partial != null) {
                updateRecordingState(
                    recordingStatus = RecordingStatus.TRANSCRIBING,
                    vadLabel = "正在完成最后 ${((partial.endMs - partial.startMs) / 1000).coerceAtLeast(1)} 秒转写"
                )
                sendChunk(chunks, partial)
            }
            chunks.close()
            processor.join()
        }
    }

    private suspend fun sendChunk(channel: Channel<PcmChunk>, chunk: PcmChunk) {
        try {
            channel.send(chunk)
        } catch (_: ClosedSendChannelException) {
            // Recording is already stopping; the pipeline will finish with queued chunks.
        }
    }

    private suspend fun processChunk(
        session: RecordingSessionEntity,
        chunk: PcmChunk,
        recognizer: SenseVoiceRecognizer,
        diarization: SpeakerDiarizationEngine
    ) {
        val recognizerSegments = chunk.toVadSegments(LocalProcessingPolicy.MAX_RECOGNIZER_SEGMENT_MS)
        updateRecordingState(
            recordingStatus = RecordingStatus.TRANSCRIBING,
            vadLabel = "正在转写第 ${chunk.sequence} 批（${chunk.startMs / 1000}-${chunk.endMs / 1000} 秒）"
        )

        var wroteAnySegment = false
        recognizerSegments.forEachIndexed { index, segment ->
            updateRecordingState(
                recordingStatus = RecordingStatus.TRANSCRIBING,
                vadLabel = "正在转写第 ${chunk.sequence} 批 ${index + 1}/${recognizerSegments.size}"
            )

            val asr = recognizer.recognize(segment)
            if (asr is AppResult.Failure) {
                handleCaptureFailure(session.id, asr.error)
                return
            }

            val asrResult = (asr as AppResult.Success).value
            if (asrResult.text.isBlank()) return@forEachIndexed

            val speaker = labelSpeaker(diarization, segment)
            if (speaker is AppResult.Failure) {
                handleCaptureFailure(session.id, speaker.error)
                return
            }

            val transcriptSegment = transcriptAssembler.assemble(
                sessionId = session.id,
                vadStartMs = segment.startMs,
                vadEndMs = segment.endMs,
                asrResult = asrResult,
                speaker = (speaker as AppResult.Success).value
            )
            repository.appendSegment(transcriptSegment)
            wroteAnySegment = true
        }

        if (!wroteAnySegment) {
            updateRecordingState(
                recordingStatus = RecordingStatus.CAPTURING_AUDIO,
                vadLabel = "第 ${chunk.sequence} 批为空白，继续录音"
            )
            return
        }

        val segments = repository.getSegments(session.id)
        repository.saveSpeakerProfiles(session.id, speakerProfileManager.buildProfiles(session.id, segments))
        val updated = repository.getSession(session.id)
        updateRecordingState(
            currentSession = updated,
            currentSegments = segments,
            recordingStatus = RecordingStatus.CAPTURING_AUDIO,
            vadLabel = "已写入 ${segments.size} 段，继续录音并分片缓存",
            error = null
        )
    }

    private suspend fun labelSpeaker(
        diarization: SpeakerDiarizationEngine,
        segment: VadSegment
    ): AppResult<SpeakerSegment> {
        LocalProcessingPolicy.speakerFallbackForLongSegment(segment)?.let { fallback ->
            return AppResult.Success(fallback)
        }
        return diarization.label(segment)
    }

    private suspend fun handleCaptureFailure(sessionId: String, error: AppError) {
        val failureState = RecordingLifecyclePolicy.captureFailureState(error)
        if (!failureState.persistFailure) {
            updateRecordingState(
                recordingStatus = failureState.recordingStatus,
                vadLabel = failureState.vadLabel ?: mutableState.value.vadLabel,
                error = failureState.error
            )
            return
        }
        val session = repository.getSession(sessionId)
        if (session != null) {
            repository.saveSession(session.copy(status = RecordingStatus.ERROR, errorMessage = error.toString()))
        }
        updateRecordingState(
            recordingStatus = failureState.recordingStatus,
            error = failureState.error
        )
    }

    private suspend fun updateRecordingState(
        currentSession: RecordingSessionEntity? = mutableState.value.currentSession,
        currentSegments: List<TranscriptSegmentEntity> = mutableState.value.currentSegments,
        recordingStatus: RecordingStatus = mutableState.value.recordingStatus,
        vadLabel: String = mutableState.value.vadLabel,
        error: AppError? = mutableState.value.error
    ) {
        withContext(Dispatchers.Main.immediate) {
            mutableState.value = mutableState.value.copy(
                currentSession = currentSession,
                currentSegments = currentSegments,
                recordingStatus = recordingStatus,
                vadLabel = vadLabel,
                error = error
            )
        }
    }

    private suspend fun startForegroundServiceSafely(foregroundServiceType: Int): AppError? {
        val context = getApplication<Application>()
        return CaptureForegroundService.startAndWait(context, foregroundServiceType)
    }

    private fun modelGateError(settings: AppSettings = mutableState.value.settings): AppError? {
        return RecordingLifecyclePolicy.modelGateError(settings, mutableState.value.modelStatus)
    }

    private fun updateStopRequestedState() {
        mutableState.value = mutableState.value.copy(
            recordingStatus = RecordingStatus.TRANSCRIBING,
            vadLabel = "正在停止录音并转写最后一段",
            error = null
        )
    }

    private fun stopForegroundService() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, CaptureForegroundService::class.java))
    }

    override fun onCleared() {
        activeCaptureManager?.stop()
        super.onCleared()
    }

}
