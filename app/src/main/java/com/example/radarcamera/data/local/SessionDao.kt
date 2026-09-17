package com.example.radarcamera.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.radarcamera.domain.PitchType
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT sessions.*, players.name AS playerName FROM sessions INNER JOIN players ON players.id = sessions.playerId WHERE sessions.endedAt IS NULL ORDER BY sessions.startedAt DESC") fun observeOpen(): Flow<List<SessionListItem>>
    @Query("SELECT sessions.*, players.name AS playerName FROM sessions INNER JOIN players ON players.id = sessions.playerId WHERE sessions.endedAt IS NOT NULL AND sessions.discardedAt IS NULL ORDER BY sessions.endedAt DESC") fun observeFinished(): Flow<List<SessionListItem>>
    @Query("SELECT sessions.*, players.name AS playerName FROM sessions INNER JOIN players ON players.id = sessions.playerId WHERE sessions.playerId=:playerId AND sessions.endedAt IS NOT NULL AND sessions.discardedAt IS NULL ORDER BY sessions.endedAt DESC") fun observeFinishedForPlayer(playerId:String): Flow<List<SessionListItem>>
    @Query("SELECT sessions.*, players.name AS playerName FROM sessions INNER JOIN players ON players.id = sessions.playerId WHERE sessions.discardedAt IS NOT NULL ORDER BY sessions.discardedAt DESC") fun observeDiscarded(): Flow<List<SessionListItem>>
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun get(id: String): SessionEntity?
    @Query("SELECT id FROM sessions WHERE playerId = :playerId AND endedAt IS NULL AND discardedAt IS NULL ORDER BY createdAt DESC LIMIT 1") suspend fun getOpenForPlayer(playerId: String): String?
    @Query("SELECT id FROM sessions WHERE playerId = :playerId AND endedAt IS NULL AND discardedAt IS NULL ORDER BY createdAt DESC LIMIT 1") fun observeOpenForPlayer(playerId: String): Flow<String?>
    @Query("SELECT * FROM sessions WHERE id = :id AND discardedAt IS NULL") suspend fun getActive(id: String): SessionEntity?
    @Upsert suspend fun save(session: SessionEntity)
    @Query("UPDATE sessions SET currentPitchType = :pitchType, updatedAt = :now, revision = revision + 1 WHERE id = :id") suspend fun updateCurrentPitchType(id: String, pitchType: PitchType, now: Long): Int
    @Query("SELECT COUNT(*) FROM sessions WHERE playerId = :playerId") suspend fun countForPlayer(playerId: String): Int
    @Query("UPDATE sessions SET startedAt=:now, updatedAt=:now WHERE id=:id AND startedAt=0") suspend fun start(id:String, now:Long):Int
    @Query("UPDATE sessions SET endedAt=:now, updatedAt=:now WHERE id=:id AND endedAt IS NULL") suspend fun finish(id:String, now:Long):Int
    @Query("UPDATE sessions SET endedAt=:endedAt, updatedAt=:endedAt WHERE id=:id AND endedAt IS NULL") suspend fun finishIfMissing(id:String, endedAt:Long):Int
    @Query("UPDATE sessions SET discardedAt=:now, updatedAt=:now WHERE id=:id AND discardedAt IS NULL") suspend fun discard(id:String, now:Long):Int
    @Query("UPDATE sessions SET discardedAt=NULL, updatedAt=:now WHERE id=:id AND discardedAt IS NOT NULL") suspend fun restore(id:String, now:Long):Int
    @Query("SELECT * FROM sessions ORDER BY id") suspend fun getAll(): List<SessionEntity>
    @Query("DELETE FROM sessions") suspend fun deleteAll()
    @Query("DELETE FROM sessions WHERE id=:id AND discardedAt IS NOT NULL") suspend fun deleteDiscarded(id:String):Int
}
