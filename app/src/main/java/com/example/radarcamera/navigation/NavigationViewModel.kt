package com.example.radarcamera.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.radarcamera.security.AndroidMonotonicClock
import com.example.radarcamera.security.CoachingAccessController
import com.example.radarcamera.security.CoachingAccessResult
import com.example.radarcamera.security.RecoveryMode

enum class Route(val requiresUnlock: Boolean = false) {
    HOME, LIVE, COACHING_ACCESS, INITIAL_COACHING_SETUP, COACHING_RECOVERY, PIN,
    COACHING(true), PROFILE(true), PLAYERS(true), PLAYER_DETAIL(true), PLAYER_EDITOR(true), SESSIONS(true), SESSION_EDITOR(true), SESSION_ANALYSIS(true), HISTORY(true), DISCARDED_SESSIONS(true), BACKUP(true)
}
enum class SessionDetailOrigin { OPEN_SESSIONS, NORMAL_HISTORY, DELETED_SESSIONS }

// Estado de proceso, deliberadamente sin SavedStateHandle ni rememberSaveable.
class NavigationViewModel(
    private val coachingAccessController: CoachingAccessController =
        CoachingAccessController(AndroidMonotonicClock)
) : ViewModel() {
    var route by mutableStateOf(Route.HOME)
        private set
    var editorId: String? = null
        private set
    var playerId: String? = null
        private set
    var archivedPlayers = false
        private set
    var sessionId: String? = null
        private set
    var sessionCreationPlayerId: String? = null
        private set
    var sessionEditorKey by mutableStateOf(0)
        private set
    var historyPlayerId: String? = null
        private set
    var analysisSessionId: String? = null
        private set
    var sessionDetailOrigin: SessionDetailOrigin = SessionDetailOrigin.OPEN_SESSIONS
        private set
    var coachingRecoveryMode: RecoveryMode? = null
        private set

    private var screenStore = ViewModelStore()
    private var coachingStack = NavigationStack(CoachingDestination.PlayerList)

    val coachingDestination: CoachingDestination
        get() = coachingStack.current
    val screenOwner: ViewModelStoreOwner
        get() = object : ViewModelStoreOwner { override val viewModelStore = screenStore }

    fun go(destination: Route) {
        val next = if (destination.requiresUnlock && !coachingAccessController.isUnlocked()) Route.PIN else destination
        if (next != route) {
            screenStore.clear()
            screenStore = ViewModelStore()
            route = next
        }
    }

    fun beginCoachingAccess() = go(Route.COACHING_ACCESS)
    fun showInitialCoachingSetup() = go(Route.INITIAL_COACHING_SETUP)
    fun showPinAccess() = go(Route.PIN)
    fun showCoachingRecovery(mode: RecoveryMode) {
        coachingRecoveryMode = mode
        go(Route.COACHING_RECOVERY)
    }

    fun unlock() {
        coachingAccessController.onPinVerified(true)
        coachingStack = NavigationStack(CoachingDestination.PlayerList)
        showPlayers()
    }

    fun isCoachingUnlocked(): Boolean = coachingAccessController.isUnlocked()

    fun onProcessBackground() {
        coachingAccessController.onBackground()
    }

    fun onProcessForeground() {
        if (coachingAccessController.onForeground() == CoachingAccessResult.Locked && route.requiresUnlock) {
            go(Route.PIN)
        }
    }

    fun lock() {
        coachingAccessController.lock()
        coachingStack = NavigationStack(CoachingDestination.PlayerList)
        editorId = null
        playerId = null
        archivedPlayers = false
        sessionId = null
        go(Route.HOME)
    }

    fun showPlayers(archived: Boolean = false) {
        archivedPlayers = archived
        coachingStack.replace(CoachingDestination.PlayerList)
        go(Route.PLAYERS)
    }

    fun editPlayer(id: String?, archived: Boolean) {
        editorId = id
        archivedPlayers = archived
        go(Route.PLAYER_EDITOR)
    }
    fun showPlayerDetail(id: String) {
        playerId = id
        sessionCreationPlayerId = null
        coachingStack.push(CoachingDestination.PlayerDetail(id))
        go(Route.PLAYER_DETAIL)
    }
    fun showSessions(playerId: String? = null) {
        sessionId = null
        sessionCreationPlayerId = playerId
        if (playerId != null) this.playerId = playerId
        coachingStack.push(CoachingDestination.Session(null, SessionDetailOrigin.OPEN_SESSIONS, playerId))
        go(Route.SESSIONS)
    }
    fun showSessionCreation(playerId: String) {
        sessionId = null
        sessionCreationPlayerId = playerId
        this.playerId = playerId
        sessionEditorKey += 1
        coachingStack.push(CoachingDestination.Session(null, SessionDetailOrigin.OPEN_SESSIONS, playerId))
        go(Route.SESSION_EDITOR)
    }
    fun showHistory() { historyPlayerId = null; go(Route.HISTORY) }
    fun showPlayerHistory(playerId: String) {
        historyPlayerId = playerId
        coachingStack.push(CoachingDestination.PlayerHistory(playerId))
        go(Route.HISTORY)
    }
    fun showDiscardedSessions(playerId: String? = null) {
        coachingStack.push(CoachingDestination.DeletedSessions(playerId))
        go(Route.DISCARDED_SESSIONS)
    }
    fun showBackup() { go(Route.BACKUP) }
    fun editSession(id: String?, origin: SessionDetailOrigin = SessionDetailOrigin.OPEN_SESSIONS, playerId: String? = null) {
        sessionId = id
        sessionDetailOrigin = origin
        if (id == null && playerId != null) sessionCreationPlayerId = playerId
        else if (id != null) sessionCreationPlayerId = null
        sessionEditorKey += 1
        val destination = CoachingDestination.Session(id, origin, playerId ?: sessionCreationPlayerId)
        if (route == Route.SESSION_EDITOR || route == Route.SESSION_ANALYSIS) {
            coachingStack.replace(destination)
            screenStore.clear()
            screenStore = ViewModelStore()
        } else {
            coachingStack.push(destination)
            go(Route.SESSION_EDITOR)
        }
    }
    fun showSessionAnalysis(id:String) { analysisSessionId=id; go(Route.SESSION_ANALYSIS) }

    fun back() {
        when (route) {
            Route.COACHING_ACCESS, Route.INITIAL_COACHING_SETUP, Route.COACHING_RECOVERY, Route.PIN -> lock()
            Route.PROFILE, Route.SESSIONS, Route.BACKUP -> go(Route.COACHING)
            Route.PLAYERS -> {
                coachingStack.pop()
                go(Route.COACHING)
            }
            Route.PLAYER_DETAIL -> {
                coachingStack.pop()
                go(Route.PLAYERS)
            }
            Route.HISTORY -> {
                if (coachingStack.current is CoachingDestination.PlayerHistory) coachingStack.pop()
                go(Route.COACHING)
            }
            Route.DISCARDED_SESSIONS -> {
                if (coachingStack.current is CoachingDestination.DeletedSessions) coachingStack.pop()
                go(Route.COACHING)
            }
            Route.PLAYER_EDITOR -> go(Route.PLAYERS)
            Route.SESSION_EDITOR -> {
                coachingStack.pop()
                if (sessionId == null && sessionCreationPlayerId != null) {
                    val returningToPlayerId = sessionCreationPlayerId
                    sessionCreationPlayerId = null
                    playerId = returningToPlayerId
                    go(Route.PLAYER_DETAIL)
                } else {
                    go(when (sessionDetailOrigin) {
                        SessionDetailOrigin.NORMAL_HISTORY -> Route.HISTORY
                        SessionDetailOrigin.DELETED_SESSIONS -> Route.DISCARDED_SESSIONS
                        SessionDetailOrigin.OPEN_SESSIONS -> Route.SESSIONS
                    })
                }
            }
            Route.SESSION_ANALYSIS -> editSession(analysisSessionId)
            else -> lock()
        }
    }

    override fun onCleared() { screenStore.clear() }
}
