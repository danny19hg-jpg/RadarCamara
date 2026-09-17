package com.example.radarcamera.data.repository

import androidx.room.withTransaction
import com.example.radarcamera.data.local.CoachEntity
import com.example.radarcamera.data.local.RadarDatabase
import java.util.UUID

class CoachRepository(private val database: RadarDatabase) {
    fun observe() = database.coaches().observe()
    suspend fun get() = database.coaches().get()

    suspend fun save(name: String, academy: String, country: String) {
        require(name.trim().isNotEmpty() && name.trim().length <= 100) {
            "Escribe un nombre de 1 a 100 caracteres."
        }
        require(academy.trim().length <= 120 && country.trim().length <= 80) {
            "Academia: hasta 120 caracteres. País: hasta 80."
        }
        database.withTransaction {
            val previous = database.coaches().get()
            val now = System.currentTimeMillis()
            database.coaches().save(CoachEntity(
                id = previous?.id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                academy = academy.trim().ifEmpty { null },
                country = country.trim().ifEmpty { null },
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
                revision = (previous?.revision ?: 0) + 1
            ))
        }
    }
}
