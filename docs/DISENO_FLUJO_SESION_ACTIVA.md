# Flujo simplificado de sesión activa

La preparación crea una única sesión con jugador y deporte. Los campos heredados se mantienen en Room: tipo inicial `FASTBALL`, objetivo `0` y grabación desactivada. El repositorio devuelve el UUID confirmado por Room.

La preparación emite una navegación consumible `SessionCreated(id)` una sola vez. La ruta activa carga ese ID, establece el baseline de radar y es la única responsable de cursor, admisión de eventos, tipo actual y video. La cámara nunca condiciona el lanzamiento deportivo.

Las sesiones existentes no se transforman. Historial, descarte recuperable, URI MediaStore y el protocolo `/status` permanecen sin cambios.
