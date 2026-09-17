package com.example.radarcamera.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationStackTest {
    @Test
    fun playerDetailBackReturnsToSamePlayerHistory() {
        val stack = NavigationStack(CoachingDestination.PlayerList)
        stack.push(CoachingDestination.PlayerDetail("player-1"))
        stack.push(CoachingDestination.PlayerHistory("player-1"))
        stack.push(CoachingDestination.Session("session-1", SessionDetailOrigin.NORMAL_HISTORY))

        assertEquals(CoachingDestination.PlayerHistory("player-1"), stack.pop())
        assertEquals(CoachingDestination.PlayerHistory("player-1"), stack.current)
    }

    @Test
    fun deletedSessionBackReturnsToTheSameDeletedList() {
        val stack = NavigationStack(CoachingDestination.PlayerList)
        stack.push(CoachingDestination.DeletedSessions("player-1"))
        stack.push(CoachingDestination.Session("session-1", SessionDetailOrigin.DELETED_SESSIONS))

        assertEquals(CoachingDestination.DeletedSessions("player-1"), stack.pop())
        assertEquals(CoachingDestination.DeletedSessions("player-1"), stack.current)
    }

    @Test
    fun replacingSessionDoesNotDuplicateDestination() {
        val stack = NavigationStack(CoachingDestination.PlayerList)
        stack.push(CoachingDestination.Session("session-1", SessionDetailOrigin.OPEN_SESSIONS))
        stack.replace(CoachingDestination.Session("session-2", SessionDetailOrigin.OPEN_SESSIONS))

        assertEquals(2, stack.entries.size)
        assertEquals(CoachingDestination.Session("session-2", SessionDetailOrigin.OPEN_SESSIONS), stack.current)
        assertEquals(CoachingDestination.PlayerList, stack.pop())
    }
}
