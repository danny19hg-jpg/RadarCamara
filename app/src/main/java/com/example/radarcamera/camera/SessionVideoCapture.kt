package com.example.radarcamera.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.radarcamera.domain.PitchType

data class SessionVideoRequest(val pitchId: String, val eventId: Long, val mph: Double, val pitchType: PitchType, val receivedAt: Long, val number: Int, val destination: VideoDestination)

enum class SessionVideoState(val label: String) {
    PREPARING("Preparando"), READY("Listo"), CAPTURING("Grabando"), EXPORTING("Exportando")
}

@Composable
internal fun SessionVideoCapture(request: SessionVideoRequest?, onConsumed: (String) -> Unit, onPending: (String, String) -> Unit, onAvailable: (String, String, VideoDestination) -> Unit, onUnavailable: (String, String) -> Unit, onFailed: (String, String) -> Unit, onStateChanged: (SessionVideoState) -> Unit) {
    val context = LocalContext.current
    val controller = remember { CameraVideoController() }
    val exporter = remember { VideoOverlayExporter(context.applicationContext) }
    var permitted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var ready by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it }
    LaunchedEffect(Unit) { if (!permitted) launcher.launch(Manifest.permission.CAMERA) }
    if (permitted) CameraPreviewVideo(controller, Modifier.clipToBounds()) { bound ->
        if (!bound) { ready = false; onStateChanged(SessionVideoState.PREPARING) } else controller.iniciarPreRoll(context, { ready = false; onStateChanged(SessionVideoState.PREPARING) }, { ready = true; onStateChanged(SessionVideoState.READY) }, { clip ->
            val pitchId = clip.metadata.pitchId
            if (pitchId != null) {
                onPending(pitchId, clip.rawFile.absolutePath)
                onStateChanged(SessionVideoState.EXPORTING)
                val destination = clip.metadata.destination
                if (destination != null) exporter.enqueue(clip.rawFile, clip.metadata, clip.ventana, {}, { uri -> onAvailable(pitchId, uri, destination) }, { error -> onFailed(pitchId, error) })
            }
        }, { error -> ready = false; onStateChanged(SessionVideoState.PREPARING); request?.pitchId?.let { onUnavailable(it, "CAMARA_PREPARANDO: $error") } })
    }
    LaunchedEffect(request?.pitchId) {
        val item = request ?: return@LaunchedEffect
        if (!permitted || !ready) onUnavailable(item.pitchId, "CAMARA_PREPARANDO") else {
            val metadata = ClipMetadata(pitchId = item.pitchId, evento = item.eventId, velocidad = item.mph, sesionActiva = true, tipo = item.pitchType.label, numeroTipo = item.number, totalSesion = item.number, perfilNombre = "", nombreArchivoFinal = item.destination.displayName, destination = item.destination)
            if (!controller.registrarEvento(metadata)) onUnavailable(item.pitchId, "CAMARA_PREPARANDO") else onStateChanged(SessionVideoState.CAPTURING)
        }
        onConsumed(item.pitchId)
    }
}
