package com.example.radarcamera.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.Sport

@Entity(tableName = "sessions", foreignKeys = [ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("playerId")])
data class SessionEntity(@PrimaryKey val id: String, val playerId: String, val sport: Sport, val initialPitchType: PitchType, val currentPitchType: PitchType, val target: Int, val recordingEnabled: Boolean = false, val startedAt: Long, val endedAt: Long? = null, val discardedAt: Long? = null, val createdAt: Long, val updatedAt: Long, val revision: Long = 1)

data class SessionListItem(@Embedded val session: SessionEntity, val playerName: String)
