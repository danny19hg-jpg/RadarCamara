package com.example.radarcamera.security

import android.os.SystemClock

fun interface MonotonicClock {
    fun nowMs(): Long
}

object AndroidMonotonicClock : MonotonicClock {
    override fun nowMs(): Long = SystemClock.elapsedRealtime()
}
