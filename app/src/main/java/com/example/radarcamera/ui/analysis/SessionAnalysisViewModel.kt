package com.example.radarcamera.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.repository.PlayerRepository
import com.example.radarcamera.data.repository.PitchRepository
import com.example.radarcamera.data.repository.SessionRepository
import com.example.radarcamera.domain.analysis.SessionAnalysis
import com.example.radarcamera.domain.analysis.SessionAnalysisEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionAnalysisUiState {
    data object Loading: SessionAnalysisUiState
    data object Empty: SessionAnalysisUiState
    data class Content(val playerName:String,val sport:String,val endedAt:Long?,val analysis:SessionAnalysis):SessionAnalysisUiState
    data object NotFound: SessionAnalysisUiState
    data class Error(val message:String):SessionAnalysisUiState
}

class SessionAnalysisViewModel(sessions:SessionRepository, pitches:PitchRepository, players:PlayerRepository, sessionId:String):ViewModel() {
    private val _state=MutableStateFlow<SessionAnalysisUiState>(SessionAnalysisUiState.Loading)
    val state=_state.asStateFlow()
    init { viewModelScope.launch { try {
        val session=sessions.getActive(sessionId)
        if(session==null || session.endedAt==null) { _state.value=SessionAnalysisUiState.NotFound; return@launch }
        val player=players.get(session.playerId)
        if(player==null) { _state.value=SessionAnalysisUiState.NotFound; return@launch }
        pitches.observe(sessionId).collect { list ->
            val analysis=SessionAnalysisEngine.analyze(list)
            _state.value=if(list.isEmpty()) SessionAnalysisUiState.Empty else SessionAnalysisUiState.Content(player.name,session.sport.label,session.endedAt,analysis)
        }
    } catch(_:Exception) { _state.value=SessionAnalysisUiState.Error("No se pudo cargar el análisis.") } } }
}
