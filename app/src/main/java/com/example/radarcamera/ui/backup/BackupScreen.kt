package com.example.radarcamera.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.radarcamera.ui.common.CoachingPage

@Composable fun BackupScreen(viewModel:BackupViewModel,onBack:()->Unit){
 val state by viewModel.state.collectAsState(); var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf(false) }
 val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(viewModel::export)}
 val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(viewModel::inspect)}
 CoachingPage("Datos y respaldo",onBack){
  Text("El respaldo contiene información personal. No incluye MP4 ni PIN; guárdalo en un lugar seguro.")
  Button(enabled=!state.busy,onClick={create.launch(viewModel.suggestedName())},modifier=Modifier.fillMaxWidth()){Text("Crear respaldo")}
  OutlinedButton(enabled=!state.busy,onClick={open.launch(arrayOf("application/json","application/octet-stream","*/*"))},modifier=Modifier.fillMaxWidth()){Text("Restaurar respaldo")}
  state.preview?.let { d-> Text("Respaldo: ${d.manifest.exportedAt}\nJugadores: ${d.players.size} · Sesiones: ${d.sessions.size} · Lanzamientos: ${d.pitches.size}\nRestaurar reemplazará todos los datos deportivos actuales; los MP4 no se borrarán."); OutlinedTextField(pin,{pin=it},label={Text("PIN actual")},singleLine=true); Button(enabled=!state.busy,onClick={confirm=true},colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error),modifier=Modifier.fillMaxWidth()){Text("Confirmar restauración")}}
  state.message?.let{Text(it,color=MaterialTheme.colorScheme.primary)}; state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 }
 if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("¿Restaurar datos?")},text={Text("Se reemplazarán jugadores, sesiones y lanzamientos. El PIN y los videos no cambiarán.")},confirmButton={TextButton(onClick={confirm=false;viewModel.restore(pin)}){Text("Restaurar")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancelar")}})
}
