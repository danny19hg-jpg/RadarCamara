package com.example.radarcamera.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "coaches", indices = [Index(value = ["localProfile"], unique = true)])
data class CoachEntity(
    @PrimaryKey val id: String,
    val name: String,
    val academy: String?,
    val country: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1,
    // Una instalación tiene un solo perfil local; el identificador sigue siendo UUID.
    val localProfile: Int = 1
)
