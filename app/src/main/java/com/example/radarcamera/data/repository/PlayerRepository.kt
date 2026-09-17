package com.example.radarcamera.data.repository

import androidx.room.withTransaction
import com.example.radarcamera.data.local.PlayerEntity
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.domain.PlayerDraft
import com.example.radarcamera.domain.parseOptionalDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class PlayerRepository(
    private val database: RadarDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val historyChecker: PlayerHistoryChecker = NoPersistedPlayerHistory
) {
    fun observe(archived: Boolean) = database.players().observe(archived)
    fun observePlayer(id: String) = database.players().observeById(id)
    fun observeOpenSession(playerId: String) = database.sessions().observeOpenForPlayer(playerId)
    suspend fun get(id: String) = database.players().get(id)

    suspend fun save(id: String?, draft: PlayerDraft): String {
        require(draft.errors(LocalDate.now(clock)).isEmpty()) { "Revisa los datos del jugador." }
        return database.withTransaction {
            val previous = id?.let { database.players().get(it) }
            check(id == null || previous != null) { "El jugador ya no está disponible." }
            val now = clock.millis()
            val player = PlayerEntity(
                id = previous?.id ?: UUID.randomUUID().toString(),
                name = draft.name.trim(),
                birthDate = requireNotNull(draft.birthDateOrNull()).toString(),
                sport = draft.sport,
                position = "Pitcher",
                throwingHand = draft.throwingHand,
                heightCm = parseOptionalDecimal(draft.heightCm),
                weightKg = parseOptionalDecimal(draft.weightKg),
                category = draft.category.trim(),
                teamAcademy = draft.teamAcademy.trim().ifBlank { null },
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
                revision = (previous?.revision ?: 0) + 1,
                archivedAt = previous?.archivedAt
            )
            database.players().save(player)
            player.id
        }
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        val now = clock.millis()
        check(database.players().setArchived(id, if (archived) now else null, now) == 1) {
            "El jugador ya no está disponible."
        }
    }

    suspend fun deletePermanently(id: String): PlayerDeletionResult = database.withTransaction {
        if (database.players().get(id) == null) return@withTransaction PlayerDeletionResult.NotFound
        // El verificador consulta el historial persistido dentro de esta misma transacción.
        val associatedHistoryCount = historyChecker.associatedHistoryCount(id)
        when (PlayerDeletionPolicy.evaluate(associatedHistoryCount)) {
            PlayerDeletionEligibility.HasHistory -> PlayerDeletionResult.HasHistory
            PlayerDeletionEligibility.Allowed -> if (database.players().delete(id) == 1) {
                PlayerDeletionResult.Deleted
            } else {
                PlayerDeletionResult.NotFound
            }
        }
    }
}
