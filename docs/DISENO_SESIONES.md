# Continuación de v0.8: sesiones y lanzamientos (diseño, no implementado)

## Incrementos previstos

1. Nueva sesión: jugador, deporte, tipo y objetivo 10/20/30/50/personalizado.
2. Cámara en primer plano con aviso de preparación; meta alcanzada avisa y no cierra.
3. Registro local y métricas; después asociación robusta con MP4.
4. Récords, descarte, historial y reportes locales.
5. Herramientas de desarrollador solo debug; conservar simulación y aislarla de datos reales.

## Modelo previsto

- SessionEntity: UUID, playerId (FK), entrenador/snapshots, deporte fijo, tipo inicial/actual, objetivo, estado, inicio/finalización y siguiente ordinal.
- PitchEntity: UUID, sessionId (FK), esp32EventId, radarEpochId, tipo capturado al recibir el evento, número de sesión, MPH, hora del teléfono, validez/descarte, indicador histórico de récord, estado/URI del video, preRollMs=4000 y postRollMs=1000.
- VideoExportEntity: pitchId único, estado, archivos necesarios, nombre determinista y error/ventana de recorte.
- RadarCursorEntity: fuente/ciclo local y último evento observado. No inventar eventos que /status no permite recuperar.
- Crear índices únicos (sessionId, número) y (radarEpochId, esp32EventId).
- Mantener claves UUID, createdAt/updatedAt y revisión; archivo/descarte sin borrado de historial.
- Los jugadores actuales están asociados a la instalación. Añadir asociaciones futuras mediante una migración que preserve UUID y datos, sin depender del nombre.
- Antes de añadir tablas, pasar a esquema 2 y probar migración desde el esquema 1 versionado.

## Reglas de negocio

- El jugador/deporte del lanzamiento se resuelven por sesión; snapshots conservan el contexto histórico.
- El tipo puede cambiar en sesión, pero no cambia metadatos de un evento/clip ya capturado.
- Récord por jugador + deporte + tipo, solo lecturas válidas reales, superación estricta (no empates).
- Primera lectura establece marca inicial. Separar récord histórico del máximo vigente.
- Descartar recalcula máximos y progresión de récords en transacción; nunca borra automáticamente su video.
- Métricas: cantidad válida, máxima, mínima, promedio y progreso; sin datos mostrar —.
- No grabar en el MP4 un distintivo de récord que después pueda ser invalidado; MPH sí permanentes.

## Integración gradual

- Extraer cliente radar y controlador/exportador en commits separados, preservando primero su comportamiento.
- UUID/snapshot/marca monotónica al recibir el evento; no desplazar el instante de recorte esperando a Room.
- Registrar la lectura por separado del éxito de video. URI inicialmente nula, con estado pendiente/no disponible/fallido/disponible.
- Room y MediaStore no son una transacción: publicar con nombre basado en pitchId y reconciliar exportaciones interrumpidas.
- Un fallo de video no equivale a lectura deportiva descartada.
- Mantener limitaciones de recarga 4 s, RAW creciente y eventos perdidos documentadas.
- La URI MediaStore pertenece al dispositivo. Preparar metadatos para sincronización futura no implementa transporte ni nube.
- Requerir compilación, pruebas automáticas y comparación física en cada cambio del motor.
