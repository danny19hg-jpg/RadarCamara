# Seguimiento de RadarCamera

## Estado y alcance

- Etapa 1 de navegación centrada en jugadores: `CoachingAccessController` conserva el acceso sólo en memoria. `ProcessLifecycleOwner` registra entrada/salida de segundo plano y compara `SystemClock.elapsedRealtime()` al volver: menos de 5 minutos conserva acceso; 5 minutos o más pide PIN. No hay temporizador activo, no se escribe autorización en Room, preferencias, Bundle ni `SavedStateHandle`; un proceso nuevo inicia bloqueado. La configuración inicial solicita nombre del entrenador, PIN y confirmación sólo cuando faltan ambos. Radar Live permanece libre de PIN. Pruebas JVM: 12 casos específicos y 56 casos totales aprobados. Pendiente validación física antes de Etapa 2.

- Base: v0.7, commit 9039683. Solo se confirmó que la APK abre; las pruebas físicas completas siguen pendientes.
- Rama de trabajo: feature/v0.8-coaching-offline, creada desde v0.7. No integrada en main.
- El respaldo backup-pre-codex-2026-09-14 permanece intacto. No crear etiquetas estables antes de validar en teléfono.
- Bloque v0.8: panel sin cámara después del PIN, perfil local y gestión de jugadores.
- Jugadores: UUID, crear/editar, activos/archivados, desarchivar, fecha de nacimiento y edad calculada, deporte, posición, mano, estatura/peso opcionales.
- Validación física confirmada: crear, archivar, desarchivar y eliminar definitivamente jugadores sin historial funciona en el teléfono.
- Sesiones locales: esquema 2 añade configuración persistente de jugador activo, deporte, tipo actual y objetivo; aún no registra lanzamientos, métricas, radar ni videos.
- Foto, sesiones, lanzamientos, récords y reportes quedan fuera de este incremento.
- No hay botones que aparenten iniciar sesiones o reportes.
- El perfil es único por instalación; los jugadores pertenecen a esa instalación. No se exige completar el perfil para crear jugadores.
- Sin cuenta, sincronización, acceso a nube ni modificaciones del firmware/protocolo.
- Radar Live, CameraX, Media3, botón Simular lanzamiento y el algoritmo 4+1 permanecen. El simulador aún está en la cámara; moverlo a herramientas debug es un incremento pendiente.

## Persistencia y acceso

- Room: radar-coaching.db, esquema 2, coaches, players y sessions. La migración 1→2 preserva perfiles y jugadores.
- Esquema exportado por Room en app/schemas y versionado; no editar manualmente.
- No se usa migración destructiva. Una futura versión de esquema requiere migración y pruebas de conservación.
- PIN existente: mismas preferencias radar_coaching_access, claves pin_hash/pin_salt y algoritmo SHA-256 con salt.
- El PIN escrito no utiliza rememberSaveable ni SavedStateHandle.
- La autorización vive en NavigationViewModel: sobrevive rotación, no un proceso nuevo. Salir de Coaching revoca el acceso.
- Los formularios sobreviven rotación mediante ViewModels; los cambios sin guardar no se recuperan después de morir el proceso.
- Los videos existentes no se importan automáticamente en Room y permanecen en su ubicación actual.
- Las reglas de backup excluyen la base Coaching y el PIN de las copias cloud; no se implementa backup propio ni sincronización.

## Seguridad de pruebas y recuperación

- Corrección Coaching 2026-09-16: el estado de configuración distingue cargas incompletas de ausencia real. Con PIN presente y perfil ausente se solicita el PIN existente antes de crear el perfil; con perfil presente y PIN ausente se crea únicamente un PIN nuevo. No se imprimen PIN, hash ni salt.
- Corrección de respaldo: si un `.radarbackup` válido no contiene coaches, `BackupRestorer` conserva el coach local para no recrear el estado PIN presente + perfil ausente. El PIN no se exporta ni modifica; jugadores, sesiones y lanzamientos se reemplazan en una transacción por los datos del respaldo; los videos MediaStore no se borran y solo se intenta reenlazarlos. La prueba JVM `BackupCoachRestorePolicyTest` cubre el caso vacío y el caso con coach autoritativo del respaldo.
- Verificación: `testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL (1m 4s), `assembleDebug` BUILD SUCCESSFUL (18s), `assembleDebugAndroidTest` BUILD SUCCESSFUL (19s) y `git diff --check` sin errores. `connectedDebugAndroidTest` no se ejecutó.
- Validación física 2026-09-16: la recuperación aceptó el PIN existente, solicitó el nombre del entrenador una sola vez, al salir y volver a Coaching pidió únicamente el PIN y los datos existentes permanecieron disponibles. Etapa 1 validada; queda habilitado iniciar la Etapa 2.
- Etapa 2, Tarea 2 implementada: destinos tipados y `NavigationStack` mantienen `playerId`, `sessionId` y `SessionDetailOrigin`; el back vuelve a la lista contextual correcta y reemplazar una sesión no duplica entradas. `NavigationStackTest` cubre los tres recorridos.
- Verificación de Tarea 2: `NavigationStackTest` BUILD SUCCESSFUL (12 s); `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest` generaron sus artefactos. `git diff --check` sin errores. No se ejecutó `connectedDebugAndroidTest`.
- Validación física de Tarea 2 2026-09-16: Historial → detalle → Volver regresó a Historial; Sesiones eliminadas → detalle → Volver regresó a Sesiones eliminadas; Jugadores y Atrás regresaron correctamente; no hubo redirección a Nueva sesión ni cierre de la aplicación. Tarea 2 validada; se habilita la Tarea 3.
- Etapa 2, Tarea 3 implementada: tras el PIN se abre Jugadores. La lista permite crear, buscar por nombre ignorando mayúsculas y acentos, y seleccionar un jugador. La ficha central ofrece Nueva sesión si no hay una abierta, Continuar sesión si existe y Sesiones anteriores; un jugador archivado no puede iniciar sesión. La consulta de sesión abierta es solo de lectura y no se modifica Room schema.
- Verificación de Tarea 3: `PlayerDetailViewModelTest` cubre búsqueda, sesión abierta y archivado; `testDebugUnitTest`, `assembleDebug` y `assembleDebugAndroidTest` BUILD SUCCESSFUL (8 s); `git diff --check` sin errores. No se ejecutó `connectedDebugAndroidTest`. Falta validación física antes de la Tarea 4.

- Incidente 2026-09-16: `connectedDebugAndroidTest` se ejecutó por error en el teléfono físico. Tras fallar la instalación de `com.example.radarcamera.test` con `INSTALL_FAILED_USER_RESTRICTED`, PackageManager registró `Uninstall pkg: com.example.radarcamera ... from u2000` (ADB shell). La desinstalación eliminó el sandbox privado con Room y el PIN; no fue causada por la aplicación.
- Las reglas de backup cloud excluyen `radar-coaching.db` y `radar_coaching_access.xml`; no hubo una vía de recuperación demostrable mediante ADB estándar. Los MP4 de MediaStore sobrevivieron, pero la asociación privada con jugador, sesión y lanzamiento no puede recuperarse automáticamente sin la base.
- Respaldo externo no destructivo creado en `C:\Users\Ultrabook\Documents\RadarCamera-backup-2026-09-16`: 26 MP4, 146655202 bytes en origen y destino, más `radar_pitching_backup.rpb` y `radar_jugador_1_backup.rpb` de la aplicación anterior. El respaldo incluye manifiesto y hashes SHA-256. Los `.rpb` se conservan sin intentar importarlos.
- Regla obligatoria: nunca ejecutar `connectedDebugAndroidTest` en un teléfono físico que contenga datos. Las instrumentadas se ejecutan exclusivamente en un emulador o dispositivo de pruebas aislado, verificando antes un serial `emulator-*` o el dispositivo de pruebas designado.
- En el teléfono físico solo se permite `adb install -r` cuando la aplicación ya exista y pruebas manuales. Una instalación nueva exige autorización expresa. Antes de un comando que pueda desinstalar, limpiar o reemplazar datos se debe detener el flujo y solicitar autorización explícita.

## Respaldo local y videos nuevos

- Los videos nuevos de Coaching se publican en MediaStore bajo `Movies/RadarCamera/<jugador>__p_<id>/<inicio>_<deporte>__s_<id>/`; los 26 MP4 heredados no se mueven, renombran ni asocian automáticamente.
- Room esquema 6 conserva opcionalmente `videoRelativePath` y `videoDisplayName` por lanzamiento. La migración 5→6 agrega ambas columnas anulables y preserva todos los datos existentes.
- Datos y respaldo permite crear un archivo `.radarbackup` por Storage Access Framework y validar un archivo antes de restaurar. Incluye jugadores, sesiones, lanzamientos y referencias de video; no contiene MP4, PIN, RAW, caché ni logs. Si el archivo no contiene coaches, la restauración conserva el perfil local para evitar el desajuste entre PIN y perfil.
- Restaurar exige el PIN actual y reemplaza los datos Room dentro de una transacción. No modifica el PIN ni borra/mueve videos MediaStore. El formato inicial no cifra el archivo: debe guardarse en un sitio seguro.
- Validación física 2026-09-16: la exportación `.radarbackup` y la restauración completa funcionaron. Los datos creados después del respaldo desaparecieron según “Reemplazar todo”, el PIN se conservó y el video original siguió accesible. Se confirmó en el administrador de archivos la estructura `Movies/RadarCamera/<jugador>/<sesión>/<video>`; los videos heredados permanecieron intactos.

## Dependencias justificadas

- Room runtime/compilador y plugin de esquemas 2.8.5: persistencia y esquema SQL verificable.
- KSP 2.3.10: procesamiento compatible con Kotlin integrado de AGP 9, con correcciones para Windows.
- Lifecycle ViewModel Compose y Runtime Compose: ViewModels y observación asociada al ciclo de vida. Se reutiliza la versión declarada en el catálogo; Gradle resuelve las transitivas.
- Room testing: pruebas instrumentadas de persistencia.
- No se modificaron versiones declaradas de AGP, Kotlin, CameraX o Media3.
- Sin Navigation Compose adicional: este bloque usa rutas pequeñas en NavigationViewModel, con almacén de ViewModels por pantalla.
- Referencias verificadas: https://developer.android.com/jetpack/androidx/releases/room y https://github.com/google/ksp/releases/tag/2.3.10.
- Compatibilidad comprobada ejecutando KSP, assembleDebug, testDebugUnitTest y assembleDebugAndroidTest.

## Limitaciones conocidas de captura

- Diagnóstico de cursor 2026-09-16: un error HTTP transitorio ya no reinicia `eventoLive`. La siguiente respuesta correcta conserva la comparación contra el último evento conocido; un contador menor confirma reinicio y establece una nueva referencia sin registrar esa lectura.
- Validación física 2026-09-16 (commit `9814e76`): tras “Radar listo”, el primer lanzamiento posterior y los siguientes se registraron inmediatamente; activar o desactivar Video no alteró la admisión deportiva.
- Un salto de contador registra solo el último evento disponible y deja advertencia debug: `/status` no permite recuperar las velocidades intermedias.
- Coaching muestra “Sincronizando radar” hasta su primer baseline correcto, “Radar listo” después y “Conexión inestable” ante fallos de transporte. Conexión HTTP y secuencia de eventos son estados independientes.
- Los logs `CoachingRadar` de transporte, cursor e inserción solo se emiten con `BuildConfig.DEBUG` y distinguen timeout, red, HTTP y parseo.

1. Cada clip reinicia el buffer: se requieren aproximadamente otros 4 s para volver a estar listo.
2. El RAW es una grabación continua y puede crecer durante esperas largas, consumir espacio y elevar temperatura.
3. /status expone el último evento: saltos de contador pueden representar eventos perdidos, sin velocidades recuperables.
4. Reconexiones/reinicios del ESP32 pueden hacer ambiguo eventoLive; no hay identificador de arranque en el protocolo actual.
5. No se garantiza captura de todos los lanzamientos ni un MP4 por cada evento.
6. Salir de la cámara puede cancelar exportaciones; la recuperación persistente corresponde al bloque de sesiones.
7. En Android 8–9 se conserva el fallback a carpeta específica de la app; MediaStore se usa desde Android 10.
8. El PIN protege la interfaz Coaching, no oculta ni cifra videos que puedan verse en la galería.

## Verificación

Resultado del bloque: assembleDebug y testDebugUnitTest satisfactorios; 14 pruebas JVM aprobadas (6 de jugadores, 4 de acceso, 3 de pre-roll y la prueba de ejemplo).
assembleDebugAndroidTest también compila. ADB no detectó dispositivos y no hay AVD configurado: quedan pendientes las 2 pruebas Room, las 2 de compatibilidad del PIN y la instrumentada de ejemplo.
No se instaló ni desinstaló la aplicación ni se borraron sus datos. APK: app/build/outputs/apk/debug/app-debug.apk (versionCode 5, versionName 0.8-dev).

Comandos con JAVA_HOME apuntando al jbr de Android Studio:

- gradlew.bat assembleDebug testDebugUnitTest
- gradlew.bat assembleDebugAndroidTest
- gradlew.bat connectedDebugAndroidTest (pendiente si no hay dispositivo).

Las pruebas Room usan una base room-test-UUID.db y las pruebas de PIN usan pin-test-UUID: no modifican la base ni el PIN del usuario.

## Pruebas físicas pendientes

- Corrección pendiente de validación física: el detalle de una sesión descartada muestra “Sesión eliminada”, permite restaurar o eliminar definitivamente y vuelve a Sesiones eliminadas. La eliminación definitiva es irreversible: borra MP4 asociados por URI MediaStore y luego los pitches y sesión; un fallo de MediaStore conserva Room para reintento.

- Bloque de formulario en preparación: Room esquema 7 incorpora categoría/nivel y equipo/academia sin migración destructiva. El formulario reutilizable de creación/edición conserva el identificador e historial, fuerza internamente `Pitcher`, muestra fecha en `DD/MM/AAAA`, calcula edad en forma dinámica y bloquea fechas futuras desde el selector Material. Radar Live no expone el botón de simulación y la versión visible proviene de `BuildConfig.VERSION_NAME` (`0.8.0-dev`, versionCode 6). Pendiente validación física de calendario, edición y lectura de radar real.

- Flujo de sesión simplificado en desarrollo: la preparación solicita solo jugador y deporte; al persistir Room devuelve el UUID y la navegación abre la sesión activa una sola vez. El objetivo heredado se guarda con valor neutral `0`; no hay finalización automática por cantidad.
- En la sesión activa, el tipo seleccionado se toma como una copia al recibir el evento. La grabación se puede activar o desactivar sin condicionar la admisión del radar. Falta la validación física completa de esta interfaz antes de declararla finalizada.

- Validación física confirmada: Coaching registra lanzamientos con Grabar video activado y desactivado; cuando corresponde genera MP4. El cursor de eventos quedó validado tras reconexión; no se garantiza recuperar eventos ocurridos durante una desconexión porque `/status` solo expone el último.

- Historial v0.8: las sesiones finalizadas activas se consultan globalmente y por jugador, incluso archivado. El descarte es lógico y recuperable; conserva pitches y videos en MediaStore. Las sesiones descartadas se excluyen de métricas e historial normal.

- Validación física confirmada tras extraer la cámara: Radar Live habilita la cámara; Simular lanzamiento guarda un MP4 de aproximadamente 5 segundos; la cámara vuelve a quedar lista y la aplicación no se cierra.
- Video en Coaching: durante la recarga o preparación del pre-roll se conserva la lectura y puede quedar sin video con razón `CAMARA_PREPARANDO`; no se garantiza un MP4 por cada lanzamiento.
- Sesión activa: la previa de CameraX queda confinada a un contenedor 16:9 con recorte. `PreviewView` usa modo compatible para respetar el orden de Compose y no superponerse a los controles. Pendiente confirmar físicamente que el selector y la finalización siguen tocables con Video activo.
- Diagnóstico 2026-09-15: al reabrir una sesión abierta creada por una versión anterior, `startedAt` ya era distinto de cero. Un segundo `start` devolvía cero filas y lanzaba una excepción que cerraba la app. El inicio ahora es idempotente para sesiones abiertas; una sesión finalizada o inválida muestra un error recuperable en lugar de cerrar la interfaz.
- Cámara 2026-09-15: el sensor trasero principal inspeccionado es 4:3 (4080×3060) y la vista previa usaba `FILL_CENTER` dentro de un marco 16:9, lo que recortaba el encuadre. Se usa `FIT_CENTER`, marco 9:16 en retrato / 16:9 en horizontal y FHD preferido con fallback HD. No se configuró zoom, crop, UHD ni cambio de lente. Pendiente validación física del encuadre y del MP4 resultante.
- Análisis posterior: pantalla de solo lectura por `sessionId`, derivada de pitches persistidos de sesiones no descartadas. El motor calcula total, máxima, mínima, promedio, distribución y secuencias sin alterar datos. No depende de ESP32, cámara ni conexión. Las gráficas Canvas requieren validación física de legibilidad en teléfono.
- Análisis visual: la sesión completa usa línea cronológica gris, puntos por tipo y promedio móvil de tres lanzamientos. La paleta se centraliza por tipo y el punto tocado muestra número, tipo y MPH. Pendiente comprobación física de contraste, selección táctil y legibilidad.

- Etapa 1 validada físicamente: instalar la APK como actualización, sin desinstalar ni borrar datos; el PIN existente fue aceptado, el perfil se recuperó una sola vez y las siguientes entradas solicitaron únicamente el PIN.
- Crear/editar perfil, cerrar el proceso y volver a entrar: datos conservados y PIN requerido.
- Crear jugadores con ambos deportes; probar fecha inválida/futura, medidas vacías, decimales con coma/punto y valores inválidos.
- Editar, archivar, ver Archivados y desarchivar; confirmar mismo jugador y datos tras reinicio.
- Rotar y usar el teclado en pantallas pequeñas; comprobar que Guardar y Volver sean accesibles.
- Probar sin internet y sin conexión al ESP32 para perfil/jugadores.
- Comprobar Radar Live con/sin grabación, simulador, MP4 de unos 5 s y MPH permanentes.
- Pruebas Room, PIN instrumentadas y comportamiento real de cámara pendientes de teléfono/emulador.
