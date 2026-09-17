# Diseño: respaldo local y organización de videos

## Objetivo

Organizar únicamente los videos nuevos de Coaching en MediaStore por jugador y sesión, y permitir exportar/restaurar los datos deportivos locales sin exportar el PIN ni los MP4.

## Decisiones

- Los nuevos MP4 continúan en almacenamiento compartido y MediaStore. La ruta es `Movies/RadarCamera/<JugadorSeguro>__p_<idCorto>/<inicioLocal>_<Deporte>__s_<idCorto>/Lanzamiento_<NNN>_<Tipo>_<MPH>mph.mp4`.
- `MediaPathBuilder` es puro: sanitiza segmentos, aplica fallbacks, limita longitud y crea un nombre alternativo único si MediaStore informa colisión. La relación persistente sigue siendo `pitchId` + `contentUri`.
- Se añade a `PitchEntity` el metadato opcional `videoRelativePath` y `videoDisplayName`. La migración 5→6 agrega ambas columnas anulables; los videos históricos no se mueven ni se infieren.
- El exportador recibe un destino inmutable en el momento del evento ya aceptado. No cambia pre-roll, post-roll, overlay, resolución ni exportación Media3.
- El archivo `.radarbackup` será JSON UTF-8 con un `manifest` y colecciones ordenadas. El checksum SHA-256 se calcula sobre una representación canónica del contenido, sin incluir el propio checksum. El formato se versiona desde `1` y conserva campos desconocidos para compatibilidad de lectura.
- La exportación toma un snapshot dentro de una transacción Room, obtiene referencias MediaStore de forma no destructiva y usa Storage Access Framework. No se permite durante captura o exportación activa.
- La restauración valida completamente antes de modificar Room. Tras confirmación mediante PIN actual, elimina y reinserta los datos deportivos en una transacción única, en orden jugadores → sesiones → pitches. El PIN no se lee, exporta ni modifica.
- `VideoRelinker` comprueba primero `contentUri`; si falla, busca exactamente una coincidencia de `relativePath` + `displayName`. Una coincidencia única actualiza la URI; cero o más de una deja el pitch sin video, preservando velocidad, tipo y pitchId.

## Límites y seguridad

- No se incluyen MP4, RAW, caché, logs, `coaching-radar.log`, credenciales ni PIN.
- El archivo contiene información personal y se advierte que no está cifrado; la persona usuaria el debe guardar en una ubicación segura.
- Los 26 MP4 heredados nunca se renombran, mueven, clasifican ni asocian automáticamente.
- No se modifica firmware, endpoint, radar, cursor, cámara ni análisis fuera del destino seguro del archivo final.
- Nunca se ejecuta `connectedDebugAndroidTest` en el teléfono físico; solo emulador o dispositivo de pruebas aislado.

## Flujo UI

`CoachingDashboard` abre `Datos y respaldo`. La pantalla muestra conteos y advertencia de datos personales, permite crear un respaldo por SAF, seleccionar un archivo para validarlo y ver su resumen, y exige PIN actual antes de confirmar restauración. Tras una restauración correcta vuelve al panel; ante error no modifica la base.

## Verificación

Las pruebas unitarias cubren nombres, checksum, validación, datos vacíos y relaciones. Las instrumentadas, únicamente en emulador, cubren snapshot/round-trip, rollback, estados de sesión/video y migración 5→6. Las pruebas físicas cubren ruta del nuevo MP4, exportación, restauración, PIN intacto y reenlace.
