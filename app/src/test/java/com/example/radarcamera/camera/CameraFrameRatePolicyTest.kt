package com.example.radarcamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraFrameRatePolicyTest {

    @Test
    fun `selects required 60 fps when the session is supported`() {
        assertEquals(
            CameraFrameRateMode.FPS_60_REQUIRED,
            selectCameraFrameRateMode(isFps60SessionSupported = true)
        )
    }

    @Test
    fun `falls back explicitly to 30 fps when 60 fps is unsupported`() {
        assertEquals(
            CameraFrameRateMode.FPS_30_FALLBACK,
            selectCameraFrameRateMode(isFps60SessionSupported = false)
        )
    }

    @Test
    fun `fallback quality is restricted to FHD then HD`() {
        assertEquals(
            listOf(CameraVideoQuality.FHD, CameraVideoQuality.HD),
            fallbackVideoQualityOrder()
        )
    }
}
