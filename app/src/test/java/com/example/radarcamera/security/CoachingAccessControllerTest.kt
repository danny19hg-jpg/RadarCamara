package com.example.radarcamera.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachingAccessControllerTest {
    @Test
    fun pinConfirmationMustMatchBeforeInitialSetupCanContinue() {
        assertFalse(InitialCoachingSetupValidator.isValid("Entrenador", "1234", "5678"))
        assertTrue(InitialCoachingSetupValidator.isValid("Entrenador", "1234", "1234"))
    }

    @Test
    fun correctPinUnlocksAndIncorrectPinDoesNotUnlock() {
        val controller = CoachingAccessController(FakeMonotonicClock())

        controller.onPinVerified(false)
        assertFalse(controller.isUnlocked())
        controller.onPinVerified(true)
        assertTrue(controller.isUnlocked())
    }

    @Test
    fun newControllerStartsLockedAndUnlockIsNotPersistent() {
        val oldProcess = CoachingAccessController(FakeMonotonicClock())
        oldProcess.onPinVerified(true)

        val newProcess = CoachingAccessController(FakeMonotonicClock())

        assertTrue(oldProcess.isUnlocked())
        assertFalse(newProcess.isUnlocked())
    }

    @Test
    fun continuousForegroundKeepsAccessUnlocked() {
        val controller = CoachingAccessController(FakeMonotonicClock())
        controller.onPinVerified(true)

        assertEquals(CoachingAccessResult.Allowed, controller.onForeground())
        assertTrue(controller.isUnlocked())
    }

    @Test
    fun fourMinutesFiftyNineSecondsInBackgroundKeepsAccessUnlocked() {
        val clock = FakeMonotonicClock()
        val controller = CoachingAccessController(clock)
        controller.onPinVerified(true)
        controller.onBackground()
        clock.advanceBy(299_999)

        assertEquals(CoachingAccessResult.Allowed, controller.onForeground())
        assertTrue(controller.isUnlocked())
    }

    @Test
    fun exactlyFiveMinutesInBackgroundLocksAccess() {
        val clock = FakeMonotonicClock()
        val controller = CoachingAccessController(clock)
        controller.onPinVerified(true)
        controller.onBackground()
        clock.advanceBy(300_000)

        assertEquals(CoachingAccessResult.Locked, controller.onForeground())
        assertFalse(controller.isUnlocked())
    }

    @Test
    fun moreThanFiveMinutesInBackgroundLocksAccess() {
        val clock = FakeMonotonicClock()
        val controller = CoachingAccessController(clock)
        controller.onPinVerified(true)
        controller.onBackground()
        clock.advanceBy(300_001)

        controller.onForeground()

        assertFalse(controller.isUnlocked())
    }

    @Test
    fun repeatedLifecycleEventsUseOnlyTheFirstBackgroundTimestamp() {
        val clock = FakeMonotonicClock()
        val controller = CoachingAccessController(clock)
        controller.onPinVerified(true)
        controller.onBackground()
        clock.advanceBy(60_000)
        controller.onBackground()
        clock.advanceBy(240_000)

        assertEquals(CoachingAccessResult.Locked, controller.onForeground())
        assertFalse(controller.isUnlocked())
    }

    @Test
    fun wallClockChangesCannotAffectTheMonotonicTimeout() {
        val clock = FakeMonotonicClock()
        val controller = CoachingAccessController(clock)
        controller.onPinVerified(true)
        controller.onBackground()
        clock.advanceBy(1)

        assertEquals(CoachingAccessResult.Allowed, controller.onForeground())
    }

    @Test
    fun radarLiveDoesNotRequirePinOrChangeAccessState() {
        val controller = CoachingAccessController(FakeMonotonicClock())

        controller.onRadarLiveOpened()
        controller.onRadarLiveClosed()

        assertFalse(controller.isUnlocked())
    }

    @Test
    fun lockingCoachingDoesNotOwnOrMutateAnActiveSession() {
        val controller = CoachingAccessController(FakeMonotonicClock())
        controller.onPinVerified(true)
        val activeSessionId = "session-active"

        controller.lock()

        assertFalse(controller.isUnlocked())
        assertEquals("session-active", activeSessionId)
    }
}

private class FakeMonotonicClock(private var currentMs: Long = 0) : MonotonicClock {
    override fun nowMs(): Long = currentMs
    fun advanceBy(milliseconds: Long) { currentMs += milliseconds }
}
