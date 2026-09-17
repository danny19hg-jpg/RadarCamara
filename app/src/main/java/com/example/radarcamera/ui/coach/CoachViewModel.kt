package com.example.radarcamera.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.data.repository.CoachRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoachUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val loadFailed: Boolean = false,
    val name: String = "",
    val academy: String = "",
    val country: String = "",
    val error: String? = null,
    val saved: Boolean = false
)

class CoachViewModel(private val repository: CoachRepository) : ViewModel() {
    private val _state = MutableStateFlow(CoachUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null, loadFailed = false)
        viewModelScope.launch {
            try {
                val coach = repository.get()
                _state.value = CoachUiState(
                    loading = false, name = coach?.name.orEmpty(),
                    academy = coach?.academy.orEmpty(), country = coach?.country.orEmpty()
                )
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(loading = false, loadFailed = true, error = "No se pudo cargar el perfil.")
            }
        }
    }

    fun edit(name: String, academy: String, country: String) {
        _state.value = _state.value.copy(name = name, academy = academy, country = country, error = null)
    }

    fun save() {
        val current = _state.value
        if (current.loading || current.saving || current.loadFailed) return
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                repository.save(current.name, current.academy, current.country)
                _state.value = _state.value.copy(saving = false, saved = true)
            } catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) {
                _state.value = _state.value.copy(saving = false, error = e.message)
            } catch (_: Exception) {
                _state.value = _state.value.copy(saving = false, error = "No se pudo guardar. Tus cambios siguen en pantalla.")
            }
        }
    }
}
