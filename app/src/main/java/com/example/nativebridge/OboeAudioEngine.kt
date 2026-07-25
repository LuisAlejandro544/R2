package com.example.nativebridge

import android.util.Log

/**
 * Interface Native JNI con el motor de Audio en C++ basado en AAudio / Oboe.
 * Proporciona procesamiento de audio de ultra baja latencia y mezcla de canales
 * (micrófono + audio interno del sistema).
 */
class OboeAudioEngine {

    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("oboe_audio_engine")
            isNativeLoaded = true
            Log.i("OboeAudioEngine", "Librería nativa C++ Oboe cargada con éxito")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("OboeAudioEngine", "Librería C++ oboe_audio_engine no cargada. Usando fallback de Kotlin/AudioRecord: ${e.message}")
            isNativeLoaded = false
        }
    }

    fun isNativeEngineAvailable(): Boolean = isNativeLoaded

    fun startAudioEngine(sampleRate: Int = 48000, channelCount: Int = 2, recordMic: Boolean = true): Boolean {
        return if (isNativeLoaded) {
            nativeStartAudioEngine(sampleRate, channelCount, recordMic)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Motor de audio Oboe iniciado en modo simulado (Sample rate: $sampleRate, Canales: $channelCount)")
            true
        }
    }

    fun stopAudioEngine() {
        if (isNativeLoaded) {
            nativeStopAudioEngine()
        } else {
            Log.i("OboeAudioEngine", "Fallback: Motor de audio Oboe detenido")
        }
    }

    fun setMicGain(gainDb: Float) {
        if (isNativeLoaded) {
            nativeSetMicGain(gainDb)
        }
    }

    fun configureDualAudioMixing(enableDualAudio: Boolean, internalAudioVol: Float = 1.0f, micVol: Float = 1.0f) {
        if (isNativeLoaded) {
            nativeSetDualAudioMixing(enableDualAudio, internalAudioVol, micVol)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Mezcla Dual Audio Oboe (Juego + Mic) = $enableDualAudio [Juego: $internalAudioVol, Mic: $micVol]")
        }
    }

    fun configureNoiseGate(enableNoiseGate: Boolean, thresholdDb: Float = -35.0f) {
        if (isNativeLoaded) {
            nativeSetNoiseGate(enableNoiseGate, thresholdDb)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Supresión de Ruido C++ / Noise Gate = $enableNoiseGate [Umbral: $thresholdDb dB]")
        }
    }

    fun configureCompressor(enableCompressor: Boolean, thresholdDb: Float = -18.0f, ratio: Float = 4.0f) {
        if (isNativeLoaded) {
            nativeSetCompressor(enableCompressor, thresholdDb, ratio)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Compresor Dinámico C++ = $enableCompressor [Umbral: $thresholdDb dB, Ratio: ${ratio}:1]")
        }
    }

    fun configureGainBooster(enableGainBooster: Boolean, boostDb: Float = 3.0f) {
        if (isNativeLoaded) {
            nativeSetGainBooster(enableGainBooster, boostDb)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Amplicador de Ganancia (Gain Booster) C++ = $enableGainBooster [+$boostDb dB]")
        }
    }

    fun configureEqualizer(enableEqualizer: Boolean, lowGain: Float = 2.0f, midGain: Float = 0.0f, highGain: Float = 1.5f) {
        if (isNativeLoaded) {
            nativeSetEqualizer(enableEqualizer, lowGain, midGain, highGain)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Ecualizador de 3 Bandas C++ = $enableEqualizer [Graves: $lowGain dB, Medios: $midGain dB, Agudos: $highGain dB]")
        }
    }

    fun configureAudioDucking(enableDucking: Boolean, thresholdDb: Float = -28.0f, attenuationDb: Float = -10.0f) {
        if (isNativeLoaded) {
            nativeSetAudioDucking(enableDucking, thresholdDb, attenuationDb)
        } else {
            Log.i("OboeAudioEngine", "Fallback: Audio Ducking C++ (Atenuación automática del juego al hablar) = $enableDucking [Umbral Voz: $thresholdDb dB, Atenuación Juego: $attenuationDb dB]")
        }
    }

    // Métodos nativos JNI C++ (oboe_audio_engine.cpp)
    private external fun nativeStartAudioEngine(sampleRate: Int, channelCount: Int, recordMic: Boolean): Boolean
    private external fun nativeStopAudioEngine()
    private external fun nativeSetMicGain(gainDb: Float)
    private external fun nativeSetDualAudioMixing(enableDualAudio: Boolean, internalAudioVol: Float, micVol: Float)
    private external fun nativeSetNoiseGate(enableNoiseGate: Boolean, thresholdDb: Float)
    private external fun nativeSetCompressor(enableCompressor: Boolean, thresholdDb: Float, ratio: Float)
    private external fun nativeSetGainBooster(enableGainBooster: Boolean, boostDb: Float)
    private external fun nativeSetEqualizer(enableEqualizer: Boolean, lowGain: Float, midGain: Float, highGain: Float)
    private external fun nativeSetAudioDucking(enableDucking: Boolean, thresholdDb: Float, attenuationDb: Float)
}
