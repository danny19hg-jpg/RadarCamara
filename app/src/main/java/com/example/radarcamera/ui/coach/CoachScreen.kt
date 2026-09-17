package com.example.radarcamera.ui.coach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.ui.common.*

@Composable
fun CoachScreen(viewModel: CoachViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    BackHandler(enabled = state.saving) { }
    CoachingPage("Perfil del entrenador", onBack, backEnabled = !state.saving) {
        if (state.loading) {
            LoadingMessage()
        } else if (state.loadFailed) {
            ErrorMessage(state.error, viewModel::load)
        } else {
            Text("Perfil local. Academia y país son opcionales.")
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.edit(it, state.academy, state.country) },
                label = { Text("Nombre") }, singleLine = true,
                enabled = !state.saving, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.academy,
                onValueChange = { viewModel.edit(state.name, it, state.country) },
                label = { Text("Academia (opcional)") }, singleLine = true,
                enabled = !state.saving, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.country,
                onValueChange = { viewModel.edit(state.name, state.academy, it) },
                label = { Text("País (opcional)") }, singleLine = true,
                enabled = !state.saving, modifier = Modifier.fillMaxWidth()
            )
            ErrorMessage(state.error)
            Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.saving) "Guardando…" else "Guardar perfil")
            }
            if (state.error != null) TextButton(onClick = viewModel::load, enabled = !state.saving) {
                Text("Volver a cargar el perfil")
            }
        }
    }
}
