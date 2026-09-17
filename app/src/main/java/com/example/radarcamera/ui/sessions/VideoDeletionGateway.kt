package com.example.radarcamera.ui.sessions

fun interface VideoDeletionGateway { fun delete(uri: String): Int }

internal fun deleteDistinctVideos(uris: List<String>, gateway: VideoDeletionGateway) {
    uris.filter { it.isNotBlank() }.distinct().forEach { gateway.delete(it) }
}
