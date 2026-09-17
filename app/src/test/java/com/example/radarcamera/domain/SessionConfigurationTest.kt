package com.example.radarcamera.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionConfigurationTest {
    @Test fun exposesOnlyPitchTypesValidForTheSelectedSport() {
        assertEquals(
            listOf("Fastball", "Curveball", "Slider", "Changeup", "Sinker", "Cutter"),
            PitchType.forSport(Sport.BASEBALL).map { it.label }
        )
        assertEquals(
            listOf("Fastball", "Dropball", "Riseball", "Changeup", "Curveball", "Screwball"),
            PitchType.forSport(Sport.SOFTBALL).map { it.label }
        )
    }

    @Test fun resolvesPresetAndPositiveCustomGoals() {
        assertEquals(20, SessionGoal.preset(20).valueOrNull())
        assertEquals(37, SessionGoal.custom("37").valueOrNull())
        assertNull(SessionGoal.custom("0").valueOrNull())
        assertNull(SessionGoal.custom("1.5").valueOrNull())
        assertTrue(SessionGoal.custom("999999999999").errors().isNotEmpty())
    }
}
