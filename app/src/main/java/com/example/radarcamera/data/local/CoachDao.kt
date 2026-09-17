package com.example.radarcamera.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    @Query("SELECT * FROM coaches WHERE localProfile = 1 LIMIT 1")
    suspend fun get(): CoachEntity?
    @Query("SELECT * FROM coaches WHERE localProfile = 1 LIMIT 1")
    fun observe(): Flow<CoachEntity?>
    @Upsert
    suspend fun save(coach: CoachEntity)
    @Query("SELECT * FROM coaches ORDER BY id") suspend fun getAll(): List<CoachEntity>
    @Query("DELETE FROM coaches") suspend fun deleteAll()
}
