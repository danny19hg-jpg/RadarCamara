package com.example.radarcamera.camera

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.radarcamera.calcularVentanaPreRoll
import com.example.radarcamera.debeDetenerDespuesDelEvento
import com.example.radarcamera.preRollDisponible
import java.io.File

internal data class ClipMetadata(
    val pitchId: String? = null,
    val evento: Long,
    val velocidad: Double,
    val sesionActiva: Boolean,
    val tipo: String,
    val numeroTipo: Int,
    val totalSesion: Int,
    val perfilNombre: String,
    val nombreArchivoFinal: String,
    val destination: VideoDestination? = null
)

internal data class PreRollClip(
    val rawFile: File,
    val metadata: ClipMetadata,
    val ventana: VentanaPreRoll
)

// ============================================================
// CÃƒÂMARA + GRABACIÃƒâ€œN RAW
// ============================================================

internal class CameraVideoController {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var detencionSolicitada = false
    private var liberado = false
    private var contexto: Context? = null
    private var duracionGrabadaNanos = 0L
    private var instanteUltimoEstadoNanos = 0L
    private var preRollNotificado = false
    private var eventoPendiente: Pair<ClipMetadata, Long>? = null

    private var onBufferStarted: () -> Unit = {}
    private var onPreRollReady: () -> Unit = {}
    private var onClipReady: (PreRollClip) -> Unit = {}
    private var onError: (String) -> Unit = {}

    fun bindCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onReady: (Boolean) -> Unit
    ) {
        val context = previewView.context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.FHD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                        )
                    )
                    .build()
                val capture = VideoCapture.withOutput(recorder)

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )

                videoCapture = capture
                onReady(true)

            } catch (e: Exception) {
                e.printStackTrace()
                videoCapture = null
                onReady(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun iniciarPreRoll(
        context: Context,
        onBufferStarted: () -> Unit,
        onPreRollReady: () -> Unit,
        onClipReady: (PreRollClip) -> Unit,
        onError: (String) -> Unit
    ): Boolean {

        this.contexto = context.applicationContext
        this.onBufferStarted = onBufferStarted
        this.onPreRollReady = onPreRollReady
        this.onClipReady = onClipReady
        this.onError = onError
        liberado = false

        if (recording != null) return true

        return iniciarNuevoBuffer()
    }

    fun registrarEvento(metadata: ClipMetadata): Boolean {
        if (recording == null || eventoPendiente != null) {
            return false
        }

        val posicionEventoMs = duracionEstimadaMs()

        if (!preRollDisponible(posicionEventoMs)) {
            return false
        }

        eventoPendiente = metadata to posicionEventoMs
        return true
    }

    private fun duracionEstimadaMs(): Long {
        if (instanteUltimoEstadoNanos == 0L) {
            return duracionGrabadaNanos / 1_000_000L
        }

        val transcurridoDesdeEstadoNanos =
            (SystemClock.elapsedRealtimeNanos() -
                    instanteUltimoEstadoNanos).coerceAtLeast(0L)

        return (
                duracionGrabadaNanos +
                        transcurridoDesdeEstadoNanos
                ) / 1_000_000L
    }

    private fun iniciarNuevoBuffer(): Boolean {

        if (liberado || recording != null) return false

        val context = contexto ?: return false

        val capture = videoCapture ?: return false

        val rawDir = File(
            context.cacheDir,
            "radar_raw"
        ).apply {
            mkdirs()
        }

        val rawFile = File(
            rawDir,
            "BUFFER_${System.currentTimeMillis()}.mp4"
        )

        if (rawFile.exists()) rawFile.delete()

        return try {
            val outputOptions = FileOutputOptions.Builder(rawFile)
                .build()

            val pendingRecording = capture.output.prepareRecording(
                context,
                outputOptions
            )

            detencionSolicitada = false
            duracionGrabadaNanos = 0L
            instanteUltimoEstadoNanos = 0L
            preRollNotificado = false
            eventoPendiente = null

            recording = pendingRecording.start(
                ContextCompat.getMainExecutor(context)
            ) { event ->

                when (event) {
                    is VideoRecordEvent.Start -> {
                        instanteUltimoEstadoNanos =
                            SystemClock.elapsedRealtimeNanos()
                        onBufferStarted()
                    }

                    is VideoRecordEvent.Status -> {
                        duracionGrabadaNanos =
                            event.recordingStats.recordedDurationNanos
                        instanteUltimoEstadoNanos =
                            SystemClock.elapsedRealtimeNanos()

                        val duracionActualMs =
                            duracionGrabadaNanos / 1_000_000L

                        if (
                            !preRollNotificado &&
                            preRollDisponible(duracionActualMs)
                        ) {
                            preRollNotificado = true
                            onPreRollReady()
                        }

                        val posicionEventoMs =
                            eventoPendiente?.second

                        if (
                            !detencionSolicitada &&
                            posicionEventoMs != null &&
                            debeDetenerDespuesDelEvento(
                                duracionActualMs,
                                posicionEventoMs
                            )
                        ) {
                            detencionSolicitada = true
                            recording?.stop()
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        val eventoCapturado = eventoPendiente
                        val duracionFinalMs =
                            event.recordingStats.recordedDurationNanos /
                                    1_000_000L

                        recording = null
                        detencionSolicitada = false
                        eventoPendiente = null
                        preRollNotificado = false

                        if (event.hasError()) {
                            rawFile.delete()
                            this.onError(
                                "Error de video: ${event.error}"
                            )
                            iniciarNuevoBuffer()

                        } else if (eventoCapturado != null) {
                            val ventana = calcularVentanaPreRoll(
                                posicionEventoMs = eventoCapturado.second,
                                duracionRawMs = duracionFinalMs
                            )

                            iniciarNuevoBuffer()

                            onClipReady(
                                PreRollClip(
                                    rawFile = rawFile,
                                    metadata = eventoCapturado.first,
                                    ventana = ventana
                                )
                            )

                        } else {
                            rawFile.delete()
                        }
                    }
                }
            }

            true

        } catch (e: Exception) {
            recording = null
            detencionSolicitada = false
            eventoPendiente = null
            rawFile.delete()
            this.onError(
                e.message ?: "No se pudo iniciar el clip"
            )
            false
        }
    }

    fun release() {
        liberado = true
        detencionSolicitada = true
        eventoPendiente = null
        recording?.stop()
        recording = null
        videoCapture = null
    }
}

// ============================================================
