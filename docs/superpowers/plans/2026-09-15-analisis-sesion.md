# Análisis posterior de sesión Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mostrar análisis local y de solo lectura de una sesión finalizada desde sus pitches persistidos.

**Architecture:** Un motor puro genera modelos inmutables; un ViewModel observa por `sessionId`; Canvas presenta la geometría sin dependencias de gráficas. La ruta pasa solo `sessionId` y vuelve al detalle.

**Tech Stack:** Kotlin, Flow, Room existente, Jetpack Compose Canvas, CameraX/Media3 sin cambios.

**Spec:** `docs/DISENO_ANALISIS_SESION.md`

## Global Constraints

- No migrar Room ni modificar `SessionEntity`/`PitchEntity`.
- Excluir sesiones descartadas; no escribir sesiones, pitches, URI ni videos.
- No modificar radar, endpoint, cursor, cámara, pre-roll 4+1, MediaStore o firmware.
- No añadir dependencias; compilar y probar antes de cada commit.

---

### Task 1: Motor de análisis puro

**Files:**
- Create: `app/src/main/java/com/example/radarcamera/domain/analysis/SessionAnalysis.kt`
- Test: `app/src/test/java/com/example/radarcamera/domain/analysis/SessionAnalysisEngineTest.kt`

- [ ] Escribir pruebas para lista vacía, un pitch, tipos múltiples, velocidades iguales, orden por número/fecha/id, porcentajes y tipos vacíos.
- [ ] Ejecutar `gradlew.bat testDebugUnitTest --tests com.example.radarcamera.domain.analysis.SessionAnalysisEngineTest`; debe fallar por símbolos inexistentes.
- [ ] Implementar `SessionAnalysisEngine.analyze(pitches)` y modelos `SessionAnalysis`, `PitchPoint`, `PitchTypeSummary`; filtrar MPH no finitos de estadísticas, nunca de la secuencia.
- [ ] Ejecutar la misma prueba; debe pasar.
- [ ] Commit: `feat: agregar motor de análisis de sesiones`.

### Task 2: Lectura y ViewModel

**Files:**
- Modify: `data/repository/SessionRepository.kt`, `data/repository/PitchRepository.kt`
- Create: `ui/analysis/SessionAnalysisViewModel.kt`
- Test: `app/src/androidTest/java/com/example/radarcamera/data/SessionAnalysisDataTest.kt`

- [ ] Escribir pruebas Room para sesión finalizada, archivada/restaurada, vacía, inexistente y descartada excluida.
- [ ] Ejecutar `assembleDebugAndroidTest`; las pruebas nuevas deben compilar antes de producción.
- [ ] Exponer observaciones de sesión activa/no descartada y jugador por ID; combinar con pitches en ViewModel y mapear Loading/Empty/Content/NotFound/Error.
- [ ] Ejecutar pruebas JVM y Android build; crear commit `feat: observar análisis local de sesiones`.

### Task 3: Navegación y detalle

**Files:**
- Modify: `navigation/NavigationViewModel.kt`, `navigation/RadarRootApp.kt`, `ui/sessions/SessionEditorScreen.kt`
- Test: `app/src/test/java/com/example/radarcamera/navigation/SessionAnalysisNavigationTest.kt`

- [ ] Escribir prueba de `showSessionAnalysis(sessionId)` y `back()` hacia el detalle correcto.
- [ ] Añadir `SESSION_ANALYSIS`, `analysisSessionId` y ruta estable; el botón “Ver análisis” se muestra solo para sesión finalizada no descartada.
- [ ] Ejecutar `testDebugUnitTest`; crear commit `feat: navegar al análisis de sesiones`.

### Task 4: UI y gráficas Canvas

**Files:**
- Create: `ui/analysis/SessionAnalysisScreen.kt`, `ui/analysis/AnalysisCharts.kt`
- Test: `app/src/test/java/com/example/radarcamera/ui/analysis/AnalysisChartGeometryTest.kt`

- [ ] Escribir pruebas de escalas para cero, uno, velocidades iguales y decimales.
- [ ] Implementar encabezado, resumen textual, dona con leyenda, serie completa, selector por tipo y serie filtrada; usar `CoachingPage` desplazable.
- [ ] Implementar acciones para volver al detalle, sin modificar datos.
- [ ] Ejecutar `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`, `git diff --check`; crear commit `feat: mostrar análisis posterior de sesión`.

### Task 5: Entrega

- [ ] Actualizar `docs/SEGUIMIENTO.md` con límites y pruebas físicas pendientes.
- [ ] Revisar el diff completo y los commits.
- [ ] Instalar una sola APK final con `adb install -r`; verificar inicio sin afirmar validación física de gráficas.
