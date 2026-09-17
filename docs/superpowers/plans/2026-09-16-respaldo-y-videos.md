# Respaldo local y videos organizados Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Guardar nuevos MP4 en carpetas deterministas de MediaStore y exportar/restaurar datos deportivos locales de forma validable.

**Architecture:** Un builder puro genera destinos MediaStore; Room conserva sus metadatos por pitch. Un subsistema `backup` independiente transforma snapshots Room a DTO versionados, los valida y restaura transaccionalmente; Compose solo coordina SAF y estados.

**Tech Stack:** Kotlin, Room 2.8.5, Compose, MediaStore, Storage Access Framework, SHA-256.

**Spec:** `docs/superpowers/specs/2026-09-16-respaldo-y-videos-design.md`

## Global Constraints

- No incluir MP4, PIN, credenciales, RAW, caché ni logs en `.radarbackup`.
- Preservar Media3, CameraX, recorte 4+1, overlay, pitchId y MediaStore.
- Los archivos heredados no se mueven ni se asocian.
- `connectedDebugAndroidTest` solo puede usar `emulator-*` o dispositivo aislado.

---

### Task 1: Metadatos y rutas de MediaStore

**Files:** crear `camera/MediaPathBuilder.kt`, pruebas JVM; modificar `PitchEntity`, `RadarDatabase`, `PitchDao`, `PitchRepository`, `SessionVideoCapture`, `VideoOverlayExporter`.

- [ ] Escribir pruebas fallidas de sanitización, jugadores repetidos, fallback, fecha local, MPH a una decimal y nombre duplicado.
- [ ] Ejecutar las pruebas JVM y comprobar que fallan por APIs ausentes.
- [ ] Implementar `MediaPathBuilder`, pasar `VideoDestination` inmutable desde el pitch al exportador y publicar por `RELATIVE_PATH`/`DISPLAY_NAME` sin sobrescribir.
- [ ] Añadir migración 5→6 con columnas anulables `videoRelativePath` y `videoDisplayName`; persistirlas junto con la URI.
- [ ] Ejecutar pruebas JVM, `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest` y `git diff --check`.
- [ ] Commit: `feat: organizar videos nuevos por jugador y sesión`.

### Task 2: Núcleo de respaldo versionado

**Files:** crear `backup/BackupModels.kt`, `BackupExporter.kt`, `BackupValidator.kt`, `BackupRestorer.kt`, `VideoRelinker.kt` y pruebas JVM/instrumentadas; modificar DAOs, repositorios y contenedor.

- [ ] Escribir pruebas fallidas para serialización determinista, checksum, formato futuro, campos desconocidos, vacío, IDs/relaciones y PIN ausente.
- [ ] Implementar DTO v1 y snapshot transaccional ordenado; exportar JSON con manifest y checksum.
- [ ] Validar todo antes de restaurar; insertar jugadores, sesiones y pitches en una única transacción y fallar con rollback completo.
- [ ] Implementar reenlace no destructivo por URI y coincidencia única de ruta/nombre.
- [ ] Ejecutar unitarias y, solo con serial `emulator-*`, instrumentadas de round-trip, migración 5→6 y rollback.

### Task 3: Pantalla Datos y respaldo

**Files:** crear `ui/backup/BackupScreen.kt`, `BackupViewModel.kt`; modificar navegación, dashboard y factoría.

- [ ] Escribir pruebas de estado para exportación bloqueada durante video activo, validación previa, confirmación PIN y error recuperable.
- [ ] Implementar selector SAF de creación/apertura, resumen, advertencias, PIN actual y estados loading/error/success.
- [ ] Navegar desde Coaching sin pasar datos por rutas; el ViewModel obtiene todo de repositorios.
- [ ] Ejecutar verificaciones, actualizar `SEGUIMIENTO.md`, revisar diff y commit `feat: agregar respaldo y restauración local`.

### Task 4: Entrega segura

- [ ] Verificar que no haya `emulator-*` antes de omitir instrumentadas; nunca usar `8puswcmzqo6xvcnn`.
- [ ] Ejecutar `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest` y `git diff --check`.
- [ ] Instalar únicamente con `adb install -r` en el teléfono físico y comprobar inicio; no probar restauración sin respaldo exportado verificable.
- [ ] Actualizar seguimiento con pruebas físicas pendientes.
