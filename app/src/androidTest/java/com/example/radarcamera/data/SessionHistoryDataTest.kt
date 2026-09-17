package com.example.radarcamera.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.radarcamera.data.local.PitchEntity
import com.example.radarcamera.data.local.RadarDatabase
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
class SessionHistoryDataTest {
    private lateinit var context: Context; private lateinit var db: RadarDatabase
    private val name = "history-${UUID.randomUUID()}.db"
    @Before fun open() { context=InstrumentationRegistry.getInstrumentation().targetContext; db=RadarDatabase.create(context,name) }
    @After fun close() { db.close(); context.deleteDatabase(name) }
    @Test fun historyDiscardRestoreKeepsPitchesAndVideos() = runBlocking {
        val players=PlayerRepository(db); val a=players.save(null,PlayerDraft(name="Ana",birthDate="2010-01-01",position="P")); val b=players.save(null,PlayerDraft(name="Bea",birthDate="2010-01-01",position="P"))
        val sessions=SessionRepository(db); val one=sessions.create(SessionDraft(a)); val two=sessions.create(SessionDraft(b)); sessions.start(one); sessions.finish(one); sessions.start(two); sessions.finish(two)
        players.setArchived(a,true)
        db.pitches().insert(PitchEntity("pitch",one,1,1,70.0,PitchType.BASEBALL_FASTBALL,1,PitchVideoStatus.AVAILABLE,"content://video/1"))
        assertEquals(listOf(one,two).toSet(),sessions.observeFinished().first().map{it.session.id}.toSet())
        assertEquals(listOf(one),sessions.observeFinished(a).first().map{it.session.id})
        sessions.discard(one)
        assertFalse(sessions.observeFinished().first().any{it.session.id==one})
        assertTrue(sessions.observeDiscarded().first().any{it.session.id==one})
        assertTrue(PitchRepository(db).observe(one).first().isEmpty())
        assertEquals("content://video/1",PitchRepository(db).getAllIncludingDiscarded(one).single().videoUri)
        sessions.restore(one)
        assertTrue(sessions.observeFinished().first().any{it.session.id==one})
        assertEquals(PitchVideoStatus.AVAILABLE,PitchRepository(db).observe(one).first().single().videoStatus)
    }
}
