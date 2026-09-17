package com.example.radarcamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPreviewLayoutTest {
    @Test fun usesPortraitFrameOnPortraitDisplaysAndLandscapeFrameOtherwise() {
        assertEquals(9f / 16f, previewAspectRatio(true), 0f)
        assertEquals(16f / 9f, previewAspectRatio(false), 0f)
    }
}
