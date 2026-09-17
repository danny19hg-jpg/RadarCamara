package com.example.radarcamera.radar

import org.junit.Assert.assertEquals
import org.junit.Test

class CoachingEventCursorTest {
    @Test fun firstSuccessfulResponseEstablishesBaseline() {
        val cursor = CoachingEventCursor()

        assertEquals(CoachingEventDecision.BASELINE, cursor.onSuccessfulStatus(31).decision)
        assertEquals(31L, cursor.lastEventId)
    }

    @Test fun transientFailurePreservesCursorAndAcceptsFollowingEvent() {
        val cursor = CoachingEventCursor(31)

        cursor.onTransportFailure()

        assertEquals(CoachingEventDecision.NEW_EVENT, cursor.onSuccessfulStatus(32).decision)
        assertEquals(32L, cursor.lastEventId)
    }

    @Test fun multipleTransientFailuresPreserveCursorAndAcceptFollowingEvent() {
        val cursor = CoachingEventCursor(31)

        repeat(3) { cursor.onTransportFailure() }

        assertEquals(CoachingEventDecision.NEW_EVENT, cursor.onSuccessfulStatus(32).decision)
    }

    @Test fun failureThenSameEventIsDuplicate() {
        val cursor = CoachingEventCursor(31)

        cursor.onTransportFailure()

        assertEquals(CoachingEventDecision.DUPLICATE, cursor.onSuccessfulStatus(31).decision)
    }

    @Test fun counterJumpAcceptsCurrentEventAndReportsGapWithoutInventingEvents() {
        val cursor = CoachingEventCursor(31)

        val result = cursor.onSuccessfulStatus(34)

        assertEquals(CoachingEventDecision.NEW_EVENT, result.decision)
        assertEquals(2L, result.missingEventCount)
        assertEquals(34L, cursor.lastEventId)
    }

    @Test fun lowerSuccessfulEventConfirmsCounterResetAndNextEventIsAccepted() {
        val cursor = CoachingEventCursor(31)

        assertEquals(CoachingEventDecision.COUNTER_RESET, cursor.onSuccessfulStatus(2).decision)
        assertEquals(CoachingEventDecision.NEW_EVENT, cursor.onSuccessfulStatus(3).decision)
    }

    @Test fun transportFailureBeforeFirstSuccessRemainsUnsynchronized() {
        val cursor = CoachingEventCursor()

        cursor.onTransportFailure()

        assertEquals(null, cursor.lastEventId)
        assertEquals(CoachingEventDecision.BASELINE, cursor.onSuccessfulStatus(31).decision)
    }

    @Test fun cameraSettingDoesNotChangeCursorDecision() {
        val decisions = listOf(false, true).map {
            CoachingEventCursor(31).apply { onTransportFailure() }.onSuccessfulStatus(32).decision
        }

        assertEquals(listOf(CoachingEventDecision.NEW_EVENT, CoachingEventDecision.NEW_EVENT), decisions)
    }
}
