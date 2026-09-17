package com.example.radarcamera.data.repository

enum class PlayerDeletionEligibility {
    Allowed,
    HasHistory
}

object PlayerDeletionPolicy {
    fun evaluate(associatedHistoryCount: Int): PlayerDeletionEligibility =
        if (associatedHistoryCount == 0) PlayerDeletionEligibility.Allowed
        else PlayerDeletionEligibility.HasHistory
}

fun interface PlayerHistoryChecker {
    suspend fun associatedHistoryCount(playerId: String): Int
}

object NoPersistedPlayerHistory : PlayerHistoryChecker {
    override suspend fun associatedHistoryCount(playerId: String): Int = 0
}

class SessionPlayerHistoryChecker(private val database: com.example.radarcamera.data.local.RadarDatabase) : PlayerHistoryChecker {
    override suspend fun associatedHistoryCount(playerId: String): Int = database.sessions().countForPlayer(playerId)
}

sealed interface PlayerDeletionResult {
    data object Deleted : PlayerDeletionResult
    data object HasHistory : PlayerDeletionResult
    data object NotFound : PlayerDeletionResult
}
