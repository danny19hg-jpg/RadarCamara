package com.example.radarcamera.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CoachEntity::class, PlayerEntity::class, SessionEntity::class, PitchEntity::class], version = 7, exportSchema = true)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun coaches(): CoachDao
    abstract fun players(): PlayerDao
    abstract fun sessions(): SessionDao
    abstract fun pitches(): PitchDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` TEXT NOT NULL, `playerId` TEXT NOT NULL, `sport` TEXT NOT NULL, `initialPitchType` TEXT NOT NULL, `currentPitchType` TEXT NOT NULL, `target` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `revision` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_playerId` ON `sessions` (`playerId`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `pitches` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `eventId` INTEGER NOT NULL, `number` INTEGER NOT NULL, `mph` REAL NOT NULL, `pitchType` TEXT NOT NULL, `receivedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pitches_sessionId_eventId` ON `pitches` (`sessionId`, `eventId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pitches_sessionId_number` ON `pitches` (`sessionId`, `number`)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sessions` ADD COLUMN `recordingEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pitches` ADD COLUMN `videoStatus` TEXT NOT NULL DEFAULT 'NOT_RECORDED'")
                db.execSQL("ALTER TABLE `pitches` ADD COLUMN `videoUri` TEXT")
                db.execSQL("ALTER TABLE `pitches` ADD COLUMN `videoReason` TEXT")
                db.execSQL("ALTER TABLE `pitches` ADD COLUMN `videoRawPath` TEXT")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `sessions` ADD COLUMN `discardedAt` INTEGER") } }
        val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `pitches` ADD COLUMN `videoRelativePath` TEXT"); db.execSQL("ALTER TABLE `pitches` ADD COLUMN `videoDisplayName` TEXT") } }
        val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `players` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''"); db.execSQL("ALTER TABLE `players` ADD COLUMN `teamAcademy` TEXT") } }
        fun create(context: Context, name: String = "radar-coaching.db"): RadarDatabase =
            Room.databaseBuilder(context.applicationContext, RadarDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                // Sin fallbackToDestructiveMigration: los cambios futuros requieren migración.
                .build()
    }
}
