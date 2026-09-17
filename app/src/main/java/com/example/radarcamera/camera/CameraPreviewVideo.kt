package com.example.radarcamera.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

internal fun previewAspectRatio(isPortrait: Boolean): Float =
    if (isPortrait) 9f / 16f else 16f / 9f

@Composable
internal fun CameraPreviewVideo(
    controller: CameraVideoController,
    modifier: Modifier = Modifier,
    onReady: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            previewView.implementationMode =
                PreviewView.ImplementationMode.COMPATIBLE
            previewView.scaleType =
                PreviewView.ScaleType.FIT_CENTER

            controller.bindCamera(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                onReady = onReady
            )

            previewView
        }
    )
}
