# Mobile OBS - Grabar Pantalla (Android)

> **Mobile OBS** es una aplicación de grabación de pantalla de alto rendimiento para Android inspirada en Open Broadcaster Software (OBS). Integra un frontend moderno en **Jetpack Compose (Kotlin)**, un motor de audio de ultra baja latencia en **C++ (Oboe)** y un pipeline de procesamiento de video e imágenes sin copia (Zero-Copy) en **Rust**.

---

## 🚀 Características Principales

- 🎥 **Grabación de Pantalla en Alta Definición**: Soporte para presets HD 720p y Full HD 1080p, tasa de cuadros (30 FPS, 60 FPS) y bitrate configurable (4 Mbps, 8 Mbps, 12 Mbps, 16 Mbps).
- 🎙️ **Selector Intuitivo de Fuente de Audio (C++ / Oboe)**: Permite seleccionar con un toque el modo exacto que deseas grabar:
  - 🎮 **Juego + Mic**: Mezcla el audio interno del juego con tu voz mediante el motor C++ Oboe.
  - 🕹️ **Solo Juego**: Graba únicamente el sonido interno del juego/sistema en silencio externo.
  - 🎙️ **Solo Micrófono**: Graba únicamente tu voz por micrófono.
  - 🔇 **Silencio**: Grabación limpia de pantalla sin pista de audio.
- 🎬 **Sistema de Escenas y Fuentes (Rust / Scene Graph)**: Alterna fácilmente entre presets de escena profesionales ("Gaming + Facecam", "Solo Pantalla Full", "Pausa / Regresamos En Breve", "IRL / Chatting") y conmuta fuentes independientes (Pantalla de Juego, Cámara Facecam, Marca de Agua Logo, Texto Banner) en tiempo real.
- 🎛️ **Filtros de Audio Avanzados (C++ Oboe DSP)**:
  - **Supresión de Ruido / Noise Gate**: Atenúa el ruido de fondo atenuado por debajo de -35dB.
  - **Compresor Dinámico**: Nivela picos altos de volumen durante momentos intensos de juego para evitar saturación y distorsión.
  - **Amplificador de Ganancia (Gain Booster)**: Oculta o resalta voces suaves de +0dB a +12dB.
  - **Ecualizador de 3 Bandas**: Ajuste independiente de frecuencias Graves (Bass), Medios (Voice) y Agudos (Treble).
- 📷 **Superposición de Cámara Facecam (Rust / PiP)**: Opción conmutable de Picture-in-Picture (PiP) para grabar tu rostro en tiempo real sobre la pantalla.
- 🐞 **Herramientas de Debug en Tiempo Real (Solo APK Debug)**:
  - **Métricas de Rendimiento & Consumo de Hardware**: Monitor en tiempo real de uso de CPU (%), memoria JVM Heap, memoria nativa C++/Rust, conteo de hilos activos y desglose por subsistema (Video Pipeline, Motor Audio Dual, Overlay Flotante y Render Compose UI).
  - **Consumo por Subsistema**: Muestra exactamente cuánto CPU y RAM consume cada módulo individualmente en tiempo de ejecución.
  - **Visor & Trazabilidad de Logs**: Captura continua de eventos, advertencias y errores/crashes con filtro (INFO, WARN, ERROR), buscador interactivo y exportación al portapapeles.
  - **Simulador de Fallos**: Generador de advertencias y errores de prueba, junto con forzado de recolección de basura (`System.gc()`).
- ✂️ **Recorte de Video Sin Pérdida (Rust / MediaMuxer)**: Editor de video de ultra rápida velocidad que remuxea y recorta clips sin recomprimir píxeles, manteniendo la calidad original al 100%.
- ⚡ **Pipeline de Video Nativo (Rust)**: Procesamiento eficiente de fotogramas, YUV/NV12 y composición de escenas con baja huella de memoria.
- 🔔 **Servicio en Segundo Plano (Foreground Service)**: Notificación persistente con control de pausa/detención directo desde la barra de estado de Android.
- 📁 **Gestión de Grabaciones**: Reproductor integrado en la app, opción para abrir en reproductores externos, compartir mediante `FileProvider` y eliminar videos almacenados.
- 🛡️ **Manejo de Permisos Adaptativo**: Solicitud dinámica de permisos en tiempo de ejecución para `RECORD_AUDIO` y `POST_NOTIFICATIONS` en Android 13+.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología | Función |
| :--- | :--- | :--- |
| **UI & App Lifecycle** | Kotlin, Jetpack Compose, Coroutines, StateFlow, ViewModel | Interfaz moderna, reactiva, gestión de estado y permisos. |
| **Android Service** | MediaProjection API, Foreground Service, FileProvider | Captura de pantalla nativa de Android y exportación de archivos. |
| **Audio Engine** | C++17, Google Oboe / AAudio JNI Bridge (`oboe_audio_engine`) | Captura de audio de ultra baja latencia y mezcla. |
| **Video Pipeline** | Rust (`rust_obs_pipeline`), JNI bindings | Procesamiento eficiente de fotogramas y buffers en memoria. |

---

## 💻 Instalación y Compilación

### Requisitos Previos
- **Android SDK**: API Level 34+ (minSdk 24, Android 7.0+).
- **Kotlin**: 2.0+ con Jetpack Compose Compiler habilitado.
- **NDK (Native Development Kit)**: CMake 3.22+ y Clang (para el módulo C++).
- **Rust Toolchain**: `cargo` y `target` configurados para `aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android` (opcional si se utiliza la versión simulada/JNI fallback).

### Pasos de Compilación
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/mobile-obs.git
cd mobile-obs

# Compilar la aplicación Debug con Gradle
./gradlew assembleDebug

# Ejecutar los tests unitarios y de interfaz
./gradlew testDebugUnitTest
```

---

## 📲 Cómo Probarlo en el Dispositivo

1. Abre **Mobile OBS** en tu teléfono Android.
2. Configura los **Ajustes de Grabación**:
   - Activa o desactiva la captura de **Micrófono**.
   - Selecciona la calidad deseada (**720p HD** o **1080p Full HD**).
3. Haz clic en el botón principal **Iniciar Grabación**.
4. Concede los permisos de captura de pantalla (`MediaProjection`) y micrófono cuando el sistema lo solicite.
5. Minimiza la app y realiza tus actividades.
6. Detén la grabación desde la **notificación persistente** o reabriendo la app y presionando **Detener**.
7. Revisa la lista de **Tus Grabaciones** en la parte inferior para reproducir, compartir o eliminar tus videos.
