package com.example.radarcamera.ui.sessions

import org.junit.Assert.*
import org.junit.Test

class PermanentSessionDeletionCoordinatorTest {
 private class Store(var discarded:Boolean=true):PermanentSessionStore { val calls=mutableListOf<String>(); override fun isDiscarded(sessionId:String)=discarded; override fun deletePitches(sessionId:String):Int {calls+="pitches:$sessionId";return 1}; override fun deleteDiscardedSession(sessionId:String):Int {calls+="session:$sessionId";return 1} }
 @Test fun videosAreProcessedBeforePitchesAndSession() { val calls=mutableListOf<String>(); val store=Store(); val c=PermanentSessionDeletionCoordinator(VideoDeletionGateway{calls+="video:$it";0},store); assertTrue(c.delete("one",listOf("a","a","b"))); assertEquals(listOf("video:a","video:b"),calls); assertEquals(listOf("pitches:one","session:one"),store.calls) }
 @Test fun videoFailureLeavesRoomUntouchedAndCanRetry() { val store=Store(); var fail=true; val c=PermanentSessionDeletionCoordinator(VideoDeletionGateway{if(fail){fail=false;error("denied")};0},store); assertFails { c.delete("one",listOf("a")) }; assertTrue(store.calls.isEmpty()); assertTrue(c.delete("one",listOf("a"))) }
 @Test fun activeSessionCannotBePermanentlyDeleted() { val store=Store(false); val c=PermanentSessionDeletionCoordinator(VideoDeletionGateway{0},store); assertFails { c.delete("one",emptyList()) }; assertTrue(store.calls.isEmpty()) }
 private fun assertFails(block:()->Unit){ try { block(); fail("expected failure") } catch(_:IllegalStateException){} }
}
