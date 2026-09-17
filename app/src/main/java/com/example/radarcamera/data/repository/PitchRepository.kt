package com.example.radarcamera.data.repository

import androidx.room.withTransaction
import com.example.radarcamera.data.local.*
import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.PitchVideoStatus
import java.util.UUID
import kotlinx.coroutines.delay
import java.io.File

class PitchRepository(private val db: RadarDatabase) {
    fun observe(sessionId:String)=db.pitches().observe(sessionId)
    fun observeIncludingDiscarded(sessionId:String)=db.pitches().observeIncludingDiscarded(sessionId)
    suspend fun getAllIncludingDiscarded(sessionId:String)=db.pitches().getAllIncludingDiscarded(sessionId)
    suspend fun record(id:String, sessionId:String,eventId:Long,mph:Double,type:PitchType,receivedAt:Long, recordingEnabled:Boolean):PitchEntity? = db.withTransaction {
        val number=(db.pitches().maxNumber(sessionId)?:0)+1
        val pitch = PitchEntity(id,sessionId,eventId,number,mph,type,receivedAt, if (recordingEnabled) PitchVideoStatus.PENDING else PitchVideoStatus.NOT_RECORDED)
        pitch.takeIf { db.pitches().insert(it) != -1L }
    }
    suspend fun updateVideo(pitchId:String, status:PitchVideoStatus, uri:String?=null, reason:String?=null, rawPath:String?=null, relativePath:String?=null, displayName:String?=null) {
        repeat(10) {
            if (db.pitches().updateVideo(pitchId, status, uri, reason, rawPath, relativePath, displayName, PitchVideoStatus.PENDING) == 1) return
            delay(25)
        }
    }
    suspend fun recoverInterruptedVideos() { db.pitches().pendingVideos().filter { it.videoRawPath != null && !File(it.videoRawPath).exists() }.forEach { updateVideo(it.id, PitchVideoStatus.FAILED, reason = "ARCHIVO_TEMPORAL_NO_DISPONIBLE") } }
}
