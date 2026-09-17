package com.example.radarcamera

import android.content.Context
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

private const val ACCESS_STATUS_URL =
    "http://192.168.4.1/status"

private const val ACCESS_POLLING_MS = 250L
private const val ACCESS_SHOW_SPEED_MS = 4500L

private data class EstadoLive(
    val conectado: Boolean = false,
    val velocidad: Double = 0.0,
    val evento: Long = 0L
)

// ============================================================
// RAÍZ DE LA APLICACIÓN
// ============================================================

// ============================================================
// PANTALLA PRINCIPAL
// ============================================================

@Composable
internal fun PantallaInicio(
    onLive: () -> Unit,
    onCoaching: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF071018)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "RADAR BASEBALL",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Radar + cámara",
                color = Color(0xFF9EADBA),
                fontSize = 17.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(
                modifier = Modifier.height(48.dp)
            )

            Button(
                onClick = onLive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RADAR LIVE",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Ver MPH sin iniciar sesión",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onCoaching,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "COACHING",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Perfil y jugadores locales",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(38.dp)
            )

            Text(
                text = BuildConfig.VERSION_NAME,
                color = Color(0xFF6F8495),
                fontSize = 13.sp
            )
        }
    }
}

// ============================================================
// MODO RADAR LIVE
// ============================================================

@Composable
internal fun PantallaLive(
    onVolver: () -> Unit
) {

    var guardarVideos by rememberSaveable {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (guardarVideos) {
            /*
             * Al activar el interruptor se usa la cámara.
             * Se conserva el pre-roll:
             * 4 segundos antes + 1 segundo después.
             */
            RadarCameraApp()
        } else {
            /*
             * Sin grabación no se enciende la cámara ni se genera
             * un archivo temporal grande. Solo muestra las MPH.
             */
            RadarLiveSimple()
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 10.dp
                )
                .background(
                    Color.Black.copy(alpha = 0.72f),
                    RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = onVolver
            ) {
                Text(
                    text = "←",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                text = "Guardar videos",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Switch(
                checked = guardarVideos,
                onCheckedChange = {
                    guardarVideos = it
                }
            )
        }
    }
}

// ============================================================
// LIVE SIN CÁMARA
// ============================================================

@Composable
private fun RadarLiveSimple() {

    var radar by remember {
        mutableStateOf(EstadoLive())
    }

    var ultimoEvento by remember {
        mutableStateOf<Long?>(null)
    }

    var velocidadMostrada by remember {
        mutableStateOf<Double?>(null)
    }

    var eventoMostrado by remember {
        mutableStateOf(0L)
    }

    LaunchedEffect(Unit) {
        while (true) {
            radar = leerRadarLive()
            delay(ACCESS_POLLING_MS)
        }
    }

    LaunchedEffect(
        radar.conectado,
        radar.evento
    ) {
        if (!radar.conectado) {
            ultimoEvento = null
            velocidadMostrada = null
            eventoMostrado = 0L
            return@LaunchedEffect
        }

        val anterior = ultimoEvento

        if (anterior == null) {
            ultimoEvento = radar.evento
            return@LaunchedEffect
        }

        if (radar.evento > anterior) {
            ultimoEvento = radar.evento
            eventoMostrado = radar.evento
            velocidadMostrada = radar.velocidad
        }
    }

    LaunchedEffect(eventoMostrado) {

        if (eventoMostrado <= 0L) {
            return@LaunchedEffect
        }

        val eventoEsperado = eventoMostrado

        delay(ACCESS_SHOW_SPEED_MS)

        if (eventoMostrado == eventoEsperado) {
            velocidadMostrada = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050A0E))
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RADAR LIVE",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sin sesión",
                color = Color(0xFF8EA1B0),
                fontSize = 15.sp
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
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
                fontSize = 86.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "MPH",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (velocidadMostrada != null) {
                    "LANZAMIENTO DETECTADO"
                } else {
                    "Esperando lanzamiento..."
                },
                color = if (velocidadMostrada != null) {
                    Color(0xFF35D06F)
                } else {
                    Color(0xFF9EADBA)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 15.dp)
            )
        }

        Text(
            text = if (radar.conectado) {
                "● RADAR CONECTADO"
            } else {
                "● RADAR DESCONECTADO"
            },
            color = if (radar.conectado) {
                Color(0xFF35D06F)
            } else {
                Color.Red
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )
    }
}

// ============================================================
// LECTURA DEL ESP32 PARA EL MODO LIVE
// ============================================================

private suspend fun leerRadarLive(): EstadoLive =
    withContext(Dispatchers.IO) {

        var conexion: HttpURLConnection? = null

        try {
            conexion = (
                    URL(ACCESS_STATUS_URL)
                        .openConnection() as HttpURLConnection
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
                return@withContext EstadoLive()
            }

            val texto = conexion.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

            val json = JSONObject(texto)

            EstadoLive(
                conectado = true,
                velocidad = json.optDouble(
                    "velocidadLive",
                    0.0
                ),
                evento = json.optLong(
                    "eventoLive",
                    0L
                )
            )

        } catch (_: Exception) {
            EstadoLive()

        } finally {
            conexion?.disconnect()
        }
    }
