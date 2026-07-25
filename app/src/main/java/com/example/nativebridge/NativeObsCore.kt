package com.example.nativebridge

import android.util.Log

/**
 * Orquestador principal de Mobile OBS.
 * Une la captura de pantalla de Android (MediaProjection) con el motor de audio Oboe (C++)
 * y el pipeline de composición y cuadros de Rust.
 */
class NativeObsCore {

    val oboeEngine = OboeAudioEngine()
    val rustPipeline = RustVideoPipeline()

    fun initializeEngine(
        width: Int,
        height: Int,
        fps: Int = 30,
        bitrate: Int = 7_000_000,
        recordMic: Boolean = true,
        recordInternalAudio: Boolean = true,
        enableCameraPip: Boolean = false,
        enableNoiseGate: Boolean = true
    ): Boolean {
        Log.i("NativeObsCore", "Inicializando Mobile OBS Core Engine...")
        val audioOk = oboeEngine.startAudioEngine(sampleRate = 48000, channelCount = 2, recordMic = recordMic)
        oboeEngine.configureDualAudioMixing(enableDualAudio = recordInternalAudio && recordMic)
        oboeEngine.configureNoiseGate(enableNoiseGate = enableNoiseGate)

        val videoOk = rustPipeline.initPipeline(width = width, height = height, fps = fps, bitrate = bitrate)
        rustPipeline.configureCameraPipOverlay(enablePip = enableCameraPip, position = 0, scale = 0.25f)

        Log.i("NativeObsCore", "Estado de inicialización: Audio Oboe (Mezcla Dual: $recordInternalAudio, Noise Gate: $enableNoiseGate) = $audioOk, Rust Pipeline (Camera PiP: $enableCameraPip) = $videoOk")
        return audioOk && videoOk
    }

    fun configureAudioDSPFilters(
        enableNoiseGate: Boolean = true,
        noiseGateThresholdDb: Float = -35.0f,
        enableCompressor: Boolean = true,
        compressorThresholdDb: Float = -18.0f,
        compressorRatio: Float = 4.0f,
        enableGainBooster: Boolean = false,
        gainBoostDb: Float = 3.0f,
        enableEqualizer: Boolean = true,
        eqLowGain: Float = 2.0f,
        eqMidGain: Float = 0.0f,
        eqHighGain: Float = 1.5f,
        enableAudioDucking: Boolean = true,
        duckingThresholdDb: Float = -28.0f,
        duckingAttenuationDb: Float = -10.0f
    ) {
        oboeEngine.configureNoiseGate(enableNoiseGate, noiseGateThresholdDb)
        oboeEngine.configureCompressor(enableCompressor, compressorThresholdDb, compressorRatio)
        oboeEngine.configureGainBooster(enableGainBooster, gainBoostDb)
        oboeEngine.configureEqualizer(enableEqualizer, eqLowGain, eqMidGain, eqHighGain)
        oboeEngine.configureAudioDucking(enableAudioDucking, duckingThresholdDb, duckingAttenuationDb)
    }

    fun switchActiveScene(sceneId: String) {
        Log.i("NativeObsCore", "Cambiando escena activa a $sceneId")
        rustPipeline.setCurrentScene(sceneId)
    }

    fun toggleSourceVisibility(sourceId: String, isVisible: Boolean, opacity: Float = 1.0f) {
        rustPipeline.updateSourceState(sourceId, isVisible, opacity)
    }

    fun trimVideoLossless(inputPath: String, outputPath: String, startMs: Long, endMs: Long): Boolean {
        return rustPipeline.trimVideoLossless(inputPath, outputPath, startMs, endMs)
    }

    fun shutdownEngine() {
        Log.i("NativeObsCore", "Deteniendo Mobile OBS Core Engine...")
        oboeEngine.stopAudioEngine()
        rustPipeline.releasePipeline()
    }

    fun getArchitectureInfo(): String {
        val oboeStatus = if (oboeEngine.isNativeEngineAvailable()) "C++ AAudio/Oboe (Nativo)" else "Kotlin Fallback"
        val rustStatus = if (rustPipeline.isNativeEngineAvailable()) "Rust Pipeline (Nativo)" else "Kotlin Fallback"
        return "Mobile OBS Architecture:\n - Audio Engine: $oboeStatus\n - Video Processing: $rustStatus"
    }
}
