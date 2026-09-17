package com.example.radarcamera.ui.sessions

interface PermanentSessionStore { fun isDiscarded(sessionId:String):Boolean; fun deletePitches(sessionId:String):Int; fun deleteDiscardedSession(sessionId:String):Int }

class PermanentSessionDeletionCoordinator(private val videos:VideoDeletionGateway, private val store:PermanentSessionStore) {
 private var busy=false
 fun delete(sessionId:String, uris:List<String>):Boolean {
  if(busy) return false
  busy=true
  return try { check(store.isDiscarded(sessionId)); deleteDistinctVideos(uris,videos); store.deletePitches(sessionId); check(store.deleteDiscardedSession(sessionId)==1); true } finally { busy=false }
 }
}
