package com.example.radarcamera.ui.coaching

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.radarcamera.ui.common.CoachingPage

@Composable
fun CoachingDashboard(onProfile: () -> Unit, onPlayers: () -> Unit, onSessions: () -> Unit, onHistory: () -> Unit, onDiscarded: () -> Unit, onBackup: () -> Unit, onExit: () -> Unit) {
    CoachingPage("Coaching local", onExit) {
        Text("Tu perfil y tus jugadores se guardan en este teléfono.")
        ElevatedButton(onClick = onProfile, modifier = Modifier.fillMaxWidth()) { Text("Perfil del entrenador") }
        ElevatedButton(onClick = onPlayers, modifier = Modifier.fillMaxWidth()) { Text("Jugadores") }
        ElevatedButton(onClick = onSessions, modifier = Modifier.fillMaxWidth()) { Text("Nueva sesión") }
        ElevatedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Sesiones anteriores") }
        OutlinedButton(onClick = onDiscarded, modifier = Modifier.fillMaxWidth()) { Text("Sesiones eliminadas") }
        OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) { Text("Datos y respaldo") }
        Text("La cámara está disponible desde Radar Live.")
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Bloquear y salir") }
    }
}
