package com.example.radarcamera.security

private const val COACHING_LOCK_TIMEOUT_MS = 5 * 60 * 1000L

enum class CoachingAccessResult {
    Allowed,
    Locked
}

/**
 * Estado estrictamente en memoria para la puerta de Coaching.
 * No conoce sesiones, radar ni cÃ¡mara; bloquearlo no modifica datos deportivos.
 */
class CoachingAccessController(
    private val clock: MonotonicClock,
    private val timeoutMs: Long = COACHING_LOCK_TIMEOUT_MS
) {
    private var unlocked = false
    private var backgroundStartedAtMs: Long? = null

    fun onPinVerified(correct: Boolean) {
        if (correct) {
            unlocked = true
            backgroundStartedAtMs = null
        }
    }

    fun isUnlocked(): Boolean = unlocked

    fun lock() {
        unlocked = false
        backgroundStartedAtMs = null
    }

    fun onBackground() {
        if (unlocked && backgroundStartedAtMs == null) {
            backgroundStartedAtMs = clock.nowMs()
        }
    }

    fun onForeground(): CoachingAccessResult {
        val startedAt = backgroundStartedAtMs ?: return if (unlocked) {
            CoachingAccessResult.Allowed
        } else {
            CoachingAccessResult.Locked
        }
        backgroundStartedAtMs = null
        if (clock.nowMs() - startedAt >= timeoutMs) lock()
        return if (unlocked) CoachingAccessResult.Allowed else CoachingAccessResult.Locked
    }

    // Intencionalmente no modifica el permiso: Radar Live es pÃºblico.
    fun onRadarLiveOpened() = Unit
    fun onRadarLiveClosed() = Unit
}

object InitialCoachingSetupValidator {
    fun isValid(name: String, pin: String, confirmation: String): Boolean =
        name.trim().isNotEmpty() && pin.length in 4..6 && pin.all(Char::isDigit) && pin == confirmation
}
