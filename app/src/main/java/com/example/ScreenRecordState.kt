package com.example

enum class QualityPreset(val label: String, val width: Int, val height: Int, val bitrate: Int) {
    SD_720P("720p HD", 720, 1280, 3_500_000),
    FHD_1080P("1080p Full HD", 1080, 1920, 7_000_000)
}

enum class FpsOption(val fps: Int, val label: String) {
    FPS_15(15, "15 FPS"),
    FPS_24(24, "24 FPS"),
    FPS_30(30, "30 FPS"),
    FPS_48(48, "48 FPS"),
    FPS_60(60, "60 FPS")
}

enum class BitrateOption(val bitrate: Int, val label: String) {
    BITRATE_2M(2_000_000, "2 Mbps"),
    BITRATE_4M(4_000_000, "4 Mbps"),
    BITRATE_8M(8_000_000, "8 Mbps"),
    BITRATE_12M(12_000_000, "12 Mbps"),
    BITRATE_16M(16_000_000, "16 Mbps"),
    BITRATE_24M(24_000_000, "24 Mbps")
}

enum class AudioMode(
    val title: String,
    val subtitle: String,
    val recordMic: Boolean,
    val recordInternal: Boolean
) {
    DUAL("Juego + Mic", "Audio del juego y tu voz", true, true),
    GAME_ONLY("Solo Juego", "Audio interno del juego o app", false, true),
    MIC_ONLY("Solo Micrófono", "Únicamente voz por micrófono", true, false),
    MUTE("Silencio", "Sin ninguna pista de audio", false, false)
}

data class ScreenRecordState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val enableFloatingBubble: Boolean = true,
    val recordingDurationSeconds: Long = 0L,
    val audioMode: AudioMode = AudioMode.DUAL,
    val enableCameraPip: Boolean = false,
    val enableNoiseGate: Boolean = true,
    val scenes: List<ObsScene> = ObsScenePresets.getDefaultScenes(),
    val selectedSceneId: String = "scene_gaming",
    val audioFilters: AudioFilterSettings = AudioFilterSettings(),
    val qualityPreset: QualityPreset = QualityPreset.FHD_1080P,
    val selectedFps: FpsOption = FpsOption.FPS_30,
    val selectedBitrate: BitrateOption = BitrateOption.BITRATE_8M,
    val lastRecordedFilePath: String? = null,
    val error: String? = null
) {
    val recordAudio: Boolean get() = audioMode.recordMic
    val recordInternalAudio: Boolean get() = audioMode.recordInternal
    val currentScene: ObsScene? get() = scenes.find { it.id == selectedSceneId }
}
