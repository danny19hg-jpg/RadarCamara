package com.example.radarcamera.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.data.repository.PlayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private fun normalized(value: String): String = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    .lowercase()

fun filterPlayers(players: List<PlayerEntity>, query: String): List<PlayerEntity> {
    val normalizedQuery = normalized(query.trim())
    return if (normalizedQuery.isBlank()) players else players.filter { normalized(it.name).contains(normalizedQuery) }
}

data class PlayerDetailState(
    val player: PlayerEntity,
    val openSessionId: String? = null
) {
    val archived: Boolean get() = player.archivedAt != null
    val canStartSession: Boolean get() = !archived && openSessionId == null
    val canContinueSession: Boolean get() = !archived && openSessionId != null

    companion object {
        fun from(player: PlayerEntity, openSessionId: String?) = PlayerDetailState(player, openSessionId)
    }
}

class PlayerDetailViewModel(
    private val repository: PlayerRepository,
    private val playerId: String
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerDetailState?>(null)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observePlayer(playerId), repository.observeOpenSession(playerId)) { player, session ->
                player?.let { PlayerDetailState.from(it, session) }
            }.collect { _state.value = it }
        }
    }
}
