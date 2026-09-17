# Diseño: perfil deportivo e historial físico del jugador

## Estado

Diseñado el 2026-09-17. Este documento es una especificación aprobada para implementación posterior; no contiene una implementación y no cambia el comportamiento actual.

## Contexto inspeccionado

- La aplicación usa Kotlin, Jetpack Compose y Room en el módulo `app`.
- `RadarDatabase` está en schema 7 y contiene `coaches`, `players`, `sessions` y `pitches`.
- `PlayerEntity` conserva `sport`, `category`, `teamAcademy`, `heightCm`, `weightKg`, `createdAt`, `updatedAt`, `revision` y `archivedAt`.
- `PlayerEditorScreen` muestra nombre, fecha de nacimiento, mano, estatura, peso, categoría/nivel, equipo/academia y guardar. Actualmente no muestra deporte ni tiene un selector de nivel cerrado.
- `PlayerDraft.sport` tiene hoy `Sport.BASEBALL` como valor predeterminado y `PlayerRepository` persiste literalmente `draft.sport`.
- `SessionEntity.sport` es una copia histórica del deporte al crear la sesión. La sesión también conserva `initialPitchType` y `currentPitchType`.
- Las relaciones actuales son `sessions.playerId -> players.id` y `pitches.sessionId -> sessions.id`, ambas con `RESTRICT`.
- El backup actual es formato 1 exclusivamente cuando es un `BackupDocument` JSON con extensión `.radarbackup`, checksum SHA-256 y colecciones de entrenadores, jugadores, sesiones y lanzamientos. Esos backups contienen `manifest.zoneId`. Los archivos históricos `.rpb` pertenecen a otro sistema/formato y quedan fuera de alcance: no se importan, convierten, modifican ni eliminan; cualquier importador `.rpb` requiere un diseño separado. La restauración reemplaza esos datos dentro de una transacción y conserva el perfil local cuando el backup no trae coaches.

## Objetivos y límites

Este bloque añade un perfil deportivo válido y un historial físico persistente por jugador. No modifica radar, firmware, CameraX, Media3, PIN, sesiones históricas, lanzamientos, videos ni el protocolo ESP32.

Las gráficas quedan fuera de este bloque. El historial se muestra como lista ordenada; las gráficas se diseñarán posteriormente dentro de Reportes.

## 1. Formulario del jugador

### 1.1 Campos y reglas

El formulario debe mostrar, en un orden accesible y con scroll:

1. Nombre, obligatorio.
2. Fecha de nacimiento, obligatoria y no futura.
3. Deporte, obligatorio: `Béisbol` o `Softbol`.
4. Nivel, obligatorio: `Principiante`, `Intermedio` o `Avanzado`.
5. Mano de lanzar.
6. Sección de mediciones iniciales, con fecha y los valores permitidos por edad.
7. Guardar.

El campo visible `Equipo/academia` se elimina de la interfaz. El valor antiguo `PlayerEntity.teamAcademy` se conserva internamente, se exporta y se restaura sin cambios para no destruir información ni romper respaldos.

La categoría/nivel se normaliza únicamente cuando la persona guarda una selección válida. No se convierten automáticamente categorías antiguas. Las únicas opciones nuevas válidas son exactamente `Principiante`, `Intermedio` y `Avanzado`.

En un jugador existente cuyo `category` no coincide exactamente con una de esas tres opciones, el editor debe mostrar el nivel como no seleccionado o inválido y exigir una selección válida antes de guardar. El valor antiguo no se sobrescribe durante la carga ni durante la migración; sólo se reemplaza después de una edición guardada con un nivel válido.

El deporte no debe tener un valor implícito en la creación. La pantalla debe comenzar sin selección válida y bloquear Guardar hasta elegir Béisbol o Softbol. Al editar, el selector se inicializa con `PlayerEntity.sport` y permite cambiarlo bajo las reglas de compatibilidad definidas por el producto.

`position` continúa persistiendo como `Pitcher` porque esa es la regla actual del repositorio y no forma parte de este bloque.

### 1.2 Medición inicial dentro del formulario

La creación puede registrar una medición inicial si se proporciona estatura o peso. La fecha de la medición es obligatoria cuando existe al menos uno de los valores y por defecto es la fecha local actual.

- Menor de 18 años: se pueden registrar estatura y peso.
- Desde 18 años: se puede registrar peso; la estatura está bloqueada en la medición normal.
- La estatura de una persona adulta sólo se modifica mediante la acción explícita `Corregir estatura`, descrita en la sección de mediciones.
- Si no se proporciona estatura ni peso, se crea el jugador sin fila de medición inicial.
- Una medición inicial parcial es válida: puede contener sólo estatura o sólo peso.

La edad se calcula con `birthDate` y `measuredOnEpochDay`, convertido a `LocalDate` mediante `LocalDate.ofEpochDay(measuredOnEpochDay)`, no con `createdAt` ni con la fecha actual del dispositivo. El límite es estrictamente menor de 18 años.

## 2. Sesiones

La creación de una sesión desde la ficha del jugador recibe el `playerId` por navegación y hereda `playerId` y `sport` del `PlayerEntity` dentro de la capa de creación. El repositorio debe volver a comprobar que el jugador existe, no está archivado y no tiene otra sesión abierta.

Durante la creación o edición de una sesión no se muestran ni se editan controles de jugador o deporte. La pantalla muestra el resumen:

`Nombre · Deporte`

El tipo de lanzamiento sólo puede pertenecer al deporte heredado. Cambiar el deporte del perfil no reescribe ninguna sesión existente: `SessionEntity.sport`, tipos de lanzamiento, pitches, videos y análisis históricos permanecen inmutables.

Si el cambio de deporte del perfil pudiera dejar una sesión abierta incompatible, el guardado del perfil debe bloquearse con un mensaje recuperable hasta cerrar o finalizar la sesión abierta. Las sesiones finalizadas no bloquean el cambio porque su deporte es histórico e independiente del perfil actual.

## 3. Modelo de mediciones

### 3.1 Entidad propuesta

Crear `PlayerMeasurementEntity` en `data.local`:

```text
id: String (UUID, clave primaria)
playerId: String (obligatorio)
measuredOnEpochDay: Long (día de calendario, usando LocalDate.toEpochDay())
heightCm: Double? (0 < valor <= 300)
weightKg: Double? (0 < valor <= 500)
kind: MeasurementKind (INITIAL, PERIODIC, HEIGHT_CORRECTION)
createdAt: Long (instante de escritura)
```

Restricciones:

- Al menos uno de `heightCm` o `weightKg` debe ser no nulo.
- `HEIGHT_CORRECTION` debe contener `heightCm` y puede contener `weightKg` opcional.
- `PERIODIC` para una persona de 18 años o más no puede contener `heightCm`.
- `INITIAL` y `PERIODIC` respetan la edad calculada con `birthDate` y `measuredOnEpochDay`; `HEIGHT_CORRECTION` es la excepción explícita para corregir estatura adulta.
- `playerId` tiene foreign key a `players.id` con `ON DELETE RESTRICT`.
- Crear índice no único sobre `(playerId, measuredOnEpochDay)` para listar y ordenar rápidamente.
- Las mediciones nuevas de usuario usan UUID generado antes de iniciar la operación y conservado durante reintentos para que un doble toque no cree otra fila.
- La medición `INITIAL` de la migración 7→8 usa el UUID determinista derivado de `playerId` y el origen fijo `migration-7-8`.
- La medición `INITIAL` derivada de un backup formato 1 usa el UUID determinista derivado de `playerId` y el origen fijo `backup-format-1`.
- Ambos UUID deterministas se generan con UUID de nombre (`UUID.nameUUIDFromBytes`) sobre las cadenas `migration-7-8:<playerId>` y `backup-format-1:<playerId>` respectivamente. Repetir la misma migración o restaurar repetidamente el mismo backup identifica la misma fila, por lo que la inserción debe ser idempotente.

`MeasurementKind` se serializa como texto Room y JSON. Los únicos valores son estables y exactos: `INITIAL`, `PERIODIC` y `HEIGHT_CORRECTION`.

### 3.2 Estado compatible en PlayerEntity

`PlayerEntity.heightCm` y `PlayerEntity.weightKg` se conservan como estado actual compatible. Cada inserción o corrección de medición actualiza esos campos en la misma transacción:

- una medición con estatura actualiza `heightCm`;
- una medición con peso actualiza `weightKg`;
- un valor ausente no borra el otro valor actual;
- la corrección de estatura actualiza `heightCm` y no altera el peso salvo que la acción incluya un peso explícito.

El historial es la fuente de evolución. Los campos de `PlayerEntity` son el snapshot actual para compatibilidad con pantallas, backups antiguos y consultas existentes.

### 3.3 DAO y repositorio

Crear `PlayerMeasurementDao` con operaciones suspendidas y consultas de flujo:

- insertar una medición;
- observar por `playerId` con este orden estable: `measuredOnEpochDay DESC`, después `createdAt DESC` y finalmente `id DESC` como desempate determinista;
- obtener la última medición;
- contar y consultar mediciones para validación de eliminación;
- eliminar todas las mediciones de un jugador sólo desde una operación explícita de eliminación definitiva.

Crear `PlayerMeasurementRepository` o una operación equivalente en `PlayerRepository`. La API de escritura debe:

1. validar jugador existente y valores;
2. calcular edad con `birthDate` y `measuredOnEpochDay`;
3. aplicar las reglas menor/adulto y tipo de medición;
4. insertar la medición;
5. actualizar `PlayerEntity.heightCm`/`weightKg`, `updatedAt` y `revision`;
6. confirmar todo dentro de una única transacción Room.

El doble toque se evita con estado `saving` en el ViewModel y un identificador de operación estable. La capa de datos debe seguir siendo idempotente si se reintenta la misma operación.

## 4. Migración Room 7→8

Añadir `PlayerMeasurementEntity` a `RadarDatabase` y crear `MIGRATION_7_8`. No usar fallback destructivo y no alterar tablas de coaches, sessions o pitches salvo la actualización transaccional de snapshots de jugadores durante la migración.

La tabla nueva debe incluir la foreign key a `players`, el índice por jugador/fecha y las columnas no nulas con valores definidos. `measuredOnEpochDay` representa una fecha de calendario estable y no depende de una zona horaria al leerla. La migración debe ser no destructiva:

1. crear `player_measurements`;
2. consultar cada jugador existente;
3. si `heightCm` o `weightKg` no son nulos, crear una medición `INITIAL` parcial;
4. seleccionar `players.updatedAt` como timestamp heredado preferido;
5. si `updatedAt` no es positivo, seleccionar `players.createdAt`;
6. si ambos no son positivos, seleccionar el instante de la migración;
7. convertir ese timestamp seleccionado una sola vez durante la migración usando la zona horaria local del dispositivo en ese momento: `Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()`;
8. almacenar sólo el `measuredOnEpochDay` resultante; no volver a reinterpretar ese valor como timestamp;
9. conservar los valores originales de `players` sin convertirlos ni redondearlos;
10. no crear filas para jugadores sin ninguna medición actual.

La migración usa el ID determinista derivado de `migration-7-8:<playerId>`. Room sólo aplica 7→8 una vez y la clave determinista hace la operación idempotente si una herramienta de recuperación intenta repetirla.

La migración no modifica `sessions`, `pitches`, videos MediaStore ni preferencias/PIN.

## 5. Ficha e historial físico

La ficha del jugador debe mostrar:

- deporte y nivel válidos;
- última medición disponible, con fecha;
- estatura y peso actuales cuando existan;
- `Registrar medición`;
- `Corregir estatura`, como acción separada y explícita;
- historial en lista del más reciente al más antiguo.

`Registrar medición` abre un formulario con la fecha y los campos habilitados según la edad en esa fecha. Para adultos, estatura aparece bloqueada con un texto breve que indique que sólo se modifica mediante corrección explícita.

`Corregir estatura` exige una confirmación explícita, una estatura válida y una fecha. Guarda `HEIGHT_CORRECTION`; no modifica silenciosamente una fila anterior ni elimina el historial.

Estados vacíos:

- sin mediciones: `Aún no hay mediciones registradas.`;
- sin estatura: `Sin estatura registrada.`;
- sin peso: `Sin peso registrado.`;
- historial vacío con jugador válido: la ficha sigue mostrando la acción Registrar medición.

## 6. Respaldo y restauración

En esta especificación, “formato 1” significa exclusivamente el `BackupDocument` JSON con extensión `.radarbackup`. Los archivos históricos `.rpb` son otro formato y quedan expresamente fuera de alcance: no se importan, convierten, modifican ni eliminan. Un importador `.rpb` requeriría un diseño independiente.

### 6.1 Nuevo formato

El backup nuevo será `formatVersion = 2`. Mantendrá JSON, checksum SHA-256, orden canónico y el `manifest` existente. Añadirá:

- colección `measurements` en el payload;
- contador `measurements` en el manifest;
- campos de `PlayerMeasurementEntity` serializados con nombres estables.

La exportación obtiene coaches, players, sessions, pitches y measurements dentro de una sola transacción/snapshot. Las mediciones se ordenan por `id` en el JSON canónico para que el checksum sea reproducible.

### 6.2 Compatibilidad con formato 1

El parser debe aceptar formato 1 y formato 2.

- Para formato 1 se conserva la canonicalización histórica sin la clave `measurements`, de modo que los checksums existentes sigan validándose.
- Al restaurar formato 1, si un jugador contiene `heightCm` o `weightKg`, se crea una medición `INITIAL` para ese jugador usando `manifest.exportedAt` como timestamp heredado y convirtiéndolo una sola vez con `ZoneId.of(manifest.zoneId)`, `Instant.ofEpochMilli(manifest.exportedAt).atZone(zone).toLocalDate().toEpochDay()`; el resultado se guarda como `measuredOnEpochDay`.
- Si el backup antiguo no contiene valores físicos, no se crea medición.
- La conversión se hace una sola vez dentro de la transacción de restauración y usa el ID determinista derivado de `backup-format-1:<playerId>`; restaurar repetidamente el mismo backup identifica la misma medición y no crea otra fila.
- Un backup formato 2 conserva exactamente sus mediciones; no se vuelve a derivar una inicial desde los campos snapshot cuando ya existe una medición para ese jugador.

La validación comprueba IDs únicos, jugadores referenciados, valores numéricos, tipos de medición y que cada medición pertenezca a un jugador presente. Un `MeasurementKind` desconocido es un error de validación antes de modificar Room. La restauración valida todo antes de borrar datos y luego inserta coaches, players, measurements, sessions y pitches en el orden compatible con las foreign keys.

El PIN no se incluye ni se modifica. Los videos no se incluyen ni se borran; el reenlace de pitches mantiene su comportamiento actual.

## 7. Archivo y eliminación

### Archivar

Archivar un jugador sólo actualiza `PlayerEntity.archivedAt`. No elimina ni oculta permanentemente sus mediciones. La ficha archivada puede consultar el historial y no permite iniciar sesiones nuevas.

### Restaurar

Restaurar un jugador sólo limpia `archivedAt`. Todas sus mediciones permanecen intactas y vuelven a estar disponibles.

### Eliminar definitivamente

La eliminación definitiva sigue siendo una acción explícita y sólo procede cuando no existen sesiones ni lanzamientos asociados, conforme a `PlayerHistoryChecker` y `PlayerDeletionPolicy`.

Dentro de una transacción única:

1. comprobar que el jugador existe;
2. comprobar que no tiene historial de sesiones/lanzamientos;
3. eliminar explícitamente sus mediciones;
4. eliminar el jugador;
5. hacer rollback completo ante cualquier fallo.

La foreign key `playerId -> players.id` usa `RESTRICT`; no se depende de un borrado en cascada implícito. Si hay mediciones, no deben impedir una eliminación que ya fue autorizada por la política, porque el repositorio las elimina explícitamente en el paso anterior.

## 8. UI, validación y accesibilidad

- Todo el formulario debe estar dentro de un contenedor desplazable y conservar botones accesibles con teclado y pantallas pequeñas.
- Los selectores de deporte y nivel deben tener etiquetas textuales, estado seleccionado y foco visible.
- Los campos numéricos aceptan coma y punto según la utilidad existente `parseOptionalDecimal`, pero persisten como `Double` finito.
- Estatura: mayor que 0 y hasta 300 cm.
- Peso: mayor que 0 y hasta 500 kg.
- Fechas: válidas, no futuras y representadas visualmente como `DD/MM/AAAA`; la persistencia continúa usando formato ISO o `Long` según el campo.
- Los errores aparecen junto al campo o acción que puede corregirlos y no cierran la pantalla.
- Guardar y Registrar medición se deshabilitan mientras la operación está en curso.
- Los mensajes no deben depender sólo del color y deben anunciar si un campo está bloqueado por edad.
- Eliminar Equipo/academia de la UI no significa eliminar `teamAcademy` del modelo, backup o base.

## 9. Pruebas requeridas

### Dominio y formulario

- Deporte obligatorio: no se puede guardar sin elegirlo; Béisbol y Softbol se guardan correctamente.
- Nivel obligatorio: sólo Principiante, Intermedio y Avanzado; categoría antigua fuera de esas opciones exige selección al editar.
- Equipo/academia no aparece en la UI y sus datos antiguos sobreviven.
- Crear y editar conserva nombre, fecha, mano, deporte, nivel y estado compatible.
- El formulario permite scroll, muestra errores y no duplica guardados por doble toque.

### Mediciones

- Creación sin mediciones.
- Creación con sólo estatura, sólo peso o ambos.
- Menor de 18: estatura y peso permitidos.
- Exactamente 18 y mayores: peso permitido y estatura normal bloqueada.
- Corrección de estatura adulta crea `HEIGHT_CORRECTION` y conserva el historial previo.
- Valores inválidos, infinitos, cero, negativos, fuera de rango y fechas futuras son rechazados.
- Orden del historial por fecha descendente con desempate determinista.
- La última medición actualiza los snapshots de `PlayerEntity`.
- La transacción hace rollback si falla la inserción o la actualización del jugador.
- El mismo identificador de operación no crea duplicados.

### Room y migración

- Migración 7→8 conserva jugadores, sus snapshots, sesiones, pitches y referencias de video.
- Jugadores existentes con valores físicos reciben una medición inicial usando `updatedAt`, con fallback `createdAt` y luego el instante de migración.
- Jugadores sin valores físicos no reciben una fila artificial.
- La foreign key e índices de `player_measurements` son verificables.
- Archivar/restaurar conserva mediciones.
- Eliminación definitiva elimina mediciones y jugador sólo dentro de la transacción explícita.
- Un jugador con historial no puede eliminarse.

### Backup

- Backup formato 2 exporta y restaura mediciones, snapshots, sesiones y pitches.
- Backup formato 1 valida su checksum histórico y se restaura sin mediciones explícitas.
- Backup formato 1 con estatura/peso crea una única medición inicial.
- Restaurar dos veces el mismo backup antiguo no duplica mediciones.
- Fallos de validación o inserción dejan la base sin cambios.
- El PIN permanece intacto y los videos MediaStore no se borran.

### Sesiones y no regresión

- Nueva sesión hereda `playerId` y `sport` del perfil, incluso si un draft recibido intenta usar otro deporte.
- Jugador y deporte no aparecen como controles editables durante la sesión.
- El resumen muestra `Nombre · Deporte`.
- Sesiones históricas conservan su deporte, pitches, videos, análisis y referencias.
- Se conservan navegación, archivado, historial y prevención de sesiones abiertas duplicadas.

## 10. Decisiones finales

- **Formato de fecha de medición:** se persiste como `Long` (`measuredOnEpochDay`) derivado de `LocalDate.toEpochDay()`. Representa una fecha de calendario estable, no depende de zona horaria al leerla y ordena primero de forma descendente por día.
- **Auditoría:** `createdAt` es un timestamp de auditoría basado en reloj de pared. No determina la edad ni la fecha visual de medición. El orden estable del historial es `measuredOnEpochDay DESC`, `createdAt DESC` e `id DESC`.
- **Conversión histórica 7→8:** se usa una sola vez la zona horaria local del dispositivo (`ZoneId.systemDefault()`) al convertir el timestamp heredado seleccionado; después sólo se almacena y lee el epoch day.
- **Conversión histórica de backup formato 1:** se usa la zona declarada en `manifest.zoneId`; un identificador de zona inválido es error de validación antes de modificar Room.
- **Tipos de medición:** los únicos valores son `INITIAL`, `PERIODIC` y `HEIGHT_CORRECTION`.
- **Identificadores:** las mediciones nuevas usan UUID aleatorio; las iniciales de migración y backup formato 1 usan UUID de nombre determinista con su origen separado.
- **Categorías antiguas:** se mantienen literalmente en `players.category`; no se convierten durante migración. El editor exige una nueva selección válida antes de guardar.
- **Equipo/academia:** permanece en `PlayerEntity` y en backups, aunque desaparece de la UI.
- **Cambio de deporte con sesión abierta:** se bloquea para evitar que el perfil activo contradiga una sesión abierta; sesiones finalizadas permanecen inmutables.
- **Corrección adulta de estatura:** es la única vía normal para escribir una estatura adulta durante una medición posterior y queda marcada como `HEIGHT_CORRECTION`.
- **Nombre exacto de nivel persistido:** se recomienda persistir los nombres canónicos `Principiante`, `Intermedio` y `Avanzado`, manteniendo el enum de dominio estable para validación.
- **Backup formato 1:** requiere una ruta de canonicalización legacy separada de formato 2; no se debe recalcular un checksum antiguo con la nueva clave `measurements`.

## Auto-revisión

- No se elimina `teamAcademy` del esquema ni del backup, por lo que la retirada visual no destruye datos.
- No se confunden las mediciones con `PlayerEntity.heightCm` y `weightKg`: éstos siguen siendo snapshots actuales, mientras `player_measurements` es el historial.
- Se preserva `SessionEntity.sport` como dato histórico y no se deriva nuevamente al leer sesiones antiguas.
- Se define qué ocurre con categorías inválidas, backups antiguos, jugadores archivados y eliminación definitiva.
- Se define el fallback de timestamp de migración y la compatibilidad de checksum del backup.
- Se cubren transacciones, duplicados, edades límite, valores parciales y rollback.
- No quedan campos físicos sin decisión: identidad, jugador, fecha, estatura, peso, tipo y auditoría están definidos.
- No quedan decisiones de zona horaria abiertas: migración usa la zona local del dispositivo en ese momento y backup JSON formato 1 (`.radarbackup`) usa `manifest.zoneId`.

## Fuera de alcance

- Gráficas y tendencias visuales; se diseñarán en Reportes.
- Edición masiva o sincronización en nube.
- Cambios al protocolo ESP32, radar, cámara, Media3, PIN o videos.
- Importación, conversión, modificación o eliminación de archivos históricos `.rpb`; cualquier importador `.rpb` requiere un diseño separado.
- Inicio de la Tarea 4.
