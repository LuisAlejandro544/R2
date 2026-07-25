# 🤖 AGENTS.md - Reglas Persistentes de Agentes

> Este archivo define las directivas de comportamiento y convenciones de trabajo para los agentes de desarrollo en este repositorio.

---

## 🎯 Directiva Principal
**Construye exactamente lo que el usuario describe, respetando la arquitectura de Mobile OBS.**
- Mantén la calidad de la interfaz en Jetpack Compose (Material Design 3).
- No agregues dependencias pesadas innecesarias sin justificación.
- Asegura siempre que la aplicación compile sin errores ejecutando `compile_applet`.

---

## 🛠️ Convenciones de Código y Archivos

1. **Lenguaje**:
   - Todo el código de Android debe escribirse en **Kotlin**.
   - El código del motor de audio de baja latencia debe estar en **C++** (`app/src/main/cpp`).
   - El pipeline de procesamiento de fotogramas debe estar en **Rust** (`app/src/main/rust`).

2. **UI & Jetpack Compose**:
   - Usa `Scaffold`, `Card`, `LazyColumn`, `Row` y `Column`.
   - Evita colores hardcodeados directamente en los Composables; favorece variables centralizadas o la paleta M3.
   - Aplica etiquetas `testTag` en componentes interactivos clave (`record_button`, etc.).

3. **Arquitectura JNI**:
   - Encapsula todas las llamadas JNI en el paquete `com.example.nativebridge`.
   - Proporciona implementaciones de fallback en Kotlin cuando las librerías compartidas `.so` no se hayan compilado físicamente en el entorno local.

4. **Herramientas Debug**:
   - Encapsula todas las herramientas de diagnóstico en `com.example.debug`.
   - Asegúrate de que los botones de acceso UI y componentes de diagnóstico estén condicionados a `if (BuildConfig.DEBUG)` para no exponerlos en builds de producción.

---

## 📋 Checklist antes de concluir cualquier turno

- [ ] ¿El código escrito respeta la arquitectura Kotlin + C++ Oboe + Rust?
- [ ] ¿Se ejecutó `compile_applet` comprobando que el build fue exitoso?
- [ ] ¿Se actualizaron las descripciones o documentación relevante si cambió la funcionalidad?
