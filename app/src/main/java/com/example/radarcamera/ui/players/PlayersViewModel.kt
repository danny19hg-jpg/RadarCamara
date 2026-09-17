package com.example.radarcamera.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.data.repository.PlayerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayersUiState(
    val loading: Boolean = true,
    val archived: Boolean = false,
    val players: List<PlayerEntity> = emptyList(),
    val query: String = "",
    val error: String? = null,
    val busyId: String? = null
)

class PlayersViewModel(
    private val repository: PlayerRepository,
    archived: Boolean = false
) : ViewModel() {
    private val _state = MutableStateFlow(PlayersUiState(archived = archived))
    val state = _state.asStateFlow()
    private var observer: Job? = null
    init { load(archived) }

    fun load(archived: Boolean = _state.value.archived) {
        observer?.cancel()
        _state.value = _state.value.copy(loading = true, archived = archived, error = null, players = emptyList())
        observer = viewModelScope.launch {
            try {
                repository.observe(archived).collect {
                    _state.value = _state.value.copy(loading = false, players = filterPlayers(it, _state.value.query))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, error = "No se pudieron cargar los jugadores.")
            }
        }
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query, players = filterPlayers(_state.value.players, query))
        load(_state.value.archived)
    }

    fun setArchived(player: PlayerEntity) {
        if (_state.value.busyId != null) return
        _state.value = _state.value.copy(busyId = player.id, error = null)
        viewModelScope.launch {
            try {
                repository.setArchived(player.id, player.archivedAt == null)
                _state.value = _state.value.copy(busyId = null)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(busyId = null, error = "No se pudo cambiar el estado del jugador.")
            }
        }
    }
}
