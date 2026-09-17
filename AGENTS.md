# Reglas de desarrollo del proyecto

- Antes de cualquier trabajo, todo chat o agente debe leer `AGENTS.md`, `docs/ESTADO_ACTUAL.md`, `docs/SEGUIMIENTO.md`, ejecutar `git status` y revisar los commits recientes.
- `docs/ESTADO_ACTUAL.md` es el punto compacto de continuidad y se actualiza después de cada bloque importante.

- Responde y explica los cambios en español.
- Trabaja como responsable técnico de una aplicación Android de producción.
- Usa Kotlin y Jetpack Compose.
- Mantén una arquitectura modular.
- No concentres nuevas funciones en MainActivity.
- Separa radar, cámara, navegación, seguridad, datos y pantallas.
- Conserva CameraX y Media3.
- Conserva el pre-roll de 4 segundos y post-roll de 1 segundo.
- El MP4 final debe durar aproximadamente 5 segundos.
- Mantén el overlay permanente de MPH.
- Mantén temporalmente el botón Simular lanzamiento para pruebas de mesa.
- No modifiques el firmware ni el protocolo del ESP32 sin autorización.
- El endpoint actual es http://192.168.4.1/status.
- Conserva eventoLive y velocidadLive.
- No modifiques dependencias sin justificarlo.
- No edites archivos generados, build, .gradle ni local.properties.
- Ejecuta assembleDebug y las pruebas después de cambios relevantes.
- Nunca ejecutes connectedDebugAndroidTest en el teléfono físico que contenga datos de RadarCamera. Las pruebas instrumentadas solo se ejecutan en un emulador o en un dispositivo exclusivo de pruebas, identificado antes de ejecutarlas.
- En el teléfono físico solo se permite adb install -r cuando el paquete ya exista y pruebas manuales. Una instalación nueva requiere autorización expresa.
- Antes de cualquier comando que pueda desinstalar, limpiar datos o reemplazar datos, detente y solicita autorización explícita.
- No declares una función terminada sin compilarla.
- Realiza commits pequeños y recuperables.
- Solo crea etiquetas estables después de pruebas físicas en el teléfono.
