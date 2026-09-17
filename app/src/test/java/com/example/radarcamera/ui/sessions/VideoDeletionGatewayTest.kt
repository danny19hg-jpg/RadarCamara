package com.example.radarcamera.ui.sessions

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDeletionGatewayTest {
 @Test fun duplicateUrisAreDeletedOnlyOnceAndMissingDeleteDoesNotFail() { val calls=mutableListOf<String>(); deleteDistinctVideos(listOf("a","a","b"), VideoDeletionGateway { calls+=it; 0 }); assertEquals(listOf("a","b"),calls) }
 @Test fun exceptionStopsLaterUrisForRetry() { val calls=mutableListOf<String>(); try { deleteDistinctVideos(listOf("a","b","c"), VideoDeletionGateway { calls+=it; if(it=="b") error("denied"); 1 }) } catch (_:IllegalStateException) {} ; assertEquals(listOf("a","b"),calls) }
}
