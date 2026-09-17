package com.example.radarcamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionVideoStateTest {
    @Test fun cameraStatesHaveShortDistinctLabels() {
        assertEquals("Preparando", SessionVideoState.PREPARING.label)
        assertEquals("Listo", SessionVideoState.READY.label)
        assertEquals("Grabando", SessionVideoState.CAPTURING.label)
        assertEquals("Exportando", SessionVideoState.EXPORTING.label)
    }
}
