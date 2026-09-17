package com.example.radarcamera.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.R
import com.example.radarcamera.data.repository.CoachRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InitialCoachingSetupState(
    val saving: Boolean = false,
    val error: InitialSetupError? = null,
    val completed: Boolean = false
)

enum class InitialSetupError(val resourceId: Int) {
    Name(R.string.coaching_setup_name_error),
    Pin(R.string.coaching_setup_pin_error),
    PinMismatch(R.string.coaching_setup_pin_mismatch),
    Save(R.string.coaching_setup_save_error)
}

class InitialCoachingSetupViewModel(
    private val coachRepository: CoachRepository,
    private val pinStore: PinStore
) : ViewModel() {
    var state by mutableStateOf(InitialCoachingSetupState())
        private set

    fun save(name: String, pin: String, confirmation: String) {
        if (state.saving) return
        if (!InitialCoachingSetupValidator.isValid(name, pin, confirmation)) {
            state = state.copy(error = when {
                name.trim().isEmpty() -> InitialSetupError.Name
                pin.length !in 4..6 || !pin.all(Char::isDigit) -> InitialSetupError.Pin
                else -> InitialSetupError.PinMismatch
            })
            return
        }
        state = InitialCoachingSetupState(saving = true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    check(coachRepository.get() == null && !pinStore.tienePin())
                    coachRepository.save(name, "", "")
                    pinStore.guardarPin(pin)
                }
                state = InitialCoachingSetupState(completed = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state = InitialCoachingSetupState(error = InitialSetupError.Save)
            }
        }
    }
}

@Composable
fun InitialCoachingSetupScreen(
    viewModel: InitialCoachingSetupViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val state = viewModel.state
    LaunchedEffect(state.completed) { if (state.completed) onCompleted() }
    BackHandler(enabled = state.saving) { }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.coaching_setup_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.coaching_setup_description))
            OutlinedTextField(value = name, onValueChange = { name = it }, enabled = !state.saving,
                label = { Text(stringResource(R.string.coaching_setup_name)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                enabled = !state.saving, label = { Text(stringResource(R.string.coaching_setup_create_pin)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = confirmation, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmation = it },
                enabled = !state.saving, label = { Text(stringResource(R.string.coaching_setup_confirm_pin)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth())
            state.error?.let { Text(stringResource(it.resourceId), color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.save(name, pin, confirmation) }, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.saving) R.string.coaching_setup_saving else R.string.coaching_setup_create_enter))
            }
            OutlinedButton(onClick = onBack, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.coaching_common_back))
            }
        }
    }
}
