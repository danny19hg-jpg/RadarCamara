# Player-Centered Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans task-by-task. Steps use checkbox syntax.

**Goal:** Reorganizar Coaching alrededor de jugadores sin perder PIN, Room 7, sesiones, videos ni respaldo.

**Architecture:** Introducir destinos tipados y un controlador de acceso monotónico antes de mover pantallas. Las pantallas existentes se reutilizan mediante argumentos `playerId`, `sessionId` y `SessionDetailOrigin`; Room permanece esquema 7.

**Tech Stack:** Kotlin, Jetpack Compose, ViewModel, Room 2.8.5, CameraX, Media3.

**Spec:** `docs/superpowers/specs/2026-09-16-player-centered-navigation-design.md`

## Global Constraints

- No modificar firmware, endpoint `/status`, cursor, cámara ni recorte 4+1.
- No migrar Room ni borrar PIN, entrenador, jugadores, sesiones, pitches o videos.
- `BuildConfig.VERSION_NAME` es la única versión visible; etapa 1 usa `0.9.0-dev` e incrementa `versionCode`.
- Nunca ejecutar `connectedDebugAndroidTest` en `8puswcmzqo6xvcnn`; sólo emulador/dispositivo aislado.
- Cada tarea termina con `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest`, `git diff --check`, `adb install -r`, prueba física y commit; si falla, conservar commit previo y revertir sólo mediante `git revert` del commit de etapa.

## Estructura objetivo

| Archivo | Responsabilidad |
|---|---|
| `security/CoachingAccessController.kt` | Estado de acceso en memoria y expiración monotónica. |
| `navigation/CoachingDestination.kt` | Destinos sellados con IDs/origen inmutables. |
| `navigation/NavigationViewModel.kt` | Pila tipada y retorno determinista. |
| `ui/players/PlayerDetailScreen.kt` | Ficha central del jugador. |
| `ui/settings/SettingsScreen.kt` | Entrenador, PIN, respaldo, eliminadas y versión. |
| `ui/reports/PlayerReportsScreen.kt` | Reportes filtrados por `playerId`. |

### Task 1: Seguridad y acceso a Coaching

**Files:** crear `security/CoachingAccessController.kt`, `security/MonotonicClock.kt`, `security/InitialCoachingSetupScreen.kt`, tests `security/CoachingAccessControllerTest.kt`; modificar `PinStore.kt`, `PinScreen.kt`, `RadarRootApp.kt`, `app/build.gradle.kts`.

**Interfaces:** `MonotonicClock.nowMs(): Long`; `CoachingAccessController.onForeground(): AccessResult`, `onBackground()`, `unlock()`, `lock()`; `AccessResult.Allowed|Locked`.

- [x] Escribir test fallido: PIN/entrenador existentes no abre setup; 299999 ms conserva acceso; 300000 ms bloquea; proceso nuevo inicia bloqueado; Radar Live no consulta acceso.
- [x] Ejecutar `gradlew.bat testDebugUnitTest --no-daemon --tests '*CoachingAccessControllerTest'`; falló inicialmente por clases inexistentes.
- [x] Implementar reloj inyectable, setup sólo cuando faltan perfil y PIN, confirmación de PIN, observación de `ProcessLifecycleOwner` y versión `0.9.0-dev`/nuevo `versionCode`.
- [x] Ejecutar comandos globales; aceptación: no se persiste desbloqueo, datos existentes no cambian.
- [x] Commit `feat: agregar acceso temporal a Coaching`; instalar y probar PIN, fondo 4:59/5:00 y Radar Live; revertir con `git revert` si falla. La comprobación física queda pendiente antes de iniciar la Etapa 2.

### Task 2: Navegación tipada

**Files:** crear `navigation/CoachingDestination.kt`, `navigation/NavigationStack.kt`, tests `navigation/NavigationStackTest.kt`; modificar `NavigationViewModel.kt`, `RadarRootApp.kt`, `NavigationAccessTest.kt`.

**Interfaces:** `CoachingDestination.PlayerList`, `PlayerDetail(playerId)`, `PlayerHistory(playerId)`, `Session(sessionId, origin)`, `DeletedSessions(playerId?)`, `Settings`; `push`, `replace`, `pop`.

- [ ] Test fallido: pop de detalle normal vuelve a historial del mismo playerId; eliminado vuelve a su lista; no duplica destino al reemplazar sesión.
- [ ] Ejecutar test focalizado; esperar fallo por pila inexistente.
- [ ] Implementar pila como única fuente de verdad y adaptar rutas existentes, preservando `SessionDetailOrigin` de `ed81c5e`.
- [ ] Ejecutar comandos globales; aceptación: ningún back llega a Nueva sesión por accidente.
- [ ] Commit `refactor: usar navegación tipada`; instalar y recorrer back; revertir con `git revert`.

### Task 3: Jugadores y ficha central

**Files:** crear `ui/players/PlayerDetailScreen.kt`, `PlayerDetailViewModel.kt`, tests `PlayerDetailViewModelTest.kt`; modificar `PlayersScreen.kt`, `PlayersViewModel.kt`, `PlayerRepository.kt`, `PlayerDao.kt`, `RadarRootApp.kt`.

**Interfaces:** `PlayerDetailState(player, openSessionId, archived)`; `PlayerRepository.observeOpenSession(playerId)`; búsqueda local normalizada por nombre.

- [ ] Test fallido: búsqueda filtra nombre; jugador activo con abierta expone Continuar; archivado no expone Nueva sesión.
- [ ] Ejecutar test focalizado; esperar fallo por estado inexistente.
- [ ] Implementar lista entrada de Coaching, búsqueda, ficha, archivados y acciones perfil/historial/reportes/editar/archivar-restaurar.
- [ ] Ejecutar comandos globales; aceptación: historial no se pierde al archivar.
- [ ] Commit `feat: centrar Coaching en jugadores`; instalar y probar activo/archivado; revertir con `git revert`.

### Task 4: Sesiones por jugador

**Files:** modificar `SessionRepository.kt`, `SessionDao.kt`, `SessionEditorViewModel.kt`, `SessionEditorScreen.kt`, `SessionsScreen.kt`, `RadarRootApp.kt`; crear tests `PlayerSessionFlowTest.kt`.

**Interfaces:** `createForPlayer(playerId, sport): CreateSessionResult.Created(sessionId)|OpenSession(sessionId)|ArchivedPlayer`; `observeFinished(playerId)`; `observeDiscarded(playerId?)`.

- [ ] Test fallido: segunda creación devuelve abierta; archivado es rechazado; nueva sesión sólo requiere deporte; historial/eliminadas filtran playerId.
- [ ] Ejecutar test focalizado; esperar fallo por API inexistente.
- [ ] Implementar resultado único, Continuar sesión, rutas de ficha y filtros; mantener radar/cámara/pre-roll/video/tipo dentro de sesión activa.
- [ ] Ejecutar comandos globales; aceptación: no se duplica sesión ni se altera captura.
- [ ] Commit `feat: vincular sesiones al jugador`; instalar y probar sesión activa/historial/videos; revertir con `git revert`.

### Task 5: Reportes y configuración

**Files:** crear `ui/reports/PlayerReportsScreen.kt`, `PlayerReportsViewModel.kt`, `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, tests `PlayerReportsViewModelTest.kt`, `SettingsViewModelTest.kt`; modificar `SessionAnalysisViewModel.kt`, `CoachRepository.kt`, `PinStore.kt`, `BackupScreen.kt`, `PantallaInicio`/`RadarAccess_v0_7.kt`, `RadarRootApp.kt`.

**Interfaces:** `PlayerReportsViewModel(playerId)` usa pitches persistidos; `changePin(current,new,confirmation)`; configuración navega a eliminadas globales y respaldo.

- [ ] Test fallido: reportes sólo observan playerId; PIN actual incorrecto no cambia hash; inicio sólo expone Live/Coaching/Configuración/version.
- [ ] Ejecutar test focalizado; esperar fallo por pantallas/API inexistentes.
- [ ] Implementar reportes filtrados, edición entrenador/PIN, configuración y menú reducido sin eliminar entidades.
- [ ] Ejecutar comandos globales; aceptación: respaldo y eliminadas globales siguen accesibles.
- [ ] Commit `feat: agregar configuración y reportes por jugador`; instalar y probar PIN, backup y reportes; revertir con `git revert`.

### Task 6: Integración y versión estable

**Files:** modificar tests anteriores, `docs/ESTADO_ACTUAL.md`, `docs/SEGUIMIENTO.md`, `app/build.gradle.kts` sólo tras validación física.

- [ ] Escribir tests de recorridos completos: instalación existente, proceso/fondo, back, activo/archivado, abierta, historial/análisis/videos, eliminadas, backup.
- [ ] Ejecutar tests; esperar cualquier cobertura ausente revelada por fallos.
- [ ] Corregir exclusivamente la causa de cada fallo y repetir comandos globales.
- [ ] Tras prueba física completa, cambiar a `0.9.0`, incrementar `versionCode`, documentar validación y crear commit `chore: cerrar navegación centrada en jugadores`.
- [ ] Instalar actualización y verificar PIN, Room, videos y respaldo; si falla, `git revert` del commit de cierre, sin desinstalar.

## Matriz requisito → tarea

| Requisito | Tarea |
|---|---|
| Setup/PIN/5 minutos/Radar Live libre | 1 |
| Destinos, argumentos y back | 2 |
| Lista, búsqueda, ficha, archivados | 3 |
| Nueva/continuar, historial y eliminadas por jugador | 4 |
| Reportes, configuración y menú principal | 5 |
| Recorridos, accesibilidad y versión estable | 6 |

## Autorrevisión

Cobertura completa de la especificación: sí. Room 7 se conserva en las seis tareas: sí. No hay instrucciones incompletas ni dependencias implícitas: las interfaces producidas por cada tarea preceden a sus consumidores. Cada tarea deja APK comprobable y un commit reversible.
