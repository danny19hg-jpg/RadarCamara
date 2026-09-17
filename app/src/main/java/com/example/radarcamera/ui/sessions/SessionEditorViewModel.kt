package com.example.radarcamera.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.data.repository.PlayerRepository
import com.example.radarcamera.data.repository.SessionRepository
import com.example.radarcamera.data.repository.PitchRepository
import com.example.radarcamera.data.local.PitchEntity
import com.example.radarcamera.radar.RadarStatusClient
import com.example.radarcamera.radar.CoachingEventCursor
import com.example.radarcamera.radar.CoachingEventDecision
import com.example.radarcamera.radar.RadarReadResult
import com.example.radarcamera.camera.SessionVideoRequest
import com.example.radarcamera.camera.SessionVideoState
import com.example.radarcamera.camera.MediaPathBuilder
import com.example.radarcamera.camera.VideoDestination
import com.example.radarcamera.domain.PitchVideoStatus
import kotlinx.coroutines.delay
import com.example.radarcamera.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import android.os.SystemClock
import android.util.Log
import com.example.radarcamera.BuildConfig
import android.content.ContentResolver
import android.net.Uri
import com.example.radarcamera.navigation.SessionDetailOrigin

enum class CoachingRadarConnection { SYNCHRONIZING, READY, UNSTABLE }

data class SessionEditorState(val loading: Boolean = false, val saving: Boolean = false, val players: List<PlayerEntity> = emptyList(), val draft: SessionDraft = SessionDraft(), val errors: Map<String, String> = emptyMap(), val error: String? = null, val saved: Boolean = false, val createdSessionId: String? = null, val running:Boolean=false, val finished:Boolean=false, val pitches:List<PitchEntity> = emptyList(), val videoRequest: SessionVideoRequest? = null, val cameraVideoState: SessionVideoState? = null, val playerName:String="", val sessionStartedAt:Long=0, val endedAt:Long?=null, val finishConfirmation:Boolean=false, val discardConfirmation:Boolean=false, val discarding:Boolean=false, val discarded:Boolean=false, val restored:Boolean=false, val permanentlyDeleted:Boolean=false, val radarConnection: CoachingRadarConnection = CoachingRadarConnection.SYNCHRONIZING, val radarSynchronized: Boolean = false)

fun sessionDraftForPlayer(player: PlayerEntity): SessionDraft {
    val pitchType = PitchType.forSport(player.sport).firstOrNull() ?: PitchType.BASEBALL_FASTBALL
    return SessionDraft(player.id, player.sport, pitchType, 0, "", false)
}

fun sessionCreationSummary(playerName: String, sport: Sport): String = "${playerName.trim().ifBlank { "Jugador" }} · ${sport.label}"

fun sessionCreationError(player: PlayerEntity?, hasOpenSession: Boolean): String? = when {
    player == null -> "No se encontró el jugador seleccionado."
    player.archivedAt != null -> "Este jugador está archivado y no puede abrir una sesión."
    hasOpenSession -> "Ya existe una sesión abierta para este jugador."
    else -> null
}

class SessionEditorViewModel(
    private val sessions: SessionRepository,
    private val pitchRepository:PitchRepository,
    private val playersRepo: PlayerRepository,
    val sessionId: String?,
    val origin: SessionDetailOrigin = SessionDetailOrigin.OPEN_SESSIONS,
    private val creationPlayerId: String? = null,
    private val resolver: ContentResolver? = null
) : ViewModel() {
    private val _state = MutableStateFlow(SessionEditorState(loading = sessionId != null || creationPlayerId != null))
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch { playersRepo.observe(false).collect { _state.value = _state.value.copy(players = it) } }
        viewModelScope.launch { pitchRepository.recoverInterruptedVideos() }
        if (sessionId != null) load()
        else if (creationPlayerId != null) loadCreationDraft()
    }
    private fun load() = viewModelScope.launch {
        try {
            val session = sessions.get(sessionId!!) ?: error("Sesión no disponible")
            _state.value = _state.value.copy(loading = false, draft = SessionDraft(session.playerId, session.sport, session.currentPitchType, session.target, "", session.recordingEnabled), running=false, finished=session.endedAt!=null || origin == SessionDetailOrigin.DELETED_SESSIONS, playerName=playersRepo.get(session.playerId)?.name.orEmpty(), sessionStartedAt=session.startedAt, endedAt=session.endedAt ?: session.discardedAt)
            viewModelScope.launch { (if (origin == SessionDetailOrigin.DELETED_SESSIONS) pitchRepository.observeIncludingDiscarded(sessionId) else pitchRepository.observe(sessionId)).collect { _state.value=_state.value.copy(pitches=it) } }
            if (session.endedAt == null) start()
        } catch (_: Exception) { _state.value = _state.value.copy(loading = false, error = "No se pudo recuperar la sesión.") }
    }

    private fun loadCreationDraft() = viewModelScope.launch {
        try {
            val player = playersRepo.get(creationPlayerId ?: error("No se encontró el jugador seleccionado."))
                ?: error("No se encontró el jugador seleccionado.")
            require(player.archivedAt == null) { "Este jugador está archivado y no puede abrir una sesión." }
            val draft = sessionDraftForPlayer(player)
            _state.value = _state.value.copy(
                loading = false,
                draft = draft,
                playerName = player.name,
                errors = emptyMap(),
                error = null
            )
        } catch (error: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                error = error.message ?: "No se pudo preparar la sesión."
            )
        }
    }
    fun edit(draft: SessionDraft) { _state.value = _state.value.copy(draft = draft, errors = emptyMap(), error = null) }
    fun save() {
        val current = _state.value; if (current.saving || current.loading) return
        val errors = current.draft.errors(); if (errors.isNotEmpty()) { _state.value = current.copy(errors = errors); return }
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch { try {
            if (sessionId == null) {
                val createdId = sessions.create(current.draft)
                _state.value = _state.value.copy(saving = false, createdSessionId = createdId)
            } else {
                sessions.changePitchType(sessionId, current.draft.pitchType)
                _state.value = _state.value.copy(saving = false, saved = true)
            }
        } catch (_: Exception) { _state.value = _state.value.copy(saving = false, error = "No se pudo guardar la sesión.") } }
    }
    fun consumeCreatedSession(): String? {
        val createdId = _state.value.createdSessionId ?: return null
        _state.value = _state.value.copy(createdSessionId = null)
        return createdId
    }
    fun start() = viewModelScope.launch {
        val id=sessionId ?: return@launch
        if (_state.value.running || _state.value.finished) return@launch
        try {
            sessions.start(id)
            _state.value=_state.value.copy(running=true, sessionStartedAt=sessions.get(id)?.startedAt ?: _state.value.sessionStartedAt, radarConnection=CoachingRadarConnection.SYNCHRONIZING, radarSynchronized=false)
            poll(id)
        } catch (_: Exception) {
            _state.value=_state.value.copy(error="No se pudo iniciar la sesión. Revisa su estado e inténtalo nuevamente.")
        }
    }
    private fun poll(id:String) = viewModelScope.launch {
        val loopId = SystemClock.elapsedRealtimeNanos()
        val cursor = CoachingEventCursor()
        val radar = RadarStatusClient()
        debugRadarLog { "t=${SystemClock.elapsedRealtime()} sid=$id loop=$loopId START" }
        while (_state.value.running) {
            when (val read = radar.read()) {
                is RadarReadResult.Success -> {
                    val outcome = cursor.onSuccessfulStatus(read.status.eventId)
                    val snapshot = _state.value
                    _state.value = snapshot.copy(radarConnection = CoachingRadarConnection.READY, radarSynchronized = true)
                    debugRadarLog {
                        "t=${SystemClock.elapsedRealtime()} sid=$id loop=$loopId http=SUCCESS event=${read.status.eventId} mph=${read.status.mph} previous=${outcome.previousEventId} decision=${outcome.decision} recording=${snapshot.draft.recordingEnabled} camera=${snapshot.cameraVideoState}"
                    }
                    if (outcome.missingEventCount > 0) {
                        debugRadarLog { "t=${SystemClock.elapsedRealtime()} sid=$id loop=$loopId EVENT_GAP missing=${outcome.missingEventCount}" }
                    }
                    if (outcome.decision == CoachingEventDecision.NEW_EVENT) {
                        registerPitch(id, read.status.eventId, read.status.mph, snapshot, loopId)
                    }
                }
                else -> {
                    cursor.onTransportFailure()
                    _state.value = _state.value.copy(radarConnection = CoachingRadarConnection.UNSTABLE)
                    debugRadarLog { "t=${SystemClock.elapsedRealtime()} sid=$id loop=$loopId http=${read.debugLabel()} cursor=${cursor.lastEventId}" }
                }
            }
            delay(400)
        }
        debugRadarLog { "t=${SystemClock.elapsedRealtime()} sid=$id loop=$loopId END" }
    }

    private suspend fun registerPitch(id: String, eventId: Long, mph: Double, snapshot: SessionEditorState, loopId: Long) {
        val receivedAt = System.currentTimeMillis()
        val type = snapshot.draft.pitchType
        val recording = snapshot.draft.recordingEnabled
        val pitchId = UUID.randomUUID().toString()
        try {
            val inserted = pitchRepository.record(pitchId, id, eventId, mph, type, receivedAt, recording)
            debugRadarLog { "t=${SystemClock.elapsedRealtime()} sid=$id loop=$loopId INSERT pitch=$pitchId inserted=${inserted != null}" }
            if (inserted != null && recording) {
                val destination = MediaPathBuilder().destination(snapshot.playerName, snapshot.draft.playerId, id, snapshot.sessionStartedAt, snapshot.draft.sport, inserted.number, type, mph)
                _state.value = _state.value.copy(videoRequest = SessionVideoRequest(pitchId, eventId, mph, type, receivedAt, inserted.number, destination))
            }
        } catch (error: Exception) {
            _state.value = _state.value.copy(error = "No se pudo guardar el lanzamiento; el radar continúa activo.")
            if (BuildConfig.DEBUG) Log.e("CoachingRadar", "sid=$id loop=$loopId INSERT_ERROR", error)
        }
    }

    private fun debugRadarLog(message: () -> String) {
        if (BuildConfig.DEBUG) Log.d("CoachingRadar", message())
    }

    private fun RadarReadResult.debugLabel(): String = when (this) {
        RadarReadResult.Timeout -> "TIMEOUT"
        is RadarReadResult.NetworkError -> "NETWORK_ERROR:$exceptionType"
        is RadarReadResult.HttpError -> "HTTP_ERROR:$code"
        RadarReadResult.ParseError -> "PARSE_ERROR"
        is RadarReadResult.Success -> "SUCCESS"
    }
    fun consumeVideoRequest(pitchId:String) { if (_state.value.videoRequest?.pitchId == pitchId) _state.value=_state.value.copy(videoRequest=null) }
    fun videoPending(pitchId:String, rawPath:String) = updateVideo(pitchId, PitchVideoStatus.PENDING, rawPath=rawPath)
    fun videoAvailable(pitchId:String, uri:String, destination: VideoDestination) = updateVideo(pitchId, PitchVideoStatus.AVAILABLE, uri=uri, relativePath=destination.relativePath, displayName=destination.displayName)
    fun videoUnavailable(pitchId:String, reason:String) = updateVideo(pitchId, PitchVideoStatus.NOT_RECORDED, reason=reason)
    fun videoFailed(pitchId:String, reason:String) = updateVideo(pitchId, PitchVideoStatus.FAILED, reason=reason)
    private fun updateVideo(pitchId:String,status:PitchVideoStatus,uri:String?=null,reason:String?=null,rawPath:String?=null,relativePath:String?=null,displayName:String?=null)=viewModelScope.launch { pitchRepository.updateVideo(pitchId,status,uri,reason,rawPath,relativePath,displayName) }
    fun selectPitchType(type: PitchType) {
        if (type.sport != _state.value.draft.sport || _state.value.finished) return
        _state.value = _state.value.copy(draft = _state.value.draft.copy(pitchType = type))
        sessionId?.let { id -> viewModelScope.launch { sessions.changePitchType(id, type) } }
    }
    fun setRecordingEnabled(enabled: Boolean) { if (!_state.value.finished) _state.value = _state.value.copy(draft = _state.value.draft.copy(recordingEnabled = enabled), cameraVideoState = if (enabled) SessionVideoState.PREPARING else null) }
    fun updateCameraVideoState(videoState: SessionVideoState) { if (_state.value.draft.recordingEnabled) _state.value = _state.value.copy(cameraVideoState = videoState) }
    fun requestFinish() { if (_state.value.running) _state.value = _state.value.copy(finishConfirmation = true) }
    fun cancelFinish() { _state.value = _state.value.copy(finishConfirmation = false) }
    fun finish()=viewModelScope.launch { sessions.finish(sessionId?:return@launch); _state.value=_state.value.copy(running=false,finished=true,finishConfirmation=false) }
    fun leave(onComplete: () -> Unit) {
        if (!_state.value.running) { onComplete(); return }
        _state.value = _state.value.copy(running = false)
        viewModelScope.launch { sessions.finish(sessionId ?: return@launch); _state.value = _state.value.copy(finished = true); onComplete() }
    }
    fun requestDiscard() { if (origin == SessionDetailOrigin.NORMAL_HISTORY && _state.value.finished && !_state.value.discarding) _state.value=_state.value.copy(discardConfirmation=true, error=null) }
    fun requestPermanentDelete() { if (origin == SessionDetailOrigin.DELETED_SESSIONS && _state.value.finished && !_state.value.discarding) _state.value=_state.value.copy(discardConfirmation=true, error=null) }
    fun cancelDiscard() { if (!_state.value.discarding) _state.value=_state.value.copy(discardConfirmation=false) }
    fun confirmDiscard() { val id=sessionId ?: return; if (origin != SessionDetailOrigin.NORMAL_HISTORY || _state.value.discarding) return; _state.value=_state.value.copy(discarding=true,error=null); viewModelScope.launch { try { sessions.discard(id); _state.value=_state.value.copy(discarding=false,discardConfirmation=false,discarded=true) } catch (_:Exception) { _state.value=_state.value.copy(discarding=false,error="No se pudo mover la sesión a eliminadas.") } } }
    fun confirmPermanentDelete() { val id=sessionId ?: return; if (origin != SessionDetailOrigin.DELETED_SESSIONS || _state.value.discarding) return; _state.value=_state.value.copy(discarding=true,error=null); viewModelScope.launch { try { val media = requireNotNull(resolver) { "No hay acceso a MediaStore." }; deleteDistinctVideos(_state.value.pitches.mapNotNull { it.videoUri }, VideoDeletionGateway { media.delete(Uri.parse(it), null, null) }); sessions.permanentlyDeleteDiscarded(id); _state.value=_state.value.copy(discarding=false,discardConfirmation=false,permanentlyDeleted=true) } catch (_:Exception) { _state.value=_state.value.copy(discarding=false,error="No se pudieron eliminar todos los videos; la sesión permanece disponible para reintentar.") } } }
    fun restore() { val id=sessionId ?: return; if (_state.value.discarding) return; _state.value=_state.value.copy(discarding=true,error=null); viewModelScope.launch { try { sessions.restore(id); _state.value=_state.value.copy(discarding=false,restored=true) } catch (_:Exception) { _state.value=_state.value.copy(discarding=false,error="No se pudo restaurar la sesión.") } } }
}
