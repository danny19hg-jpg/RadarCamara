package com.example.radarcamera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreRollPolicyTest {

    @Test
    fun recortaCuatroSegundosAntesYUnoDespuesDelEvento() {
        val ventana = calcularVentanaPreRoll(
            posicionEventoMs = 8_000L,
            duracionRawMs = 9_050L
        )

        assertEquals(4_000L, ventana.inicioMs)
        assertEquals(9_000L, ventana.finMs)
        assertEquals(5_000L, ventana.duracionMs)
    }

    @Test
    fun noAceptaEventoHastaCargarCuatroSegundosDePreRoll() {
        assertFalse(preRollDisponible(3_999L))
        assertTrue(preRollDisponible(4_000L))
    }

    @Test
    fun detieneSolamenteDespuesDeCompletarElSegundoPosterior() {
        assertFalse(
            debeDetenerDespuesDelEvento(
                duracionActualMs = 8_999L,
                posicionEventoMs = 8_000L
            )
        )
        assertTrue(
            debeDetenerDespuesDelEvento(
                duracionActualMs = 9_000L,
                posicionEventoMs = 8_000L
            )
        )
    }
}
