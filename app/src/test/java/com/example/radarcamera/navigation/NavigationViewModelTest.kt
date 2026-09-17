package com.example.radarcamera.navigation

import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.Sport
import com.example.radarcamera.domain.ThrowingHand
import com.example.radarcamera.ui.sessions.sessionDraftForPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationViewModelTest {
    @Test
    fun showSessionsFromPlayerDetailKeepsSelectedPlayerId() {
        val navigation = NavigationViewModel()

        navigation.unlock()
        navigation.showPlayerDetail("player-7")
        navigation.showSessionCreation("player-7")

        assertEquals(Route.SESSION_EDITOR, navigation.route)
        assertEquals("player-7", navigation.sessionCreationPlayerId)
        assertEquals("player-7", navigation.playerId)
    }

    @Test
    fun backFromSessionCreationReturnsToPlayerDetail() {
        val navigation = NavigationViewModel()

        navigation.unlock()
        navigation.showPlayerDetail("player-7")
        navigation.showSessionCreation("player-7")
        navigation.back()

        assertEquals(Route.PLAYER_DETAIL, navigation.route)
        assertEquals("player-7", navigation.playerId)
    }

    @Test
    fun draftForPlayerUsesProfileSportAndPitchType() {
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

        assertEquals(player.id, draft.playerId)
        assertEquals(Sport.SOFTBALL, draft.sport)
        assertEquals(PitchType.SOFTBALL_FASTBALL, draft.pitchType)
        assertTrue(draft.pitchType.sport == player.sport)
    }
}
