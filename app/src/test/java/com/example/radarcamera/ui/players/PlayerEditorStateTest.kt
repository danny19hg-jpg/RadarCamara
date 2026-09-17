package com.example.radarcamera.ui.players

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerEditorStateTest {
    @Test fun cancellingDeletionClearsTheConfirmationWithoutMarkingThePlayerDeleted() {
        val state = PlayerEditorState(
            deleteConfirmation = PlayerDeletionConfirmation(playerId = "player-1", playerName = "Ana")
        )

        val cancelled = state.cancelDeletion()

        assertNull(cancelled.deleteConfirmation)
        assertNull(cancelled.deletedPlayerId)
    }

    @Test fun deletionConfirmationUsesThePersistedPlayerNameInsteadOfUnsavedEdits() {
        val state = PlayerEditorState(
            draft = com.example.radarcamera.domain.PlayerDraft(name = "Beto"),
            persistedPlayerName = "Ana"
        )

        val confirmation = state.deletionConfirmationFor("player-1")

        assertEquals("Ana", confirmation.playerName)
    }
}
