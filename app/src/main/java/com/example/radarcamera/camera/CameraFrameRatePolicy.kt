package com.example.radarcamera.camera

internal enum class CameraFrameRateMode {
    FPS_60_REQUIRED,
    FPS_30_FALLBACK
}

internal enum class CameraVideoQuality {
    FHD,
    HD
}

internal fun selectCameraFrameRateMode(
    isFps60SessionSupported: Boolean
): CameraFrameRateMode = if (isFps60SessionSupported) {
    CameraFrameRateMode.FPS_60_REQUIRED
} else {
    CameraFrameRateMode.FPS_30_FALLBACK
}

internal fun fallbackVideoQualityOrder(): List<CameraVideoQuality> =
    listOf(CameraVideoQuality.FHD, CameraVideoQuality.HD)
