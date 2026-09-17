package com.example.radarcamera.camera

import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.*
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import java.io.File
import java.util.ArrayDeque
import java.util.Locale

// ============================================================

internal data class VentanaPreRoll(
    val inicioMs: Long,
    val finMs: Long
) {
    val duracionMs: Long
        get() = finMs - inicioMs
}

@OptIn(UnstableApi::class)
internal class VideoOverlayExporter(
    private val context: Context
) {

    private data class ExportJob(
        val rawFile: File,
        val metadata: ClipMetadata,
        val ventana: VentanaPreRoll,
        val onProcessing: () -> Unit,
        val onSaved: (String) -> Unit,
        val onError: (String) -> Unit
    )

    private val queue = ArrayDeque<ExportJob>()
    private val ioScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var processing = false
    private var currentTransformer: Transformer? = null
    private var currentBitmap: Bitmap? = null

    fun enqueue(
        rawFile: File,
        metadata: ClipMetadata,
        ventana: VentanaPreRoll,
        onProcessing: () -> Unit,
        onSaved: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        queue.addLast(
            ExportJob(
                rawFile = rawFile,
                metadata = metadata,
                ventana = ventana,
                onProcessing = onProcessing,
                onSaved = onSaved,
                onError = onError
            )
        )

        if (!processing) {
            processNext()
        }
    }

    private fun processNext() {
        val job = queue.pollFirst()

        if (job == null) {
            processing = false
            return
        }

        processing = true
        job.onProcessing()

        val processedDir = File(
            context.cacheDir,
            "radar_processed"
        ).apply {
            mkdirs()
        }

        val processedFile = File(
            processedDir,
            "PROC_${job.metadata.nombreArchivoFinal}"
        )

        if (processedFile.exists()) processedFile.delete()

        try {
            val overlayBitmap = crearOverlayBitmap(job.metadata)
            currentBitmap = overlayBitmap

            val overlaySettings = StaticOverlaySettings.Builder()
                .setOverlayFrameAnchor(-1f, -1f)
                .setBackgroundFrameAnchor(-0.92f, -0.84f)
                .build()

            val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(
                overlayBitmap,
                overlaySettings
            )

            val overlayEffect = OverlayEffect(
                listOf(bitmapOverlay)
            )

            val effects = Effects(
                emptyList(),
                listOf(overlayEffect)
            )

            val inputMediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(job.rawFile))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(job.ventana.inicioMs)
                        .setEndPositionMs(job.ventana.finMs)
                        .build()
                )
                .build()

            val editedMediaItem = EditedMediaItem.Builder(
                inputMediaItem
            )
                .setEffects(effects)
                .build()

            val transformer = Transformer.Builder(context)
                .addListener(
                    object : Transformer.Listener {

                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            currentTransformer = null

                            ioScope.launch {
                                try {
                                    val finalUri = publicarVideoFinal(processedFile, job.metadata.destination ?: VideoDestination("Movies/RadarCamera", job.metadata.nombreArchivoFinal))

                                    job.rawFile.delete()
                                    processedFile.delete()

                                    withContext(Dispatchers.Main) {
                                        limpiarBitmapActual()
                                        job.onSaved(finalUri)
                                        terminarJob()
                                    }

                                } catch (e: Exception) {
                                    job.rawFile.delete()
                                    processedFile.delete()

                                    withContext(Dispatchers.Main) {
                                        limpiarBitmapActual()
                                        job.onError(
                                            e.message
                                                ?: "No se pudo guardar el video final"
                                        )
                                        terminarJob()
                                    }
                                }
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            currentTransformer = null
                            job.rawFile.delete()
                            processedFile.delete()
                            limpiarBitmapActual()

                            job.onError(
                                exportException.message
                                    ?: "Error procesando overlay"
                            )

                            terminarJob()
                        }
                    }
                )
                .build()

            currentTransformer = transformer

            transformer.start(
                editedMediaItem,
                processedFile.absolutePath
            )

        } catch (e: Exception) {
            job.rawFile.delete()
            processedFile.delete()
            limpiarBitmapActual()

            job.onError(
                e.message ?: "No se pudo iniciar el postprocesado"
            )

            terminarJob()
        }
    }

    private fun terminarJob() {
        processing = false
        processNext()
    }

    private fun limpiarBitmapActual() {
        currentBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        currentBitmap = null
    }

    private fun crearOverlayBitmap(
        metadata: ClipMetadata
    ): Bitmap {

        val width = 980
        val height = 300

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = AndroidColor.argb(
                180,
                0,
                0,
                0
            )
        }

        val fondo = RectF(
            0f,
            0f,
            width.toFloat(),
            height.toFloat()
        )

        canvas.drawRoundRect(
            fondo,
            34f,
            34f,
            backgroundPaint
        )

        val textoVelocidad = String.format(
            Locale.US,
            "%.1f MPH",
            metadata.velocidad
        )

        val lineaSecundaria = if (
            metadata.sesionActiva &&
            metadata.numeroTipo > 0 &&
            metadata.tipo.isNotBlank()
        ) {
            "${metadata.tipo} #${metadata.numeroTipo}"
        } else {
            "EVENTO #${metadata.evento}"
        }

        val mphPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = AndroidColor.WHITE
            textSize = 118f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        val secondaryPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = AndroidColor.WHITE
            textSize = 52f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        canvas.drawText(
            textoVelocidad,
            42f,
            135f,
            mphPaint
        )

        canvas.drawText(
            lineaSecundaria,
            46f,
            220f,
            secondaryPaint
        )

        if (
            metadata.sesionActiva &&
            metadata.perfilNombre.isNotBlank()
        ) {
            val playerPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = AndroidColor.LTGRAY
                textSize = 34f
                typeface = Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.NORMAL
                )
            }

            canvas.drawText(
                metadata.perfilNombre,
                48f,
                268f,
                playerPaint
            )
        }

        return bitmap
    }

    private fun publicarVideoFinal(
        processedFile: File,
        destination: VideoDestination
    ): String {

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            publicarEnMediaStore(
                processedFile,
                destination
            )
        } else {
            publicarFallbackPreQ(
                processedFile,
                destination.displayName
            )
        }
    }

    private fun publicarEnMediaStore(
        processedFile: File,
        destination: VideoDestination
    ): String {

        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                destination.displayName
            )
            put(
                MediaStore.Video.Media.MIME_TYPE,
                "video/mp4"
            )
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                destination.relativePath
            )
            put(
                MediaStore.Video.Media.IS_PENDING,
                1
            )
        }

        val uri = resolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IllegalStateException(
            "MediaStore no creÃƒÂ³ el archivo final"
        )

        try {
            resolver.openOutputStream(uri)?.use { salida ->
                processedFile.inputStream().use { entrada ->
                    entrada.copyTo(salida)
                }
            } ?: throw IllegalStateException(
                "No se pudo abrir el archivo final"
            )

            val listo = ContentValues().apply {
                put(
                    MediaStore.Video.Media.IS_PENDING,
                    0
                )
            }

            resolver.update(
                uri,
                listo,
                null,
                null
            )

            return uri.toString()

        } catch (e: Exception) {
            resolver.delete(
                uri,
                null,
                null
            )
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun publicarFallbackPreQ(
        processedFile: File,
        nombreArchivo: String
    ): String {

        val carpeta = File(
            context.getExternalFilesDir(
                Environment.DIRECTORY_MOVIES
            ),
            "RadarCamera"
        ).apply {
            mkdirs()
        }

        val finalFile = File(
            carpeta,
            nombreArchivo
        )

        processedFile.copyTo(
            finalFile,
            overwrite = true
        )

        MediaScannerConnection.scanFile(
            context,
            arrayOf(finalFile.absolutePath),
            arrayOf("video/mp4"),
            null
        )

        return Uri.fromFile(finalFile).toString()
    }

    fun release() {
        try {
            currentTransformer?.cancel()
        } catch (_: Exception) {
        }

        currentTransformer = null
        limpiarBitmapActual()

        while (queue.isNotEmpty()) {
            queue.pollFirst()?.rawFile?.delete()
        }

        ioScope.cancel()
    }
}

// ============================================================
