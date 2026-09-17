# Plan de implementación: perfil deportivo e historial físico del jugador

## Estado y fuente

Plan técnico creado el 2026-09-17. Está planificado, no implementado. La fuente normativa es [docs/superpowers/specs/2026-09-17-player-profile-measurements-design.md](../specs/2026-09-17-player-profile-measurements-design.md).

No se implementan tareas durante la creación de este documento. `coaching-radar.log` permanece sin seguimiento.

## Reglas de ejecución

- Kotlin, Jetpack Compose y Room; conservar la separación existente entre `data.local`, `data.repository`, `domain`, `ui`, `backup`, `navigation` y `di`.
- Cada tarea sigue RED → GREEN → regresión → commit. RED significa crear primero una prueba concreta, ejecutar el alcance indicado y confirmar el fallo esperado; después se implementa lo mínimo y se repite el mismo comando para GREEN.
- Las pruebas JVM se pueden ejecutar con `testDebugUnitTest --tests ...`.
- Las pruebas instrumentadas sólo se compilan con `assembleDebugAndroidTest` en el flujo normal. Su ejecución queda reservada a un emulador o dispositivo aislado previamente identificado; nunca al teléfono con datos.
- No ejecutar `connectedDebugAndroidTest` en el teléfono físico.
- No ejecutar comandos que desinstalen, limpien datos o reemplacen datos. La instalación física final será únicamente `adb install -r` sobre una instalación existente y sólo después de autorización explícita.
- `versionName` permanece `0.9.0-dev`; `versionCode` pasa de 8 a 9 en la tarea de versión.
- No tocar sesiones históricas, pitches, videos, PIN, radar, firmware, CameraX, Media3 ni protocolo ESP32.
- Los archivos `.rpb` históricos no se leen, importan, convierten, modifican ni eliminan.

## Contratos nuevos

Estos nombres son los contratos que el plan introduce y que cada tarea debe respetar:

- `MeasurementKind`: enum con exactamente `INITIAL`, `PERIODIC` y `HEIGHT_CORRECTION`.
- `PlayerMeasurementEntity`: entidad Room con `id`, `playerId`, `measuredOnEpochDay`, `heightCm`, `weightKg`, `kind` y `createdAt`.
- `MeasurementConverters`: funciones `@TypeConverter fun measurementKindToStorage(value: MeasurementKind): String` y `@TypeConverter fun storageToMeasurementKind(value: String): MeasurementKind`; una cadena desconocida debe lanzar error.
- `PlayerMeasurementDao`: inserción, consulta de historial por jugador, última medición, conteo y eliminación explícita por jugador.
- `PlayerMeasurementRepository`: validación por fecha/edad y escritura transaccional de medición más snapshot actual de `PlayerEntity`.
- `PlayerLevel`: enum con exactamente `BEGINNER`, `INTERMEDIATE` y `ADVANCED`, con etiquetas `Principiante`, `Intermedio` y `Avanzado`; el valor persistido en `PlayerEntity.category` seguirá usando esas etiquetas canónicas.
- Identificador determinista: UUID de nombre con UTF-8 y las semillas `migration-7-8:<playerId>` y `backup-format-1:<playerId>` para las dos fuentes heredadas.
- `BackupDocument` formato 2: conserva el modelo de formato 1 y añade `measurements` al payload y al manifest. Formato 1 significa exclusivamente JSON `.radarbackup`; `.rpb` no participa.

## Tarea 1: modelo, enum, converters y DAO

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/data/local/RadarDatabase.kt`
- `app/src/main/java/com/example/radarcamera/data/local/PlayerEntity.kt`

**Archivos nuevos:**

- `app/src/main/java/com/example/radarcamera/domain/MeasurementKind.kt`
- `app/src/main/java/com/example/radarcamera/data/local/PlayerMeasurementEntity.kt`
- `app/src/main/java/com/example/radarcamera/data/local/MeasurementConverters.kt`
- `app/src/main/java/com/example/radarcamera/data/local/PlayerMeasurementDao.kt`
- `app/src/test/java/com/example/radarcamera/data/local/PlayerMeasurementEntityTest.kt`

**Interfaces:** `PlayerMeasurementEntity` consume `PlayerEntity.id`; `PlayerMeasurementDao` produce consultas y mutaciones para el repositorio; `MeasurementConverters` produce almacenamiento textual Room.

**RED:** crear `PlayerMeasurementEntityTest` con valores nulos permitidos sólo en `heightCm`/`weightKg`, los tres `MeasurementKind`, y orden esperado; ejecutar `.\gradlew.bat testDebugUnitTest --tests com.example.radarcamera.data.local.PlayerMeasurementEntityTest`; debe fallar porque los tipos no existen.

**Implementación:** definir la entidad, foreign key `playerId -> players.id` con `RESTRICT`, índice no único `(playerId, measuredOnEpochDay)`, converters estrictos y DAO con orden `measuredOnEpochDay DESC`, `createdAt DESC`, `id DESC`. No cambiar aún la versión efectiva de Room hasta la tarea 2.

**GREEN/regresión:** repetir la prueba RED; después ejecutar la clase y las pruebas JVM de dominio existentes.

**Commit:** incluir sólo los cinco archivos nuevos y los cambios mínimos de registro necesarios en `RadarDatabase.kt`; mensaje `feat: definir modelo de mediciones del jugador`.

**Checkpoint:** revisar que no exista `ROUTINE`, `measuredAt`, cascada de borrado ni campo temporal que calcule edad.

## Tarea 2: migración Room 7→8 y schema exportado

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/data/local/RadarDatabase.kt`
- `app/build.gradle.kts`
- `app/schemas/com.example.radarcamera.data.local.RadarDatabase/7.json`

**Archivos nuevos esperados:**

- `app/schemas/com.example.radarcamera.data.local.RadarDatabase/8.json`
- `app/src/androidTest/java/com/example/radarcamera/data/RadarDatabaseMigration8Test.kt`

**Interfaces:** `RadarDatabase` añade `measurements(): PlayerMeasurementDao`, registra `MIGRATION_7_8` y pasa a versión 8; Room genera y valida el schema 8.

**RED:** crear una base SQLite schema 7 con jugador y valores físicos y comprobar en `RadarDatabaseMigration8Test` que exista `player_measurements`; compilar la prueba con `.\gradlew.bat assembleDebugAndroidTest`; el primer intento debe fallar porque no existe `MIGRATION_7_8`/schema 8.

**Implementación:** añadir la entidad a `@Database`, registrar converters, crear tabla e índices sin fallback destructivo y registrar la migración. No alterar `sessions`, `pitches`, coaches, PIN ni MediaStore.

**GREEN/regresión:** ejecutar sólo la prueba en emulador/dispositivo aislado identificado si se requiere validación instrumentada; en cualquier caso ejecutar `assembleDebugAndroidTest` y revisar el schema 8 generado. Nunca ejecutar en el teléfono físico.

**Commit:** incluir `RadarDatabase.kt`, `8.json` y `RadarDatabaseMigration8Test.kt`; mensaje `feat: migrar Room a schema 8 para mediciones`.

**Checkpoint:** confirmar que schema 7 conserva identidad y que schema 8 sólo agrega la tabla de mediciones y sus índices.

## Tarea 3: migración inicial de estatura y peso

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/data/local/RadarDatabase.kt`
- `app/src/main/java/com/example/radarcamera/data/local/PlayerEntity.kt`

**Archivos de prueba:**

- `app/src/androidTest/java/com/example/radarcamera/data/RadarDatabaseMigration8Test.kt`

**Interfaces:** `MIGRATION_7_8` consume `updatedAt`, `createdAt`, `heightCm`, `weightKg` y `id` de `players`; produce una medición `INITIAL` o ninguna.

**RED:** añadir casos para timestamp preferido `updatedAt`, fallback `createdAt`, fallback al instante de migración, jugador sin valores y conservación exacta de snapshots; deben fallar antes de la lógica de migración.

**Implementación:** elegir el primer timestamp positivo; convertir una sola vez con `ZoneId.systemDefault()` a `LocalDate.toEpochDay()`; usar ID `UUID.nameUUIDFromBytes("migration-7-8:<playerId>".toByteArray(Charsets.UTF_8))`; insertar sólo si existe estatura o peso; no modificar los valores originales.

**GREEN/regresión:** repetir la prueba instrumentada en entorno aislado; compilar `assembleDebugAndroidTest` y conservar las pruebas existentes de migraciones.

**Commit:** incluir migración y pruebas de casos heredados; mensaje `feat: migrar mediciones fisicas existentes`.

**Checkpoint:** inspeccionar que sesiones, pitches, referencias de video y PIN no hayan sido tocados por la migración.

## Tarea 4: reglas de edad y tipos de medición

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/domain/PlayerDraft.kt`

**Archivos nuevos:**

- `app/src/main/java/com/example/radarcamera/domain/PlayerLevel.kt`
- `app/src/main/java/com/example/radarcamera/domain/PlayerMeasurementRules.kt`
- `app/src/test/java/com/example/radarcamera/domain/PlayerMeasurementRulesTest.kt`

**Interfaces:** `PlayerMeasurementRules` recibe `birthDate`, `measuredOnEpochDay`, `MeasurementKind`, estatura y peso; produce errores de validación y edad calculada.

**RED:** cubrir fecha de cumpleaños número 18, menor de 18, adulto, `PERIODIC` con estatura bloqueada, `HEIGHT_CORRECTION` permitido, valores parciales y enum desconocido; ejecutar `.\gradlew.bat testDebugUnitTest --tests com.example.radarcamera.domain.PlayerMeasurementRulesTest`; debe fallar por ausencia de reglas.

**Implementación:** usar `LocalDate.ofEpochDay(measuredOnEpochDay)` y `Period.between(birthDate, measuredOnDate).years`; validar estatura `0 < x <= 300`, peso `0 < x <= 500`, al menos un valor, y las reglas exactas por `MeasurementKind`.

**GREEN/regresión:** repetir la clase y `PlayerValidationTest`.

**Commit:** incluir `PlayerLevel.kt`, `PlayerMeasurementRules.kt` y su prueba; mensaje `feat: definir reglas de edad y mediciones`.

**Checkpoint:** confirmar que `createdAt` no interviene en edad, fecha visual ni validación.

## Tarea 5: repositorio y transacciones de medición

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/data/repository/PlayerRepository.kt`
- `app/src/main/java/com/example/radarcamera/data/local/PlayerDao.kt`
- `app/src/main/java/com/example/radarcamera/di/AppContainer.kt`

**Archivos nuevos:**

- `app/src/main/java/com/example/radarcamera/data/repository/PlayerMeasurementRepository.kt`
- `app/src/androidTest/java/com/example/radarcamera/data/PlayerMeasurementRepositoryTest.kt`

**Interfaces:** `PlayerMeasurementRepository` recibe `playerId`, fecha epoch day, valores y `MeasurementKind`; produce la medición insertada y actualiza `PlayerEntity` en una transacción. `AppContainer` produce el repositorio compartido.

**RED:** probar creación parcial, actualización de ambos snapshots, valor ausente que no borra el otro, rollback y rechazo de jugador inexistente; ejecutar la clase instrumentada en emulador aislado; debe fallar antes del repositorio.

**Implementación:** usar `database.withTransaction`, validar con `PlayerMeasurementRules`, insertar la medición, actualizar jugador con revisión/`updatedAt`, y exponer flujo ordenado y última medición. El ViewModel conservará `saving` y el ID de operación para evitar doble toque.

**GREEN/regresión:** repetir tests en emulador aislado y compilar el APK de tests; conservar `RoomPersistenceTest` sin cambios destructivos.

**Commit:** incluir repositorio, actualizaciones DAO necesarias, `AppContainer` y tests; mensaje `feat: persistir mediciones junto al perfil`.

**Checkpoint:** provocar un fallo entre inserción y actualización en una prueba controlada y confirmar rollback completo.

## Tarea 6: eliminación explícita y archivo/restauración

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/data/repository/PlayerRepository.kt`
- `app/src/main/java/com/example/radarcamera/data/local/PlayerDao.kt`

**Archivos de prueba:**

- `app/src/androidTest/java/com/example/radarcamera/data/PlayerMeasurementRepositoryTest.kt`
- `app/src/androidTest/java/com/example/radarcamera/data/RoomPersistenceTest.kt`

**Interfaces:** `PlayerRepository.setArchived` conserva mediciones; `deletePermanently` elimina mediciones explícitamente sólo después de `PlayerHistoryChecker`/`PlayerDeletionPolicy` autorizarlo.

**RED:** probar que archivar/restaurar conserva historial, que jugador con sesiones no se elimina y que jugador sin historial elimina mediciones y perfil; debe fallar antes de conectar el DAO de eliminación.

**Implementación:** ejecutar eliminación de mediciones y jugador en una misma transacción; no usar cascade; preservar el comportamiento de archivo actual.

**GREEN/regresión:** ejecutar en emulador aislado y compilar instrumentadas; revisar que no se toquen sesiones o pitches.

**Commit:** incluir repositorio/DAO y pruebas; mensaje `feat: proteger mediciones al archivar y eliminar`.

**Checkpoint:** comprobar que la foreign key `RESTRICT` no permite huérfanos y que la eliminación autorizada borra primero mediciones.

## Tarea 7: formulario de jugador

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/ui/players/PlayerEditorScreen.kt`
- `app/src/main/java/com/example/radarcamera/ui/players/PlayerEditorViewModel.kt`
- `app/src/main/java/com/example/radarcamera/domain/PlayerDraft.kt`
- `app/src/test/java/com/example/radarcamera/ui/players/PlayerEditorStateTest.kt`
- `app/src/test/java/com/example/radarcamera/domain/PlayerValidationTest.kt`

**Archivos nuevos:**

- `app/src/test/java/com/example/radarcamera/ui/players/PlayerEditorFormTest.kt`

**Interfaces:** `PlayerEditorViewModel` consume `PlayerDraft`/`PlayerLevel` y el repositorio; `PlayerEditorScreen` produce eventos `edit` y `save` sin exponer `teamAcademy`.

**RED:** probar que crear inicia deporte y nivel sin selección, que Guardar se bloquea hasta elegirlos, que aparecen Béisbol/Softbol y los tres niveles, que no aparece Equipo/academia y que una categoría antigua exige selección; la prueba Compose/JVM debe fallar antes de modificar UI/estado.

**Implementación:** quitar `PlayerDraft` implícito `BASEBALL` del flujo de creación mediante estado de selección explícita; añadir selectores de deporte y nivel; conservar `teamAcademy` cargado y persistido sin control visual; envolver el formulario en scroll; deshabilitar guardado durante escritura.

**GREEN/regresión:** ejecutar pruebas JVM/Compose disponibles; compilar instrumentadas si la prueba usa Compose Android; conservar `PlayerDetailViewModelTest`.

**Commit:** incluir dominio, ViewModel, pantalla y pruebas; mensaje `feat: hacer obligatorio deporte y nivel del jugador`.

**Checkpoint:** inspeccionar visualmente por código que no exista ningún label/control `Equipo/academia` en el formulario y que no se borre el campo al editar.

## Tarea 8: medición inicial desde el formulario

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/ui/players/PlayerEditorScreen.kt`
- `app/src/main/java/com/example/radarcamera/ui/players/PlayerEditorViewModel.kt`

**Archivos nuevos:**

- `app/src/main/java/com/example/radarcamera/ui/players/PlayerMeasurementEditorState.kt`
- `app/src/test/java/com/example/radarcamera/ui/players/PlayerMeasurementEditorStateTest.kt`

**Interfaces:** el editor produce un draft de medición opcional con `measuredOnEpochDay`, estatura, peso y `INITIAL`; el repositorio consume ese draft después de guardar el jugador.

**RED:** probar creación sin medición, sólo estatura, sólo peso, ambos, fecha por defecto y reglas de edad; debe fallar antes de conectar el editor al repositorio.

**Implementación:** usar fecha de calendario local convertida a epoch day; permitir estatura/peso según edad; ejecutar creación de jugador y medición inicial en una operación transaccional coordinada, evitando duplicar por doble toque.

**GREEN/regresión:** repetir pruebas de estado y repositorio; confirmar que datos antiguos sin medición siguen siendo válidos.

**Commit:** incluir estado, ViewModel/pantalla y pruebas; mensaje `feat: registrar medicion inicial del jugador`.

**Checkpoint:** confirmar que una creación sin valores físicos no crea fila y que una medición parcial sí es válida.

## Tarea 9: ficha e historial físico

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/ui/players/PlayerDetailScreen.kt`
- `app/src/main/java/com/example/radarcamera/ui/players/PlayerDetailViewModel.kt`
- `app/src/main/java/com/example/radarcamera/di/AppContainer.kt`

**Archivos nuevos:**

- `app/src/main/java/com/example/radarcamera/ui/players/PlayerMeasurementsViewModel.kt`
- `app/src/main/java/com/example/radarcamera/ui/players/PlayerMeasurementsScreen.kt`
- `app/src/test/java/com/example/radarcamera/ui/players/PlayerMeasurementsViewModelTest.kt`
- `app/src/test/java/com/example/radarcamera/ui/players/PlayerMeasurementsScreenTest.kt`

**Interfaces:** `PlayerMeasurementsViewModel` consume `PlayerMeasurementRepository` y `playerId`; produce última medición, historial ordenado, estados vacíos, acción Registrar y acción Corregir estatura.

**RED:** probar lista descendente, estados vacíos, botón Registrar, adulto sin estatura editable y corrección `HEIGHT_CORRECTION`; debe fallar antes de existir el ViewModel/pantalla.

**Implementación:** añadir a la ficha última medición, historial, acciones y formulario desplazable; bloquear estatura normal desde 18 años; exigir confirmación para corregir y conservar historial previo.

**GREEN/regresión:** ejecutar pruebas JVM/Compose disponibles y compilar instrumentadas si corresponden.

**Commit:** incluir ViewModel, pantalla, integración de ficha y pruebas; mensaje `feat: mostrar historial fisico del jugador`.

**Checkpoint:** revisar que la ficha archivada conserva lectura del historial y no habilita Nueva sesión.

## Tarea 10: BackupDocument formato 2

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/backup/BackupModels.kt`
- `app/src/main/java/com/example/radarcamera/backup/BackupService.kt`
- `app/src/main/java/com/example/radarcamera/ui/backup/BackupScreen.kt`
- `app/src/main/java/com/example/radarcamera/ui/backup/BackupViewModel.kt`

**Archivos nuevos:**

- `app/src/test/java/com/example/radarcamera/backup/BackupMeasurementFormatTest.kt`

**Interfaces:** `BackupDocument` formato 2 añade `measurements` y su contador; canonicalización y checksum siguen siendo deterministas. El parser debe distinguir formato 1 y formato 2.

**RED:** probar exportación/importación de `measurements`, checksum, orden canónico y rechazo de `MeasurementKind` desconocido; debe fallar con el modelo formato 1 actual.

**Implementación:** ampliar manifest/payload a formato 2, serializar `measuredOnEpochDay`, `heightCm`, `weightKg`, `kind`, `createdAt`, ordenar mediciones por `id`, mantener canonicalización legacy de formato 1 y validar antes de Room.

**GREEN/regresión:** ejecutar pruebas JVM de backup y `BackupCoachRestorePolicyTest`.

**Commit:** incluir `BackupModels.kt`, `BackupMeasurementFormatTest.kt` y cambios de ViewModel/pantalla de conteo; mensaje `feat: exportar mediciones en backup formato 2`.

**Checkpoint:** comparar un backup formato 1 existente y confirmar que su checksum no se recalcula con `measurements`.

## Tarea 11: restauración formato 1 y exclusión `.rpb`

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/backup/BackupModels.kt`
- `app/src/main/java/com/example/radarcamera/backup/BackupService.kt`
- `app/src/test/java/com/example/radarcamera/backup/BackupCoachRestorePolicyTest.kt`

**Archivos nuevos:**

- `app/src/test/java/com/example/radarcamera/backup/BackupLegacyMeasurementTest.kt`

**Interfaces:** `BackupValidator` valida formato 1/2; `BackupRestorer` consume sólo `BackupDocument` JSON `.radarbackup`; ningún componente acepta `.rpb`.

**RED:** probar formato 1 con snapshots físicos, conversión usando `manifest.zoneId`, ID `backup-format-1:<playerId>`, restauración repetida y tipo desconocido; debe fallar antes de la compatibilidad.

**Implementación:** convertir `manifest.exportedAt` con `ZoneId.of(manifest.zoneId)` una sola vez a epoch day; crear ID determinista; restaurar coaches/players/measurements/sessions/pitches en transacción; validar todo antes de borrar Room; rechazar extensión/formato `.rpb` sin abrirlo ni modificarlo.

**GREEN/regresión:** repetir pruebas JVM; para cobertura Room/rollback compilar y ejecutar sólo en emulador aislado. Verificar que PIN y MediaStore siguen fuera de la restauración.

**Commit:** incluir backup parser/restaurador y pruebas; mensaje `feat: restaurar mediciones desde backup legacy`.

**Checkpoint:** confirmar que la restauración reemplaza datos deportivos, conserva sólo el coach local según la política existente y no opera sobre `.rpb`.

## Tarea 12: flujo de sesión y no regresión histórica

**Archivos existentes:**

- `app/src/main/java/com/example/radarcamera/data/repository/SessionRepository.kt`
- `app/src/main/java/com/example/radarcamera/ui/sessions/SessionEditorViewModel.kt`
- `app/src/main/java/com/example/radarcamera/ui/sessions/SessionEditorScreen.kt`
- `app/src/test/java/com/example/radarcamera/navigation/NavigationViewModelTest.kt`
- `app/src/test/java/com/example/radarcamera/ui/sessions/SessionEditorViewModelRegressionTest.kt`
- `app/src/androidTest/java/com/example/radarcamera/data/RoomPersistenceTest.kt`

**Interfaces:** `SessionRepository.create` consume el `playerId` del draft pero produce siempre `player.sport`; `SessionEditorScreen` sólo presenta `Nombre · Deporte` durante creación.

**RED:** añadir prueba que envíe un draft con deporte distinto al perfil y comprobar que falle o revele la divergencia; añadir prueba de que no hay selector jugador/deporte en creación; debe fallar si vuelve a usar el draft.

**Implementación:** mantener/ajustar la protección de `SessionRepository.create`, bloquear cambio de deporte del perfil con sesión abierta, no reescribir `SessionEntity.sport` histórico y conservar navegación contextual.

**GREEN/regresión:** ejecutar las pruebas JVM de navegación/sesión y compilar instrumentadas; no cambiar pitches/videos/cámara.

**Commit:** incluir sólo archivos de sesión y pruebas de no regresión; mensaje `test: blindar herencia del deporte en sesiones`.

**Checkpoint:** crear mentalmente/por prueba una sesión Softbol, cambiar el perfil después de finalizarla y verificar que la sesión histórica sigue Softbol.

## Tarea 13: versión y documentación del bloque

**Archivos existentes:**

- `app/build.gradle.kts`
- `docs/ESTADO_ACTUAL.md`
- `docs/SEGUIMIENTO.md`
- `docs/superpowers/specs/2026-09-17-player-profile-measurements-design.md`

**Interfaces:** Gradle expone `versionName = 0.9.0-dev` y `versionCode = 9`; la documentación registra implementación y alcance sin modificar especificación aprobada.

**RED:** crear una verificación de configuración que espere versionCode 9/versionName 0.9.0-dev; debe fallar con la configuración actual 8/0.9.0-dev.

**Implementación:** cambiar sólo versionCode a 9 y actualizar documentación de estado/seguimiento con el resultado real de pruebas. Mantener `.rpb` fuera de alcance y `coaching-radar.log` sin seguimiento.

**GREEN/regresión:** verificar la configuración con el comando de inspección del proyecto y ejecutar la suite indicada en la tarea 14.

**Commit:** incluir `app/build.gradle.kts` y documentación actualizada; mensaje `chore: versionar perfil fisico del jugador`.

**Checkpoint:** confirmar que versionName no cambió y que no se editaron dependencias, `local.properties` ni archivos generados.

## Tarea 14: suite final, compilación y prueba física

**Archivos implicados:** todos los cambios anteriores; no se agregan nuevos archivos funcionales.

**RED:** antes de la corrección final, ejecutar las pruebas de regresión seleccionadas y registrar cualquier fallo por tarea; no saltar directamente a la APK física.

**Implementación/verificación GREEN:** con JDK de Android Studio:

```text
.\gradlew.bat testDebugUnitTest --rerun-tasks --console=plain
.\gradlew.bat assembleDebug --console=plain
.\gradlew.bat assembleDebugAndroidTest --console=plain
```

`connectedDebugAndroidTest` queda prohibido en el teléfono. Si se requiere ejecución instrumentada, usar exclusivamente un emulador aislado identificado antes.

Revisar además:

```text
git diff --check
git status --short
```

La prueba física final sólo después de todos los checks y autorización: confirmar que `com.example.radarcamera` ya está instalado, ejecutar únicamente `adb install -r app/build/outputs/apk/debug/app-debug.apk`, abrir la aplicación y detenerse para validación manual. Nunca desinstalar, limpiar datos ni restaurar sobre el teléfono desde comandos automáticos.

**Commit final previsto:** no mezclarlo con commits anteriores; si sólo cambia documentación de resultados, mensaje `docs: cerrar validacion del perfil fisico`. No crear etiqueta estable antes de la prueba física.

**Checkpoint:** detenerse para que la persona pruebe creación/edición, deporte, nivel, mediciones, corrección adulta, historial, backup y sesiones. No iniciar la Tarea 4.

## Matriz de trazabilidad

| Requisito de la especificación | Tarea | Prueba principal |
|---|---:|---|
| `PlayerMeasurementEntity`, nullabilidad, `MeasurementKind` | 1 | `PlayerMeasurementEntityTest`, `PlayerMeasurementRulesTest` |
| UUIDs, converters, índices y foreign key | 1 | `PlayerMeasurementEntityTest`, migración Room |
| Room 7→8 no destructiva | 2 | `RadarDatabaseMigration8Test` |
| Migración de estatura/peso existente | 3 | `RadarDatabaseMigration8Test` |
| Edad por `birthDate` + epoch day | 4 | `PlayerMeasurementRulesTest` |
| `PERIODIC` y `HEIGHT_CORRECTION` | 4, 9 | reglas y ViewModel/pantalla de mediciones |
| Transacción medición + snapshot actual | 5 | `PlayerMeasurementRepositoryTest` |
| Deporte obligatorio | 7 | `PlayerEditorFormTest`, `PlayerValidationTest` |
| Nivel obligatorio y categorías antiguas | 7 | `PlayerEditorFormTest` |
| Ocultar `teamAcademy` sin perderlo | 7, 10 | `PlayerEditorFormTest`, backup round-trip |
| Medición inicial desde creación | 8 | `PlayerMeasurementEditorStateTest`, repositorio |
| Última medición e historial ordenado | 9 | `PlayerMeasurementsViewModelTest`, `PlayerMeasurementsScreenTest` |
| Registrar medición | 9 | `PlayerMeasurementsScreenTest` |
| Corrección de estatura adulta | 4, 9 | reglas y ViewModel |
| Estados vacíos, scroll, accesibilidad y doble toque | 7, 8, 9 | pruebas de UI/estado y revisión física |
| Backup formato 2 | 10 | `BackupMeasurementFormatTest` |
| Restauración JSON `.radarbackup` formato 1 | 11 | `BackupLegacyMeasurementTest` |
| Tipos desconocidos rechazados antes de Room | 10, 11 | `BackupMeasurementFormatTest` |
| `.rpb` fuera de alcance | 11 | prueba de rechazo sin lectura/modificación |
| Archivo y restauración del jugador | 6 | `PlayerMeasurementRepositoryTest` |
| Eliminación definitiva explícita | 6 | `RoomPersistenceTest` ampliada |
| Sesión hereda jugador y deporte | 12 | `SessionRepository`/navegación regression tests |
| Sesiones históricas, pitches y videos sin cambios | 3, 6, 12 | migración y `RoomPersistenceTest` |
| PIN y MediaStore sin cambios | 11, 14 | backup/política y prueba física |
| `versionName` 0.9.0-dev y `versionCode` 9 | 13 | verificación de configuración |
| Suite JVM, APK y APK de tests | 14 | `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` |
| Prueba física final sin destrucción | 14 | `adb install -r` y validación manual |

## Auto-revisión y riesgos

- Cobertura: cada apartado de la especificación tiene una tarea y una prueba concreta en la matriz; `.rpb`, PIN, videos, radar, cámara, firmware y Tarea 4 están explícitamente excluidos.
- Nombres: todos los tipos nuevos tienen archivo, responsabilidad y firma definida; los nombres existentes se basan en el código inspeccionado.
- Concreción: no se usan marcadores de trabajo sin resolver, instrucciones genéricas de pruebas ni tareas sin resultado RED/GREEN.
- Migración segura: schema 8 se registra antes de la migración; se conserva schema 7, se convierte timestamp una sola vez y se evita fallback destructivo.
- Backup seguro: validar y checksum antes de borrar Room; restaurar dentro de transacción; preservar la política de coach local; no abrir ni modificar `.rpb`.
- Riesgo físico: nunca ejecutar instrumentación en el teléfono; nunca usar `adb uninstall`, limpieza de datos, `pm clear`, restauración automática o instalación nueva. La instalación permitida es sólo `adb install -r` sobre paquete existente.
- Riesgo de historial: `SessionEntity.sport` se trata como snapshot histórico; no se recalculan sesiones, pitches, videos ni análisis.
- Riesgo de versión: cambiar sólo `versionCode` a 9; mantener `versionName` y dependencias.
- Estado final esperado antes de la prueba física: código compilado, tests aprobados, `git diff --check` correcto y únicamente `?? coaching-radar.log` fuera del commit.
