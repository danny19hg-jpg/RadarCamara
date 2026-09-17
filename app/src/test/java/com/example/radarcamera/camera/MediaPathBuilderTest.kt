package com.example.radarcamera.camera

import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.Sport
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPathBuilderTest {
    private val builder = MediaPathBuilder(ZoneId.of("America/Santiago"))

    @Test fun `builds session path from local start time and stable ids`() {
        val destination = builder.destination(
            playerName = "Danny Hernández",
            playerId = "a1b2c3d4-1111-2222-3333-444444444444",
            sessionId = "f6e7d8c9-1111-2222-3333-444444444444",
            sessionStartedAt = Instant.parse("2026-09-16T17:35:22Z").toEpochMilli(),
            sport = Sport.BASEBALL,
            pitchNumber = 1,
            pitchType = PitchType.BASEBALL_FASTBALL,
            mph = 78.44
        )

        assertEquals("Movies/RadarCamera/Danny_Hernandez__p_a1b2c3d4/2026-09-16_14-35-22_Baseball__s_f6e7d8c9", destination.relativePath)
        assertEquals("Lanzamiento_001_Fastball_78.4mph.mp4", destination.displayName)
    }

    @Test fun `sanitizes unsafe names and supplies fallbacks`() {
        val destination = builder.destination("../CON", "x", "y", 0, Sport.SOFTBALL, 4, null, Double.NaN)

        assertTrue(destination.relativePath.startsWith("Movies/RadarCamera/Jugador__p_x/"))
        assertEquals("Lanzamiento_004_Sin_tipo_0.0mph.mp4", destination.displayName)
    }

    @Test fun `same player names remain separated by player id`() {
        val a = builder.destination("Ana", "11111111-a", "session-a", 0, Sport.BASEBALL, 1, PitchType.BASEBALL_FASTBALL, 60.0)
        val b = builder.destination("Ana", "22222222-b", "session-b", 0, Sport.BASEBALL, 1, PitchType.BASEBALL_FASTBALL, 60.0)

        assertTrue(a.relativePath.contains("__p_11111111"))
        assertTrue(b.relativePath.contains("__p_22222222"))
    }
}
