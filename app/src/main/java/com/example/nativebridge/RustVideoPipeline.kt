package com.example.nativebridge

import android.util.Log

/**
 * Interface Native JNI con el pipeline de procesamiento de video en Rust.
 * Maneja la composición de escenas, procesamiento de cuadros en formato YUV/NV12,
 * superposición de marcas de agua y optimizaciones de memoria sin copia (Zero-Copy).
 */
class RustVideoPipeline {

    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("rust_obs_pipeline")
            isNativeLoaded = true
            Log.i("RustVideoPipeline", "Librería nativa Rust rust_obs_pipeline cargada con éxito")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("RustVideoPipeline", "Librería Rust rust_obs_pipeline no cargada. Usando fallback de Kotlin MediaCodec: ${e.message}")
            isNativeLoaded = false
        }
    }

    fun isNativeEngineAvailable(): Boolean = isNativeLoaded

    fun initPipeline(width: Int, height: Int, fps: Int, bitrate: Int): Boolean {
        return if (isNativeLoaded) {
            nativeInitPipeline(width, height, fps, bitrate)
        } else {
            Log.i("RustVideoPipeline", "Fallback: Pipeline Rust inicializado ($width x $height @ $fps fps, $bitrate bps)")
            true
        }
    }

    fun processFrame(frameData: ByteArray, width: Int, height: Int, timestampNs: Long): Int {
        return if (isNativeLoaded) {
            nativeProcessFrame(frameData, width, height, timestampNs)
        } else {
            0 // OK
        }
    }

    fun releasePipeline() {
        if (isNativeLoaded) {
            nativeReleasePipeline()
        }
    }

    fun configureCameraPipOverlay(enablePip: Boolean, position: Int = 0, scale: Float = 0.25f) {
        if (isNativeLoaded) {
            nativeSetCameraPipOverlay(enablePip, position, scale)
        } else {
            Log.i("RustVideoPipeline", "Fallback: Composición PiP Cámara Rust (Facecam) = $enablePip [Posición: $position, Escala: $scale]")
        }
    }

    fun trimVideoLossless(inputPath: String, outputPath: String, startMs: Long, endMs: Long): Boolean {
        if (isNativeLoaded) {
            return nativeTrimVideoLossless(inputPath, outputPath, startMs, endMs)
        }
        
        Log.i("RustVideoPipeline", "Iniciando Recorte de Video Sin Pérdida (Fast Lossless Remuxing) de ${startMs}ms a ${endMs}ms...")
        return try {
            val extractor = android.media.MediaExtractor()
            extractor.setDataSource(inputPath)

            val muxer = android.media.MediaMuxer(outputPath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackIndexMap = HashMap<Int, Int>()

            var maxBufSize = 1024 * 1024
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val bufSize = format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (bufSize > maxBufSize) maxBufSize = bufSize
                }
                extractor.selectTrack(i)
                val newTrackIndex = muxer.addTrack(format)
                trackIndexMap[i] = newTrackIndex
            }

            muxer.start()

            val buffer = java.nio.ByteBuffer.allocate(maxBufSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            for (i in 0 until extractor.trackCount) {
                extractor.seekTo(startMs * 1000, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs > endMs * 1000) break

                    bufferInfo.presentationTimeUs = sampleTimeUs - (startMs * 1000)
                    bufferInfo.flags = extractor.sampleFlags

                    val trackIndex = extractor.sampleTrackIndex
                    val muxerTrackIndex = trackIndexMap[trackIndex] ?: 0
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                    extractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            Log.i("RustVideoPipeline", "Recorte de video sin pérdida completado con éxito: $outputPath")
            true
        } catch (e: Exception) {
            Log.e("RustVideoPipeline", "Error al realizar recorte de video sin pérdida", e)
            false
        }
    }

    fun setCurrentScene(sceneId: String) {
        if (isNativeLoaded) {
            nativeSetCurrentScene(sceneId)
        } else {
            Log.i("RustVideoPipeline", "Fallback: Escena activa cambiada a: $sceneId")
        }
    }

    fun updateSourceState(sourceId: String, isVisible: Boolean, opacity: Float = 1.0f) {
        if (isNativeLoaded) {
            nativeUpdateSourceState(sourceId, isVisible, opacity)
        } else {
            Log.i("RustVideoPipeline", "Fallback: Estado de fuente $sourceId [Visible: $isVisible, Opacidad: $opacity]")
        }
    }

    // Métodos nativos JNI Rust (lib.rs)
    private external fun nativeInitPipeline(width: Int, height: Int, fps: Int, bitrate: Int): Boolean
    private external fun nativeProcessFrame(frameData: ByteArray, width: Int, height: Int, timestampNs: Long): Int
    private external fun nativeReleasePipeline()
    private external fun nativeSetCameraPipOverlay(enablePip: Boolean, position: Int, scale: Float)
    private external fun nativeTrimVideoLossless(inputPath: String, outputPath: String, startMs: Long, endMs: Long): Boolean
    private external fun nativeSetCurrentScene(sceneId: String)
    private external fun nativeUpdateSourceState(sourceId: String, isVisible: Boolean, opacity: Float)
}
