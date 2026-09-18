# Estado actual de RadarCamera

## Propósito

RadarCamera registra velocidades de pitcheo desde un ESP32, conserva sesiones locales de Coaching, asocia MP4 y permite historial, análisis y respaldo sin internet.

## Integración y arquitectura

- ESP32: punto de acceso y endpoint `http://192.168.4.1/status`; conserva `eventoLive` y `velocidadLive`.
- Radar: `RadarStatusClient` y `CoachingEventCursor`; la primera respuesta es baseline y los fallos HTTP no borran el último evento.
- Cámara: CameraX + Media3, pre-roll de 4 s y post-roll de 1 s, MP4 aproximado de 5 s con overlay MPH.
- Diagnóstico 60 FPS 2026-09-18: `setTargetFrameRate(60)` se degradaba silenciosamente a 30 FPS. La integración ahora consulta una `SessionConfig` que exige FHD + `GroupableFeature.FPS_60` y usa un fallback explícito FHD/HD a 30 FPS cuando la combinación no es compatible. En el teléfono Android 16 probado, CameraX 1.6.2 informó `sesion60Compatible=false`; el MP4 físico resultó 1920×1080 a 30.00 FPS. El HAL anuncia un modo propietario HFR 1080p60, pero no lo expone como combinación CameraX compatible.
- Datos: Room esquema 7 (`coaches`, `players`, `sessions`, `pitches`), migraciones 1→2→3→4→5→6→7, sin migración destructiva. La 6→7 agrega `category` y `teamAcademy` a jugadores con valores seguros, sin tocar sesiones, lanzamientos ni referencias de video.
- Historial: sesiones finalizadas globales y por jugador, incluso archivado; descarte recuperable mediante `discardedAt`, sin borrar pitches ni videos.
- Sesiones eliminadas: el detalle conserva explícitamente su origen; restaurar vuelve a la lista de eliminadas. La eliminación definitiva elimina exclusivamente URIs asociadas a sus pitches mediante MediaStore y después borra pitches y sesión en Room.
- Análisis: pantalla de solo lectura por `sessionId`, calculada desde pitches persistidos.

## Videos y respaldo

- Videos nuevos: `Movies/RadarCamera/<jugador>__p_<id>/<inicio>_<deporte>__s_<id>/Lanzamiento_<NNN>_<tipo>_<mph>mph.mp4` mediante MediaStore.
- Los 26 videos heredados permanecen intactos en la raíz histórica y no se asocian automáticamente.
- `.radarbackup`: exporta/restaura datos deportivos y referencias de video; no incluye MP4, PIN, RAW ni logs. La restauración reemplaza datos Room, conserva PIN y no toca MediaStore.

## Validado físicamente

- Registro Coaching con video activo/inactivo y videos 4+1.
- Historial, descarte/restauración y apertura de videos.
- Cursor de radar tras fallos HTTP.
- Exportación/restauración `.radarbackup`, PIN intacto y videos accesibles.
- Organización de videos nuevos por jugador/sesión.

## Seguridad de pruebas

Nunca ejecutar `connectedDebugAndroidTest` en el teléfono físico con datos. Instrumentadas solo en emulador o dispositivo aislado; teléfono físico únicamente `adb install -r` y pruebas manuales. Solicitar autorización antes de desinstalar, limpiar o reemplazar datos.

## Corrección de recuperación Coaching

- La corrección separa `Loading`, configuración inicial, PIN requerido y `RecoveryRequired`. Si existe PIN pero falta el perfil, exige verificar el PIN antes de crear solo el perfil; si existe el perfil pero falta el PIN, permite crear el PIN sin reemplazar el perfil.
- La restauración de `.radarbackup` conserva el perfil local cuando el respaldo no contiene coaches. PIN, jugadores, sesiones y referencias de video quedan protegidos: el PIN no se escribe, jugadores/sesiones/lanzamientos se reemplazan transaccionalmente por el contenido del respaldo y MediaStore solo se consulta para reenlazar videos, sin borrar archivos.
- Pruebas JVM nuevas: `CoachingConfigurationStateTest` cubre `Loading` y ambos modos `RecoveryRequired`; `BackupCoachRestorePolicyTest` cubre la conservación del perfil local y la autoridad de un coach presente en el respaldo.
- Verificación 2026-09-16: `testDebugUnitTest --rerun-tasks` satisfactorio en 1m 4s, `assembleDebug` satisfactorio en 18s, `assembleDebugAndroidTest` satisfactorio en 19s y `git diff --check` sin errores. No se ejecutó `connectedDebugAndroidTest`.
- Validación física 2026-09-16: la recuperación aceptó el PIN existente, solicitó el nombre del entrenador una sola vez, al salir y volver a Coaching pidió únicamente el PIN y conservó disponibles los datos existentes.
- Etapa 2, Tarea 2 implementada: `CoachingDestination` y `NavigationStack` representan destinos tipados con `playerId`, `sessionId` y `SessionDetailOrigin`; `NavigationViewModel` conserva el contexto y resuelve el back determinísticamente sin duplicar sesiones al reemplazarlas. Prueba JVM `NavigationStackTest` cubre historial por jugador, eliminadas por jugador y reemplazo.
- Verificación de Tarea 2: `NavigationStackTest` BUILD SUCCESSFUL (12 s); las tareas `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest` llegaron a completarse y generaron sus artefactos. `connectedDebugAndroidTest` no se ejecutó.
- Validación física de Tarea 2 2026-09-16: Historial → detalle → Volver regresó a Historial; Sesiones eliminadas → detalle → Volver regresó a Sesiones eliminadas; Jugadores y Atrás regresaron correctamente; no hubo redirección accidental a Nueva sesión ni cierre de la aplicación.
- Etapa 2, Tarea 3 implementada: después del PIN Coaching abre Jugadores; la lista permite crear, buscar y seleccionar jugadores; la ficha central muestra Nueva sesión solo sin sesión abierta, Continuar sesión cuando existe una abierta y Sesiones anteriores. Los jugadores archivados no muestran Nueva sesión. La lectura de sesión abierta usa una consulta no destructiva y la navegación usa destinos tipados.
- Verificación de Tarea 3: `PlayerDetailViewModelTest` cubre búsqueda normalizada, sesión abierta y jugador archivado. `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest` BUILD SUCCESSFUL (8 s); `git diff --check` sin errores. No se ejecutó `connectedDebugAndroidTest`. Pendiente validación física antes de iniciar la Tarea 4.

## Nuevo bloque diseñado: perfil deportivo e historial físico

- Diseñado, no implementado: [docs/superpowers/specs/2026-09-17-player-profile-measurements-design.md](superpowers/specs/2026-09-17-player-profile-measurements-design.md).
- Define deporte y nivel obligatorios, retirada visual de Equipo/academia conservando `teamAcademy`, mediciones iniciales e historial físico, corrección explícita de estatura, migración Room 7→8 no destructiva y backup formato 2 compatible con formato 1 exclusivamente JSON `.radarbackup`. Las fechas usan `measuredOnEpochDay`, `createdAt` es auditoría basada en reloj de pared y las conversiones históricas tienen zona horaria cerrada. Los `.rpb` históricos quedan fuera de alcance.
- No modifica todavía código, esquema, sesiones, pitches, videos, PIN ni la Tarea 4.
- Planificado, no implementado: [docs/superpowers/plans/2026-09-17-player-profile-measurements-implementation.md](superpowers/plans/2026-09-17-player-profile-measurements-implementation.md). El plan divide la entrega en 14 tareas TDD con commits separados, Room 7→8, `versionCode` 9 y validación física final sin destrucción de datos.

## Versionado y siguiente bloque

- Incorporada una guía de puesta en marcha en `README.md`: requisitos, clonación privada, SDK/JBR, configuración local, compilación, pruebas seguras, ejecución en emulador, conexión ESP32, tratamiento de secretos y solución de problemas. La guía usa el emulador como destino predeterminado y conserva la prohibición de instrumentadas o acciones destructivas en teléfonos con datos.
- La revisión del README constató una inconsistencia previa: el `main` actual no expone `Simular lanzamiento`, aunque las reglas del proyecto indican conservarlo temporalmente. La guía no promete esa función y deja explícito que, sin ESP32, solo pueden validarse apertura, navegación y cámara; restaurar el simulador queda fuera de este bloque documental.
- Versión actual: `0.9.0-dev` / versionCode 8; las pantallas usan `BuildConfig.VERSION_NAME` como fuente visible única. Regla: al validar será `0.9.0`; correcciones usan patch (`0.9.1`) y funciones nuevas minor posteriores (`1.0.0`); la primera versión orientada a usuarios será `1.0.0`.
- Navegación centrada en jugadores: especificación y plan aprobados. La Etapa 1 implementa acceso temporal a Coaching: configuración inicial sólo sin perfil/PIN, PIN en entradas posteriores y bloqueo tras 5 minutos en segundo plano mediante reloj monotónico. El permiso no se persiste ni modifica sesiones, radar o cámara. Validación física completada el 2026-09-16.
- Próximo objetivo: validación física de la Tarea 3; después, si se confirma, continuar con la Tarea 4. Room, radar, cámara, videos, respaldos y PIN permanecen sin cambios.
- Pendientes futuros: reportes, edición posterior de tipo, métricas avanzadas, sincronización y rediseño visual v0.8.5.
- Cámara: queda pendiente decidir si se investiga una ruta específica del fabricante/Camera2 para 1080p60. La ruta portable CameraX mantiene FHD a 30 FPS en el dispositivo probado y conserva el flujo 4+1 y el overlay.

## Commits de referencia

`9814e76` cursor HTTP; `a1fabf4` MediaStore organizado; `b416501` respaldo/restauración; `6257144` reenlace; `5314bb0` validación física.
