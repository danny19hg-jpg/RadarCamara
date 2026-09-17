package com.example.radarcamera.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.example.radarcamera.data.local.PitchEntity
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.domain.PitchVideoStatus

internal class VideoRelinker(private val resolver: ContentResolver, private val database: RadarDatabase) {
    suspend fun relink(pitches: List<PitchEntity>) {
        pitches.filter { it.videoStatus == PitchVideoStatus.AVAILABLE || it.videoUri != null }.forEach { pitch ->
            if (isReadable(pitch.videoUri)) return@forEach
            val path = pitch.videoRelativePath; val name = pitch.videoDisplayName
            val matches = if (path != null && name != null) find(path, name) else emptyList()
            when (matches.size) {
                1 -> database.pitches().relinkVideo(pitch.id, PitchVideoStatus.AVAILABLE, matches.single(), null)
                else -> database.pitches().relinkVideo(pitch.id, PitchVideoStatus.FAILED, null, if (matches.isEmpty()) "VIDEO_NO_ENCONTRADO" else "VIDEO_AMBIGUO")
            }
        }
    }
    private fun isReadable(value: String?): Boolean = try { value != null && resolver.openAssetFileDescriptor(Uri.parse(value), "r")?.use { true } == true } catch (_: Exception) { false }
    private fun find(path: String, name: String): List<String> = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID), "${MediaStore.Video.Media.RELATIVE_PATH}=? AND ${MediaStore.Video.Media.DISPLAY_NAME}=?", arrayOf(path, name), null)?.use { cursor -> buildList { while (cursor.moveToNext()) add(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0).toString()).toString()) } } ?: emptyList()
}
