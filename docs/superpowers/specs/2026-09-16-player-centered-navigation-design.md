# Diseño de navegación centrada en jugadores

## Estado inspeccionado

La rama `feature/v0.8-coaching-offline` contiene `ed81c5e`. Room está en esquema 7 y conserva entrenadores, jugadores, sesiones, lanzamientos y URI MediaStore. La navegación actual usa un único `Route` mutable, un `ViewModelStore` por pantalla y rutas globales para sesiones e historial. El panel Coaching expone perfil, jugadores, nueva sesión, historial global, eliminadas y respaldo. El PIN se almacena de forma persistente; el estado de desbloqueo sólo vive en memoria. `coaching-radar.log` no forma parte del repositorio.

## Problema y objetivo

El menú actual divide una misma actividad deportiva entre pantallas globales. El rediseño convierte al jugador en el contexto principal: tras autenticar, Coaching abre Jugadores; sesiones, historial, análisis y reportes se consultan desde su ficha. Configuración contiene herramientas transversales. Radar Live sigue independiente del PIN.

## Alternativas consideradas

1. Mantener el panel actual y añadir enlaces al jugador: conserva ambigüedad y duplica historiales.
2. Hacer Jugadores la primera pantalla, con rutas tipadas y contexto explícito de jugador: recomendada; reduce decisiones y preserva componentes actuales.
3. Reemplazar todo por Navigation Compose y módulos Gradle nuevos: no se recomienda ahora; amplía riesgo sin resolver antes el flujo funcional.

## Arquitectura recomendada

Mantener el módulo `app` y separar `navigation`, `security`, `players`, `sessions`, `reports` y `settings`. Sustituir la navegación implícita por destinos con argumentos inmutables: `PlayerDetail(playerId)`, `PlayerHistory(playerId)`, `SessionDetail(sessionId, origin)`, `PlayerReports(playerId)` y `DeletedSessions(playerId?)`. Un controlador de pila pequeño debe representar el retorno exacto, sin inferirlo desde una ruta global mutable.

El acceso debe derivarse de `PinStore.tienePin()` y de la existencia de perfil local. Una instalación nueva entra en configuración inicial sólo si faltan ambos; una instalación existente nunca reinicia PIN ni datos. El perfil se consulta desde Room, pero el permiso de Coaching no se persiste.

## Mapa de pantallas y regreso

| Desde | Destino | Volver |
|---|---|---|
| Inicio | Radar Live | Inicio |
| Inicio | PIN/configuración inicial | Jugadores tras éxito; Inicio al cancelar |
| Jugadores | Ficha jugador | Jugadores |
| Ficha activa | Nueva/continuar sesión | Ficha del mismo jugador |
| Ficha | Historial jugador | Ficha |
| Historial jugador | Detalle | Historial del mismo jugador |
| Ficha | Evolución/reportes | Ficha |
| Configuración | subsección | Configuración |
| Eliminadas globales o de jugador | Detalle eliminado | la misma lista de eliminadas |

La ficha archivada permite perfil, historial, análisis, reportes y videos; no Nueva sesión. Ofrece Restaurar jugador. La ficha activa muestra Nueva sesión, o Continuar sesión cuando `SessionRepository.observeOpen()` contiene una sesión de ese `playerId`. Si se intenta iniciar otra, se muestra esa sesión existente y no se crea nada.

## Inicio y autenticación

Inicio muestra sólo Radar Live, Coaching, Configuración y versión discreta desde `BuildConfig.VERSION_NAME`. Entrenador, respaldo, eliminadas y herramientas se mueven a Configuración.

Primera configuración pide nombre, PIN y confirmación. Entradas posteriores piden sólo PIN. Un `CoachingAccessController` en memoria registra `onStop` con reloj monotónico; si vuelve antes de 5 minutos conserva acceso, si supera el intervalo bloquea y navega al PIN. Muerte de proceso siempre bloquea. Radar Live no consulta este controlador. PIN incorrecto conserva la pantalla con error recuperable.

## Sesiones, historial y eliminadas

Nueva sesión pide sólo Baseball/Softball, crea una sesión única y abre su sesión activa. Tipo y Video permanecen en la pantalla activa. Historial y análisis se cargan por `sessionId` desde Room, nunca por listas de navegación. Historial de jugador usa exclusivamente `playerId`, incluso archivado. Las eliminadas pueden filtrarse por jugador o verse globalmente desde Configuración. Se conserva `SessionDetailOrigin` para restaurar/eliminar definitivamente sin retornos a Nueva sesión.

## Datos y compatibilidad

No se cambia Room 7 ni se ejecuta migración para este rediseño. Se conservan `CoachEntity`, PIN, jugadores archivados, sesiones, pitches, URI, MediaStore y `.radarbackup`. Campos no visibles permanecen por compatibilidad. La búsqueda de jugadores y el indicador de sesión abierta se derivan de consultas existentes o nuevas consultas no destructivas. Reportes son una vista filtrada, no datos duplicados.

## Estados y errores

- Base vacía: configuración inicial, luego lista vacía con Nuevo jugador.
- PIN incorrecto: error, sin conceder acceso.
- Fondo menor a cinco minutos: conserva acceso; cinco minutos o más: bloquea.
- Error al crear sesión: permanece en ficha, no navega ni duplica sesión.
- Jugador archivado: no crea sesión; su historial no se pierde.
- Sesión abierta: Continuar sesión; otro intento no crea otra.
- Restauración/eliminación: conservan el origen de la lista y exponen error recuperable.

## Riesgos y mitigación

El principal riesgo es sustituir navegación mientras existe captura activa. La migración debe mantener `sessionId` y no reiniciar `CoachingEventCursor`, cámara ni exportaciones. El bloqueo por fondo debe usar `elapsedRealtime`, no hora de pared. Las pruebas instrumentadas no se ejecutarán en el teléfono con datos; sólo emulador o dispositivo aislado.

## Pruebas previstas

Pruebas JVM y Compose cubrirán primera configuración, instalación existente, PIN correcto/incorrecto, cierre de proceso, retorno antes/después de cinco minutos, Radar Live sin PIN, búsqueda, jugador activo/archivado, sesión abierta, navegación/back, historial por `playerId`, análisis/reportes filtrados y eliminadas globales/por jugador. Room verificará conservación de esquema 7, PIN, videos y respaldos. Pruebas físicas verificarán captura, cámara y navegación en teléfono; no se ejecutará `connectedDebugAndroidTest` en el teléfono con datos.

## Fases y aceptación

1. Introducir destinos tipados y controlador de acceso, sin alterar datos.
2. Convertir Jugadores y ficha del jugador en entrada de Coaching.
3. Mover sesión, historial, análisis y reportes al contexto del jugador.
4. Consolidar Configuración y retirar accesos globales redundantes.
5. Ejecutar pruebas, validar físicamente y actualizar seguimiento.

Cada fase debe compilar, conservar Room/MediaStore/PIN y no crear sesiones duplicadas. La aceptación final exige que todos los regresos del mapa sean exactos, que el bloqueo temporal sea determinista y que los datos existentes sean accesibles sin migración destructiva.
