package com.example.radarcamera.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationAccessTest {
    @Test fun protectedDestinationsRequireUnlock() {
        Route.entries.filter { it.requiresUnlock }.forEach {
            val navigation = NavigationViewModel()
            navigation.go(it)
            assertEquals(Route.PIN, navigation.route)
        }
    }

    @Test fun newProcessModelNeverInheritsAuthorization() {
        val oldProcess = NavigationViewModel()
        oldProcess.unlock()
        oldProcess.go(Route.PROFILE)
        val newProcess = NavigationViewModel()
        newProcess.go(Route.PROFILE)
        assertEquals(Route.PIN, newProcess.route)
    }

    @Test fun lockRevokesAccessAndLiveNeedsNoPin() {
        val navigation = NavigationViewModel()
        navigation.unlock()
        assertEquals(Route.PLAYERS, navigation.route)
        navigation.lock()
        navigation.go(Route.PLAYERS)
        assertEquals(Route.PIN, navigation.route)
        navigation.go(Route.LIVE)
        assertEquals(Route.LIVE, navigation.route)
    }

    @Test fun backFromEditorReturnsToArchivedListWithoutLeavingCoaching() {
        val navigation = NavigationViewModel()
        navigation.unlock()
        navigation.editPlayer("test-player", true)
        navigation.back()
        assertEquals(Route.PLAYERS, navigation.route)
        assertTrue(navigation.archivedPlayers)
        navigation.back()
        assertEquals(Route.COACHING, navigation.route)
    }

    @Test fun sessionDetailBackRespectsHistoryOrigin() {
        val navigation = NavigationViewModel(); navigation.unlock()
        navigation.showHistory(); navigation.editSession("normal", SessionDetailOrigin.NORMAL_HISTORY); navigation.back()
        assertEquals(Route.HISTORY, navigation.route)
        navigation.showDiscardedSessions(); navigation.editSession("deleted", SessionDetailOrigin.DELETED_SESSIONS); navigation.back()
        assertEquals(Route.DISCARDED_SESSIONS, navigation.route)
    }
}
