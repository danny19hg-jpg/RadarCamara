package com.example.radarcamera.di

import android.content.Context
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.data.repository.CoachRepository
import com.example.radarcamera.data.repository.PlayerRepository
import com.example.radarcamera.data.repository.SessionPlayerHistoryChecker
import com.example.radarcamera.data.repository.SessionRepository
import com.example.radarcamera.data.repository.PitchRepository

class AppContainer(context: Context) {
    val database by lazy { RadarDatabase.create(context.applicationContext) }
    val coachRepository by lazy { CoachRepository(database) }
    val playerRepository by lazy { PlayerRepository(database, historyChecker = SessionPlayerHistoryChecker(database)) }
    val sessionRepository by lazy { SessionRepository(database) }
    val pitchRepository by lazy { PitchRepository(database) }
}
