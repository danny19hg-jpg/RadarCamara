package com.example.radarcamera.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoachingPage(
    title: String,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 20.dp)) {
            TextButton(onClick = onBack, enabled = backEnabled) { Text("← Volver") }
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            val body = Modifier.weight(1f).fillMaxWidth()
            Column(
                if (scroll) body.verticalScroll(rememberScrollState()) else body,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun LoadingMessage() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Text("Cargando…")
    }
}

@Composable
fun ErrorMessage(message: String?, retry: (() -> Unit)? = null) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (retry != null) OutlinedButton(onClick = retry) { Text("Reintentar") }
    }
}
