package com.example.radarcamera.data.repository

import androidx.room.withTransaction
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.data.local.SessionEntity
import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.SessionDraft
import java.time.Clock
import java.util.UUID

class SessionRepository(private val database: RadarDatabase, private val clock: Clock = Clock.systemDefaultZone()) {
    fun observeOpen() = database.sessions().observeOpen()
    fun observeFinished(playerId: String? = null) = if (playerId == null) database.sessions().observeFinished() else database.sessions().observeFinishedForPlayer(playerId)
    fun observeDiscarded() = database.sessions().observeDiscarded()
    suspend fun get(id: String) = database.sessions().get(id)
    suspend fun getActive(id: String) = database.sessions().getActive(id)
    suspend fun create(draft: SessionDraft): String {
        require(draft.errors().isEmpty()) { "Revisa la configuración de la sesión." }
        return database.withTransaction {
            val player = database.players().get(draft.playerId)
            check(player != null) { "No se encontró el jugador seleccionado." }
            check(player.archivedAt == null) { "Este jugador está archivado y no puede abrir una sesión." }
            check(database.sessions().getOpenForPlayer(player.id) == null) { "Ya existe una sesión abierta para este jugador." }
            val now = clock.millis()
            val session = SessionEntity(UUID.randomUUID().toString(), player.id, player.sport, draft.pitchType, draft.pitchType, 0, draft.recordingEnabled, 0, createdAt = now, updatedAt = now)
            database.sessions().save(session)
            session.id
        }
    }
    suspend fun start(id:String) = database.withTransaction {
        val session = database.sessions().get(id) ?: error("La sesión ya no está disponible.")
        check(session.endedAt == null) { "La sesión ya finalizó." }
        if (session.startedAt == 0L) {
            check(database.sessions().start(id, clock.millis()) == 1) { "La sesión no se puede iniciar." }
        }
    }
    suspend fun finish(id:String) { check(database.sessions().finish(id, clock.millis()) == 1) { "La sesión no se puede finalizar." } }
    suspend fun discard(id:String) = database.withTransaction { val now=clock.millis(); val session=database.sessions().get(id) ?: error("La sesión no está disponible."); if(session.endedAt==null) database.sessions().finishIfMissing(id, now); check(database.sessions().discard(id, now)==1){"La sesión no se puede eliminar."} }
    suspend fun restore(id:String) = database.withTransaction { val session=database.sessions().get(id) ?: error("La sesión no está disponible."); val endedAt=session.endedAt ?: requireNotNull(session.discardedAt){"La sesión no está descartada."}; database.sessions().finishIfMissing(id, endedAt); check(database.sessions().restore(id, clock.millis())==1){"La sesión no se puede restaurar."} }
    suspend fun permanentlyDeleteDiscarded(id:String) = database.withTransaction {
        val session = database.sessions().get(id) ?: error("La sesión ya no está disponible.")
        check(session.discardedAt != null) { "Solo se pueden eliminar definitivamente sesiones descartadas." }
        database.pitches().deleteForSession(id)
        check(database.sessions().deleteDiscarded(id) == 1) { "La sesión no se puede eliminar definitivamente." }
    }
    suspend fun changePitchType(id: String, pitchType: PitchType) = database.withTransaction {
        val session = database.sessions().get(id) ?: error("La sesión ya no está disponible.")
        require(pitchType.sport == session.sport) { "El tipo no corresponde al deporte de la sesión." }
        check(database.sessions().updateCurrentPitchType(id, pitchType, clock.millis()) == 1)
    }
}
