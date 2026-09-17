package com.example.radarcamera.radar

/**
 * Cursor local de eventos exitosamente recibidos desde /status.
 * Los problemas de transporte no modifican la secuencia conocida.
 */
internal class CoachingEventCursor(initialEventId: Long? = null) {
    var lastEventId: Long? = initialEventId
        private set

    fun onTransportFailure() = Unit

    fun onSuccessfulStatus(eventId: Long): CoachingEventResult {
        val previous = lastEventId
        if (previous == null) {
            lastEventId = eventId
            return CoachingEventResult(CoachingEventDecision.BASELINE, previous)
        }
        if (eventId == previous) {
            return CoachingEventResult(CoachingEventDecision.DUPLICATE, previous)
        }
        if (eventId < previous) {
            lastEventId = eventId
            return CoachingEventResult(CoachingEventDecision.COUNTER_RESET, previous)
        }

        lastEventId = eventId
        return CoachingEventResult(
            decision = CoachingEventDecision.NEW_EVENT,
            previousEventId = previous,
            missingEventCount = (eventId - previous - 1).coerceAtLeast(0),
        )
    }
}

internal enum class CoachingEventDecision { BASELINE, DUPLICATE, NEW_EVENT, COUNTER_RESET }

internal data class CoachingEventResult(
    val decision: CoachingEventDecision,
    val previousEventId: Long?,
    val missingEventCount: Long = 0,
)
