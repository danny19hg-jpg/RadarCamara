package com.example.radarcamera.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.radarcamera.domain.PitchType
import com.example.radarcamera.domain.PitchVideoStatus

@Entity(tableName = "pitches", foreignKeys = [ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["sessionId", "eventId"], unique = true), Index(value = ["sessionId", "number"], unique = true)])
data class PitchEntity(@PrimaryKey val id: String, val sessionId: String, val eventId: Long, val number: Int, val mph: Double, val pitchType: PitchType, val receivedAt: Long, val videoStatus: PitchVideoStatus = PitchVideoStatus.NOT_RECORDED, val videoUri: String? = null, val videoReason: String? = null, val videoRawPath: String? = null, val videoRelativePath: String? = null, val videoDisplayName: String? = null)
