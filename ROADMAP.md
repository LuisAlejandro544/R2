# 🗺️ ROADMAP - Mobile OBS (Android)

Este documento detalla la hoja de ruta de desarrollo para transformar **Mobile OBS** en la suite completa de transmisión y grabación móvil para Android.

---

## 📌 Estado Actual: Fase 1 - Grabación de Pantalla Base & Arquitectura Híbrida (COMPLETADO ✅)

- [x] Captura de pantalla nativa con `MediaProjection` API.
- [x] Servicio en segundo plano (`ScreenRecordService`) con notificación interactiva.
- [x] Interfaz en Jetpack Compose con controles de resolución (720p/1080p), FPS (30/60 FPS), bitrate (4-16 Mbps), mezcla dual de audio, supresión de ruido (Noise Gate) y cámara rostros (Facecam PiP).
- [x] Permiso dinámico de Cámara en tiempo de ejecución para Facecam PiP.
- [x] Lista de grabaciones locales con reproductor de video integrado en la app y editor de **Recorte de Video Sin Pérdida (Lossless Trimming)**.
- [x] Compartición segura mediante `FileProvider` e intenciones de Android.
- [x] JNI Bridges actualizados en **C++ (Mezcla de Audio + Noise Gate DSP)** y **Rust (Overlay Facecam PiP + Lossless Video Trimming)** con modo fallback seguro.

---

## 🚀 Fase 2: Optimización C++ Oboe y Captura Directa de Audio del Sistema (Q3 2026)

- [x] **Oboe C++ Native Mixing Setup**:
  - JNI Bridge para controlar niveles independientes de volumen entre juego y micrófono.
- [x] **Reducción de Ruido Nativa (Noise Gate C++ DSP)**:
  - Filtro atenuador de ruido de fondo integrado en `oboe_audio_engine.cpp` con umbral configurable.
- [x] **Recorte de Video Rápido Sin Pérdida (Rust / MediaMuxer)**:
  - Módulo de edición de video lossless sin recomprimir fotogramas.
- [x] **AudioPlaybackCapture API & Filtros Avanzados (Android 10+ / C++ Oboe)**:
  - Enlace de buffers PCM con Compresor Dinámico, Amplificador de Ganancia (Gain Booster) y Ecualizador de 3 Bandas (Graves, Medios, Agudos) en `oboe_audio_engine.cpp`.
- [x] **Sistema de Escenas y Fuentes (Rust / Scene Graph)**:
  - Soporte para múltiples escenas (Gaming + Facecam, Solo Pantalla, Pausa, IRL Focus) con control de visibilidad y capas Z de fuentes independientes (Pantalla, Cámara PiP, Marca de Agua, Texto).
- [x] **Cámara Frontal PiP (Picture-in-Picture)**:
  - Opción de habilitar/deshabilitar la cámara de rostro sobre la grabación mediante el motor Rust.
- [x] **Herramientas de Diagnóstico y Debug (Solo APK Debug)**:
  - Panel interactivo modal con monitor de CPU/RAM en tiempo real, desglose de consumo por subsistema (Video Pipeline, Audio Oboe, Facecam, Overlay y UI Render), visor de logs con filtros y simulador de fallos/errores.
  - *Próximamente*: Formas personalizadas de PiP (círculo, borde brillante, mover arrastrando).
- [ ] **Overlays e Imágenes**:
  - Añadir logos, marcos personalizados, alertas y widgets de texto en tiempo real sobre el video usando el motor Rust.

---

## 📡 Fase 4: Transmisión en Vivo / Streaming RTMP & SRT (Q1 2027)

- [ ] **Cliente RTMP / RTMPS**:
  - Transmisión en directo a plataformas como Twitch, YouTube Live, Facebook Gaming y Kick.
- [ ] **Protocolo SRT (Secure Reliable Transport)**:
  - Transmisión de alta estabilidad con corrección de errores en redes móviles 4G/5G.
- [ ] **Chat en Pantalla / Reacciones**:
  - Widget flotante para leer el chat de la transmisión sin interrumpir el juego.

---

## 🔌 Fase 5: Ecosistema de Extensiones y Filtros (Q2 2027)

- [ ] **Filtros de Procesamiento de Video**:
  - Ajustes de brillo, contraste, croma (Chroma Key / Fondo Verde) usando fragment shaders en Rust / OpenGL ES.
- [ ] **Soporte para Mandos y Accesos Rápidos**:
  - Atajos de teclado/mando Bluetooth para cambiar de escena o silenciar micrófono.
