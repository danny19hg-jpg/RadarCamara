package com.example.radarcamera.ui.sessions

import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.domain.Sport
import com.example.radarcamera.domain.ThrowingHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionEditorViewModelRegressionTest {
    @Test
    fun creationSummaryUsesPlayerNameAndSport() {
        val summary = sessionCreationSummary("Ana García", Sport.SOFTBALL)
        assertEquals("Ana García · Softbol", summary)
    }

    @Test
    fun creationValidationBlocksMissingArchivedAndAlreadyOpenPlayer() {
        val validPlayer = PlayerEntity(
            id = "p-1",
            name = "Ana",
            birthDate = "2010-01-01",
            sport = Sport.SOFTBALL,
            position = "Pitcher",
            throwingHand = ThrowingHand.RIGHT,
            heightCm = null,
            weightKg = null,
            createdAt = 1L,
            updatedAt = 1L
        )

        assertEquals("No se encontró el jugador seleccionado.", sessionCreationError(null, false))
        assertEquals("Este jugador está archivado y no puede abrir una sesión.", sessionCreationError(validPlayer.copy(archivedAt = 99L), false))
        assertEquals("Ya existe una sesión abierta para este jugador.", sessionCreationError(validPlayer, true))
        assertNull(sessionCreationError(validPlayer, false))
    }

    @Test
    fun draftUsesProfileSportAndPitchTypeForSoftballPlayer() {
        val player = PlayerEntity(
            id = "softball-player",
            name = "Ana",
            birthDate = "2010-01-01",
            sport = Sport.SOFTBALL,
            position = "Pitcher",
            throwingHand = ThrowingHand.RIGHT,
            heightCm = null,
            weightKg = null,
            createdAt = 1L,
            updatedAt = 1L
        )

        val draft = sessionDraftForPlayer(player)

        assertEquals("softball-player", draft.playerId)
        assertEquals(Sport.SOFTBALL, draft.sport)
        assertEquals(com.example.radarcamera.domain.PitchType.SOFTBALL_FASTBALL, draft.pitchType)
    }
}
