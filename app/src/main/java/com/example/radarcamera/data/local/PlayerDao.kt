package com.example.radarcamera.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE (:archived = 1 AND archivedAt IS NOT NULL) OR (:archived = 0 AND archivedAt IS NULL) ORDER BY name COLLATE NOCASE, id")
    fun observe(archived: Boolean): Flow<List<PlayerEntity>>
    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun get(id: String): PlayerEntity?
    @Query("SELECT * FROM players WHERE id = :id")
    fun observeById(id: String): Flow<PlayerEntity?>
    @Upsert
    suspend fun save(player: PlayerEntity)
    @Query("UPDATE players SET archivedAt = :archivedAt, updatedAt = :now, revision = revision + 1 WHERE id = :id")
    suspend fun setArchived(id: String, archivedAt: Long?, now: Long): Int
    @Query("DELETE FROM players WHERE id = :id")
    suspend fun delete(id: String): Int
    @Query("SELECT * FROM players ORDER BY id") suspend fun getAll(): List<PlayerEntity>
    @Query("DELETE FROM players") suspend fun deleteAll()
}
