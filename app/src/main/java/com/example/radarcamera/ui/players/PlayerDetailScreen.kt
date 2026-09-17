package com.example.radarcamera.ui.players

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.domain.ageOn
import com.example.radarcamera.ui.common.CoachingPage
import java.time.LocalDate

@Composable
fun PlayerDetailScreen(
    viewModel: PlayerDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String, Boolean) -> Unit,
    onNewSession: (String) -> Unit,
    onContinueSession: (String) -> Unit,
    onHistory: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = false) { onBack() }
    val detail = state ?: run {
        CoachingPage("Jugador", onBack) { Text("Cargando jugador…") }
        return
    }
    CoachingPage(detail.player.name, onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${detail.player.sport.label} · ${detail.player.throwingHand.label}")
            val age = runCatching { ageOn(LocalDate.parse(detail.player.birthDate)) }.getOrNull()
            Text(age?.let { "$it años" } ?: "Sin fecha de nacimiento")
            Text(detail.player.category.ifBlank { "Sin categoría" })
            if (detail.archived) {
                Text("Jugador archivado")
            } else if (detail.canStartSession) {
                Button(onClick = { onNewSession(detail.player.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Nueva sesión")
                }
            }
            if (!detail.archived) {
                detail.openSessionId?.let { sessionId ->
                    OutlinedButton(onClick = { onContinueSession(sessionId) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Continuar sesión")
                    }
                }
            }
            OutlinedButton(onClick = { onHistory(detail.player.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sesiones anteriores")
            }
            OutlinedButton(onClick = { onEdit(detail.player.id, detail.archived) }, modifier = Modifier.fillMaxWidth()) {
                Text("Editar jugador")
            }
        }
    }
}
