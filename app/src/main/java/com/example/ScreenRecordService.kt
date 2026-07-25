package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.nativebridge.NativeObsCore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    companion object {
        const val ACTION_START = "com.example.ACTION_START_RECORDING"
        const val ACTION_STOP = "com.example.ACTION_STOP_RECORDING"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME = "com.example.ACTION_RESUME_RECORDING"
        const val ACTION_TOGGLE_MUTE = "com.example.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_FACECAM = "com.example.ACTION_TOGGLE_FACECAM"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_RECORD_AUDIO = "extra_record_audio"
        const val EXTRA_RECORD_INTERNAL_AUDIO = "extra_record_internal_audio"
        const val EXTRA_ENABLE_CAMERA_PIP = "extra_enable_camera_pip"
        const val EXTRA_QUALITY_WIDTH = "extra_quality_width"
        const val EXTRA_QUALITY_HEIGHT = "extra_quality_height"
        const val EXTRA_QUALITY_BITRATE = "extra_quality_bitrate"
        const val EXTRA_QUALITY_FPS = "extra_quality_fps"
        const val EXTRA_ENABLE_NOISE_GATE = "extra_enable_noise_gate"

        private const val CHANNEL_ID = "screen_record_channel_id"
        private const val NOTIFICATION_ID = 8801
        private const val TAG = "ScreenRecordService"
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecordInternal: AudioRecord? = null
    private val nativeObsCore = NativeObsCore()

    private var timerJob: Job? = null
    private var durationSeconds = 0L
    private var currentOutputFile: File? = null
    private var floatingOverlay: FloatingBubbleOverlay? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                val recordAudio = intent.getBooleanExtra(EXTRA_RECORD_AUDIO, true)
                val recordInternalAudio = intent.getBooleanExtra(EXTRA_RECORD_INTERNAL_AUDIO, true)
                val enableCameraPip = intent.getBooleanExtra(EXTRA_ENABLE_CAMERA_PIP, false)
                val enableNoiseGate = intent.getBooleanExtra(EXTRA_ENABLE_NOISE_GATE, true)
                val reqWidth = intent.getIntExtra(EXTRA_QUALITY_WIDTH, 1080)
                val reqHeight = intent.getIntExtra(EXTRA_QUALITY_HEIGHT, 1920)
                val bitrate = intent.getIntExtra(EXTRA_QUALITY_BITRATE, 8_000_000)
                val fps = intent.getIntExtra(EXTRA_QUALITY_FPS, 30)

                if (resultCode != 0 && resultData != null) {
                    startRecordingService(resultCode, resultData, recordAudio, recordInternalAudio, enableCameraPip, enableNoiseGate, reqWidth, reqHeight, bitrate, fps)
                } else {
                    ScreenRecordRepository.setError("Error: Permiso de captura denegado")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                com.example.debug.DebugLogger.i("ScreenRecordService", "Solicitud de detención de servicio recibida.")
                stopRecordingService()
            }
            ACTION_PAUSE -> {
                com.example.debug.DebugLogger.i("ScreenRecordService", "Solicitud de pausa de grabación recibida.")
                pauseRecording()
            }
            ACTION_RESUME -> {
                com.example.debug.DebugLogger.i("ScreenRecordService", "Solicitud de reanudación de grabación recibida.")
                resumeRecording()
            }
            ACTION_TOGGLE_MUTE -> {
                com.example.debug.DebugLogger.i("ScreenRecordService", "Conmutación de micrófono recibida.")
                toggleMuteRecording()
            }
            ACTION_TOGGLE_FACECAM -> {
                com.example.debug.DebugLogger.i("ScreenRecordService", "Conmutación de Facecam PiP recibida.")
                toggleFacecamRecording()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRecordingService(
        resultCode: Int,
        resultData: Intent,
        recordAudio: Boolean,
        recordInternalAudio: Boolean,
        enableCameraPip: Boolean,
        enableNoiseGate: Boolean,
        reqWidth: Int,
        reqHeight: Int,
        bitrate: Int,
        fps: Int
    ) {
        val notification = buildNotification("Iniciando grabación...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && (recordAudio || recordInternalAudio)) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, fgType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            // Get screen metrics
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)

            // Normalize dimensions keeping aspect ratio if needed
            val screenWidth = if (metrics.widthPixels > 0) metrics.widthPixels else reqWidth
            val screenHeight = if (metrics.heightPixels > 0) metrics.heightPixels else reqHeight
            val densityDpi = metrics.densityDpi

            // Prepare output file
            val moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
            if (!moviesDir.exists()) moviesDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            currentOutputFile = File(moviesDir, "Grabacion_$timestamp.mp4")

            // Create MediaRecorder
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
            mediaRecorder = recorder

            val shouldRecordAnyAudio = recordAudio || recordInternalAudio

            if (shouldRecordAnyAudio) {
                val audioSource = if (recordInternalAudio && !recordAudio) {
                    MediaRecorder.AudioSource.VOICE_RECOGNITION
                } else {
                    MediaRecorder.AudioSource.MIC
                }
                recorder.setAudioSource(audioSource)
            }
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(currentOutputFile!!.absolutePath)
            recorder.setVideoSize(screenWidth, screenHeight)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (shouldRecordAnyAudio) {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128_000)
                recorder.setAudioSamplingRate(44100)
            }
            recorder.setVideoEncodingBitRate(bitrate)
            recorder.setVideoFrameRate(fps)
            recorder.prepare()

            // Setup MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                ScreenRecordRepository.setError("No se pudo iniciar la proyección de pantalla")
                stopSelf()
                return
            }

            // Iniciar captura de audio interno (Android 10+) si está habilitado
            if (recordInternalAudio && mediaProjection != null) {
                setupInternalAudioCapture(mediaProjection!!)
            }

            // MediaProjection Callback for Android 14+
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection detenido por el sistema")
                    stopRecordingService()
                }
            }, null)

            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecordVD",
                screenWidth,
                screenHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            // Initialize Mobile OBS Native Engine (C++ Oboe + Rust Pipeline)
            nativeObsCore.initializeEngine(
                width = screenWidth,
                height = screenHeight,
                fps = fps,
                bitrate = bitrate,
                recordMic = recordAudio,
                recordInternalAudio = recordInternalAudio,
                enableCameraPip = enableCameraPip,
                enableNoiseGate = enableNoiseGate
            )

            val currentState = ScreenRecordRepository.state.value
            nativeObsCore.configureAudioDSPFilters(
                enableNoiseGate = currentState.audioFilters.enableNoiseGate,
                noiseGateThresholdDb = currentState.audioFilters.noiseGateThresholdDb,
                enableCompressor = currentState.audioFilters.enableCompressor,
                compressorThresholdDb = currentState.audioFilters.compressorThresholdDb,
                compressorRatio = currentState.audioFilters.compressorRatio,
                enableGainBooster = currentState.audioFilters.enableGainBooster,
                gainBoostDb = currentState.audioFilters.gainBoostDb,
                enableEqualizer = currentState.audioFilters.enableEqualizer,
                eqLowGain = currentState.audioFilters.eqLowGain,
                eqMidGain = currentState.audioFilters.eqMidGain,
                eqHighGain = currentState.audioFilters.eqHighGain
            )
            nativeObsCore.switchActiveScene(currentState.selectedSceneId)

            recorder.start()
            ScreenRecordRepository.setRecording(true)
            ScreenRecordRepository.setError(null)
            ScreenRecordRepository.updateState { it.copy(lastRecordedFilePath = currentOutputFile?.absolutePath) }

            if (currentState.enableFloatingBubble) {
                floatingOverlay = FloatingBubbleOverlay(applicationContext)
                floatingOverlay?.show()
            }

            startTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Error preparando o iniciando la grabación", e)
            ScreenRecordRepository.setError("Error al iniciar la grabación: ${e.localizedMessage}")
            cleanUpRecorder()
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                ScreenRecordRepository.setPaused(true)
                updateNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Error al pausar la grabación", e)
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                ScreenRecordRepository.setPaused(false)
                updateNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Error al reanudar la grabación", e)
            }
        }
    }

    private fun toggleMuteRecording() {
        val newMuteState = !ScreenRecordRepository.state.value.isMuted
        ScreenRecordRepository.setMuted(newMuteState)
        updateNotification()
    }

    private fun toggleFacecamRecording() {
        val newPipState = !ScreenRecordRepository.state.value.enableCameraPip
        ScreenRecordRepository.setEnableCameraPip(newPipState)
    }

    private fun startTimer() {
        durationSeconds = 0L
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000)
                if (!ScreenRecordRepository.state.value.isPaused) {
                    durationSeconds++
                    ScreenRecordRepository.setDuration(durationSeconds)
                    updateNotification()
                }
            }
        }
    }

    private fun stopRecordingService() {
        timerJob?.cancel()
        timerJob = null

        floatingOverlay?.dismiss()
        floatingOverlay = null

        nativeObsCore.shutdownEngine()
        cleanUpRecorder()

        ScreenRecordRepository.setRecording(false)
        ScreenRecordRepository.loadVideos(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun setupInternalAudioCapture(projection: MediaProjection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val sampleRate = 48000
                val minBufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecordInternal = AudioRecord.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                            .build()
                    )
                    .setAudioPlaybackCaptureConfig(config)
                    .setBufferSizeInBytes(if (minBufferSize > 0) minBufferSize * 2 else 4096)
                    .build()

                audioRecordInternal?.startRecording()
                Log.i(TAG, "AudioPlaybackCaptureConfiguration iniciado con éxito para capturar audio de juegos/apps")
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo iniciar la captura de audio interno (AudioPlaybackCapture)", e)
            }
        }
    }

    private fun cleanUpRecorder() {
        try {
            audioRecordInternal?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "audioRecordInternal.stop() exception", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up internal audio capture", e)
        } finally {
            audioRecordInternal = null
        }

        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "mediaRecorder.stop() exception", e)
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up recorder", e)
        } finally {
            mediaRecorder = null
        }

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabar Pantalla",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación de servicio de grabación de pantalla en curso"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 Grabando Pantalla")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        val timerStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        val notification = buildNotification("Tiempo transcurrido: $timerStr")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        floatingOverlay?.dismiss()
        floatingOverlay = null
        cleanUpRecorder()
        super.onDestroy()
    }
}
