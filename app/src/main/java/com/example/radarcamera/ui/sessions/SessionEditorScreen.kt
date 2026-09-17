package com.example.radarcamera.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.domain.*
import com.example.radarcamera.camera.SessionVideoCapture
import com.example.radarcamera.ui.common.*
import android.content.Intent
import android.net.Uri
import com.example.radarcamera.navigation.SessionDetailOrigin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun SessionEditorScreen(viewModel: SessionEditorViewModel, onBack: () -> Unit, onCreated: (String) -> Unit, onAnalysis: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val draft = state.draft; val creating = viewModel.sessionId == null; val context = LocalContext.current
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    LaunchedEffect(state.createdSessionId) { viewModel.consumeCreatedSession()?.let(onCreated) }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    LaunchedEffect(state.discarded) { if (state.discarded) onBack() }
    LaunchedEffect(state.permanentlyDeleted) { if (state.permanentlyDeleted) onBack() }
    LaunchedEffect(state.restored) { if (state.restored) onBack() }
    CoachingPage(if (creating) "Nueva sesión" else if (viewModel.origin == SessionDetailOrigin.DELETED_SESSIONS) "Sesión eliminada" else "Sesión activa", { viewModel.leave(onBack) }, !state.saving) {
        when {
            state.loading -> LoadingMessage()
            else -> {
                if (creating) {
                    val currentError = state.error
                    if (currentError != null) {
                        Text(currentError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver a Jugadores") }
                    } else {
                        Card(Modifier.fillMaxWidth()) { Text("${state.playerName.ifBlank { "Jugador" }} · ${draft.sport.label}", modifier = Modifier.padding(12.dp)) }
                        Text("Al iniciar se abre la sesión y la lectura actual del radar se toma como referencia.")
                    }
                } else Card(Modifier.fillMaxWidth()) {
                    Text("${state.playerName} · ${draft.sport.label}", modifier = Modifier.padding(12.dp))
                }
                if (creating) Text("Al iniciar se abre la sesión y la lectura actual del radar se toma como referencia.")
                if (!creating) {
                    if (!state.finished) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Switch(checked = draft.recordingEnabled, onCheckedChange = viewModel::setRecordingEnabled); Text("Video: ${cameraStatus(state)}") }
                        Text(
                            when (state.radarConnection) {
                                CoachingRadarConnection.SYNCHRONIZING -> "Sincronizando radar…"
                                CoachingRadarConnection.READY -> "Radar listo"
                                CoachingRadarConnection.UNSTABLE -> "Conexión inestable"
                            },
                        )
                        SelectField("Tipo de lanzamiento", draft.pitchType.label, PitchType.forSport(draft.sport), { it.label }, true, viewModel::selectPitchType)
                    }
                    if (state.running && draft.recordingEnabled) {
                        Box(Modifier.fillMaxWidth().aspectRatio(com.example.radarcamera.camera.previewAspectRatio(isPortrait)).clipToBounds()) { SessionVideoCapture(state.videoRequest, viewModel::consumeVideoRequest, viewModel::videoPending, viewModel::videoAvailable, viewModel::videoUnavailable, viewModel::videoFailed, viewModel::updateCameraVideoState) }
                    }
                    LastPitchCard(state.pitches.lastOrNull(), draft.pitchType)
                    Text("Total: ${state.pitches.size}")
                    if (state.running) Button(onClick=viewModel::requestFinish, modifier=Modifier.fillMaxWidth()){Text("Finalizar sesión")}
                    state.pitches.forEach { pitch ->
                        Text("#${pitch.number} · ${pitch.mph} MPH · ${pitch.pitchType.label} · ${videoLabel(pitch.videoStatus, pitch.videoReason)}")
                        pitch.videoUri?.let { uri -> OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) }) { Text("Abrir video") } }
                    }
                }
                ErrorMessage(state.error)
                if (creating && state.error == null) Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "Guardando…" else "Iniciar sesión") }
                if (state.finished) {
                    viewModel.sessionId?.let { id -> Button(onClick={onAnalysis(id)},modifier=Modifier.fillMaxWidth()){Text("Ver análisis")} }
                    if (viewModel.origin != SessionDetailOrigin.OPEN_SESSIONS) Text("Zona de peligro", color = MaterialTheme.colorScheme.error)
                    if (viewModel.origin == SessionDetailOrigin.DELETED_SESSIONS) {
                        OutlinedButton(onClick = viewModel::restore, enabled = !state.discarding, modifier = Modifier.fillMaxWidth()) { Text("Restaurar sesión") }
                        Button(onClick = viewModel::requestPermanentDelete, enabled = !state.discarding, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Eliminar definitivamente") }
                    } else if (viewModel.origin == SessionDetailOrigin.NORMAL_HISTORY) Button(onClick = viewModel::requestDiscard, enabled = !state.discarding, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Mover a sesiones eliminadas") }
                }
            }
        }
    }
    if (state.finishConfirmation) AlertDialog(onDismissRequest=viewModel::cancelFinish, title={Text("Finalizar sesión")}, text={Text("La sesión dejará de aceptar lanzamientos nuevos. Las exportaciones ya iniciadas podrán completarse.")}, confirmButton={TextButton(onClick=viewModel::finish){Text("Finalizar sesión")}}, dismissButton={TextButton(onClick=viewModel::cancelFinish){Text("Cancelar")}})
    if (state.discardConfirmation) AlertDialog(onDismissRequest=viewModel::cancelDiscard, title={Text(if(viewModel.origin == SessionDetailOrigin.DELETED_SESSIONS) "Eliminar definitivamente" else "Mover a sesiones eliminadas")}, text={Text(if(viewModel.origin == SessionDetailOrigin.DELETED_SESSIONS) "${state.playerName.ifBlank { "Jugador" }} · ${formatSessionTime(state.endedAt)} · ${state.pitches.size} lanzamientos · ${state.pitches.count { it.videoUri != null }} videos. Esta acción es irreversible y borrará los MP4 asociados del teléfono." else "${state.playerName.ifBlank { "Jugador" }} · ${formatSessionTime(state.endedAt)} · ${state.pitches.size} lanzamientos. Se moverá a Sesiones eliminadas y podrá restaurarse. Los videos no se borrarán.")}, confirmButton={TextButton(onClick={if(viewModel.origin == SessionDetailOrigin.DELETED_SESSIONS)viewModel.confirmPermanentDelete() else viewModel.confirmDiscard()}, enabled=!state.discarding){Text(if(state.discarding) "Eliminando…" else if(viewModel.origin == SessionDetailOrigin.DELETED_SESSIONS) "Eliminar definitivamente" else "Mover")}}, dismissButton={TextButton(onClick=viewModel::cancelDiscard, enabled=!state.discarding){Text("Cancelar")}})
}

internal fun formatSessionTime(value: Long?): String = value?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it)) } ?: "fecha no disponible"

private fun videoLabel(status: PitchVideoStatus, reason: String?): String = when (status) {
    PitchVideoStatus.PENDING -> "Video pendiente"
    PitchVideoStatus.AVAILABLE -> "Video disponible"
    PitchVideoStatus.FAILED -> "Video fallido"
    PitchVideoStatus.NOT_RECORDED -> if (reason == "CAMARA_PREPARANDO") "Sin video: cámara preparando" else "Sin video"
}

private fun cameraStatus(state: SessionEditorState): String = when {
    !state.draft.recordingEnabled -> "Apagado"
    else -> state.cameraVideoState?.label ?: "Preparando"
}

@Composable private fun LastPitchCard(pitch: com.example.radarcamera.data.local.PitchEntity?, selectedType: PitchType) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(if (pitch == null) "Esperando lanzamiento" else "Lanzamiento #${pitch.number}")
            Text("Tipo: ${pitch?.pitchType?.label ?: selectedType.label}")
            Text("${pitch?.mph ?: "—"} MPH", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable private fun <T> SelectField(label: String, selected: String, options: List<T>, text: (T) -> String, enabled: Boolean, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("$label: $selected") }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(text(option)) }, onClick = { expanded = false; onSelected(option) }) } } }
}
