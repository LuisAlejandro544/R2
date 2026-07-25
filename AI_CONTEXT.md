# 🤖 AI CONTEXT - Manual para Agentes de IA

> **ATENCIÓN AGENTES DE IA / LLMs**: Este documento es el manual de contexto para cualquier modelo de lenguaje o asistente de código de IA que trabaje en este proyecto. **Léelo detenidamente antes de realizar cualquier cambio.**

---

## 📌 Contexto General del Proyecto

- **Nombre del proyecto**: Mobile OBS (Grabar Pantalla)
- **Propósito**: Crear una suite de grabación de pantalla y streaming móvil inspirada en OBS Studio.
- **Enfoque Actual**: 100% centrado en **grabación de pantalla**, gestión de videos grabados y arquitectura híbrida nativa. El streaming RTMP/SRT está diferido para fases futuras.
- **Stack principal**: Kotlin 2.0+ (Jetpack Compose), Android NDK C++ (Google Oboe Audio), Rust (`cdylib` JNI Pipeline).

---

## ⚠️ Reglas Críticas e Inviolables para IA

### 1. Garantía de Compilación (`compile_applet`)
- **Regla N.º 1**: Todo cambio debe mantener la compilación en verde. Después de modificar código, ejecuta `compile_applet` para verificar.
- **JNI Fallback Obligatorio**: Todas las clases wrapper JNI (`OboeAudioEngine.kt`, `RustVideoPipeline.kt`) **DEBEN** capturar `UnsatisfiedLinkError` dentro de un bloque `try-catch` al ejecutar `System.loadLibrary()`. Si la librería `.so` nativa no está presente en el entorno de build, la aplicación **NUNCA DEBE CRASHAR**; en su lugar, debe conmutar transparentemente a la implementación o simulación en Kotlin.

### 2. Preservación del Paquete y Nombres
- **Namespace Android**: Mantiene siempre `com.example` como namespace para evitar romper clases autogeneradas R o imports.
- **Application ID**: Configurado como `com.aistudio.screenrecorder.vxmpzq` en `app/build.gradle.kts`. **NO** cambies el applicationId existente a menos que el usuario lo pida expresamente.
- **Metadata**: Mantén `metadata.json` sincronizado con `res/values/strings.xml` (`app_name` = "Grabar Pantalla").
- **Capabilities**: **NUNCA** elimines `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` de `metadata.json`.

### 3. Permisos de Android & Foreground Services
- `ScreenRecordService` requiere `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`.
- Todas las intenciones de permisos de captura de pantalla deben realizarse dinámicamente mediante `registerForActivityResult` / `rememberLauncherForActivityResult`.
- Los archivos grabados se comparten mediante `FileProvider` con la autoridad `${applicationId}.fileprovider`.

---

## 🧩 Patrones de Código Recomendados

### Estado y ViewModel en Compose
- Utiliza `StateFlow` y `collectAsStateWithLifecycle()` o `collectAsState()`.
- Centraliza las operaciones de datos en `ScreenRecordRepository` (Singleton) o repositorios limpios.

### Estilo de Interfaz (Material Design 3)
- Paleta oscura moderna basada en tonos `#0F121C`, `#181D2D`, `#00D2FF` (Cian de acento) y `#FF3B30` (Rojo de grabación).
- Bordes redondeados de 16dp a 28dp para tarjetas.
- Target de toque mínimo de 48dp x 48dp para accesibilidad.
- Incluye siempre `testTag` en botones principales (ej: `Modifier.testTag("record_button")`).

---

## 🛠️ Cómo Extender las Capas Nativas

- **Para agregar funciones a C++ Oboe**:
  1. Define la función `external fun` privada en `OboeAudioEngine.kt` (ej: `configureDualAudioMixing`, `configureNoiseGate`).
  2. Implementa la función JNI correspondiente con el nombre mangled `Java_com_example_nativebridge_OboeAudioEngine_nombreFuncion` en `oboe_audio_engine.cpp` (ej: `Java_com_example_nativebridge_OboeAudioEngine_nativeSetNoiseGate`).
  3. Proporciona una ruta alternativa en la rama `else` cuando `isNativeLoaded == false`.

- **Para agregar funciones a Rust Video Pipeline**:
  1. Define la función en `RustVideoPipeline.kt` (ej: `configureCameraPipOverlay`, `trimVideoLossless`).
  2. Agrega la función etiquetada con `#[no_mangle] pub extern "system" fn Java_com_example_nativebridge_RustVideoPipeline_...` en `app/src/main/rust/src/lib.rs` (ej: `nativeTrimVideoLossless`).
  3. El módulo Rust incluye un fallback seguro en Kotlin utilizando `MediaExtractor` y `MediaMuxer` para remuxing sin pérdida cuando las librerías binarias `.so` no están compiladas físicamente.
  4. El módulo Rust utiliza la directiva de compilador explícita `rust-version = "1.97.1"`, la edición `2021`, los tipos de crate `["cdylib", "staticlib"]` y la librería `serde` en `Cargo.toml`.

---

## 📸 Permisos de Cámara para Facecam PiP
- Se ha añadido `<uses-permission android:name="android.permission.CAMERA" />` al `AndroidManifest.xml`.
- En la UI (`ScreenRecordScreen.kt`), al activar el switch de **Cámara Rostro / Facecam (Rust)** se solicita dinámicamente el permiso `Manifest.permission.CAMERA` antes de habilitar la función.

---

## 🐞 Herramientas de Debug & Diagnóstico (`com.example.debug`)
- **Exclusividad Debug**: Todas las herramientas de diagnóstico y botones de acceso están condicionados a `if (BuildConfig.DEBUG)` en `ScreenRecordScreen.kt`. No aparecerán en builds de Release.
- **`DebugLogger`**: Singleton thread-safe que registra logs con nivel (`INFO`, `WARN`, `ERROR`, `CRASH`) y captura automáticamente `UncaughtExceptionHandler` para loguear excepciones no controladas.
- **`PerformanceMonitor`**: Monitorea continuamente el % de CPU consumido, la memoria Heap JVM (usada vs máx), memoria nativa allocated y calcula la estimación de consumo por subsistema (Video Pipeline, Audio Oboe Engine, Overlay Flotante, Facecam y Compose Render UI).
- **`DebugToolsBottomSheet`**: ModalBottomSheet UI con pestañas para visualizar métricas, buscar/filtrar logs, copiar trazas al portapapeles y simular errores o forzar `System.gc()`.
