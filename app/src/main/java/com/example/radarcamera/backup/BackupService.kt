package com.example.radarcamera.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.radarcamera.BuildConfig
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.data.local.CoachEntity

internal object BackupCoachRestorePolicy {
    fun coachesToRestore(backupCoaches: List<CoachEntity>, currentLocalCoach: CoachEntity?): List<CoachEntity> =
        if (backupCoaches.isEmpty() && currentLocalCoach != null) listOf(currentLocalCoach) else backupCoaches
}

internal class BackupExporter(private val database: RadarDatabase) {
    suspend fun export(now: Long = System.currentTimeMillis()): BackupDocument = database.withTransaction {
        BackupDocument.create(BuildConfig.VERSION_NAME, database.coaches().getAll(), database.players().getAll(), database.sessions().getAll(), database.pitches().getAll(), now)
    }
}

internal class BackupValidator {
    fun validate(text: String): BackupDocument = BackupDocument.parse(text).also { it.validate() }
}

internal class BackupRestorer(private val context: Context, private val database: RadarDatabase) {
    suspend fun restore(document: BackupDocument) {
        document.validate()
        database.withTransaction {
            val currentLocalCoach = database.coaches().get()
            database.pitches().deleteAll()
            database.sessions().deleteAll()
            database.players().deleteAll()
            database.coaches().deleteAll()
            BackupCoachRestorePolicy.coachesToRestore(document.coaches, currentLocalCoach).forEach { database.coaches().save(it) }
            document.players.forEach { database.players().save(it) }
            document.sessions.forEach { database.sessions().save(it) }
            document.pitches.forEach { database.pitches().insert(it).also { result -> check(result != -1L) { "No se pudo restaurar un lanzamiento." } } }
        }
        VideoRelinker(context.contentResolver, database).relink(document.pitches)
    }
}
