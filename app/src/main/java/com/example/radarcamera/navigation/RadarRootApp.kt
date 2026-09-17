package com.example.radarcamera.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.radarcamera.PantallaInicio
import com.example.radarcamera.PantallaLive
import com.example.radarcamera.RadarApplication
import com.example.radarcamera.BuildConfig
import com.example.radarcamera.security.PinStore
import com.example.radarcamera.security.PantallaPin
import com.example.radarcamera.security.CoachingConfigurationResolver
import com.example.radarcamera.security.CoachingConfigurationState
import com.example.radarcamera.security.CoachingRecoveryScreen
import com.example.radarcamera.security.CoachingRecoveryViewModel
import com.example.radarcamera.security.InitialCoachingSetupScreen
import com.example.radarcamera.security.InitialCoachingSetupViewModel
import com.example.radarcamera.ui.coach.*
import com.example.radarcamera.ui.coaching.CoachingDashboard
import com.example.radarcamera.ui.players.*
import com.example.radarcamera.ui.sessions.*
import com.example.radarcamera.ui.analysis.*
import com.example.radarcamera.ui.backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.SystemClock
import android.util.Log

@Composable
fun RadarRootApp() {
    val context = LocalContext.current
    val container = (context.applicationContext as RadarApplication).container
    val pinStore = remember { PinStore(context.applicationContext) }
    val navigation: NavigationViewModel = viewModel()
    DisposableEffect(navigation) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> navigation.onProcessBackground()
                Lifecycle.Event.ON_START -> navigation.onProcessForeground()
                else -> Unit
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = navigation.route != Route.HOME) { navigation.back() }

    // El almacén de cada pantalla se conserva al rotar y se limpia al salir de ella.
    CompositionLocalProvider(LocalViewModelStoreOwner provides navigation.screenOwner) {
        when (navigation.route) {
            Route.HOME -> PantallaInicio(
                onLive = { navigation.go(Route.LIVE) },
                onCoaching = navigation::beginCoachingAccess
            )
            Route.LIVE -> PantallaLive(onVolver = navigation::back)
            Route.COACHING_ACCESS -> CoachingAccessGate(
                hasPin = pinStore.tienePin(),
                loadProfile = { container.coachRepository.get() != null },
                onState = { state ->
                    when (state) {
                        CoachingConfigurationState.FirstSetup -> navigation.showInitialCoachingSetup()
                        CoachingConfigurationState.PinRequired -> navigation.showPinAccess()
                        is CoachingConfigurationState.RecoveryRequired -> navigation.showCoachingRecovery(state.mode)
                        CoachingConfigurationState.Loading -> Unit
                    }
                },
                onBack = navigation::back,
                resolveState = CoachingConfigurationResolver::resolve
            )
            Route.INITIAL_COACHING_SETUP -> {
                val model: InitialCoachingSetupViewModel = viewModel(factory = ScreenModelFactory {
                    InitialCoachingSetupViewModel(container.coachRepository, pinStore)
                })
                InitialCoachingSetupScreen(model, navigation::back, navigation::unlock)
            }
            Route.COACHING_RECOVERY -> navigation.coachingRecoveryMode?.let { mode ->
                val model: CoachingRecoveryViewModel = viewModel(factory = ScreenModelFactory { CoachingRecoveryViewModel(mode, container.coachRepository, pinStore) })
                CoachingRecoveryScreen(model, mode, navigation::back, navigation::unlock)
            }
            Route.PIN -> PantallaPin(
                creandoPin = false, pinStore = pinStore,
                onVolver = navigation::lock, onCorrecto = navigation::unlock
            )
            Route.COACHING -> CoachingDashboard(
                onProfile = { navigation.go(Route.PROFILE) },
                onPlayers = { navigation.showPlayers() },
                onSessions = navigation::showSessions,
                onHistory = navigation::showHistory,
                onDiscarded = navigation::showDiscardedSessions,
                onBackup = navigation::showBackup,
                onExit = navigation::lock
            )
            Route.PROFILE -> {
                val model: CoachViewModel = viewModel(factory = ScreenModelFactory {
                    CoachViewModel(container.coachRepository)
                })
                CoachScreen(model, navigation::back)
            }
            Route.PLAYERS -> {
                val model: PlayersViewModel = viewModel(factory = ScreenModelFactory {
                    PlayersViewModel(container.playerRepository, navigation.archivedPlayers)
                })
                PlayersScreen(
                    model, onBack = navigation::back,
                    onNew = { navigation.editPlayer(null, false) },
                    onEdit = { navigation.editPlayer(it.id, it.archivedAt != null) },
                    onSelect = { navigation.showPlayerDetail(it.id) }
                )
            }
            Route.PLAYER_DETAIL -> navigation.playerId?.let { id ->
                val model: PlayerDetailViewModel = viewModel(factory = ScreenModelFactory {
                    PlayerDetailViewModel(container.playerRepository, id)
                })
                PlayerDetailScreen(
                    model,
                    onBack = navigation::back,
                    onEdit = navigation::editPlayer,
                    onNewSession = navigation::showSessionCreation,
                    onContinueSession = { navigation.editSession(it) },
                    onHistory = navigation::showPlayerHistory
                )
            }
            Route.PLAYER_EDITOR -> {
                val model: PlayerEditorViewModel = viewModel(factory = ScreenModelFactory {
                    PlayerEditorViewModel(container.playerRepository, navigation.editorId)
                })
                PlayerEditorScreen(model, navigation::back, { id -> navigation.showPlayerHistory(id) })
            }
            Route.SESSIONS -> {
                val model: SessionsViewModel = viewModel(factory = ScreenModelFactory { SessionsViewModel(container.sessionRepository) })
                SessionsScreen(model, navigation::back, onNew = { navigation.editSession(null) }, onOpen = navigation::editSession)
            }
            Route.SESSION_EDITOR -> {
                val model: SessionEditorViewModel = viewModel(key = "session-${navigation.sessionEditorKey}", factory = ScreenModelFactory {
                    SessionEditorViewModel(container.sessionRepository, container.pitchRepository, container.playerRepository, navigation.sessionId, navigation.sessionDetailOrigin, navigation.sessionCreationPlayerId, context.applicationContext.contentResolver)
                })
                SessionEditorScreen(model, navigation::back, navigation::editSession, navigation::showSessionAnalysis)
            }
            Route.SESSION_ANALYSIS -> {
                navigation.analysisSessionId?.let { id ->
                    val model: SessionAnalysisViewModel = viewModel(factory = ScreenModelFactory { SessionAnalysisViewModel(container.sessionRepository,container.pitchRepository,container.playerRepository,id) })
                    SessionAnalysisScreen(model,navigation::back) { navigation.editSession(id) }
                }
            }
            Route.BACKUP -> {
                val model: BackupViewModel = viewModel(factory = ScreenModelFactory { BackupViewModel(context.applicationContext, container.database, pinStore) })
                BackupScreen(model, navigation::back)
            }
            Route.HISTORY, Route.DISCARDED_SESSIONS -> {
                val discarded = navigation.route == Route.DISCARDED_SESSIONS
                val playerId = navigation.historyPlayerId
                val model: SessionsViewModel = viewModel(factory = ScreenModelFactory { SessionsViewModel(container.sessionRepository, history = !discarded, discarded = discarded, playerId = playerId) })
                SessionsScreen(model, navigation::back, onNew = {}, onOpen = { id -> navigation.editSession(id, if (discarded) SessionDetailOrigin.DELETED_SESSIONS else SessionDetailOrigin.NORMAL_HISTORY) }, historyTitle = if (discarded) "Sesiones eliminadas" else if (playerId != null) "Historial de sesiones" else "Sesiones anteriores", showNew = false, discarded = discarded)
            }
        }
    }
}

@Composable
private fun CoachingAccessGate(
    hasPin: Boolean,
    loadProfile: suspend () -> Boolean,
    resolveState: (Boolean, Boolean, Boolean, Boolean) -> CoachingConfigurationState,
    onState: (CoachingConfigurationState) -> Unit,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<CoachingConfigurationState>(CoachingConfigurationState.Loading) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(hasPin) {
        debugAccessLog(
            previous = state,
            next = CoachingConfigurationState.Loading,
            profileLoadCompleted = false,
            profilePresent = null,
            pinCheckCompleted = true,
            pinPresent = hasPin,
            reason = "profile_load_started"
        )
        try {
            val hasProfile = withContext(Dispatchers.IO) { loadProfile() }
            val resolved = resolveState(true, hasProfile, true, hasPin)
            debugAccessLog(
                previous = state,
                next = resolved,
                profileLoadCompleted = true,
                profilePresent = hasProfile,
                pinCheckCompleted = true,
                pinPresent = hasPin,
                reason = "both_checks_completed"
            )
            state = resolved
        } catch (_: Exception) {
            debugAccessLog(
                previous = state,
                next = CoachingConfigurationState.Loading,
                profileLoadCompleted = false,
                profilePresent = null,
                pinCheckCompleted = true,
                pinPresent = hasPin,
                reason = "profile_load_failed"
            )
            error = true
        }
    }
    LaunchedEffect(state) { if (state !is CoachingConfigurationState.Loading) onState(state) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(androidx.compose.ui.res.stringResource(com.example.radarcamera.R.string.coaching_access_title), style = MaterialTheme.typography.headlineMedium)
            Text(androidx.compose.ui.res.stringResource(if (error) com.example.radarcamera.R.string.coaching_access_load_error else com.example.radarcamera.R.string.coaching_access_loading))
            if (error) androidx.compose.material3.OutlinedButton(onClick = onBack) { Text("Volver") }
        }
    }
}

private fun debugAccessLog(
    previous: CoachingConfigurationState,
    next: CoachingConfigurationState,
    profileLoadCompleted: Boolean,
    profilePresent: Boolean?,
    pinCheckCompleted: Boolean,
    pinPresent: Boolean,
    reason: String
) {
    if (BuildConfig.DEBUG) {
        Log.d(
            "CoachingAccess",
            "t=${SystemClock.elapsedRealtime()} previous=$previous next=$next " +
                "profileLoadCompleted=$profileLoadCompleted profilePresent=$profilePresent " +
                "pinCheckCompleted=$pinCheckCompleted pinPresent=$pinPresent reason=$reason"
        )
    }
}
