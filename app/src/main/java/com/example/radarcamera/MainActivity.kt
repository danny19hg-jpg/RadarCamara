package com.example.radarcamera

import com.example.radarcamera.navigation.RadarRootApp
import com.example.radarcamera.camera.CameraVideoController
import com.example.radarcamera.camera.CameraPreviewVideo
import com.example.radarcamera.camera.ClipMetadata
import com.example.radarcamera.camera.PreRollClip
import com.example.radarcamera.camera.VentanaPreRoll
import com.example.radarcamera.camera.VideoOverlayExporter
import com.example.radarcamera.radar.CoachingEventCursor
import com.example.radarcamera.radar.CoachingEventDecision

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

private const val RADAR_STATUS_URL = "http://192.168.4.1/status"
private const val POLLING_MS = 250L
private const val TIEMPO_MOSTRAR_EVENTO_MS = 4500L
private const val PRE_ROLL_MS = 4000L
private const val POST_ROLL_MS = 1000L
private const val NANOSEGUNDOS_POR_MS = 1_000_000L

internal fun calcularVentanaPreRoll(
    posicionEventoMs: Long,
    duracionRawMs: Long,
    preRollMs: Long = PRE_ROLL_MS,
    postRollMs: Long = POST_ROLL_MS
): VentanaPreRoll {
    val inicio = (posicionEventoMs - preRollMs)
        .coerceAtLeast(0L)
    val fin = (posicionEventoMs + postRollMs)
        .coerceAtMost(duracionRawMs)
        .coerceAtLeast(inicio)

    return VentanaPreRoll(
        inicioMs = inicio,
        finMs = fin
    )
}

internal fun preRollDisponible(
    duracionActualMs: Long,
    preRollMs: Long = PRE_ROLL_MS
): Boolean {
    return duracionActualMs >= preRollMs
}

internal fun debeDetenerDespuesDelEvento(
    duracionActualMs: Long,
    posicionEventoMs: Long,
    postRollMs: Long = POST_ROLL_MS
): Boolean {
    return duracionActualMs >= posicionEventoMs + postRollMs
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RadarRootApp()
            }
        }
        }
    }

// ============================================================
// DATOS DEL RADAR
// ============================================================

data class RadarEstado(
    val conectado: Boolean = false,
    val sesionActiva: Boolean = false,
    val total: Int = 0,
    val ultimaVelocidad: Double = 0.0,
    val velocidadLive: Double = 0.0,
    val eventoLive: Long = 0L,
    val ultimoTipo: String = "",
    val ultimoNumero: Int = 0,
    val perfilNombre: String = ""
)

// POSTPROCESADO MEDIA3 + OVERLAY PERMANENTE
// APP
// ============================================================

@Composable
fun RadarCameraApp() {

    val context = LocalContext.current

    val cameraController = remember {
        CameraVideoController()
    }

    val overlayExporter = remember {
        VideoOverlayExporter(context.applicationContext)
    }

    var tienePermisoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permitido ->
        tienePermisoCamara = permitido
    }

    LaunchedEffect(Unit) {
        if (!tienePermisoCamara) {
            solicitarPermiso.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    var radar by remember {
        mutableStateOf(RadarEstado())
    }

    val cursorEventosLive = remember { CoachingEventCursor() }

    var eventoMostrado by remember {
        mutableStateOf(0L)
    }

    var velocidadMostrada by remember {
        mutableStateOf<Double?>(null)
    }

    var camaraLista by remember {
        mutableStateOf(false)
    }

    var preRollListo by remember {
        mutableStateOf(false)
    }

    var grabandoClip by remember {
        mutableStateOf(false)
    }

    var procesandoClip by remember {
        mutableStateOf(false)
    }

    var estadoClip by remember {
        mutableStateOf("INICIANDO CÁMARA...")
    }

    var ultimoClipGuardado by remember {
        mutableStateOf("")
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.release()
            overlayExporter.release()
        }
    }

    // Consulta continua al ESP32.
    LaunchedEffect(Unit) {
        while (true) {
            radar = leerEstadoRadar()
            delay(POLLING_MS)
        }
    }

    // ========================================================
    // EVENTO NUEVO DEL RADAR
    // ========================================================

    LaunchedEffect(
        radar.conectado,
        radar.eventoLive
    ) {

        if (!radar.conectado) {
            velocidadMostrada = null
            eventoMostrado = 0L
            return@LaunchedEffect
        }

        when (cursorEventosLive.onSuccessfulStatus(radar.eventoLive).decision) {
            CoachingEventDecision.NEW_EVENT -> {
            eventoMostrado = radar.eventoLive
            velocidadMostrada = radar.velocidadLive

            if (
                camaraLista &&
                preRollListo &&
                !grabandoClip
            ) {

                val metadata = crearClipMetadata(radar)

                val aceptado =
                    cameraController.registrarEvento(metadata)

                if (aceptado) {
                    grabandoClip = true
                    preRollListo = false
                    estadoClip =
                        "EVENTO CAPTURADO · GRABANDO 1 s DESPUÉS..."
                }
            }
            }
            CoachingEventDecision.BASELINE,
            CoachingEventDecision.DUPLICATE,
            CoachingEventDecision.COUNTER_RESET -> Unit
        }
    }

    // MPH grande durante algunos segundos.
    LaunchedEffect(eventoMostrado) {
        if (eventoMostrado > 0L) {
            delay(TIEMPO_MOSTRAR_EVENTO_MS)
            velocidadMostrada = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (tienePermisoCamara) {
            CameraPreviewVideo(
                controller = cameraController,
                onReady = { lista ->
                    camaraLista = lista

                    if (lista) {
                        val iniciado =
                            cameraController.iniciarPreRoll(
                                context = context,

                                onBufferStarted = {
                                    preRollListo = false
                                    if (!procesandoClip) {
                                        estadoClip =
                                            "CARGANDO PRE-ROLL 4 s..."
                                    }
                                },

                                onPreRollReady = {
                                    preRollListo = true
                                    grabandoClip = false
                                    if (!procesandoClip) {
                                        estadoClip =
                                            "✓ PRE-ROLL ACTIVO · ESPERANDO EVENTO"
                                    }
                                },

                                onClipReady = { clip ->
                                    grabandoClip = false
                                    procesandoClip = true
                                    estadoClip =
                                        "PROCESANDO 4 s ANTES + 1 s DESPUÉS..."

                                    overlayExporter.enqueue(
                                        rawFile = clip.rawFile,
                                        metadata = clip.metadata,
                                        ventana = clip.ventana,

                                        onProcessing = {
                                            procesandoClip = true
                                            estadoClip =
                                                "PROCESANDO MPH EN VIDEO..."
                                        },

                                        onSaved = { uri ->
                                            procesandoClip = false
                                            ultimoClipGuardado = uri
                                            estadoClip =
                                                if (preRollListo) {
                                                    "✓ VIDEO GUARDADO · PRE-ROLL ACTIVO"
                                                } else {
                                                    "✓ VIDEO GUARDADO · RECARGANDO PRE-ROLL"
                                                }
                                        },

                                        onError = { error ->
                                            procesandoClip = false
                                            estadoClip =
                                                "ERROR PROCESANDO: $error"
                                        }
                                    )
                                },

                                onError = { error ->
                                    preRollListo = false
                                    grabandoClip = false
                                    estadoClip = "ERROR PRE-ROLL: $error"
                                }
                            )

                        if (!iniciado) {
                            estadoClip =
                                "NO SE PUDO INICIAR EL PRE-ROLL"
                        }

                    } else {
                        preRollListo = false
                        estadoClip = "CÁMARA NO DISPONIBLE"
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Se necesita permiso para usar la cámara",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }

        // ====================================================
        // ENCABEZADO
        // ====================================================

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
                .background(
                    Color.Black.copy(alpha = 0.50f)
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = "RADAR CAMERA",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = BuildConfig.VERSION_NAME,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // ====================================================
        // VELOCIDAD EN VIVO
        // ====================================================

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(
                    Color.Black.copy(alpha = 0.45f)
                )
                .padding(
                    horizontal = 28.dp,
                    vertical = 18.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            val velocidadTexto =
                velocidadMostrada?.let {
                    String.format(
                        Locale.US,
                        "%.1f",
                        it
                    )
                } ?: "--.-"

            Text(
                text = velocidadTexto,
                color = Color.White,
                fontSize = 68.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "MPH",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            when {
                velocidadMostrada != null -> {
                    Text(
                        text = "LANZAMIENTO DETECTADO",
                        color = Color.Green,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier.padding(top = 8.dp)
                    )

                    if (
                        radar.sesionActiva &&
                        radar.total > 0
                    ) {
                        Text(
                            text =
                                "${radar.ultimoTipo} #${radar.ultimoNumero}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier.padding(top = 6.dp)
                        )
                    } else {
                        Text(
                            text = "Evento #$eventoMostrado",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier =
                                Modifier.padding(top = 5.dp)
                        )
                    }
                }

                !radar.conectado -> {
                    Text(
                        text = "Esperando radar...",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier =
                            Modifier.padding(top = 8.dp)
                    )
                }

                radar.sesionActiva -> {
                    Text(
                        text =
                            "SESIÓN ACTIVA · Esperando lanzamiento...",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier =
                            Modifier.padding(top = 8.dp)
                    )
                }

                else -> {
                    Text(
                        text =
                            "MODO RADAR · Esperando lanzamiento...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // ====================================================
        // ESTADO INFERIOR
        // ====================================================

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 25.dp)
                .background(
                    Color.Black.copy(alpha = 0.60f)
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 10.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = if (radar.conectado) {
                    "● RADAR CONECTADO"
                } else {
                    "● RADAR DESCONECTADO"
                },
                color = if (radar.conectado) {
                    Color.Green
                } else {
                    Color.Red
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            if (
                radar.conectado &&
                radar.sesionActiva
            ) {
                val nombre =
                    if (radar.perfilNombre.isBlank()) {
                        "Sesión activa"
                    } else {
                        radar.perfilNombre
                    }

                Text(
                    text = nombre,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier =
                        Modifier.padding(top = 3.dp)
                )

            } else if (radar.conectado) {
                Text(
                    text = "Modo Radar Live",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier =
                        Modifier.padding(top = 3.dp)
                )
            }

            Text(
                text = estadoClip,
                color = when {
                    grabandoClip -> Color.Red
                    procesandoClip -> Color.Yellow
                    else -> Color.White
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (ultimoClipGuardado.isNotBlank()) {
                Text(
                    text = "Movies/RadarCamera",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ============================================================
// CREAR SNAPSHOT DEL EVENTO PARA EL VIDEO
// ============================================================

private fun crearClipMetadata(
    radar: RadarEstado
): ClipMetadata {

    val fecha = SimpleDateFormat(
        "yyyyMMdd_HHmmss_SSS",
        Locale.US
    ).format(Date())

    val mph = String.format(
        Locale.US,
        "%.1f",
        radar.velocidadLive
    ).replace('.', '_')

    val nombreFinal =
        "Pitch_${fecha}_E${radar.eventoLive}_${mph}MPH.mp4"

    return ClipMetadata(
        evento = radar.eventoLive,
        velocidad = radar.velocidadLive,
        sesionActiva = radar.sesionActiva,
        tipo = radar.ultimoTipo,
        numeroTipo = radar.ultimoNumero,
        totalSesion = radar.total,
        perfilNombre = radar.perfilNombre,
        nombreArchivoFinal = nombreFinal
    )
}

// ============================================================
// ESTADO DEL ESP32
// ============================================================

private suspend fun leerEstadoRadar(): RadarEstado =
    withContext(Dispatchers.IO) {

        var conexion: HttpURLConnection? = null

        try {
            conexion = (
                    URL(RADAR_STATUS_URL).openConnection()
                            as HttpURLConnection
                    ).apply {

                    requestMethod = "GET"
                    connectTimeout = 800
                    readTimeout = 800
                    useCaches = false

                    setRequestProperty(
                        "Cache-Control",
                        "no-cache"
                    )
                }

            if (
                conexion.responseCode !=
                HttpURLConnection.HTTP_OK
            ) {
                return@withContext RadarEstado()
            }

            val texto = conexion.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

            val json = JSONObject(texto)

            RadarEstado(
                conectado = true,
                sesionActiva =
                    json.optBoolean("activa", false),
                total =
                    json.optInt("total", 0),
                ultimaVelocidad =
                    json.optDouble(
                        "ultimaVelocidad",
                        0.0
                    ),
                velocidadLive =
                    json.optDouble(
                        "velocidadLive",
                        0.0
                    ),
                eventoLive =
                    json.optLong(
                        "eventoLive",
                        0L
                    ),
                ultimoTipo =
                    json.optString(
                        "ultimoTipo",
                        ""
                    ),
                ultimoNumero =
                    json.optInt(
                        "ultimoNumero",
                        0
                    ),
                perfilNombre =
                    json.optString(
                        "perfilNombre",
                        ""
                    )
            )

        } catch (e: Exception) {
            RadarEstado()

        } finally {
            conexion?.disconnect()
        }
    }
