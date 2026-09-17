package com.example.radarcamera.ui.sessions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.ui.common.*

@Composable fun SessionsScreen(viewModel: SessionsViewModel, onBack: () -> Unit, onNew: () -> Unit, onOpen: (String) -> Unit, historyTitle: String = "Nueva sesión", showNew: Boolean = true, discarded: Boolean = false) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoachingPage(historyTitle, onBack, scroll = false) {
        if (showNew) ElevatedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Configurar nueva sesión") }
        Text("Sesiones configuradas localmente")
        when { state.loading -> LoadingMessage(); state.error != null -> ErrorMessage(state.error); state.sessions.isEmpty() -> Text("Aún no hay sesiones configuradas.")
            else -> LazyColumn { items(state.sessions, key = { it.session.id }) { item ->
                ListItem(headlineContent = { Text(item.playerName) }, supportingContent = { Text("${item.session.sport.label} · ${item.session.currentPitchType.label} · objetivo ${item.session.target}") }, trailingContent = { TextButton(onClick = { onOpen(item.session.id) }) { Text("Abrir") } })
                if (discarded) TextButton(onClick = { viewModel.restore(item.session.id) }) { Text("Restaurar") }
            } }
        }
    }
}
