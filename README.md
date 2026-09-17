# RadarCamera

RadarCamera es una aplicación Android para registrar velocidades de lanzamiento recibidas desde un radar basado en ESP32, organizar jugadores y sesiones de Coaching, grabar videos cortos y consultar el historial sin depender de internet.

Esta guía explica cómo preparar un computador nuevo, descargar el proyecto privado, compilarlo, probarlo e iniciar la aplicación. Está escrita para que una persona sin experiencia previa pueda seguirla paso a paso.

> [!IMPORTANT]
> El procedimiento seguro usa un emulador Android o un dispositivo físico dedicado exclusivamente a pruebas. Nunca ejecutes pruebas instrumentadas, desinstales la aplicación ni limpies sus datos en un teléfono que contenga información real de RadarCamera.

## Contenido

1. [Qué necesitas saber antes de comenzar](#1-qué-necesitas-saber-antes-de-comenzar)
2. [Entorno y versiones del proyecto](#2-entorno-y-versiones-del-proyecto)
3. [Preparar el computador](#3-preparar-el-computador)
4. [Obtener acceso y descargar el proyecto](#4-obtener-acceso-y-descargar-el-proyecto)
5. [Abrir y configurar el proyecto](#5-abrir-y-configurar-el-proyecto)
6. [Configurar un emulador o dispositivo de pruebas](#6-configurar-un-emulador-o-dispositivo-de-pruebas)
7. [Compilar y ejecutar las pruebas seguras](#7-compilar-y-ejecutar-las-pruebas-seguras)
8. [Instalar y ejecutar la aplicación](#8-instalar-y-ejecutar-la-aplicación)
9. [Configuración inicial dentro de RadarCamera](#9-configuración-inicial-dentro-de-radarcamera)
10. [Conectar el radar ESP32](#10-conectar-el-radar-esp32)
11. [Secretos y archivos que nunca deben publicarse](#11-secretos-y-archivos-que-nunca-deben-publicarse)
12. [Estructura del proyecto](#12-estructura-del-proyecto)
13. [Flujo recomendado de desarrollo](#13-flujo-recomendado-de-desarrollo)
14. [Solución de problemas](#14-solución-de-problemas)
15. [Lista final de comprobación](#15-lista-final-de-comprobación)

## 1. Qué necesitas saber antes de comenzar

### Glosario mínimo

- **Repositorio:** carpeta del proyecto administrada por Git.
- **GitHub:** servicio privado donde se encuentra una copia del repositorio.
- **Clonar:** descargar el repositorio y su historial en un computador.
- **Android Studio:** programa principal utilizado para abrir, compilar y ejecutar la aplicación.
- **SDK de Android:** herramientas que permiten construir aplicaciones Android.
- **Gradle:** sistema que descarga dependencias y compila el proyecto.
- **Gradle Wrapper:** archivos `gradlew` y `gradlew.bat` incluidos en el repositorio. Garantizan el uso de la versión correcta de Gradle.
- **JDK/JBR:** entorno de Java necesario para ejecutar Gradle. Android Studio incluye uno llamado JBR.
- **ADB:** herramienta para comunicarse con un emulador o dispositivo Android.
- **Emulador o AVD:** teléfono Android virtual que funciona dentro del computador.
- **APK:** archivo instalable de Android generado al compilar.
- **ESP32:** dispositivo que publica la velocidad del radar por Wi-Fi.

### Qué funciona sin hardware adicional

En un emulador con cámara virtual se puede abrir la aplicación, recorrer las pantallas, ejecutar pruebas y validar la cámara. Esto no valida la comunicación real con el radar ni el comportamiento exacto de una cámara física.

El `main` actual no muestra el botón **Simular lanzamiento**. Aunque las reglas del proyecto indican que debe conservarse temporalmente, su ausencia es una inconsistencia conocida del código actual, no un problema de instalación. Sin el ESP32 se puede validar apertura, navegación y cámara, pero no generar un evento de radar desde la interfaz.

Para validar la lectura del radar y la grabación real se necesita un dispositivo Android con cámara conectado al punto de acceso Wi-Fi del ESP32.

### Qué no necesita actualmente el proyecto

La compilación `debug` no necesita:

- cuenta de Firebase;
- servidor o backend;
- clave de API;
- archivo `google-services.json`;
- certificado privado de publicación;
- contraseña del ESP32 almacenada en el repositorio.

No inventes ni agregues estos archivos para intentar solucionar una compilación.

## 2. Entorno y versiones del proyecto

### Computador de desarrollo

El proyecto fue verificado en Windows con Android Studio y su JBR integrado. Se espera que también compile en macOS o Linux porque incluye Gradle Wrapper y no usa herramientas exclusivas de Windows, pero esas plataformas todavía no están registradas como verificadas y deben configurar sus propias rutas del SDK y de Java.

| Componente | Configuración del proyecto |
| --- | --- |
| Android Gradle Plugin | 9.4.0 |
| Gradle Wrapper | 9.6.0 |
| Kotlin | 2.2.10 |
| Java exigido para el daemon de Gradle | Java 25 |
| Java del entorno verificado | JBR/OpenJDK 25.0.3 |
| Compatibilidad del código Java | Java 11 |
| SDK de compilación | Android 37.0 |
| SDK objetivo | Android 37 |
| Android mínimo | Android 8.0, API 26 |
| Módulo ejecutable | `app` |
| Identificador de la aplicación | `com.example.radarcamera` |
| Versión actual | `0.9.0-dev`, versionCode 8 |

La fuente de verdad de estas versiones está en:

- `app/build.gradle.kts`;
- `gradle/libs.versions.toml`;
- `gradle/wrapper/gradle-wrapper.properties`;
- `gradle/gradle-daemon-jvm.properties`.

El entorno utilizado para la última verificación fue Android Studio build `AI-261.26222.65.2614.16204760`. No es obligatorio reproducir ese número exacto si una versión estable más reciente soporta AGP 9.4.0, SDK 37.0 y Java 25.

No cambies una versión solo porque Android Studio propone una actualización. Toda actualización de dependencias debe justificarse, compilarse y probarse.

### Dispositivo Android

El dispositivo debe cumplir lo siguiente:

- Android 8.0/API 26 o superior;
- cámara disponible, porque el manifiesto la declara obligatoria;
- espacio libre para instalar la app y guardar MP4;
- Wi-Fi para conectarse al ESP32 durante pruebas reales;
- permiso de cámara concedido a RadarCamera.

## 3. Preparar el computador

### Paso 3.1: instalar Git

Descarga Git desde [git-scm.com/downloads](https://git-scm.com/downloads) y utiliza las opciones predeterminadas del instalador.

Abre PowerShell, Terminal o una consola y ejecuta:

```text
git --version
```

Resultado esperado:

```text
git version 2.x.x
```

Si aparece “comando no encontrado” o “no se reconoce como un comando”, cierra la consola, vuelve a abrirla y repite. Si continúa fallando, Git no quedó instalado o no fue añadido al `PATH`.

### Paso 3.2: instalar Android Studio

Instala Android Studio siguiendo la [guía oficial](https://developer.android.com/studio/install). Conserva el JBR integrado; no es necesario instalar Java por separado.

Durante el primer inicio, permite que Android Studio instale el SDK recomendado.

### Paso 3.3: instalar los componentes del SDK

En Android Studio abre **Tools > SDK Manager** y confirma que estén instalados:

- Android SDK Platform 37.0;
- Android SDK Build-Tools; el entorno verificado dispone de 36.0.0;
- Android SDK Platform-Tools;
- Android Emulator, si se usará un dispositivo virtual;
- una imagen de sistema API 37 para el emulador.

Android Studio puede descargar componentes adicionales durante la primera sincronización. Para esa etapa el computador necesita internet.

### Paso 3.4: usar el Java incluido con Android Studio

En Android Studio abre:

**File > Settings > Build, Execution, Deployment > Build Tools > Gradle**

En **Gradle JDK**, selecciona un JBR/JDK 25. Si el JBR incluido con Android Studio es Java 25, aparecerá normalmente como `Embedded JDK` o `jbr` y es la opción recomendada.

El archivo `gradle/gradle-daemon-jvm.properties` exige Java 25. Si Java 25 no está disponible localmente, Gradle puede intentar aprovisionarlo mediante Foojay usando las URLs declaradas en ese archivo. Ese proceso necesita internet. Para una instalación predecible es preferible usar directamente el JBR 25 de Android Studio.

Para ejecutar Gradle desde una consola, configura `JAVA_HOME` solo en esa consola.

Windows PowerShell, instalación predeterminada:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

macOS, ruta habitual:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Linux, ejemplo habitual:

```bash
export JAVA_HOME="/opt/android-studio/jbr"
```

Las rutas de macOS y Linux pueden cambiar según dónde se haya instalado Android Studio. No copies una ruta si esa carpeta no existe en tu computador. Comprueba la versión con `java -version`; la primera línea debe indicar Java 25.

### Paso 3.5: dejar ADB disponible en la consola

Android Studio instala ADB dentro de `platform-tools`, pero no siempre lo añade al `PATH`. Configúralo temporalmente en cada consola donde usarás los comandos de esta guía.

Windows PowerShell, ruta predeterminada:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:ANDROID_HOME\platform-tools;$env:Path"
adb version
```

macOS, ruta predeterminada:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb version
```

Linux, ruta habitual:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb version
```

Si tu SDK está en otra ubicación, usa la ruta mostrada en **Tools > SDK Manager**. El resultado debe comenzar con `Android Debug Bridge version`.

## 4. Obtener acceso y descargar el proyecto

### Paso 4.1: solicitar permiso

El repositorio es privado. El propietario debe agregar tu cuenta de GitHub como colaborador o mediante el equipo autorizado. Sin ese permiso GitHub puede responder con “Repository not found” aunque la dirección sea correcta.

### Paso 4.2: autenticar GitHub

La opción recomendada es usar Git Credential Manager, incluido normalmente con Git para Windows. También se puede usar [GitHub CLI](https://cli.github.com/) mediante:

```text
gh auth login
```

GitHub ya no acepta la contraseña de la cuenta como contraseña de Git por HTTPS. Si se utiliza un token personal, debe guardarse en el gestor de credenciales, nunca dentro de un comando, archivo o captura de pantalla.

### Paso 4.3: clonar

Ubícate en la carpeta donde quieres guardar el proyecto y ejecuta:

```text
git clone https://github.com/danny19hg-jpg/RadarCamara.git
cd RadarCamara
git status
```

Resultado esperado:

```text
On branch main
Your branch is up to date with 'origin/main'.
nothing to commit, working tree clean
```

Si GitHub solicita autenticación, completa el inicio de sesión en la ventana segura del navegador o del gestor de credenciales.

## 5. Abrir y configurar el proyecto

### Paso 5.1: abrir la carpeta correcta

En Android Studio selecciona **Open** y abre la carpeta `RadarCamara`, la que contiene estos archivos:

```text
settings.gradle.kts
build.gradle.kts
gradlew
gradlew.bat
app/
```

Si Android Studio pregunta si confías en el proyecto, confirma únicamente si lo descargaste desde el repositorio privado correcto.

### Paso 5.2: esperar la sincronización

Android Studio iniciará **Gradle Sync**. La primera sincronización puede tardar porque descarga Gradle y las dependencias desde repositorios oficiales.

No cierres Android Studio hasta que desaparezca el indicador de sincronización. Un resultado correcto no muestra errores rojos de Gradle.

### Paso 5.3: comprobar `local.properties`

Android Studio crea un archivo local llamado `local.properties` con la ruta del SDK. Este archivo depende de cada computador y está excluido por `.gitignore`.

Ejemplo seguro para Windows:

```properties
sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk
```

Ejemplo seguro para macOS:

```properties
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk
```

Ejemplo seguro para Linux:

```properties
sdk.dir=/home/TU_USUARIO/Android/Sdk
```

Reemplaza `TU_USUARIO` solo en tu copia local. No publiques la ruta real ni confirmes `local.properties` en Git.

### Paso 5.4: confirmar Gradle

Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat --version
```

macOS o Linux:

```bash
sh ./gradlew --version
```

El resultado debe indicar Gradle 9.6.0. Se usa `sh ./gradlew` porque el archivo está versionado sin permiso ejecutable Unix; así no es necesario ejecutar `chmod` ni modificar el árbol de trabajo.

## 6. Configurar un emulador o dispositivo de pruebas

### Opción recomendada: emulador

1. Abre **Tools > Device Manager**.
2. Selecciona **Create Virtual Device**.
3. Elige un teléfono con cámara virtual.
4. Elige una imagen Android API 37.
5. En la configuración avanzada, deja al menos una cámara como `Emulated` o `Webcam`.
6. Finaliza y enciende el emulador.

Consulta la [guía oficial para crear un AVD](https://developer.android.com/studio/run/managing-avds) si cambia la ubicación de estas opciones.

Comprueba la conexión:

```text
adb devices
```

Resultado de ejemplo:

```text
List of devices attached
emulator-5554   device
```

El número puede ser diferente. Usa el serial que muestre tu computador.

### Dispositivo físico dedicado

Para usar un teléfono de pruebas:

1. Activa las opciones de desarrollador.
2. Activa **Depuración USB**.
3. Conecta el cable USB.
4. Acepta la huella del computador en el teléfono.
5. Ejecuta `adb devices` y confirma que el estado sea `device`, no `unauthorized`.

Consulta la [guía oficial para ejecutar en un dispositivo](https://developer.android.com/studio/run/device).

> [!CAUTION]
> No uses como dispositivo de pruebas el teléfono que conserva jugadores, sesiones, PIN o videos reales. Nunca ejecutes `connectedDebugAndroidTest` en ese teléfono. Una desinstalación o limpieza elimina la base Room y el PIN privados de la aplicación.

## 7. Compilar y ejecutar las pruebas seguras

Todos los comandos deben ejecutarse desde la raíz del repositorio.

### Paso 7.1: pruebas JVM y APK debug

Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest assembleDebug
```

macOS o Linux:

```bash
sh ./gradlew testDebugUnitTest assembleDebug
```

Resultado esperado al final:

```text
BUILD SUCCESSFUL
```

El APK se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Paso 7.2: compilar las pruebas instrumentadas sin ejecutarlas

Este comando es seguro porque solo construye el APK de pruebas:

Windows:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

macOS o Linux:

```bash
sh ./gradlew assembleDebugAndroidTest
```

### Paso 7.3: ejecutar pruebas instrumentadas únicamente en un entorno seguro

Antes de continuar:

1. Desconecta todos los teléfonos que contengan datos.
2. Ejecuta `adb devices`.
3. Confirma que el único destino sea un serial `emulator-*` o el dispositivo de pruebas designado.

Solo entonces se permite:

Windows:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

macOS o Linux:

```bash
sh ./gradlew connectedDebugAndroidTest
```

Si aparece cualquier teléfono no autorizado para pruebas, detente. No ejecutes el comando esperando que Gradle elija el dispositivo correcto.

## 8. Instalar y ejecutar la aplicación

### Forma sencilla: Android Studio

1. Enciende el emulador.
2. Selecciona el módulo `app`.
3. Selecciona el emulador en la barra superior.
4. Presiona **Run** o el triángulo verde.
5. Espera a que aparezca RadarCamera.

### Forma verificable por consola

Primero compila:

Windows:

```powershell
.\gradlew.bat assembleDebug
```

macOS o Linux:

```bash
sh ./gradlew assembleDebug
```

Después identifica el emulador:

```text
adb devices
```

Supongamos que el serial es `emulator-5554`. Instala la APK:

```text
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

Resultado esperado:

```text
Success
```

Finalmente, este es el comando que abre la aplicación:

```text
adb -s emulator-5554 shell am start -n com.example.radarcamera/.MainActivity
```

Reemplaza `emulator-5554` por el serial mostrado por `adb devices`.

> [!WARNING]
> En un teléfono físico que ya contiene RadarCamera solo se permite una actualización mediante `adb install -r`, sin desinstalar. Una instalación nueva en un teléfono físico requiere autorización expresa del responsable del proyecto. Nunca ejecutes `adb uninstall`, `pm clear` ni “Clear app data” sin esa autorización.

## 9. Configuración inicial dentro de RadarCamera

### Permiso de cámara

Android solicitará acceso a la cámara. Concédelo para usar Radar Live y la grabación de sesiones. Si se rechaza, la aplicación puede abrir, pero no podrá completar los flujos de video.

### Coaching

En una instalación limpia, Coaching solicita:

1. nombre del entrenador;
2. un PIN;
3. confirmación del PIN.

El PIN protege la entrada a Coaching en ese dispositivo. No se sincroniza con GitHub ni debe escribirse en el código, README, logs o capturas públicas.

Después de configurarlo se pueden crear jugadores y sesiones. Los datos se almacenan localmente mediante Room.

### Videos y datos

- Los videos nuevos se publican mediante MediaStore dentro de `Movies/RadarCamera/`.
- El video final conserva aproximadamente 4 segundos previos y 1 segundo posterior al evento.
- El MP4 muestra permanentemente la velocidad en MPH.
- Los respaldos `.radarbackup` contienen datos deportivos y referencias de video, pero no contienen el MP4 ni el PIN.
- Un respaldo `.radarbackup` no está cifrado y debe tratarse como información privada.

## 10. Conectar el radar ESP32

El firmware y el protocolo del ESP32 no forman parte de este repositorio y no deben modificarse sin autorización.

### Conexión

1. Enciende el ESP32/radar.
2. Abre la configuración Wi-Fi del teléfono.
3. Conéctate al punto de acceso del ESP32 usando el nombre y contraseña entregados por el responsable. Esos datos se comparten fuera de Git.
4. Si Android avisa que la red no tiene internet, elige permanecer conectado.
5. Comprueba desde el dispositivo que responde:

```text
http://192.168.4.1/status
```

La respuesta debe contener los campos `eventoLive` y `velocidadLive`. Ejemplo ilustrativo, no una credencial ni un valor fijo:

```json
{
  "eventoLive": 123,
  "velocidadLive": 72.5
}
```

La aplicación permite tráfico HTTP local porque este endpoint no usa HTTPS.

### Comportamiento esperado

- La primera respuesta válida establece una referencia y no registra un lanzamiento.
- Un nuevo valor de `eventoLive` permite registrar la velocidad disponible.
- El endpoint solo expone el último evento; no puede recuperar velocidades intermedias perdidas.
- Un fallo temporal de red no debe borrar el último evento conocido.
- El `main` actual no ofrece simulación desde la interfaz; se necesita el ESP32 para producir eventos de radar reales.

## 11. Secretos y archivos que nunca deben publicarse

### Qué es un secreto

Un secreto es cualquier dato que permita entrar a una cuenta, firmar una aplicación, acceder a una red privada o identificar información personal. Un repositorio privado reduce la exposición, pero no convierte en seguro publicar secretos.

### Situación actual

RadarCamera no necesita secretos para compilar en modo `debug`. Si una persona te pide crear una API key, un keystore o un archivo de Firebase para ejecutar esta versión, primero confirma el requisito con el responsable técnico.

### Ejemplos

| Archivo o dato | ¿Se publica? | Motivo |
| --- | --- | --- |
| `local.properties` | No | Contiene la ruta privada del SDK de cada computador. |
| `.env` o `.env.local` | No | Puede contener contraseñas, tokens o configuración privada. |
| `.env.example` | Sí, si fuera necesario | Debe contener nombres y valores ficticios, nunca secretos reales. |
| `*.jks`, `*.keystore` | No | Permiten firmar aplicaciones. |
| `key.properties`, `keystore.properties` | No | Suelen contener contraseñas y rutas del certificado. |
| `service-account*.json` | No | Puede autorizar servicios externos. |
| Token personal de GitHub | No | Permite actuar con los permisos de una cuenta. |
| PIN de Coaching | No | Protege los datos locales del entrenador. |
| `*.db`, `*.sqlite` | No | Puede contener jugadores, sesiones y lanzamientos. |
| `*.radarbackup`, `*.rpb` | No | Son respaldos de datos del usuario. |
| `*.mp4` | No | Puede contener grabaciones privadas. |
| `*.log` | No | Puede incluir rutas, errores o datos de diagnóstico. |
| `app/schemas/.../*.json` | Sí | Son esquemas técnicos de Room, no la base con datos reales. |
| `AGENTS.md` | Sí | Contiene reglas compartidas de desarrollo, no secretos. |

### Ejemplo seguro de `.env.example`

La aplicación actual no lee un `.env`; este ejemplo solo muestra cómo documentar futuras variables sin revelar valores:

```dotenv
RADAR_WIFI_SSID=REEMPLAZAR_LOCALMENTE
RADAR_WIFI_PASSWORD=REEMPLAZAR_LOCALMENTE
SERVICIO_API_TOKEN=REEMPLAZAR_LOCALMENTE
```

El archivo privado sería `.env`, que está ignorado. Nunca reemplaces los valores del archivo de ejemplo por los reales antes de confirmarlo en Git.

### Ejemplo seguro de configuración de firma

La firma privada no está configurada ni es necesaria para `debug`. Un futuro archivo de ejemplo podría mostrar:

```properties
storeFile=/RUTA/FICTICIA/radarcamera-release.jks
storePassword=REEMPLAZAR_LOCALMENTE
keyAlias=radarcamera
keyPassword=REEMPLAZAR_LOCALMENTE
```

El archivo real y el `.jks` deben permanecer fuera del repositorio y compartirse mediante un gestor de secretos autorizado.

### Ejemplo incorrecto con un token

Nunca hagas esto:

```text
git clone https://TOKEN_REAL@github.com/danny19hg-jpg/RadarCamara.git
```

El token quedaría en el historial de la consola y podría aparecer en logs o capturas. Usa el gestor de credenciales o `gh auth login`.

### Comprobar antes de confirmar cambios

Ejecuta:

```text
git status
git diff --cached
```

Revisa cada archivo. Si aparece una credencial, una base, un respaldo, un video o un log, no hagas el commit. Quitar el archivo en un commit posterior no elimina el secreto del historial anterior.

## 12. Estructura del proyecto

```text
RadarCamara/
├── app/                         Aplicación Android
│   ├── schemas/                 Esquemas versionados de Room
│   └── src/
│       ├── main/                Código y recursos de producción
│       ├── test/                Pruebas JVM
│       └── androidTest/         Pruebas instrumentadas
├── docs/                        Estado, seguimiento, diseños y planes
├── gradle/                      Catálogo de versiones y Gradle Wrapper
├── AGENTS.md                    Reglas obligatorias del proyecto
├── README.md                    Esta guía
├── build.gradle.kts             Configuración Gradle raíz
├── settings.gradle.kts          Repositorios y módulos
├── gradlew / gradlew.bat        Ejecutores de Gradle
└── .gitignore                   Exclusiones de archivos locales
```

Dentro de `app/src/main/java/com/example/radarcamera/` las responsabilidades se separan en:

- `radar/`: consulta y secuencia de eventos del ESP32;
- `camera/`: CameraX, pre-roll, post-roll y exportación Media3;
- `data/`: Room, entidades, DAO y repositorios;
- `navigation/`: destinos y navegación;
- `security/`: PIN y control de acceso a Coaching;
- `backup/`: exportación, restauración y reenlace de videos;
- `domain/`: reglas deportivas y análisis;
- `ui/`: pantallas y ViewModels;
- `di/`: composición de dependencias.

Esta separación representa la arquitectura objetivo y la mayor parte de la implementación actual. Todavía existe lógica activa heredada de radar y cámara en `MainActivity.kt` y `RadarAccess_v0_7.kt`; no agregues más responsabilidades allí y migra únicamente dentro de tareas explícitamente planificadas.

### Archivos generados o locales

No edites ni publiques:

- `.gradle/`;
- `.idea/`;
- `.kotlin/`;
- cualquier `build/`;
- `local.properties`;
- logs, APK, bases, respaldos y videos.

Los JSON ubicados en `app/schemas/` sí se versionan, pero los genera Room. No deben editarse manualmente.

## 13. Flujo recomendado de desarrollo

Antes de trabajar:

```text
git status
git pull --ff-only
```

Lee:

- `AGENTS.md`;
- `docs/ESTADO_ACTUAL.md`;
- `docs/SEGUIMIENTO.md`.

Crea una rama para el cambio:

```text
git switch -c tipo/descripcion-corta
```

Ejemplos:

```text
git switch -c fix/corregir-cursor-radar
git switch -c feat/reporte-sesiones
git switch -c docs/mejorar-instalacion
```

Durante el trabajo:

- conserva Kotlin y Jetpack Compose;
- no concentres funciones nuevas en `MainActivity`;
- conserva CameraX, Media3, el pre-roll de 4 segundos y post-roll de 1 segundo;
- conserva `eventoLive`, `velocidadLive` y el endpoint actual;
- no cambies firmware ni protocolo del ESP32 sin autorización;
- no uses migraciones destructivas de Room;
- realiza commits pequeños y recuperables;
- compila y ejecuta las pruebas relacionadas antes de declarar terminado un cambio.

Antes de confirmar:

```text
git status
git diff --check
git diff
```

## 14. Solución de problemas

### GitHub dice “Repository not found”

Causas posibles:

- la cuenta no tiene permiso sobre el repositorio privado;
- Git está usando credenciales de otra cuenta;
- la dirección fue escrita incorrectamente.

Confirma el permiso con el propietario y vuelve a autenticar GitHub.

### `JAVA_HOME is not set`

Selecciona el JBR integrado en Android Studio o configura `JAVA_HOME` en la consola como se muestra en el paso 3.4. Verifica que la carpeta contenga `bin/java.exe` en Windows o `bin/java` en macOS/Linux.

### Android Studio no encuentra el SDK

Abre **SDK Manager**, copia la ruta mostrada como **Android SDK Location** y corrige solo tu `local.properties`.

### Falta Android SDK 37.0

Instálalo desde **Tools > SDK Manager** y repite Gradle Sync. No reduzcas `compileSdk` para evitar instalarlo.

### Gradle no puede descargar archivos

Comprueba:

- conexión a internet;
- proxy corporativo;
- firewall;
- acceso a `services.gradle.org`, `google()`, Maven Central y `api.foojay.io` cuando Gradle necesite aprovisionar Java 25.

No descargues una distribución de Gradle desde sitios no oficiales. El Wrapper verifica el SHA-256 configurado en el proyecto.

### `adb` no se reconoce

Android SDK Platform-Tools no está instalado o su carpeta no está en `PATH`. Vuelve al paso 3.5, instálalo desde SDK Manager o ejecuta ADB usando la ruta completa.

Windows, ejemplo:

```text
C:\Users\TU_USUARIO\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```

macOS, ejemplo:

```text
/Users/TU_USUARIO/Library/Android/sdk/platform-tools/adb devices
```

Linux, ejemplo:

```text
/home/TU_USUARIO/Android/Sdk/platform-tools/adb devices
```

### El dispositivo aparece como `unauthorized`

Desbloquea el teléfono y acepta la huella RSA. Si no aparece el mensaje, desconecta y conecta nuevamente el cable.

### No aparece ningún dispositivo

- Enciende el emulador antes de ejecutar ADB.
- Confirma que la depuración USB esté activa.
- Cambia el cable o puerto USB.
- En Windows, instala el controlador USB del fabricante cuando corresponda.

### La aplicación no se instala en el emulador

Confirma que el AVD use API 26 o superior y tenga una cámara declarada. Si existen varios dispositivos, añade `-s SERIAL` a cada comando ADB.

### La cámara no funciona

Comprueba el permiso en **Ajustes > Aplicaciones > RadarCamera > Permisos**. En un emulador, revisa que la cámara esté configurada como `Emulated` o `Webcam`.

### El radar no responde

1. Confirma que el teléfono esté conectado al Wi-Fi del ESP32.
2. Acepta permanecer en la red aunque no tenga internet.
3. Abre `http://192.168.4.1/status` en el dispositivo.
4. Confirma que la respuesta incluya `eventoLive` y `velocidadLive`.
5. No cambies la IP, los campos JSON ni el firmware sin autorización.

### La compilación funciona, pero el radar no

La compilación solo demuestra que el código y las dependencias son válidos. La comunicación con el ESP32 requiere la red y el hardware reales. Como el `main` actual no expone simulación, sin ESP32 solo se puede validar por separado la apertura, navegación y cámara virtual.

### Hay datos importantes en el teléfono

Detente antes de instalar, desinstalar, borrar datos o ejecutar pruebas instrumentadas. Crea un respaldo `.radarbackup` desde la aplicación y solicita autorización al responsable. El respaldo no incluye los MP4, por lo que los videos requieren protección separada.

## 15. Lista final de comprobación

Antes de considerar listo un computador nuevo, confirma:

- [ ] La cuenta tiene acceso al repositorio privado.
- [ ] `git --version` funciona.
- [ ] Android Studio está instalado.
- [ ] Gradle usa el JBR integrado.
- [ ] Android SDK Platform 37.0 está instalado.
- [ ] Platform-Tools y ADB están instalados.
- [ ] `local.properties` apunta al SDK local y no está versionado.
- [ ] `git status` muestra un árbol limpio antes de comenzar.
- [ ] Gradle Sync finaliza sin errores.
- [ ] `testDebugUnitTest` finaliza correctamente.
- [ ] `assembleDebug` genera `app-debug.apk`.
- [ ] El destino es un emulador o dispositivo exclusivo de pruebas.
- [ ] `adb devices` muestra el serial esperado.
- [ ] RadarCamera abre mediante Android Studio o el comando ADB final.
- [ ] La cámara tiene permiso.
- [ ] Para radar real, `http://192.168.4.1/status` responde con `eventoLive` y `velocidadLive`.
- [ ] Ningún secreto, respaldo, base, video o log aparece en `git status`.

## Referencias oficiales

- [Instalar Android Studio](https://developer.android.com/studio/install)
- [Crear y administrar dispositivos virtuales](https://developer.android.com/studio/run/managing-avds)
- [Ejecutar aplicaciones en un dispositivo físico](https://developer.android.com/studio/run/device)
- [Compilar desde la línea de comandos](https://developer.android.com/build/building-cmdline)
- [Clonar un repositorio de GitHub](https://docs.github.com/es/repositories/creating-and-managing-repositories/cloning-a-repository)
- [Autenticación en GitHub](https://docs.github.com/es/authentication/keeping-your-account-and-data-secure/about-authentication-to-github)
