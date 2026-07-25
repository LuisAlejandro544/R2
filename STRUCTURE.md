# 🏗️ STRUCTURE - Estructura del Proyecto Mobile OBS

Este archivo describe la organización del código fuente, carpetas principales y flujo de datos de la arquitectura híbrida Kotlin + C++ + Rust.

---

## 📂 Árbol de Directorios del Proyecto

```
/
├── README.md                           # Visión general del proyecto e instalación
├── ROADMAP.md                          # Plan de desarrollo por fases
├── STRUCTURE.md                        # Estructura de archivos y arquitectura (este archivo)
├── AI_CONTEXT.md                       # Manual de contexto y reglas para Agentes de IA
├── AGENTS.md                           # Instrucciones globales para entornos de IA
├── .github/
│   └── workflows/
│       ├── android-build.yml           # CI Workflow: Compilación con caché, keystore dinámico y artifacts
│       └── security-scan.yml           # Security Workflow: Análisis CodeQL y escaneo de vulnerabilidades Trivy
├── metadata.json                       # Configuración de plataforma Google AI Studio
├── build.gradle.kts                    # Gradle de nivel de proyecto
├── settings.gradle.kts                 # Configuración de módulos e inclusión de app
│
└── app/
    ├── build.gradle.kts                # Gradle del módulo de la aplicación Android
    ├── proguard-rules.pro              # Reglas de ofuscación de ProGuard/R8
    └── src/
        └── main/
            ├── AndroidManifest.xml     # Permisos (MediaProjection, Record Audio, Camera, Notifications)
            │
            ├── java/com/example/
            │   ├── MainActivity.kt               # Punto de entrada de la actividad principal
            │   ├── ScreenRecordScreen.kt         # Pantalla principal con toggles de FPS/Bitrate, Escenas/Fuentes, Filtros DSP y Facecam PiP
            │   ├── ScreenRecordViewModel.kt      # ViewModel para coordinar UI, permisos, recorte de video y extras del Service
            │   ├── ScreenRecordState.kt          # Estados UI (scenes, audioFilters, audioMode, enableCameraPip, FPS/Bitrate)
            │   ├── ObsSceneModels.kt             # Modelos de datos de Escenas (ObsScene, ObsSource) y Filtros DSP (AudioFilterSettings)
            │   ├── ScreenRecordRepository.kt     # Repositorio Singleton para estados y lista de videos
            │   ├── ScreenRecordService.kt        # Foreground Service con MediaProjection y orquestación JNI
            │   ├── RecordedVideo.kt              # Modelo de datos de archivos de video grabados
            │   ├── VideoPlayerDialog.kt          # Diálogo con VideoView para reproducción y editor de recorte sin pérdida
            │   │
            │   ├── debug/                        # Herramientas exclusivas para el APK Debug (logs, métricas, consumos)
            │   │   ├── DebugLogger.kt            # Logger thread-safe, captura de excepciones y buffer de mensajes
            │   │   ├── PerformanceMonitor.kt     # Medición de CPU %, RAM JVM/Native y consumo por subsistema
            │   │   └── DebugToolsBottomSheet.kt  # Panel UI modal con pestañas de Consumos, Logs, Pruebas y Build Info
            │   │
            │   ├── nativebridge/
            │   │   ├── OboeAudioEngine.kt        # JNI Wrapper C++ con configureDualAudioMixing y configureNoiseGate
            │   │   ├── RustVideoPipeline.kt      # JNI Wrapper Rust con configureCameraPipOverlay y trimVideoLossless
            │   │   └── NativeObsCore.kt          # Orquestador nativo principal (Audio + Video)
            │   │
            │   └── ui/theme/                     # Colores, Tipografías y Temas M3
            │       ├── Color.kt
            │       ├── Theme.kt
            │       └── Type.kt
            │
            ├── cpp/                              # Motor Nativo en C++
            │   ├── CMakeLists.txt                # Configuración de compilación CMake
            │   └── oboe_audio_engine.cpp         # Mezcla de audio en C++ (Juego + Micrófono)
            │
            ├── rust/                             # Pipeline Nativo en Rust
            │   ├── Cargo.toml                    # Dependencias del Crate de Rust (jni, log)
            │   └── src/
            │       └── lib.rs                    # Composición de escenas y overlay PiP para Facecam
            │
            └── res/                              # Recursos de Android (Iconos, Strings, XML)
                ├── drawable/                     # Iconos y gradientes
                ├── values/                       # strings.xml, colors.xml, themes.xml
                └── xml/                          # file_paths.xml para FileProvider
```

---

## 🔄 Flujo de Datos y Arquitectura

1. **Capa de Interfaz (UI)**:
   - `MainActivity` inicia `ScreenRecordScreen` (Jetpack Compose).
   - `ScreenRecordViewModel` observa el estado reactivo desde `ScreenRecordRepository`.
   - Cuando el usuario presiona **Iniciar Grabación**, `ScreenRecordScreen` solicita la intención de captura de pantalla mediante `MediaProjectionManager`.

2. **Capa de Servicio (Service & Native Execution)**:
   - La intención obtenida se envía a `ScreenRecordService` vía `startForegroundService()`.
   - `ScreenRecordService` instancia `NativeObsCore`.
   - `NativeObsCore` inicializa:
     - **C++ (`OboeAudioEngine.kt` -> `oboe_audio_engine.cpp`)**: Prepara la captura de audio de baja latencia con Oboe.
     - **Rust (`RustVideoPipeline.kt` -> `lib.rs`)**: Inicializa el pipeline de procesamiento de cuadros de video.
   - `MediaRecorder` y `VirtualDisplay` capturan la pantalla de Android y guardan la salida en el directorio de películas del sistema (`/Android/data/.../files/Movies`).

3. **Capa de Gestión de Contenido**:
   - Una vez finalizada la grabación, `ScreenRecordRepository` escanea la carpeta de archivos y actualiza la lista en tiempo real.
   - La UI renderiza la tarjeta del video permitiendo reproducir con `VideoPlayerDialog`, compartir mediante `FileProvider` o eliminar el archivo.
