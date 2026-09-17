package com.example.radarcamera.ui.players

import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.domain.Sport
import com.example.radarcamera.domain.ThrowingHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDetailViewModelTest {
    private val activePlayer = PlayerEntity(
        id = "player-1", name = "José Álvarez", birthDate = "2005-01-02", sport = Sport.BASEBALL,
        position = "Pitcher", throwingHand = ThrowingHand.RIGHT, heightCm = null, weightKg = null,
        createdAt = 1, updatedAt = 1
    )

    @Test
    fun searchFiltersNamesIgnoringCaseAndAccents() {
        val players = listOf(activePlayer, activePlayer.copy(id = "player-2", name = "María López"))

        assertEquals(listOf(activePlayer), filterPlayers(players, "jose"))
        assertEquals(listOf(activePlayer.copy(id = "player-2", name = "María López")), filterPlayers(players, "LOPEZ"))
    }

    @Test
    fun activePlayerWithOpenSessionExposesContinueOnly() {
        val state = PlayerDetailState.from(activePlayer, openSessionId = "session-1")

        assertEquals("session-1", state.openSessionId)
        assertTrue(state.canContinueSession)
        assertFalse(state.canStartSession)
    }

    @Test
    fun archivedPlayerDoesNotExposeNewSession() {
        val state = PlayerDetailState.from(activePlayer.copy(archivedAt = 10), openSessionId = null)

        assertFalse(state.canStartSession)
        assertFalse(state.canContinueSession)
        assertTrue(state.archived)
    }
}
