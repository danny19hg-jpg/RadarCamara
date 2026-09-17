package com.example.radarcamera.camera

import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.Sport
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class VideoDestination(
    val relativePath: String,
    val displayName: String
)

internal class MediaPathBuilder(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun destination(
        playerName: String,
        playerId: String,
        sessionId: String,
        sessionStartedAt: Long,
        sport: Sport,
        pitchNumber: Int,
        pitchType: PitchType?,
        mph: Double
    ): VideoDestination {
        val player = safeSegment(playerName, "Jugador")
        val type = safeSegment(pitchType?.label.orEmpty(), "Sin_tipo")
        val playerShort = shortId(playerId)
        val sessionShort = shortId(sessionId)
        val sessionDate = Instant.ofEpochMilli(sessionStartedAt)
            .atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.US))
        val sportLabel = if (sport == Sport.BASEBALL) "Baseball" else "Softball"
        val speed = if (mph.isFinite()) mph else 0.0
        return VideoDestination(
            relativePath = "Movies/RadarCamera/${player}__p_${playerShort}/${sessionDate}_${sportLabel}__s_${sessionShort}",
            displayName = "Lanzamiento_${pitchNumber.coerceAtLeast(0).toString().padStart(3, '0')}_${type}_${String.format(Locale.US, "%.1f", speed)}mph.mp4"
        )
    }

    private fun shortId(value: String): String = value.filter { it.isLetterOrDigit() }.take(8).ifBlank { "sin_id" }

    private fun safeSegment(value: String, fallback: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim(' ', '.', '_')
            .take(48)
        return normalized.takeUnless { it.isBlank() || it.uppercase(Locale.US) in reservedNames } ?: fallback
    }

    private companion object {
        val reservedNames = setOf("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "LPT1", "LPT2", "LPT3")
    }
}
