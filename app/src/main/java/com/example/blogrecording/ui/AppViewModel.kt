package com.example.blogrecording.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.media.projection.MediaProjection
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blogrecording.asr.SenseVoiceRecognizer
import com.example.blogrecording.asr.TranscriptAssembler
import com.example.blogrecording.audio.AudioCaptureManager
import com.example.blogrecording.audio.InternalAudioCaptureManager
import com.example.blogrecording.audio.InternalAudioCapturePolicy
import com.example.blogrecording.audio.MicAudioCaptureManager
import com.example.blogrecording.audio.PcmChunk
import com.example.blogrecording.audio.PcmChunker
import com.example.blogrecording.audio.PcmAudioStream
import com.example.blogrecording.audio.SilenceDetector
import com.example.blogrecording.common.AppError
import com.example.blogrecording.common.AppResult
import com.example.blogrecording.common.toUserMessage
import com.example.blogrecording.data.AppSettings
import com.example.blogrecording.data.AudioSourceType
import com.example.blogrecording.data.BundledModelInstaller
import com.example.blogrecording.data.ImportedContentKind
import com.example.blogrecording.data.ImportedContentMetadata
import com.example.blogrecording.data.ImportedContentStatus
import com.example.blogrecording.data.PodcastSessionStatus
import com.example.blogrecording.data.RecordingSegmentStatus
import com.example.blogrecording.data.RecordingSessionEntity
import com.example.blogrecording.data.RecordingStatus
import com.example.blogrecording.data.Repository
import com.example.blogrecording.data.SettingsStore
import com.example.blogrecording.data.TranscriptSegmentEntity
import com.example.blogrecording.data.isInterruptedOnStartup
import com.example.blogrecording.diarization.SpeakerDiarizationEngine
import com.example.blogrecording.diarization.SpeakerSegment
import com.example.blogrecording.diarization.SpeakerProfileManager
import com.example.blogrecording.importing.DecodedLocalMedia
import com.example.blogrecording.importing.LocalMediaImporter
import com.example.blogrecording.importing.UrlImportSourceKind
import com.example.blogrecording.importing.UrlMediaImportPolicy
import com.example.blogrecording.importing.UrlMediaImporter
import com.example.blogrecording.recording.ActiveRecordingSegment
import com.example.blogrecording.recording.RecordingController
import com.example.blogrecording.recording.SegmentRecorder
import com.example.blogrecording.recording.SegmentStartRequest
import com.example.blogrecording.recording.SegmentStopResult
import com.example.blogrecording.security.ApiKeyStore
import com.example.blogrecording.service.CaptureNotificationState
import com.example.blogrecording.service.CaptureForegroundService
import com.example.blogrecording.summary.DeepSeekSummaryClient
import com.example.blogrecording.summary.SessionHighlightGenerator
import com.example.blogrecording.summary.SessionSummaryUseCase
import com.example.blogrecording.summary.SummaryRepository
import com.example.blogrecording.ui.state.AppScreen
import com.example.blogrecording.ui.state.AppUiState
import com.example.blogrecording.ui.state.ProcessingStageUiState
import com.example.blogrecording.ui.state.RenameDialogUiState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val repository = Repository(application)
    private val localMediaImporter = LocalMediaImporter(application)
    private val urlMediaImporter = UrlMediaImporter(application)
    private val apiKeyStore = ApiKeyStore(application)
    private val bundledModelInstaller = BundledModelInstaller(application)
    private val summaryRepository = SummaryRepository(DeepSeekSummaryClient())
    private val sessionSummaryUseCase = SessionSummaryUseCase(
        sessionRepository = repository,
        readApiKey = { apiKeyStore.readApiKey() },
        generateSummary = { apiKey, transcript, settings ->
            summaryRepository.generateSummary(apiKey, transcript, settings)
        }
    )
    private val transcriptAssembler = TranscriptAssembler()
    private val speakerProfileManager = SpeakerProfileManager()
    private val silenceDetector = SilenceDetector()
    private val recordingController: RecordingController by lazy {
        RecordingController(
            sessionRepository = repository,
            recorder = ViewModelSegmentRecorder()
        )
    }

    private val mutableState = MutableStateFlow(AppUiState(hasApiKey = apiKeyStore.hasApiKey()))
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    private var summaryJob: Job? = null
    private var captureJob: Job? = null
    private var activeCaptureManager: AudioCaptureManager? = null
    private var pendingInternalAudioProjection: MediaProjection? = null
    private val captureDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val recognitionDispatcher = Dispatchers.Default.limitedParallelism(1)

    init {
        viewModelScope.launch {
            repository.markInterruptedSessions()
            val modelPaths = bundledModelInstaller.installIfBundled()
            settingsStore.updateModelPaths(modelPaths)
            combine(
                settingsStore.settings,
                repository.sessions,
                repository.observeSessions()
            ) { settings, sessions, podcastSessions ->
                Triple(settings, sessions, podcastSessions)
            }.collect { (settings, sessions, podcastSessions) ->
                val selectedId = mutableState.value.selectedSessionId
                val selected = selectedId?.let { id -> sessions.firstOrNull { it.id == id } }
                val podcastDetails = podcastSessions.mapNotNull { session ->
                    repository.observeSessionDetail(session.id).first()
                }
                mutableState.value = mutableState.value.copy(
                    home = HomeUiStateMapper.map(
                        details = podcastDetails,
                        renameDialog = mutableState.value.home.renameDialog,
                        error = mutableState.value.error,
                        processingStage = mutableState.value.processingStage,
                        processingSessionId = mutableState.value.processingSessionId,
                        hasApiKey = apiKeyStore.hasApiKey()
                    ),
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
        mutableState.value = UiNavigationPolicy.navigate(mutableState.value, screen)
    }

    fun openDetail(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            val segments = repository.getSegments(sessionId)
            val detail = repository.observeSessionDetail(sessionId).first()
            mutableState.value = UiNavigationPolicy.openDetail(mutableState.value, sessionId, session, segments).copy(
                currentPodcastSummary = detail?.session?.summary,
                currentTagLabels = detail?.session?.tagGeneration?.tags
                    ?.sortedBy { it.order }
                    ?.map { it.text }
                    .orEmpty(),
                currentHighlights = detail?.session?.highlights?.items.orEmpty()
            )
        }
    }

    fun toggleHighlightFavorite(highlightId: String) {
        val sessionId = mutableState.value.selectedSessionId ?: return
        viewModelScope.launch {
            val detail = repository.observeSessionDetail(sessionId).first() ?: return@launch
            val updatedHighlights = SessionHighlightGenerator.toggleFavorite(
                highlights = detail.session.highlights,
                highlightId = highlightId,
                nowMillis = System.currentTimeMillis()
            )
            when (val result = repository.updateHighlights(sessionId, updatedHighlights)) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    currentHighlights = result.value.highlights.items,
                    error = null
                )
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(error = result.error)
            }
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
        Log.w(TAG, "media_projection_denied")
        pendingInternalAudioProjection?.stop()
        pendingInternalAudioProjection = null
        mutableState.value = mutableState.value.copy(
            recordingStatus = RecordingStatus.ERROR,
            audioSourceType = AudioSourceType.INTERNAL_AUDIO,
            vadLabel = "需要允许屏幕和音频捕获",
            processingStage = ProcessingStageUiState.mediaProjectionDenied(),
            error = AppError.MediaProjectionDenied
        )
    }

    fun prepareInternalAudioAuthorization(sessionId: String? = null) {
        Log.i(TAG, "prepare_internal_authorization sessionIdPresent=${sessionId != null}")
        mutableState.value = mutableState.value.copy(
            audioSourceType = AudioSourceType.INTERNAL_AUDIO,
            processingStage = ProcessingStageUiState.authorizingSystemAudio(),
            processingSessionId = sessionId,
            error = null
        )
    }

    fun createPodcastSession() {
        viewModelScope.launch {
            repository.createSession(title = null, sourceType = AudioSourceType.MICROPHONE)
        }
    }

    fun onLocalMediaImportCanceled() {
        mutableState.value = mutableState.value.copy(
            processingStage = ProcessingStageUiState.idle(),
            processingSessionId = null,
            error = null
        )
    }

    fun importLocalMedia(uri: Uri) {
        if (recordingController.currentState().isRecording) {
            mutableState.value = mutableState.value.copy(
                processingStage = ProcessingStageUiState.error(AppError.LocalMediaImportBlocked.toUserMessage()),
                error = AppError.LocalMediaImportBlocked
            )
            return
        }
        viewModelScope.launch(captureDispatcher) {
            runLocalMediaImport(uri)
        }
    }

    fun importUrlMedia(url: String) {
        if (recordingController.currentState().isRecording) {
            mutableState.value = mutableState.value.copy(
                processingStage = ProcessingStageUiState.error(AppError.LocalMediaImportBlocked.toUserMessage()),
                error = AppError.LocalMediaImportBlocked
            )
            return
        }
        viewModelScope.launch(captureDispatcher) {
            runUrlMediaImport(url)
        }
    }

    fun requestRenamePodcastSession(sessionId: String) {
        val card = mutableState.value.home.cards.firstOrNull { it.sessionId == sessionId } ?: return
        mutableState.value = mutableState.value.copy(
            home = mutableState.value.home.copy(
                renameDialog = RenameDialogUiState(
                    sessionId = sessionId,
                    initialTitle = card.title
                )
            )
        )
    }

    fun dismissRenamePodcastSession() {
        mutableState.value = mutableState.value.copy(
            home = mutableState.value.home.copy(renameDialog = null)
        )
    }

    fun renamePodcastSession(sessionId: String, title: String) {
        viewModelScope.launch {
            if (title.isBlank()) {
                mutableState.value = mutableState.value.copy(error = AppError.Unknown("标题不能为空"))
                return@launch
            }
            when (val result = repository.renameSession(sessionId, title.trim())) {
                is AppResult.Success -> mutableState.value = mutableState.value.copy(
                    home = mutableState.value.home.copy(renameDialog = null),
                    error = null
                )
                is AppResult.Failure -> mutableState.value = mutableState.value.copy(error = result.error)
            }
        }
    }

    fun startMicrophoneRecording(sessionId: String? = null) {
        viewModelScope.launch {
            val result = if (sessionId == null) {
                recordingController.startMicrophone(title = null)
            } else {
                recordingController.resumeMicrophone(sessionId)
            }
            handleRecordingControllerResult(result)
        }
    }

    fun pausePodcastRecording(sessionId: String) {
        viewModelScope.launch {
            handleRecordingControllerResult(recordingController.pause(sessionId))
        }
    }

    fun resumePodcastRecording(sessionId: String) {
        startMicrophoneRecording(sessionId)
    }

    fun finishPodcastSession(sessionId: String) {
        viewModelScope.launch {
            handleRecordingControllerResult(recordingController.finalize(sessionId))
        }
    }

    fun startInternalRecording(resultCode: Int, data: Intent, sessionId: String? = null) {
        viewModelScope.launch {
            Log.i(TAG, "start_internal_callback sessionIdPresent=${sessionId != null} resultCode=$resultCode")
            val context = getApplication<Application>()
            val serviceError = startForegroundServiceSafely(
                foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
                notificationState = CaptureNotificationState(
                    captureSource = CaptureNotificationState.SOURCE_SYSTEM_AUDIO,
                    recordingState = CaptureNotificationState.STATE_RECORDING,
                    stageText = "系统内录授权已通过"
                )
            )
            if (serviceError != null) {
                Log.w(TAG, "start_internal_service_failed error=${serviceError::class.simpleName}")
                mutableState.value = mutableState.value.copy(
                    recordingStatus = RecordingStatus.ERROR,
                    audioSourceType = AudioSourceType.INTERNAL_AUDIO,
                    processingStage = ProcessingStageUiState.error(serviceError.toUserMessage()),
                    error = serviceError
                )
                return@launch
            }
            val projection = try {
                context.getSystemService(MediaProjectionManager::class.java).getMediaProjection(resultCode, data)
            } catch (error: SecurityException) {
                Log.w(TAG, "start_internal_projection_security error=${error.javaClass.simpleName}")
                mutableState.value = mutableState.value.copy(
                    recordingStatus = RecordingStatus.ERROR,
                    audioSourceType = AudioSourceType.INTERNAL_AUDIO,
                    vadLabel = "需要允许屏幕和音频捕获",
                    processingStage = ProcessingStageUiState.mediaProjectionDenied(),
                    error = AppError.MediaProjectionDenied
                )
                context.stopService(Intent(context, CaptureForegroundService::class.java))
                return@launch
            }
            Log.i(TAG, "start_internal_projection_ready projectionPresent=${projection != null}")
            pendingInternalAudioProjection = projection
            val result = if (sessionId == null) {
                recordingController.startSystemAudio(title = null)
            } else {
                recordingController.resumeSystemAudio(sessionId)
            }
            if (result is AppResult.Failure) {
                Log.w(TAG, "start_internal_controller_failed error=${result.error::class.simpleName}")
                pendingInternalAudioProjection?.stop()
                pendingInternalAudioProjection = null
                stopForegroundService()
            } else {
                Log.i(TAG, "start_internal_controller_started sessionIdPresent=${sessionId != null}")
            }
            handleRecordingControllerResult(result)
        }
    }

    fun stopRecording() {
        val activePodcastSessionId = recordingController.currentState().activeSessionId
        if (activePodcastSessionId != null) {
            pausePodcastRecording(activePodcastSessionId)
            return
        }
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
                        processingStage = ProcessingStageUiState.completed("录制已完成，可生成总结"),
                        processingSessionId = completed.id,
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
        startSummaryForPodcastSession(session.id)
    }

    fun startSummaryForPodcastSession(sessionId: String) {
        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isGeneratingSummary = true,
                recordingStatus = RecordingStatus.SUMMARIZING,
                processingStage = ProcessingStageUiState.summarizing(),
                processingSessionId = sessionId,
                error = null
            )
            updateForegroundNotification(
                sourceType = mutableState.value.audioSourceType,
                recordingState = CaptureNotificationState.STATE_SUMMARIZING,
                stage = ProcessingStageUiState.summarizing()
            )
            when (val result = sessionSummaryUseCase.start(sessionId, mutableState.value.settings)) {
                is AppResult.Success -> {
                    val updated = repository.getSession(sessionId)
                    val segments = repository.getSegments(sessionId)
                    val detail = repository.observeSessionDetail(sessionId).first()
                    mutableState.value = mutableState.value.copy(
                        isGeneratingSummary = false,
                        recordingStatus = updated?.status ?: RecordingStatus.COMPLETED,
                        processingStage = ProcessingStageUiState.completed("总结已生成"),
                        processingSessionId = sessionId,
                        currentSession = updated ?: mutableState.value.currentSession,
                        currentPodcastSummary = detail?.session?.summary,
                        currentTagLabels = if (mutableState.value.selectedSessionId == sessionId) {
                            detail?.session?.tagGeneration?.tags
                                ?.sortedBy { it.order }
                                ?.map { it.text }
                                .orEmpty()
                        } else {
                            mutableState.value.currentTagLabels
                        },
                        currentHighlights = if (mutableState.value.selectedSessionId == sessionId) {
                            detail?.session?.highlights?.items.orEmpty()
                        } else {
                            mutableState.value.currentHighlights
                        },
                        currentSegments = if (mutableState.value.selectedSessionId == sessionId) {
                            segments
                        } else {
                            mutableState.value.currentSegments
                        },
                        error = null
                    )
                }
                is AppResult.Failure -> {
                    val updated = repository.getSession(sessionId)
                    val detail = repository.observeSessionDetail(sessionId).first()
                    mutableState.value = mutableState.value.copy(
                        isGeneratingSummary = false,
                        recordingStatus = updated?.status ?: mutableState.value.recordingStatus,
                        processingStage = ProcessingStageUiState.error(result.error.toUserMessage()),
                        processingSessionId = sessionId,
                        currentSession = updated ?: mutableState.value.currentSession,
                        currentPodcastSummary = detail?.session?.summary,
                        currentTagLabels = if (mutableState.value.selectedSessionId == sessionId) {
                            detail?.session?.tagGeneration?.tags
                                ?.sortedBy { it.order }
                                ?.map { it.text }
                                .orEmpty()
                        } else {
                            mutableState.value.currentTagLabels
                        },
                        currentHighlights = if (mutableState.value.selectedSessionId == sessionId) {
                            detail?.session?.highlights?.items.orEmpty()
                        } else {
                            mutableState.value.currentHighlights
                        },
                        error = result.error
                    )
                }
            }
        }
    }

    fun generateSummary(session: RecordingSessionEntity) {
        startSummaryForPodcastSession(session.id)
    }

    fun deleteCurrentSession() {
        val sessionId = mutableState.value.currentSession?.id ?: return
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            mutableState.value = UiNavigationPolicy.deleteCurrentSession(mutableState.value)
        }
    }

    private suspend fun startRecording(
        sourceType: AudioSourceType,
        captureManager: AudioCaptureManager,
        foregroundServiceType: Int,
        foregroundServiceAlreadyStarted: Boolean = false,
        existingSession: RecordingSessionEntity? = null,
        recordingSegmentId: String? = null
    ): AppResult<Unit> {
        captureJob?.cancel()
        activeCaptureManager?.stop()
        captureJob = null
        activeCaptureManager = null
        Log.i(
            TAG,
            "start_recording_requested source=$sourceType existingSession=${existingSession != null} segmentPresent=${recordingSegmentId != null}"
        )

        val settings = mutableState.value.settings
        val gateError = modelGateError(settings)
        val session = existingSession ?: repository.createSession(sourceType, settings)
        if (gateError != null) {
            Log.w(TAG, "start_recording_model_gate source=$sourceType error=${gateError::class.simpleName}")
            if (existingSession == null) {
                val failed = session.copy(status = RecordingStatus.ERROR, errorMessage = gateError.toString())
                repository.saveSession(failed)
            } else {
                repository.updateStatus(
                    sessionId = session.id,
                    status = PodcastSessionStatus.ERROR,
                    errorMessage = gateError.toString()
                )
            }
            mutableState.value = mutableState.value.copy(
                currentSession = session.copy(status = RecordingStatus.ERROR, errorMessage = gateError.toString()),
                selectedSessionId = session.id,
                recordingStatus = RecordingStatus.ERROR,
                audioSourceType = sourceType,
                processingStage = ProcessingStageUiState.error(gateError.toUserMessage()),
                processingSessionId = session.id,
                error = gateError
            )
            return AppResult.Failure(gateError)
        }

        val serviceError = if (foregroundServiceAlreadyStarted) null else startForegroundServiceSafely(foregroundServiceType)
        if (serviceError != null) {
            Log.w(TAG, "start_recording_service_failed source=$sourceType error=${serviceError::class.simpleName}")
            if (existingSession == null) {
                val failed = session.copy(status = RecordingStatus.ERROR, errorMessage = serviceError.toString())
                repository.saveSession(failed)
            } else {
                repository.updateStatus(
                    sessionId = session.id,
                    status = PodcastSessionStatus.ERROR,
                    errorMessage = serviceError.toString()
                )
            }
            mutableState.value = mutableState.value.copy(
                currentSession = session.copy(status = RecordingStatus.ERROR, errorMessage = serviceError.toString()),
                selectedSessionId = session.id,
                recordingStatus = RecordingStatus.ERROR,
                audioSourceType = sourceType,
                processingStage = ProcessingStageUiState.error(serviceError.toUserMessage()),
                processingSessionId = session.id,
                error = serviceError
            )
            return AppResult.Failure(serviceError)
        }

        activeCaptureManager = captureManager
        val capturing = session.copy(status = RecordingStatus.CAPTURING_AUDIO)
        if (existingSession == null) {
            repository.saveSession(capturing)
        }
        val stage = ProcessingStageUiState.capturing(sourceType.sourceLabel())
        mutableState.value = mutableState.value.copy(
            currentSession = capturing,
            selectedSessionId = capturing.id,
            currentSegments = emptyList(),
            recordingStatus = RecordingStatus.CAPTURING_AUDIO,
            audioSourceType = sourceType,
            vadLabel = "录音中，按 ${settings.transcriptionChunkDurationMs / 1000} 秒分片异步转写",
            processingStage = stage,
            processingSessionId = capturing.id,
            error = null
        )
        updateForegroundNotification(
            sourceType = sourceType,
            recordingState = CaptureNotificationState.STATE_RECORDING,
            stage = stage,
            session = capturing
        )
        captureJob = viewModelScope.launch(captureDispatcher) {
            runCatching {
                Log.i(TAG, "capture_job_started source=$sourceType session=${capturing.id.take(8)}")
                runRecordingPipeline(capturing, settings, captureManager, recordingSegmentId)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "capture_job_failed source=$sourceType error=${error.javaClass.simpleName}")
                stopForegroundService()
                handleCaptureFailure(
                    sessionId = capturing.id,
                    recordingSegmentId = recordingSegmentId,
                    error = AppError.RecordingPipelineFailed(error.message ?: error.javaClass.simpleName)
                )
            }.onSuccess {
                Log.i(TAG, "capture_job_completed source=$sourceType session=${capturing.id.take(8)}")
                if (activeCaptureManager === captureManager) {
                    activeCaptureManager = null
                }
            }
        }
        Log.i(TAG, "capture_job_scheduled source=$sourceType session=${capturing.id.take(8)}")
        return AppResult.Success(Unit)
    }

    private suspend fun runRecordingPipeline(
        session: RecordingSessionEntity,
        settings: AppSettings,
        captureManager: AudioCaptureManager,
        recordingSegmentId: String? = null
    ) = coroutineScope {
        val chunkDurationMs = settings.transcriptionChunkDurationMs.coerceIn(10_000L, 600_000L)
        Log.i(
            TAG,
            "pipeline_start session=${session.id.take(8)} source=${session.sourceType} chunkMs=$chunkDurationMs"
        )
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
                    diarization = diarization,
                    recordingSegmentId = recordingSegmentId
                )
            }
        }
        try {
            captureManager.start().collect { captureResult ->
                when (captureResult) {
                    is AppResult.Failure -> {
                        Log.w(TAG, "capture_result_failure error=${captureResult.error::class.simpleName}")
                        handleCaptureFailure(session.id, recordingSegmentId, captureResult.error)
                    }
                    is AppResult.Success -> {
                        val readyChunks = chunker.offer(captureResult.value)
                        if (readyChunks.isNotEmpty()) {
                            Log.i(
                                TAG,
                                "chunk_ready count=${readyChunks.size} bufferedMs=${chunker.currentDurationMs}"
                            )
                        }
                        readyChunks.forEach { chunk -> sendChunk(chunks, chunk) }
                        updateRecordingState(
                            recordingStatus = RecordingStatus.CAPTURING_AUDIO,
                            vadLabel = "录音中，已缓存 ${chunker.currentDurationMs / 1000} 秒，按 ${chunkDurationMs / 1000} 秒分片转写",
                            processingStage = ProcessingStageUiState.buffering(
                                bufferedMs = chunker.currentDurationMs,
                                targetMs = chunkDurationMs
                            ),
                            processingSessionId = session.id
                        )
                    }
                }
            }
        } finally {
            val partial = chunker.flush()
            if (partial != null) {
                Log.i(TAG, "chunk_flush_partial chunk=${partial.sequence} durationMs=${partial.endMs - partial.startMs}")
                updateRecordingState(
                    recordingStatus = RecordingStatus.TRANSCRIBING,
                    vadLabel = "正在完成最后 ${((partial.endMs - partial.startMs) / 1000).coerceAtLeast(1)} 秒转写",
                    processingStage = ProcessingStageUiState.transcribing(partial.sequence),
                    processingSessionId = session.id
                )
                sendChunk(chunks, partial)
            }
            chunks.close()
            processor.join()
            Log.i(TAG, "pipeline_closed session=${session.id.take(8)}")
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
        diarization: SpeakerDiarizationEngine,
        recordingSegmentId: String? = null
    ) {
        val chunkEnergy = silenceDetector.averageAmplitude(chunk.samples)
        val recognizerSegments = TranscriptionChunkPolicy.recognizerSegments(
            chunk = chunk,
            sourceType = session.sourceType,
            hasMeaningfulAudio = ::hasMeaningfulAudio
        )
        Log.i(
            TAG,
            "chunk_prepare chunk=${chunk.sequence} avgAmp=${chunkEnergy.toInt()} recognizerSegments=${recognizerSegments.size}"
        )
        updateRecordingState(
            recordingStatus = RecordingStatus.TRANSCRIBING,
            vadLabel = "准备调用 SenseVoice 转写第 ${chunk.sequence} 批，音频能量 ${chunkEnergy.toInt()}",
            processingStage = ProcessingStageUiState.transcribing(
                chunkSequence = chunk.sequence,
                message = "音频能量 ${chunkEnergy.toInt()}，准备调用 SenseVoice"
            ),
            processingSessionId = session.id
        )

        if (recognizerSegments.isEmpty()) {
            Log.i(TAG, "chunk_skip_silence chunk=${chunk.sequence} avgAmp=${chunkEnergy.toInt()}")
            updateRecordingState(
                recordingStatus = RecordingStatus.CAPTURING_AUDIO,
                vadLabel = "第 ${chunk.sequence} 批音频能量 ${chunkEnergy.toInt()}，低于转写阈值，继续录音",
                processingStage = ProcessingStageUiState.internalAudioUnavailable(),
                processingSessionId = session.id,
                error = null
            )
            return
        }

        var wroteAnySegment = false
        recognizerSegments.forEachIndexed { index, segment ->
            val recognizerSegment = segment.prepareForRecognition(session.sourceType)
            val segmentEnergy = silenceDetector.averageAmplitude(recognizerSegment.samples)
            Log.i(
                TAG,
                "asr_attempt chunk=${chunk.sequence} segment=${index + 1}/${recognizerSegments.size} avgAmp=${segmentEnergy.toInt()}"
            )
            updateRecordingState(
                recordingStatus = RecordingStatus.TRANSCRIBING,
                vadLabel = "SenseVoice 正在识别第 ${chunk.sequence} 批 ${index + 1}/${recognizerSegments.size}，音频能量 ${segmentEnergy.toInt()}",
                processingStage = ProcessingStageUiState.transcribing(
                    chunkSequence = chunk.sequence,
                    segmentIndex = index + 1,
                    segmentCount = recognizerSegments.size,
                    message = "音频能量 ${segmentEnergy.toInt()}，SenseVoice 正在识别"
                ),
                processingSessionId = session.id
            )

            val asr = recognizer.recognize(recognizerSegment)
            if (asr is AppResult.Failure) {
                Log.w(
                    TAG,
                    "asr_failure chunk=${chunk.sequence} segment=${index + 1}/${recognizerSegments.size} error=${asr.error::class.simpleName}"
                )
                handleCaptureFailure(session.id, recordingSegmentId, asr.error)
                return
            }

            val asrResult = (asr as AppResult.Success).value
            if (!TranscriptionResultPolicy.shouldPersist(asrResult.text)) {
                Log.i(TAG, "asr_blank chunk=${chunk.sequence} segment=${index + 1}/${recognizerSegments.size}")
                updateRecordingState(
                    recordingStatus = RecordingStatus.TRANSCRIBING,
                    vadLabel = "SenseVoice 已返回空文本，继续处理下一段",
                    processingStage = ProcessingStageUiState.transcribing(
                        chunkSequence = chunk.sequence,
                        segmentIndex = index + 1,
                        segmentCount = recognizerSegments.size
                    ),
                    processingSessionId = session.id,
                    error = null
                )
                return@forEachIndexed
            }

            val speaker = labelSpeaker(diarization, segment)
            if (speaker is AppResult.Failure) {
                handleCaptureFailure(session.id, recordingSegmentId, speaker.error)
                return
            }

            val transcriptSegment = transcriptAssembler.assemble(
                sessionId = session.id,
                recordingSegmentId = recordingSegmentId,
                vadStartMs = segment.startMs,
                vadEndMs = segment.endMs,
                asrResult = asrResult,
                speaker = (speaker as AppResult.Success).value
            )
            repository.appendSegment(transcriptSegment)
            wroteAnySegment = true
            Log.i(TAG, "asr_saved chunk=${chunk.sequence} segment=${index + 1}/${recognizerSegments.size}")
            updateRecordingState(
                recordingStatus = RecordingStatus.TRANSCRIBING,
                vadLabel = "已保存第 ${chunk.sequence} 批 ${index + 1}/${recognizerSegments.size} 的转写",
                processingStage = ProcessingStageUiState.transcribing(
                    chunkSequence = chunk.sequence,
                    segmentIndex = index + 1,
                    segmentCount = recognizerSegments.size
                ),
                processingSessionId = session.id,
                error = null
            )
        }

        if (!wroteAnySegment) {
            updateRecordingState(
                recordingStatus = RecordingStatus.CAPTURING_AUDIO,
                vadLabel = "第 ${chunk.sequence} 批为空白，继续录音",
                processingStage = ProcessingStageUiState.internalAudioUnavailable(),
                processingSessionId = session.id,
                error = null
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
            processingStage = ProcessingStageUiState.buffering(
                bufferedMs = 0L,
                targetMs = mutableState.value.settings.transcriptionChunkDurationMs
            ),
            processingSessionId = session.id,
            error = null
        )
    }

    private fun hasMeaningfulAudio(segment: VadSegment): Boolean {
        val stream = PcmAudioStream(
            samples = segment.samples,
            sampleRate = segment.sampleRate,
            channelCount = 1,
            timestampMs = segment.startMs
        )
        return !silenceDetector.isSilent(stream)
    }

    private fun VadSegment.prepareForRecognition(sourceType: AudioSourceType): VadSegment {
        if (sourceType != AudioSourceType.INTERNAL_AUDIO) return this
        val peak = samples.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
        if (peak <= 0 || peak >= INTERNAL_AUDIO_TARGET_PEAK) return this
        val gain = (INTERNAL_AUDIO_TARGET_PEAK.toFloat() / peak)
            .coerceAtMost(INTERNAL_AUDIO_MAX_GAIN)
        if (gain <= 1.0f) return this
        return copy(
            samples = ShortArray(samples.size) { index ->
                (samples[index] * gain)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
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

    private suspend fun startSegmentRecording(request: SegmentStartRequest): AppResult<Unit> {
        val session = repository.getSession(request.sessionId)
            ?: return AppResult.Failure(AppError.Unknown("记录不存在"))
        return when (request.sourceType) {
            AudioSourceType.MICROPHONE -> {
                val manager = MicAudioCaptureManager(getApplication())
                startRecording(
                    sourceType = AudioSourceType.MICROPHONE,
                    captureManager = manager,
                    foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                    existingSession = session,
                    recordingSegmentId = request.segmentId
                )
            }
            AudioSourceType.INTERNAL_AUDIO -> {
                Log.i(TAG, "start_segment_internal session=${request.sessionId.take(8)} segment=${request.segmentId.take(8)}")
                val projection = pendingInternalAudioProjection
                    ?: return AppResult.Failure(AppError.MediaProjectionDenied).also {
                        Log.w(TAG, "start_segment_internal_missing_projection session=${request.sessionId.take(8)}")
                    }
                pendingInternalAudioProjection = null
                val preferredCaptureUids = preferredInternalCaptureUids()
                Log.i(
                    TAG,
                    "preferred_internal_capture_uids=${preferredCaptureUids.joinToString(",")}"
                )
                val manager = InternalAudioCaptureManager(
                    mediaProjection = projection,
                    context = getApplication(),
                    preferredCaptureUids = preferredCaptureUids
                )
                val result = startRecording(
                    sourceType = AudioSourceType.INTERNAL_AUDIO,
                    captureManager = manager,
                    foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
                    foregroundServiceAlreadyStarted = true,
                    existingSession = session,
                    recordingSegmentId = request.segmentId
                )
                if (result is AppResult.Failure) {
                    Log.w(TAG, "start_segment_internal_failed error=${result.error::class.simpleName}")
                    manager.stop()
                }
                result
            }
            AudioSourceType.LOCAL_MEDIA -> AppResult.Failure(AppError.LocalMediaImportBlocked)
        }
    }

    private suspend fun runLocalMediaImport(uri: Uri) {
        val settings = mutableState.value.settings
        val gateError = modelGateError(settings)
        if (gateError != null) {
            withContext(Dispatchers.Main.immediate) {
                mutableState.value = mutableState.value.copy(
                    processingStage = ProcessingStageUiState.error(gateError.toUserMessage()),
                    error = gateError
                )
            }
            return
        }

        val source = when (val sourceResult = localMediaImporter.readSource(uri)) {
            is AppResult.Success -> sourceResult.value
            is AppResult.Failure -> {
                updateLocalImportError(sessionId = null, error = sourceResult.error)
                return
            }
        }
        val now = System.currentTimeMillis()
        val initialMetadata = ImportedContentMetadata(
            kind = ImportedContentKind.LOCAL_MEDIA,
            displayName = source.displayName,
            mimeType = source.mimeType,
            sizeBytes = source.sizeBytes,
            durationMs = null,
            status = ImportedContentStatus.COPYING,
            errorMessage = null,
            importedAt = now,
            updatedAt = now
        )
        val session = when (val created = repository.createImportedSession(
            title = source.displayName,
            metadata = initialMetadata
        )) {
            is AppResult.Success -> created.value
            is AppResult.Failure -> {
                updateLocalImportError(sessionId = null, error = created.error)
                return
            }
        }
        withContext(Dispatchers.Main.immediate) {
            mutableState.value = mutableState.value.copy(
                selectedSessionId = session.id,
                currentSession = repository.getSession(session.id),
                currentSegments = emptyList(),
                audioSourceType = AudioSourceType.LOCAL_MEDIA,
                recordingStatus = RecordingStatus.TRANSCRIBING,
                processingStage = ProcessingStageUiState.importing("正在读取所选音视频"),
                processingSessionId = session.id,
                error = null
            )
        }

        localMediaImporter.decode(uri).collect { result ->
            when (result) {
                is AppResult.Failure -> updateLocalImportError(session.id, result.error, initialMetadata)
                is AppResult.Success -> processDecodedLocalMedia(
                    sessionId = session.id,
                    metadata = initialMetadata,
                    decoded = result.value,
                    settings = settings
                )
            }
        }
    }

    private suspend fun runUrlMediaImport(url: String) {
        val settings = mutableState.value.settings
        val gateError = modelGateError(settings)
        if (gateError != null) {
            withContext(Dispatchers.Main.immediate) {
                mutableState.value = mutableState.value.copy(
                    processingStage = ProcessingStageUiState.error(gateError.toUserMessage()),
                    error = gateError
                )
            }
            return
        }
        val source = when (val validation = UrlMediaImportPolicy.validate(url)) {
            is AppResult.Success -> validation.value
            is AppResult.Failure -> {
                updateLocalImportError(sessionId = null, error = validation.error)
                return
            }
        }
        withContext(Dispatchers.Main.immediate) {
            mutableState.value = mutableState.value.copy(
                audioSourceType = AudioSourceType.LOCAL_MEDIA,
                recordingStatus = RecordingStatus.TRANSCRIBING,
                processingStage = ProcessingStageUiState.importing(
                    message = "正在解析 ${source.kind.toUserLabel()} 链接"
                ),
                processingSessionId = null,
                error = null
            )
        }
        val now = System.currentTimeMillis()
        val initialMetadata = ImportedContentMetadata(
            kind = ImportedContentKind.URL_MEDIA,
            displayName = source.displayName,
            mimeType = null,
            sizeBytes = null,
            durationMs = null,
            status = ImportedContentStatus.RESOLVING,
            errorMessage = null,
            importedAt = now,
            updatedAt = now,
            sourceUrl = source.sanitizedUrl,
            sourceHost = source.host
        )
        val session = when (val created = repository.createImportedSession(
            title = source.displayName,
            metadata = initialMetadata
        )) {
            is AppResult.Success -> created.value
            is AppResult.Failure -> {
                updateLocalImportError(sessionId = null, error = created.error)
                return
            }
        }
        withContext(Dispatchers.Main.immediate) {
            mutableState.value = mutableState.value.copy(
                selectedSessionId = session.id,
                currentSession = repository.getSession(session.id),
                currentSegments = emptyList(),
                processingStage = ProcessingStageUiState.importing("正在解析链接媒体"),
                processingSessionId = session.id,
                error = null
            )
        }
        val resolved = when (val resolvedResult = urlMediaImporter.resolve(source)) {
            is AppResult.Success -> resolvedResult.value
            is AppResult.Failure -> {
                updateLocalImportError(session.id, resolvedResult.error, initialMetadata)
                return
            }
        }
        val downloadingMetadata = initialMetadata.copy(
            displayName = resolved.displayName,
            mimeType = resolved.mimeType,
            sizeBytes = resolved.sizeBytes,
            status = ImportedContentStatus.DOWNLOADING,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateImportedContent(
            sessionId = session.id,
            metadata = downloadingMetadata,
            status = PodcastSessionStatus.PROCESSING
        )
        updateRecordingState(
            currentSession = repository.getSession(session.id),
            currentSegments = emptyList(),
            recordingStatus = RecordingStatus.TRANSCRIBING,
            vadLabel = "正在下载远程媒体",
            processingStage = ProcessingStageUiState.importing("正在下载远程音视频"),
            processingSessionId = session.id,
            error = null
        )
        val downloaded = when (val downloadResult = urlMediaImporter.download(source, resolved)) {
            is AppResult.Success -> downloadResult.value
            is AppResult.Failure -> {
                updateLocalImportError(session.id, downloadResult.error, downloadingMetadata)
                return
            }
        }
        val downloadedMetadata = downloadingMetadata.copy(
            displayName = downloaded.displayName,
            mimeType = downloaded.mimeType ?: downloadingMetadata.mimeType,
            sizeBytes = downloaded.sizeBytes,
            status = ImportedContentStatus.COPYING,
            updatedAt = System.currentTimeMillis()
        )
        localMediaImporter.decodeCachedFile(downloaded.file).collect { result ->
            when (result) {
                is AppResult.Failure -> updateLocalImportError(session.id, result.error, downloadedMetadata)
                is AppResult.Success -> processDecodedLocalMedia(
                    sessionId = session.id,
                    metadata = downloadedMetadata,
                    decoded = result.value,
                    settings = settings
                )
            }
        }
    }

    private suspend fun processDecodedLocalMedia(
        sessionId: String,
        metadata: ImportedContentMetadata,
        decoded: DecodedLocalMedia,
        settings: AppSettings
    ) {
        val decodingMetadata = metadata.copy(
            durationMs = decoded.durationMs,
            status = ImportedContentStatus.DECODING,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateImportedContent(
            sessionId = sessionId,
            metadata = decodingMetadata,
            status = PodcastSessionStatus.PROCESSING
        )
        updateRecordingState(
            recordingStatus = RecordingStatus.TRANSCRIBING,
            vadLabel = "正在解码并准备转写本地音视频",
            processingStage = ProcessingStageUiState.importing(
                message = "已读取音轨，准备转文字",
                progressLabel = decoded.durationMs?.let { "${(it / 1000).coerceAtLeast(1)} 秒" }
            ),
            processingSessionId = sessionId,
            error = null
        )
        val segment = when (val segmentResult = repository.appendImportedSegment(
            sessionId = sessionId,
            startedAt = System.currentTimeMillis(),
            durationMs = decoded.durationMs ?: decoded.streams.durationMs(),
            sampleRate = decoded.sampleRate,
            channelCount = decoded.channelCount
        )) {
            is AppResult.Success -> segmentResult.value
            is AppResult.Failure -> {
                updateLocalImportError(sessionId, segmentResult.error, decodingMetadata)
                return
            }
        }
        val legacySession = repository.getSession(sessionId)
            ?: RecordingSessionEntity(
                id = sessionId,
                title = metadata.displayName,
                createdAt = metadata.importedAt,
                updatedAt = System.currentTimeMillis(),
                sourceType = AudioSourceType.LOCAL_MEDIA,
                status = RecordingStatus.TRANSCRIBING,
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
        val chunker = PcmChunker(settings.transcriptionChunkDurationMs.coerceIn(10_000L, 600_000L))
        val recognizer = SenseVoiceRecognizer(settings.senseVoiceModelPath)
        val diarization = SpeakerDiarizationEngine(
            modelPath = settings.diarizationModelPath,
            enabled = settings.enableSpeakerDiarization,
            maxSpeakerCount = settings.maxSpeakerCount
        )
        decoded.streams.forEach { stream ->
            chunker.offer(stream).forEach { chunk ->
                processChunk(
                    session = legacySession.copy(status = RecordingStatus.TRANSCRIBING),
                    chunk = chunk,
                    recognizer = recognizer,
                    diarization = diarization,
                    recordingSegmentId = segment.id
                )
            }
        }
        chunker.flush()?.let { chunk ->
            processChunk(
                session = legacySession.copy(status = RecordingStatus.TRANSCRIBING),
                chunk = chunk,
                recognizer = recognizer,
                diarization = diarization,
                recordingSegmentId = segment.id
            )
        }
        val completedMetadata = decodingMetadata.copy(
            status = ImportedContentStatus.COMPLETED,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        val transcriptSegments = repository.getSegments(sessionId)
        repository.updateImportedContent(
            sessionId = sessionId,
            metadata = completedMetadata,
            status = if (transcriptSegments.isEmpty()) PodcastSessionStatus.PAUSED else PodcastSessionStatus.READY_FOR_SUMMARY
        )
        val updated = repository.getSession(sessionId)
        val stage = if (transcriptSegments.isEmpty()) {
            ProcessingStageUiState.internalAudioUnavailable()
        } else {
            ProcessingStageUiState.completed("本地音视频导入完成，可生成总结")
        }
        updateRecordingState(
            currentSession = updated,
            currentSegments = transcriptSegments,
            recordingStatus = if (transcriptSegments.isEmpty()) RecordingStatus.ERROR else RecordingStatus.COMPLETED,
            vadLabel = if (transcriptSegments.isEmpty()) "未识别到可转写声音" else "导入完成，已写入 ${transcriptSegments.size} 段转写",
            processingStage = stage,
            processingSessionId = sessionId,
            error = null
        )
    }

    private suspend fun updateLocalImportError(
        sessionId: String?,
        error: AppError,
        metadata: ImportedContentMetadata? = null
    ) {
        val message = error.toUserMessage()
        if (sessionId != null && metadata != null) {
            repository.updateImportedContent(
                sessionId = sessionId,
                metadata = metadata.copy(
                    status = ImportedContentStatus.FAILED,
                    errorMessage = message,
                    updatedAt = System.currentTimeMillis()
                ),
                status = PodcastSessionStatus.ERROR
            )
        }
        withContext(Dispatchers.Main.immediate) {
            mutableState.value = mutableState.value.copy(
                recordingStatus = RecordingStatus.ERROR,
                processingStage = ProcessingStageUiState.error(message),
                processingSessionId = sessionId,
                error = error
            )
        }
    }

    private fun preferredInternalCaptureUids(): List<Int> {
        val packageManager = getApplication<Application>().packageManager
        return InternalAudioCapturePolicy.preferredCapturePackages.mapNotNull { packageName ->
            runCatching {
                packageManager.getApplicationInfo(packageName, 0).uid.also { uid ->
                    Log.i(TAG, "preferred_internal_capture_package package=$packageName uid=$uid")
                }
            }.onFailure {
                Log.w(
                    TAG,
                    "preferred_internal_capture_package_failed package=$packageName error=${it.javaClass.simpleName}"
                )
            }.getOrNull()
        }.distinct()
    }

    private suspend fun stopSegmentRecording(
        activeSegment: ActiveRecordingSegment
    ): AppResult<SegmentStopResult> {
        val job = captureJob
        val manager = activeCaptureManager
        updateStopRequestedState()
        manager?.stop()
        activeCaptureManager = null
        captureJob = null
        return try {
            job?.join()
            val endedAt = System.currentTimeMillis()
            val startedAt = repository.observeSessionDetail(activeSegment.sessionId)
                .first()
                ?.recordingSegments
                ?.firstOrNull { it.id == activeSegment.segmentId }
                ?.startedAt
                ?: endedAt
            AppResult.Success(
                SegmentStopResult(
                    endedAt = endedAt,
                    durationMs = (endedAt - startedAt).coerceAtLeast(0L)
                )
            ).also {
                updateRecordingState(
                    recordingStatus = RecordingStatus.COMPLETED,
                    processingStage = ProcessingStageUiState.paused(),
                    processingSessionId = activeSegment.sessionId,
                    error = null
                )
            }
        } finally {
            stopForegroundService()
        }
    }

    private suspend fun handleCaptureFailure(sessionId: String, error: AppError) {
        handleCaptureFailure(sessionId = sessionId, recordingSegmentId = null, error = error)
    }

    private suspend fun handleCaptureFailure(
        sessionId: String,
        recordingSegmentId: String?,
        error: AppError
    ) {
        val failureState = RecordingLifecyclePolicy.captureFailureState(error)
        if (!failureState.persistFailure) {
            updateRecordingState(
                recordingStatus = failureState.recordingStatus,
                vadLabel = failureState.vadLabel ?: mutableState.value.vadLabel,
                processingStage = ProcessingStageUiState.internalAudioUnavailable(),
                processingSessionId = sessionId,
                error = failureState.error
            )
            return
        }
        val session = repository.getSession(sessionId)
        if (session != null) {
            repository.saveSession(session.copy(status = RecordingStatus.ERROR, errorMessage = error.toString()))
        }
        if (recordingSegmentId != null) {
            repository.markSegmentTranscriptionFailed(
                sessionId = sessionId,
                recordingSegmentId = recordingSegmentId,
                errorMessage = "Transcription failed"
            )
        }
        updateRecordingState(
            recordingStatus = failureState.recordingStatus,
            processingStage = ProcessingStageUiState.error(error.toUserMessage()),
            processingSessionId = sessionId,
            error = failureState.error
        )
    }

    private fun handleRecordingControllerResult(result: AppResult<*>) {
        if (result is AppResult.Failure) {
            mutableState.value = mutableState.value.copy(
                processingStage = ProcessingStageUiState.error(result.error.toUserMessage()),
                error = result.error
            )
        }
    }

    private inner class ViewModelSegmentRecorder : SegmentRecorder {
        override suspend fun start(request: SegmentStartRequest): AppResult<Unit> {
            return startSegmentRecording(request)
        }

        override suspend fun stop(activeSegment: ActiveRecordingSegment): AppResult<SegmentStopResult> {
            return stopSegmentRecording(activeSegment)
        }
    }

    private suspend fun updateRecordingState(
        currentSession: RecordingSessionEntity? = mutableState.value.currentSession,
        currentSegments: List<TranscriptSegmentEntity> = mutableState.value.currentSegments,
        recordingStatus: RecordingStatus = mutableState.value.recordingStatus,
        vadLabel: String = mutableState.value.vadLabel,
        processingStage: ProcessingStageUiState = mutableState.value.processingStage,
        processingSessionId: String? = mutableState.value.processingSessionId,
        error: AppError? = mutableState.value.error
    ) {
        withContext(Dispatchers.Main.immediate) {
            mutableState.value = mutableState.value.copy(
                currentSession = currentSession,
                currentSegments = currentSegments,
                recordingStatus = recordingStatus,
                vadLabel = vadLabel,
                processingStage = processingStage,
                processingSessionId = processingSessionId,
                error = error
            )
            updateForegroundNotification(
                sourceType = mutableState.value.audioSourceType,
                recordingState = recordingStatus.toNotificationState(),
                stage = processingStage,
                session = currentSession
            )
        }
    }

    private suspend fun startForegroundServiceSafely(
        foregroundServiceType: Int,
        notificationState: CaptureNotificationState = CaptureNotificationState()
    ): AppError? {
        val context = getApplication<Application>()
        return CaptureForegroundService.startAndWait(context, foregroundServiceType, notificationState)
    }

    private fun modelGateError(settings: AppSettings = mutableState.value.settings): AppError? {
        return RecordingLifecyclePolicy.modelGateError(settings, mutableState.value.modelStatus)
    }

    private fun updateStopRequestedState() {
        mutableState.value = mutableState.value.copy(
            recordingStatus = RecordingStatus.TRANSCRIBING,
            vadLabel = "正在停止录音并转写最后一段",
            processingStage = ProcessingStageUiState.transcribing(1),
            error = null
        )
    }

    private fun updateForegroundNotification(
        sourceType: AudioSourceType?,
        recordingState: String,
        stage: ProcessingStageUiState,
        session: RecordingSessionEntity? = mutableState.value.currentSession
    ) {
        val source = sourceType ?: return
        val context = getApplication<Application>()
        val foregroundServiceType = when (source) {
            AudioSourceType.INTERNAL_AUDIO -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            AudioSourceType.MICROPHONE -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            AudioSourceType.LOCAL_MEDIA -> return
        }
        val intent = CaptureForegroundService.buildStartIntent(
            context = context,
            foregroundServiceType = foregroundServiceType,
            notificationState = CaptureNotificationState(
                podcastTitle = session?.title,
                captureSource = source.toNotificationSource(),
                recordingState = recordingState,
                stageText = stage.toNotificationText(),
                activeSessionId = session?.id
            )
        )
        runCatching { context.startService(intent) }
    }

    private fun ProcessingStageUiState.toNotificationText(): String {
        return listOfNotNull(title, progressLabel).joinToString(" ")
    }

    private fun AudioSourceType.sourceLabel(): String {
        return when (this) {
            AudioSourceType.INTERNAL_AUDIO -> "系统内录"
            AudioSourceType.MICROPHONE -> "麦克风"
            AudioSourceType.LOCAL_MEDIA -> "本地导入"
        }
    }

    private fun AudioSourceType.toNotificationSource(): String {
        return when (this) {
            AudioSourceType.INTERNAL_AUDIO -> CaptureNotificationState.SOURCE_SYSTEM_AUDIO
            AudioSourceType.MICROPHONE -> CaptureNotificationState.SOURCE_MICROPHONE
            AudioSourceType.LOCAL_MEDIA -> CaptureNotificationState.SOURCE_AUDIO
        }
    }

    private fun UrlImportSourceKind.toUserLabel(): String {
        return when (this) {
            UrlImportSourceKind.XIAOYUZHOU_EPISODE -> "小宇宙单期"
            UrlImportSourceKind.DIRECT_MEDIA -> "直链媒体"
            UrlImportSourceKind.RSS_ENCLOSURE -> "RSS enclosure"
            UrlImportSourceKind.UNSUPPORTED -> "URL"
        }
    }

    private fun RecordingStatus.toNotificationState(): String {
        return when (this) {
            RecordingStatus.TRANSCRIBING,
            RecordingStatus.VAD_DETECTING,
            RecordingStatus.DIARIZING -> CaptureNotificationState.STATE_PROCESSING
            RecordingStatus.SUMMARIZING -> CaptureNotificationState.STATE_SUMMARIZING
            RecordingStatus.COMPLETED,
            RecordingStatus.ERROR,
            RecordingStatus.NOT_STARTED -> CaptureNotificationState.STATE_PAUSED
            RecordingStatus.CAPTURING_AUDIO -> CaptureNotificationState.STATE_RECORDING
        }
    }

    private fun List<PcmAudioStream>.durationMs(): Long {
        return sumOf { stream ->
            val samplesPerSecond = stream.sampleRate.toLong() * stream.channelCount.coerceAtLeast(1)
            if (samplesPerSecond <= 0L) 0L else (stream.samples.size.toLong() * 1000L) / samplesPerSecond
        }
    }

    private fun stopForegroundService() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, CaptureForegroundService::class.java))
    }

    override fun onCleared() {
        activeCaptureManager?.stop()
        pendingInternalAudioProjection?.stop()
        super.onCleared()
    }

    private companion object {
        const val TAG = "BlogRecordingPipeline"
        const val INTERNAL_AUDIO_TARGET_PEAK = 12_000
        const val INTERNAL_AUDIO_MAX_GAIN = 12.0f
    }
}
