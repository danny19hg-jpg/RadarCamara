package com.example.radarcamera.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.radarcamera.domain.PlayerDraft
import com.example.radarcamera.domain.Sport
import com.example.radarcamera.domain.ThrowingHand

@Entity(tableName = "players", indices = [Index(value = ["archivedAt", "name"])])
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val birthDate: String,
    val sport: Sport,
    val position: String,
    val throwingHand: ThrowingHand,
    val heightCm: Double?,
    val weightKg: Double?,
    val category: String = "",
    val teamAcademy: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 1,
    val archivedAt: Long? = null
) {
    fun toDraft() = PlayerDraft(
        name, birthDate, sport, position, throwingHand,
        heightCm?.toString().orEmpty(), weightKg?.toString().orEmpty(), category, teamAcademy.orEmpty()
    )
}
