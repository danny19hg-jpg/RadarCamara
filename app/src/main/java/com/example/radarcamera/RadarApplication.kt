package com.example.radarcamera

import android.app.Application
import com.example.radarcamera.di.AppContainer

class RadarApplication : Application() {
    val container by lazy { AppContainer(this) }
}
