package com.example.radarcamera.ui.players

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.domain.*
import com.example.radarcamera.ui.common.*

@Composable
fun PlayerEditorScreen(viewModel: PlayerEditorViewModel, onBack: () -> Unit, onHistory: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    LaunchedEffect(state.deletedPlayerId) { if (state.deletedPlayerId != null) onBack() }
    BackHandler(enabled = state.saving || state.deleting) { }
    val editingEnabled = !state.saving && !state.deleting
    CoachingPage(if (viewModel.playerId == null) "Crear jugador" else "Editar jugador", onBack, editingEnabled) {
        when {
            state.loading -> LoadingMessage()
            state.loadFailed -> ErrorMessage(state.error, viewModel::load)
            else -> {
                Text("Estatura, peso y equipo/academia son opcionales.")
                PlayerField("Nombre", draft.name, state.fieldErrors["name"], editingEnabled) {
                    viewModel.edit(draft.copy(name = it))
                }
                BirthDateField(draft.birthDate, state.fieldErrors["birthDate"], editingEnabled) { viewModel.edit(draft.copy(birthDate = it)) }
                draft.birthDateOrNull()?.takeIf { !it.isAfter(java.time.LocalDate.now()) }?.let {
                    Text("Edad: ${ageOn(it)} años")
                }
                ChoiceField("Mano de lanzar", draft.throwingHand, ThrowingHand.entries.filter { it != ThrowingHand.BOTH }, { it.label }, editingEnabled) {
                    viewModel.edit(draft.copy(throwingHand = it))
                }
                PlayerField("Estatura en cm (opcional)", draft.heightCm, state.fieldErrors["heightCm"], editingEnabled, KeyboardType.Decimal) {
                    viewModel.edit(draft.copy(heightCm = it))
                }
                PlayerField("Peso en kg (opcional)", draft.weightKg, state.fieldErrors["weightKg"], editingEnabled, KeyboardType.Decimal) {
                    viewModel.edit(draft.copy(weightKg = it))
                }
                PlayerField("Categoría/nivel", draft.category, null, editingEnabled) { viewModel.edit(draft.copy(category = it)) }
                PlayerField("Equipo/academia (opcional)", draft.teamAcademy, null, editingEnabled) { viewModel.edit(draft.copy(teamAcademy = it)) }
                ErrorMessage(state.error)
                Button(onClick = viewModel::save, enabled = editingEnabled, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) "Guardando…" else "Guardar jugador")
                }
                if (viewModel.playerId != null) {
                    OutlinedButton(onClick = { onHistory(viewModel.playerId) }, enabled = editingEnabled, modifier = Modifier.fillMaxWidth()) { Text("Historial de sesiones") }
                    OutlinedButton(
                        onClick = viewModel::requestDeletion,
                        enabled = editingEnabled,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Eliminar jugador") }
                }
            }
        }
    }
    state.deleteConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeletion,
            title = { Text("Eliminar a ${confirmation.playerName}") },
            text = { Text("Esta eliminación no se puede deshacer. Solo puedes eliminar jugadores sin sesiones ni lanzamientos asociados.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeletion, enabled = !state.deleting) {
                    Text(if (state.deleting) "Eliminando…" else "Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeletion, enabled = !state.deleting) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun BirthDateField(value:String,error:String?,enabled:Boolean,onChange:(String)->Unit){
 var open by remember { mutableStateOf(false) }
 val date=value.toDateOrNull()
 val selectableDates = remember {
  object : SelectableDates {
   override fun isSelectableDate(utcTimeMillis: Long): Boolean =
    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= LocalDate.now()
  }
 }
 val state=rememberDatePickerState(initialSelectedDateMillis=date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(), selectableDates=selectableDates)
 OutlinedTextField(value=date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sin fecha",onValueChange={},readOnly=true,enabled=enabled,isError=error!=null,label={Text("Fecha de nacimiento")},supportingText={if(error!=null)Text(error)},modifier=Modifier.fillMaxWidth(),trailingIcon={TextButton(onClick={open=true},enabled=enabled){Text("Elegir")}})
 if(open) DatePickerDialog(onDismissRequest={open=false},confirmButton={TextButton(onClick={state.selectedDateMillis?.let{onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())};open=false}){Text("Aceptar")}},dismissButton={TextButton(onClick={open=false}){Text("Cancelar")}}){DatePicker(state=state)}
}
private fun String.toDateOrNull():LocalDate?=try{LocalDate.parse(this)}catch(_:Exception){null}

@Composable
private fun PlayerField(
    label: String, value: String, error: String?, enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = true, enabled = enabled, isError = error != null,
        supportingText = { if (error != null) Text(error) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun <T> ChoiceField(
    label: String, selected: T, options: List<T>, text: (T) -> String,
    enabled: Boolean, onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${text(selected)}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(text = { Text(text(item)) }, onClick = { expanded = false; onSelected(item) })
            }
        }
    }
}
