package com.example.radarcamera.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao interface PitchDao {
 @Query("SELECT pitches.* FROM pitches INNER JOIN sessions ON sessions.id=pitches.sessionId WHERE pitches.sessionId=:sessionId AND sessions.discardedAt IS NULL ORDER BY pitches.number") fun observe(sessionId:String): Flow<List<PitchEntity>>
 @Query("SELECT * FROM pitches WHERE sessionId=:sessionId ORDER BY number") suspend fun getAllIncludingDiscarded(sessionId:String):List<PitchEntity>
 @Query("SELECT * FROM pitches WHERE sessionId=:sessionId ORDER BY number") fun observeIncludingDiscarded(sessionId:String):Flow<List<PitchEntity>>
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insert(pitch:PitchEntity):Long
 @Query("SELECT MAX(number) FROM pitches WHERE sessionId=:sessionId") suspend fun maxNumber(sessionId:String):Int?
 @Query("UPDATE pitches SET videoStatus=:status, videoUri=:uri, videoReason=:reason, videoRawPath=:rawPath, videoRelativePath=:relativePath, videoDisplayName=:displayName WHERE id=:pitchId AND videoStatus=:expected") suspend fun updateVideo(pitchId:String, status:com.example.radarcamera.domain.PitchVideoStatus, uri:String?, reason:String?, rawPath:String?, relativePath:String?, displayName:String?, expected:com.example.radarcamera.domain.PitchVideoStatus):Int
 @Query("SELECT * FROM pitches WHERE videoStatus='PENDING'") suspend fun pendingVideos():List<PitchEntity>
 @Query("SELECT * FROM pitches ORDER BY id") suspend fun getAll():List<PitchEntity>
 @Query("DELETE FROM pitches") suspend fun deleteAll()
 @Query("DELETE FROM pitches WHERE sessionId=:sessionId") suspend fun deleteForSession(sessionId:String):Int
 @Query("UPDATE pitches SET videoStatus=:status, videoUri=:uri, videoReason=:reason WHERE id=:pitchId") suspend fun relinkVideo(pitchId:String, status:com.example.radarcamera.domain.PitchVideoStatus, uri:String?, reason:String?):Int
}
