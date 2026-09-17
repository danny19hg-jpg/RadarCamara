package com.example.radarcamera.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.R
import com.example.radarcamera.data.repository.CoachRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CoachingRecoveryState(val verified: Boolean = false, val busy: Boolean = false, val error: Int? = null, val completed: Boolean = false)

class CoachingRecoveryViewModel(private val mode: RecoveryMode, private val coaches: CoachRepository, private val pinStore: PinStore) : ViewModel() {
    var state by mutableStateOf(CoachingRecoveryState()); private set
    fun verify(pin: String) { if (state.busy) return; state = state.copy(busy = true, error = null); viewModelScope.launch { val ok = withContext(Dispatchers.IO) { pinStore.verificarPin(pin) }; state = CoachingRecoveryState(verified = ok, error = if (ok) null else R.string.coaching_recovery_pin_incorrect) } }
    fun complete(name: String, pin: String, confirmation: String) {
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (mode) {
                        RecoveryMode.VerifyExistingPinThenCreateProfile -> { check(state.verified && coaches.get() == null && pinStore.tienePin()); coaches.save(name, "", "") }
                        RecoveryMode.CreatePinForExistingProfile -> { check(coaches.get() != null && !pinStore.tienePin()); check(InitialCoachingSetupValidator.isValid("profile", pin, confirmation)); pinStore.guardarPin(pin) }
                    }
                }
                state = CoachingRecoveryState(completed = true)
            } catch (_: Exception) { state = CoachingRecoveryState(verified = state.verified, error = R.string.coaching_recovery_operation_error) }
        }
    }
}

@Composable fun CoachingRecoveryScreen(model: CoachingRecoveryViewModel, mode: RecoveryMode, onBack: () -> Unit, onCompleted: () -> Unit) {
    var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }; val state = model.state
    LaunchedEffect(state.completed) { if (state.completed) onCompleted() }
    Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.coaching_recovery_title), style = MaterialTheme.typography.headlineMedium); Text(stringResource(R.string.coaching_recovery_warning))
        if (mode == RecoveryMode.VerifyExistingPinThenCreateProfile && !state.verified) {
            Text(stringResource(R.string.coaching_recovery_verify_pin)); OutlinedTextField(pin, { pin = it }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation()); Button({ model.verify(pin) }, enabled = !state.busy) { Text(stringResource(R.string.coaching_common_verify)) }
        } else if (mode == RecoveryMode.VerifyExistingPinThenCreateProfile) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.coaching_setup_name)) }); Button({ model.complete(name, "", "") }, enabled = !state.busy && name.isNotBlank()) { Text(stringResource(R.string.coaching_recovery_create_profile)) }
        } else {
            Text(stringResource(R.string.coaching_recovery_create_pin)); OutlinedTextField(pin, { pin = it }, label = { Text(stringResource(R.string.coaching_setup_create_pin)) }, visualTransformation = PasswordVisualTransformation()); OutlinedTextField(confirm, { confirm = it }, label = { Text(stringResource(R.string.coaching_setup_confirm_pin)) }, visualTransformation = PasswordVisualTransformation()); Button({ model.complete("", pin, confirm) }, enabled = !state.busy) { Text(stringResource(R.string.coaching_common_continue)) }
        }
        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }; OutlinedButton(onBack, enabled = !state.busy) { Text(stringResource(R.string.coaching_common_back)) }
    } }
}
