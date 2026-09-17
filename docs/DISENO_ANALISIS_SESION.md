# Diseño: análisis posterior de sesión

## Propósito

El análisis es una pantalla de solo lectura, accesible desde el detalle de una sesión finalizada válida. No participa en la captura, el radar, la cámara ni la escritura de Room. Recibe solo `sessionId` por navegación y observa sesión, jugador y pitches persistidos.

## Capas

- `domain/analysis`: modelos inmutables y `SessionAnalysisEngine`. Recibe `List<PitchEntity>`, ordena por `number`, después por `receivedAt` e `id`, y devuelve resumen, distribución, puntos y resumen por tipo. No depende de Compose, Room ni red.
- `ui/analysis`: `SessionAnalysisViewModel` combina observaciones de repositorios por `sessionId`; expone Loading, Empty, Content, NotFound y Error. No escribe datos.
- `ui/analysis/charts`: Canvas Compose con geometría calculada en funciones puras. Dona y serie temporal siempre incluyen texto/leyenda; no dependen solo de color.
- `navigation`: ruta `SESSION_ANALYSIS` con `analysisSessionId`. El detalle continúa siendo el origen y destino de volver.

## Reglas y compatibilidad

Una sesión descartada no se carga para análisis normal: `PitchDao.observe` ya exige `discardedAt IS NULL`. Sesiones, pitches, URI y estado de video no se modifican; no se requiere migración. El motor filtra MPH no finitos para estadísticas y conserva cada pitch de tipo vacío como `Sin tipo`; valores de enum no reconocibles se presentan con etiqueta legible. El redondeo a una decimal se realiza en la UI.

## Gráficas

No existe librería de gráficas declarada. Se usarán `Canvas` pequeños sobre cálculos de dominio comprobables. La dona muestra solo tipos presentes. La línea ordena por lanzamiento; su escala agrega margen y maneja cero/uno/velocidades iguales sin NaN ni división por cero. En retrato, toda la pantalla usa scroll vertical.

## Revisión

El diseño no modifica Room, cámara, MediaStore, cursor, `/status`, deduplicación ni captura 4+1. Tampoco introduce dependencia nueva ni nube. Las métricas se derivan exclusivamente de pitches persistidos de sesiones no descartadas.
