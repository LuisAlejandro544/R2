package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class ScreenRecordViewModel : ViewModel() {

    val state: StateFlow<ScreenRecordState> = ScreenRecordRepository.state
    val videos: StateFlow<List<RecordedVideo>> = ScreenRecordRepository.videos

    fun init(context: Context) {
        ScreenRecordRepository.loadVideos(context)
    }

    fun selectScene(sceneId: String) {
        ScreenRecordRepository.setSelectedScene(sceneId)
    }

    fun toggleSourceVisibility(sceneId: String, sourceId: String) {
        ScreenRecordRepository.toggleSourceVisibility(sceneId, sourceId)
    }

    fun updateAudioFilters(update: (AudioFilterSettings) -> AudioFilterSettings) {
        ScreenRecordRepository.updateAudioFilters(update)
    }

    fun selectAudioMode(mode: AudioMode) {
        ScreenRecordRepository.setAudioMode(mode)
    }

    fun toggleEnableCameraPip(enabled: Boolean) {
        ScreenRecordRepository.setEnableCameraPip(enabled)
    }

    fun toggleEnableNoiseGate(enabled: Boolean) {
        ScreenRecordRepository.setEnableNoiseGate(enabled)
    }

    fun selectQualityPreset(preset: QualityPreset) {
        ScreenRecordRepository.setQualityPreset(preset)
    }

    fun selectFpsOption(fpsOption: FpsOption) {
        ScreenRecordRepository.setFpsOption(fpsOption)
    }

    fun selectBitrateOption(bitrateOption: BitrateOption) {
        ScreenRecordRepository.setBitrateOption(bitrateOption)
    }

    fun clearError() {
        ScreenRecordRepository.setError(null)
    }

    fun startServiceWithResult(context: Context, resultCode: Int, data: Intent) {
        val currentState = state.value
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecordService.EXTRA_RECORD_AUDIO, currentState.recordAudio)
            putExtra(ScreenRecordService.EXTRA_RECORD_INTERNAL_AUDIO, currentState.recordInternalAudio)
            putExtra(ScreenRecordService.EXTRA_ENABLE_CAMERA_PIP, currentState.enableCameraPip)
            putExtra(ScreenRecordService.EXTRA_ENABLE_NOISE_GATE, currentState.enableNoiseGate)
            putExtra(ScreenRecordService.EXTRA_QUALITY_WIDTH, currentState.qualityPreset.width)
            putExtra(ScreenRecordService.EXTRA_QUALITY_HEIGHT, currentState.qualityPreset.height)
            putExtra(ScreenRecordService.EXTRA_QUALITY_BITRATE, currentState.selectedBitrate.bitrate)
            putExtra(ScreenRecordService.EXTRA_QUALITY_FPS, currentState.selectedFps.fps)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun toggleFloatingBubble(enabled: Boolean) {
        ScreenRecordRepository.setEnableFloatingBubble(enabled)
    }

    fun pauseRecording(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeRecording(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun toggleMuteRecording(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_TOGGLE_MUTE
        }
        context.startService(intent)
    }

    fun stopRecording(context: Context) {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun shareVideo(context: Context, video: RecordedVideo) {
        val file = File(video.path)
        if (!file.exists()) return

        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir grabación"))
    }

    fun playVideoExternal(context: Context, video: RecordedVideo) {
        val file = File(video.path)
        if (!file.exists()) return

        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            ScreenRecordRepository.setError("No se encontró ningún reproductor de video en el dispositivo")
        }
    }

    fun deleteVideo(context: Context, video: RecordedVideo) {
        viewModelScope.launch {
            ScreenRecordRepository.deleteVideo(video, context)
        }
    }

    fun trimVideoLossless(context: Context, video: RecordedVideo, startMs: Long, endMs: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = File(video.path)
            if (!file.exists()) {
                onComplete(false)
                return@launch
            }
            val parentDir = file.parentFile ?: context.filesDir
            val trimmedFileName = "TRIMMED_${System.currentTimeMillis()}_${file.name}"
            val outputFile = File(parentDir, trimmedFileName)

            val nativeObsCore = com.example.nativebridge.NativeObsCore()
            val success = nativeObsCore.trimVideoLossless(
                inputPath = file.absolutePath,
                outputPath = outputFile.absolutePath,
                startMs = startMs,
                endMs = endMs
            )

            if (success) {
                ScreenRecordRepository.loadVideos(context)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun refreshVideos(context: Context) {
        ScreenRecordRepository.loadVideos(context)
    }
}
