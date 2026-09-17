package com.example.radarcamera.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.local.SessionListItem
import com.example.radarcamera.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionsUiState(val loading: Boolean = true, val sessions: List<SessionListItem> = emptyList(), val error: String? = null)

class SessionsViewModel(private val repository: SessionRepository, private val history: Boolean = false, private val discarded: Boolean = false, private val playerId: String? = null) : ViewModel() {
    private val _state = MutableStateFlow(SessionsUiState())
    val state = _state.asStateFlow()
    init { viewModelScope.launch { try { (if (discarded) repository.observeDiscarded() else if (history) repository.observeFinished(playerId) else repository.observeOpen()).collect { _state.value = SessionsUiState(false, it) } } catch (_: Exception) { _state.value = SessionsUiState(false, error = "No se pudieron recuperar las sesiones.") } } }
    fun restore(id:String) = viewModelScope.launch { repository.restore(id) }
}
