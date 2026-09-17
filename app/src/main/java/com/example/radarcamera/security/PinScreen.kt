package com.example.radarcamera.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaPin(
    creandoPin: Boolean,
    pinStore: PinStore,
    onVolver: () -> Unit,
    onCorrecto: () -> Unit
) {
    // El PIN escrito nunca se serializa en Bundle ni SavedStateHandle.
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = busy) { }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (creandoPin) "Crear PIN" else "Acceso Coaching",
                style = MaterialTheme.typography.headlineMedium)
            Text(if (creandoPin) "Crea un PIN de 4 a 6 números." else "Ingresa el PIN del entrenador.")
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { pin = it; error = null } },
                label = { Text("PIN") }, enabled = !busy, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (creandoPin) OutlinedTextField(
                value = confirmation,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { confirmation = it; error = null } },
                label = { Text("Confirmar PIN") }, enabled = !busy, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !busy, modifier = Modifier.fillMaxWidth(),
                onClick = {
                    when {
                        pin.length !in 4..6 -> error = "El PIN debe tener entre 4 y 6 números."
                        creandoPin && pin != confirmation -> error = "Los PIN no coinciden."
                        else -> {
                            busy = true
                            val enteredPin = pin
                            scope.launch {
                                try {
                                    val accepted = withContext(Dispatchers.IO) {
                                        if (creandoPin) {
                                            check(!pinStore.tienePin()) { "Ya existe un PIN." }
                                            pinStore.guardarPin(enteredPin)
                                            true
                                        } else pinStore.verificarPin(enteredPin)
                                    }
                                    pin = ""
                                    confirmation = ""
                                    busy = false
                                    if (accepted) onCorrecto() else error = "PIN incorrecto."
                                } catch (e: CancellationException) { throw e }
                                catch (_: Exception) {
                                    busy = false
                                    pin = ""
                                    confirmation = ""
                                    error = "No se pudo verificar o guardar el PIN. Inténtalo nuevamente."
                                }
                            }
                        }
                    }
                }
            ) { Text(if (busy) "Comprobando…" else if (creandoPin) "Crear y entrar" else "Entrar") }
            OutlinedButton(onClick = onVolver, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        }
    }
}
