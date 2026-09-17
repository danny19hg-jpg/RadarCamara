package com.example.radarcamera.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PlayerValidationTest {
    private val today = LocalDate.of(2026, 9, 14)
    private val valid = PlayerDraft(name = "Ana", birthDate = "2010-05-20")

    @Test fun acceptsOptionalMeasurementsAndTrimsName() {
        assertTrue(valid.copy(name = " Ana ").errors(today).isEmpty())
        assertNull(parseOptionalDecimal(""))
    }

    @Test fun rejectsBlankNameAndOverlongNameWithoutRequiringPosition() {
        assertTrue(valid.copy(name = " ").errors(today).containsKey("name"))
        assertTrue(valid.copy(name = "a".repeat(101)).errors(today).containsKey("name"))
        assertFalse(valid.copy(position = "").errors(today).containsKey("position"))
    }

    @Test fun rejectsInvalidAndFutureDates() {
        listOf("", "2010-02-30", "2026-09-15", "1800-01-01", "20/05/2010").forEach {
            assertTrue(it, valid.copy(birthDate = it).errors(today).containsKey("birthDate"))
        }
    }

    @Test fun validatesFinitePositiveMeasurements() {
        listOf("NaN", "Infinity", "-1", "0", "abc", "301").forEach {
            assertTrue(it, valid.copy(heightCm = it).errors(today).containsKey("heightCm"))
        }
        assertTrue(valid.copy(weightKg = "501").errors(today).containsKey("weightKg"))
        assertTrue(valid.copy(heightCm = "170,5", weightKg = "65.2").errors(today).isEmpty())
        assertEquals(170.5, parseOptionalDecimal("170,5")!!, 0.0)
    }

    @Test fun calculatesAgeAtBirthdayWithoutPersistingIt() {
        val birth = LocalDate.of(2010, 9, 15)
        assertEquals(15, ageOn(birth, today))
        assertEquals(16, ageOn(birth, today.plusDays(1)))
        assertEquals(18, ageOn(LocalDate.of(2008, 2, 29), LocalDate.of(2026, 3, 1)))
    }

    @Test fun acceptsBothSportsAndAllThrowingHands() {
        Sport.entries.forEach { sport ->
            ThrowingHand.entries.forEach { hand ->
                assertTrue(valid.copy(sport = sport, throwingHand = hand).errors(today).isEmpty())
            }
        }
    }
}
