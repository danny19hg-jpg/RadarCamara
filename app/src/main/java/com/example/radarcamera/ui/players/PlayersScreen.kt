package com.example.radarcamera.ui.players

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.domain.ageOn
import com.example.radarcamera.ui.common.*
import java.time.LocalDate

@Composable
fun PlayersScreen(
    viewModel: PlayersViewModel,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (PlayerEntity) -> Unit,
    onSelect: (PlayerEntity) -> Unit = onEdit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var archiveCandidate by remember { mutableStateOf<PlayerEntity?>(null) }
    BackHandler(enabled = state.busyId != null) { }
    CoachingPage("Jugadores", onBack, backEnabled = state.busyId == null, scroll = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !state.archived, onClick = { viewModel.load(false) },
                label = { Text("Activos") }, enabled = state.busyId == null)
            FilterChip(selected = state.archived, onClick = { viewModel.load(true) },
                label = { Text("Archivados") }, enabled = state.busyId == null)
        }
        Button(onClick = onNew, enabled = state.busyId == null, modifier = Modifier.fillMaxWidth()) {
            Text("Crear jugador")
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::search,
            label = { Text("Buscar jugador") },
            singleLine = true,
            enabled = state.busyId == null,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorMessage(state.error, retry = { viewModel.load() })
        if (state.loading) LoadingMessage()
        else if (state.players.isEmpty() && state.error == null)
            Text(if (state.archived) "No hay jugadores archivados." else "Todavía no hay jugadores. Crea el primero.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.players, key = { it.id }) { player ->
                Card(onClick = { onSelect(player) }, modifier = Modifier.fillMaxWidth(), enabled = state.busyId == null) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(player.name, style = MaterialTheme.typography.titleMedium)
                        val age = runCatching { ageOn(LocalDate.parse(player.birthDate)) }.getOrNull()
                        Text("${age?.let { "$it años" } ?: "Sin fecha"} · ${player.category.ifBlank { "Sin categoría" }}")
                        Text("Mano de lanzar: ${player.throwingHand.label}")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { onEdit(player) }, enabled = state.busyId == null) { Text("Editar") }
                            TextButton(
                                onClick = {
                                    if (player.archivedAt == null) archiveCandidate = player
                                    else viewModel.setArchived(player)
                                },
                                enabled = state.busyId == null
                            ) { Text(if (player.archivedAt == null) "Archivar" else "Desarchivar") }
                        }
                    }
                }
            }
        }
    }
    archiveCandidate?.let { player ->
        AlertDialog(
            onDismissRequest = { archiveCandidate = null },
            title = { Text("Archivar a ${player.name}") },
            text = { Text("Se conservarán sus datos. Puedes desarchivarlo cuando quieras.") },
            confirmButton = {
                TextButton(onClick = { archiveCandidate = null; viewModel.setArchived(player) }) { Text("Archivar") }
            },
            dismissButton = { TextButton(onClick = { archiveCandidate = null }) { Text("Cancelar") } }
        )
    }
}
