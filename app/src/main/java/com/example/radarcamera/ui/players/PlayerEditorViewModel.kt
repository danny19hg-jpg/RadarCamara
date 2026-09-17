package com.example.radarcamera.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.repository.PlayerRepository
import com.example.radarcamera.domain.PlayerDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerEditorState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val draft: PlayerDraft = PlayerDraft(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val loadFailed: Boolean = false,
    val saved: Boolean = false,
    val persistedPlayerName: String = "",
    val deleting: Boolean = false,
    val deleteConfirmation: PlayerDeletionConfirmation? = null,
    val deletedPlayerId: String? = null
)

data class PlayerDeletionConfirmation(val playerId: String, val playerName: String)

fun PlayerEditorState.cancelDeletion(): PlayerEditorState = copy(deleteConfirmation = null)
fun PlayerEditorState.deletionConfirmationFor(playerId: String) =
    PlayerDeletionConfirmation(playerId, persistedPlayerName)

class PlayerEditorViewModel(
    private val repository: PlayerRepository,
    val playerId: String?
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerEditorState(loading = playerId != null))
    val state = _state.asStateFlow()
    init { if (playerId != null) load() }

    fun load() {
        val id = playerId ?: return
        _state.value = _state.value.copy(loading = true, error = null, loadFailed = false)
        viewModelScope.launch {
            try {
                val player = repository.get(id) ?: error("Jugador no disponible")
                _state.value = PlayerEditorState(draft = player.toDraft(), persistedPlayerName = player.name)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, loadFailed = true, error = "No se pudo cargar el jugador.")
            }
        }
    }

    fun edit(draft: PlayerDraft) {
        _state.value = _state.value.copy(draft = draft, fieldErrors = emptyMap(), error = null)
    }

    fun save() {
        val current = _state.value
        if (current.loading || current.saving || current.loadFailed) return
        val errors = current.draft.errors()
        if (errors.isNotEmpty()) {
            _state.value = current.copy(fieldErrors = errors)
            return
        }
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                repository.save(playerId, current.draft)
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(saving = false, error = "No se pudo guardar. Revisa los datos e inténtalo nuevamente.")
            }
        }
    }

    fun requestDeletion() {
        val id = playerId ?: return
        val current = _state.value
        if (current.loading || current.saving || current.deleting || current.loadFailed) return
        _state.value = current.copy(
            deleteConfirmation = current.deletionConfirmationFor(id),
            error = null
        )
    }

    fun cancelDeletion() {
        _state.value = _state.value.cancelDeletion()
    }

    fun confirmDeletion() {
        val current = _state.value
        val confirmation = current.deleteConfirmation ?: return
        if (current.deleting) return
        _state.value = current.copy(deleting = true, error = null)
        viewModelScope.launch {
            try {
                when (repository.deletePermanently(confirmation.playerId)) {
                    com.example.radarcamera.data.repository.PlayerDeletionResult.Deleted -> {
                        _state.value = _state.value.copy(
                            deleting = false,
                            deleteConfirmation = null,
                            deletedPlayerId = confirmation.playerId
                        )
                    }
                    com.example.radarcamera.data.repository.PlayerDeletionResult.HasHistory -> {
                        _state.value = _state.value.copy(
                            deleting = false,
                            deleteConfirmation = null,
                            error = "Este jugador tiene historial. Archívalo para conservarlo."
                        )
                    }
                    com.example.radarcamera.data.repository.PlayerDeletionResult.NotFound -> {
                        _state.value = _state.value.copy(
                            deleting = false,
                            deleteConfirmation = null,
                            error = "El jugador ya no está disponible."
                        )
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(
                    deleting = false,
                    deleteConfirmation = null,
                    error = "No se pudo eliminar el jugador. Inténtalo nuevamente."
                )
            }
        }
    }
}
