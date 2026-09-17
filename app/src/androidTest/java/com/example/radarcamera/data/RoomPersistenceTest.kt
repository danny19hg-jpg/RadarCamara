package com.example.radarcamera.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.data.repository.CoachRepository
import com.example.radarcamera.data.repository.PlayerDeletionResult
import com.example.radarcamera.data.repository.PlayerHistoryChecker
import com.example.radarcamera.data.repository.PlayerRepository
import com.example.radarcamera.data.repository.PitchRepository
import com.example.radarcamera.data.repository.SessionRepository
import com.example.radarcamera.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private lateinit var context: Context
    private lateinit var database: RadarDatabase
    // Base exclusiva de la prueba: nunca utiliza radar-coaching.db.
    private val databaseName = "room-test-${UUID.randomUUID()}.db"

    @Before fun open() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = RadarDatabase.create(context, databaseName)
    }

    @After fun close() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun savesEditsArchivesAndReopensWithoutLosingData() = runBlocking {
        val coaches = CoachRepository(database)
        coaches.save("Danny", "Academia", "Chile")
        val coachId = coaches.get()!!.id
        coaches.save("Danny Hernández", "", "")
        assertEquals(coachId, coaches.get()!!.id)
        assertNull(coaches.get()!!.academy)

        val players = PlayerRepository(database)
        val id = players.save(null, PlayerDraft(
            name = "Ana", birthDate = "2010-05-20", sport = Sport.SOFTBALL,
            position = "Pitcher", throwingHand = ThrowingHand.LEFT,
            heightCm = "170,5", weightKg = "65"
        ))
        UUID.fromString(id)
        val created = players.get(id)!!.createdAt
        players.setArchived(id, true)
        assertTrue(players.observe(false).first().isEmpty())
        assertEquals(id, players.observe(true).first().single().id)
        players.save(id, players.get(id)!!.toDraft().copy(name = "Ana editada"))
        assertNotNull(players.get(id)!!.archivedAt)
        database.close()
        database = RadarDatabase.create(context, databaseName)

        val reopened = PlayerRepository(database)
        val saved = reopened.get(id)!!
        assertEquals("Ana editada", saved.name)
        assertEquals(created, saved.createdAt)
        assertEquals(Sport.SOFTBALL, saved.sport)
        assertEquals(ThrowingHand.LEFT, saved.throwingHand)
        assertEquals(170.5, saved.heightCm!!, 0.0)
        assertEquals("Danny Hernández", CoachRepository(database).get()!!.name)
        reopened.setArchived(id, false)
        assertEquals(id, reopened.observe(false).first().single().id)
        assertTrue(reopened.observe(true).first().isEmpty())
    }

    @Test fun rejectsInvalidPlayerWithoutWriting() = runBlocking {
        val players = PlayerRepository(database)
        try {
            players.save(null, PlayerDraft())
            fail("Debe rechazar un jugador inválido")
        } catch (_: IllegalArgumentException) {
            assertTrue(players.observe(false).first().isEmpty())
        }
    }

    @Test fun permanentlyDeletesAnArchivedPlayerWithoutHistory() = runBlocking {
        val players = PlayerRepository(database)
        val id = players.save(null, PlayerDraft(name = "Ana", birthDate = "2010-05-20", position = "Pitcher"))
        players.setArchived(id, true)

        assertEquals(PlayerDeletionResult.Deleted, players.deletePermanently(id))
        assertNull(players.get(id))
        assertTrue(players.observe(false).first().isEmpty())
        assertTrue(players.observe(true).first().isEmpty())
    }

    @Test fun keepsPlayerWhenTheDataLayerReportsAssociatedHistory() = runBlocking {
        val players = PlayerRepository(database, historyChecker = PlayerHistoryChecker { 1 })
        val id = players.save(null, PlayerDraft(name = "Ana", birthDate = "2010-05-20", position = "Pitcher"))

        assertEquals(PlayerDeletionResult.HasHistory, players.deletePermanently(id))
        assertNotNull(players.get(id))
    }

    @Test fun savesRecoversAndChangesTheCurrentPitchTypeOfASession() = runBlocking {
        val players = PlayerRepository(database)
        val playerId = players.save(null, PlayerDraft(name = "Ana", birthDate = "2010-05-20", position = "Pitcher"))
        val sessions = SessionRepository(database)
        val sessionId = sessions.create(SessionDraft(playerId, Sport.SOFTBALL, PitchType.SOFTBALL_DROPBALL, 20))
        database.close()
        database = RadarDatabase.create(context, databaseName)

        val reopened = SessionRepository(database)
        assertEquals(0, reopened.get(sessionId)!!.target)
        reopened.changePitchType(sessionId, PitchType.SOFTBALL_RISEBALL)
        assertEquals(PitchType.SOFTBALL_RISEBALL, reopened.get(sessionId)!!.currentPitchType)
        assertEquals(PlayerDeletionResult.HasHistory, PlayerRepository(database, historyChecker = PlayerHistoryChecker { database.sessions().countForPlayer(it) }).deletePermanently(playerId))
    }

    @Test fun neutralTargetDoesNotFinishSessionAutomatically() = runBlocking {
        val players = PlayerRepository(database)
        val playerId = players.save(null, PlayerDraft(name = "Ana", birthDate = "2010-05-20", position = "Pitcher"))
        val sessions = SessionRepository(database)
        val sessionId = sessions.create(SessionDraft(playerId, Sport.SOFTBALL, PitchType.SOFTBALL_FASTBALL, 50))
        sessions.start(sessionId)

        PitchRepository(database).record("pitch-1", sessionId, 1, 55.0, PitchType.SOFTBALL_FASTBALL, 1, false)

        assertEquals(0, sessions.get(sessionId)!!.target)
        assertNull(sessions.get(sessionId)!!.endedAt)
    }

    @Test fun reopeningAnAlreadyStartedLegacySessionDoesNotFail() = runBlocking {
        val players = PlayerRepository(database)
        val playerId = players.save(null, PlayerDraft(name = "Ana", birthDate = "2010-05-20", position = "Pitcher"))
        val sessions = SessionRepository(database)
        val sessionId = sessions.create(SessionDraft(playerId))

        sessions.start(sessionId)
        sessions.start(sessionId)

        assertTrue(sessions.get(sessionId)!!.startedAt > 0)
    }

    @Test fun migratesVersionOneDataWithoutLosingCoachOrPlayer() = runBlocking {
        val migrationName = "room-migration-${UUID.randomUUID()}.db"
        context.deleteDatabase(migrationName)
        context.openOrCreateDatabase(migrationName, Context.MODE_PRIVATE, null).apply {
            execSQL("CREATE TABLE coaches (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, academy TEXT, country TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, revision INTEGER NOT NULL, localProfile INTEGER NOT NULL)")
            execSQL("CREATE UNIQUE INDEX index_coaches_localProfile ON coaches (localProfile)")
            execSQL("CREATE TABLE players (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, birthDate TEXT NOT NULL, sport TEXT NOT NULL, position TEXT NOT NULL, throwingHand TEXT NOT NULL, heightCm REAL, weightKg REAL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, revision INTEGER NOT NULL, archivedAt INTEGER)")
            execSQL("CREATE INDEX index_players_archivedAt_name ON players (archivedAt, name)")
            execSQL("INSERT INTO coaches VALUES ('coach-1','Danny',NULL,NULL,1,1,1,1)")
            execSQL("INSERT INTO players VALUES ('player-1','Ana','2010-05-20','BASEBALL','Pitcher','RIGHT',NULL,NULL,1,1,1,NULL)")
            setVersion(1); close()
        }
        val migrated = RadarDatabase.create(context, migrationName)
        assertEquals("Danny", CoachRepository(migrated).get()!!.name)
        assertEquals("Ana", PlayerRepository(migrated).get("player-1")!!.name)
        assertTrue(SessionRepository(migrated).observeOpen().first().isEmpty())
        migrated.close(); context.deleteDatabase(migrationName); Unit
    }

    @Test fun migratesVersionSixPlayersWithoutLosingSessionsOrPitches() = runBlocking {
        val migrationName = "room-migration-6-7-${UUID.randomUUID()}.db"
        context.deleteDatabase(migrationName)
        context.openOrCreateDatabase(migrationName, Context.MODE_PRIVATE, null).apply {
            execSQL("CREATE TABLE coaches (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, academy TEXT, country TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, revision INTEGER NOT NULL, localProfile INTEGER NOT NULL)")
            execSQL("CREATE UNIQUE INDEX index_coaches_localProfile ON coaches (localProfile)")
            execSQL("CREATE TABLE players (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, birthDate TEXT NOT NULL, sport TEXT NOT NULL, position TEXT NOT NULL, throwingHand TEXT NOT NULL, heightCm REAL, weightKg REAL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, revision INTEGER NOT NULL, archivedAt INTEGER)")
            execSQL("CREATE INDEX index_players_archivedAt_name ON players (archivedAt, name)")
            execSQL("CREATE TABLE sessions (id TEXT NOT NULL PRIMARY KEY, playerId TEXT NOT NULL, sport TEXT NOT NULL, initialPitchType TEXT NOT NULL, currentPitchType TEXT NOT NULL, target INTEGER NOT NULL, recordingEnabled INTEGER NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER, discardedAt INTEGER, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, revision INTEGER NOT NULL, FOREIGN KEY(playerId) REFERENCES players(id) ON DELETE RESTRICT)")
            execSQL("CREATE INDEX index_sessions_playerId ON sessions (playerId)")
            execSQL("CREATE TABLE pitches (id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, eventId INTEGER NOT NULL, number INTEGER NOT NULL, mph REAL NOT NULL, pitchType TEXT NOT NULL, receivedAt INTEGER NOT NULL, videoStatus TEXT NOT NULL, videoUri TEXT, videoReason TEXT, videoRawPath TEXT, videoRelativePath TEXT, videoDisplayName TEXT, FOREIGN KEY(sessionId) REFERENCES sessions(id) ON DELETE RESTRICT)")
            execSQL("CREATE UNIQUE INDEX index_pitches_sessionId_eventId ON pitches (sessionId, eventId)")
            execSQL("CREATE UNIQUE INDEX index_pitches_sessionId_number ON pitches (sessionId, number)")
            execSQL("INSERT INTO players VALUES ('player-6','Ana','2010-05-20','BASEBALL','Catcher','RIGHT',170,NULL,1,1,1,NULL)")
            execSQL("INSERT INTO sessions VALUES ('session-6','player-6','BASEBALL','BASEBALL_FASTBALL','BASEBALL_FASTBALL',0,1,1,2,NULL,1,2,1)")
            execSQL("INSERT INTO pitches VALUES ('pitch-6','session-6',7,1,72.5,'BASEBALL_FASTBALL',2,'AVAILABLE','content://video/7',NULL,NULL,'Movies/RadarCamera/','pitch.mp4')")
            setVersion(6); close()
        }
        val migrated = RadarDatabase.create(context, migrationName)
        val player = PlayerRepository(migrated).get("player-6")!!
        assertEquals("", player.category)
        assertNull(player.teamAcademy)
        assertEquals("session-6", SessionRepository(migrated).get("session-6")!!.id)
        assertEquals("content://video/7", migrated.pitches().getAllIncludingDiscarded("session-6").single().videoUri)
        migrated.close(); context.deleteDatabase(migrationName); Unit
    }
}
