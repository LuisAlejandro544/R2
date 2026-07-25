package com.example

enum class ObsSourceType(val label: String) {
    GAME_CAPTURE("Captura de Juego/Pantalla"),
    FACECAM_PIP("Cámara Frontal Facecam (PiP)"),
    IMAGE_OVERLAY("Marca de Agua / Logo"),
    TEXT_WATERMARK("Texto / Banner en Vivo")
}

data class ObsSource(
    val id: String,
    val name: String,
    val type: ObsSourceType,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val position: String = "BOTTOM_RIGHT", // "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "CENTER"
    val scale: Float = 0.25f,
    val zIndex: Int = 1
)

data class ObsScene(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val sources: List<ObsSource>
)

data class AudioFilterSettings(
    val enableNoiseGate: Boolean = true,
    val noiseGateThresholdDb: Float = -35.0f,
    val enableCompressor: Boolean = true,
    val compressorThresholdDb: Float = -18.0f,
    val compressorRatio: Float = 4.0f,
    val enableGainBooster: Boolean = false,
    val gainBoostDb: Float = 3.0f, // 0dB a +12dB
    val enableEqualizer: Boolean = true,
    val eqLowGain: Float = 2.0f,  // Graves dB
    val eqMidGain: Float = 0.0f,  // Medios dB
    val eqHighGain: Float = 1.5f  // Agudos dB
)

object ObsScenePresets {
    fun getDefaultScenes(): List<ObsScene> {
        return listOf(
            ObsScene(
                id = "scene_gaming",
                name = "Gaming + Facecam",
                description = "Pantalla completa de juego con cámara PiP y marca de agua",
                iconName = "Gamepad",
                sources = listOf(
                    ObsSource("src_game", "Pantalla de Juego", ObsSourceType.GAME_CAPTURE, isVisible = true, zIndex = 0),
                    ObsSource("src_pip", "Cámara Facecam", ObsSourceType.FACECAM_PIP, isVisible = true, position = "BOTTOM_RIGHT", scale = 0.25f, zIndex = 1),
                    ObsSource("src_logo", "Logo Mobile OBS", ObsSourceType.IMAGE_OVERLAY, isVisible = true, position = "TOP_LEFT", scale = 0.15f, zIndex = 2)
                )
            ),
            ObsScene(
                id = "scene_fullscreen",
                name = "Solo Pantalla Full",
                description = "Captura limpia sin distracciones ni cámara",
                iconName = "ScreenShare",
                sources = listOf(
                    ObsSource("src_game", "Pantalla de Juego", ObsSourceType.GAME_CAPTURE, isVisible = true, zIndex = 0),
                    ObsSource("src_pip", "Cámara Facecam", ObsSourceType.FACECAM_PIP, isVisible = false, position = "BOTTOM_RIGHT", scale = 0.25f, zIndex = 1),
                    ObsSource("src_logo", "Logo Mobile OBS", ObsSourceType.IMAGE_OVERLAY, isVisible = false, position = "TOP_LEFT", scale = 0.15f, zIndex = 2)
                )
            ),
            ObsScene(
                id = "scene_brb",
                name = "Pausa / Regresamos En Breve",
                description = "Cámara centrada con banner de aviso de pausa",
                iconName = "HourglassEmpty",
                sources = listOf(
                    ObsSource("src_pip", "Cámara Facecam (Centro)", ObsSourceType.FACECAM_PIP, isVisible = true, position = "CENTER", scale = 0.50f, zIndex = 1),
                    ObsSource("src_text", "Banner '¡Volvemos Pronto!'", ObsSourceType.TEXT_WATERMARK, isVisible = true, position = "TOP_RIGHT", scale = 0.30f, zIndex = 2)
                )
            ),
            ObsScene(
                id = "scene_irl",
                name = "IRL / Chatting Focus",
                description = "Cámara principal destacada con audio filtrado",
                iconName = "Person",
                sources = listOf(
                    ObsSource("src_pip", "Cámara Facecam Full", ObsSourceType.FACECAM_PIP, isVisible = true, position = "CENTER", scale = 1.0f, zIndex = 1),
                    ObsSource("src_text", "Overlay Nombre de Canal", ObsSourceType.TEXT_WATERMARK, isVisible = true, position = "BOTTOM_LEFT", scale = 0.20f, zIndex = 2)
                )
            )
        )
    }
}
