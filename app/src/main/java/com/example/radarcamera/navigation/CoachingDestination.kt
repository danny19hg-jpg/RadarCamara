package com.example.radarcamera.navigation

sealed interface CoachingDestination {
    data object PlayerList : CoachingDestination
    data class PlayerDetail(val playerId: String) : CoachingDestination
    data class PlayerHistory(val playerId: String) : CoachingDestination
    data class Session(val sessionId: String?, val origin: SessionDetailOrigin, val playerId: String? = null) : CoachingDestination
    data class DeletedSessions(val playerId: String?) : CoachingDestination
    data object Settings : CoachingDestination
}
