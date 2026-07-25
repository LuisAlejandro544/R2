package com.example

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object ScreenRecordRepository {

    private val _state = MutableStateFlow(ScreenRecordState())
    val state: StateFlow<ScreenRecordState> = _state.asStateFlow()

    private val _videos = MutableStateFlow<List<RecordedVideo>>(emptyList())
    val videos: StateFlow<List<RecordedVideo>> = _videos.asStateFlow()

    fun updateState(transform: (ScreenRecordState) -> ScreenRecordState) {
        _state.value = transform(_state.value)
    }

    fun setRecording(isRecording: Boolean) {
        _state.value = _state.value.copy(
            isRecording = isRecording,
            isPaused = if (!isRecording) false else _state.value.isPaused,
            recordingDurationSeconds = if (isRecording) 0L else _state.value.recordingDurationSeconds
        )
    }

    fun setPaused(isPaused: Boolean) {
        _state.value = _state.value.copy(isPaused = isPaused)
    }

    fun setMuted(isMuted: Boolean) {
        _state.value = _state.value.copy(isMuted = isMuted)
    }

    fun setEnableFloatingBubble(enabled: Boolean) {
        _state.value = _state.value.copy(enableFloatingBubble = enabled)
    }

    fun setDuration(seconds: Long) {
        _state.value = _state.value.copy(recordingDurationSeconds = seconds)
    }

    fun setAudioMode(mode: AudioMode) {
        _state.value = _state.value.copy(audioMode = mode)
    }

    fun setEnableCameraPip(enabled: Boolean) {
        _state.value = _state.value.copy(enableCameraPip = enabled)
    }

    fun setEnableNoiseGate(enabled: Boolean) {
        _state.value = _state.value.copy(enableNoiseGate = enabled)
    }

    fun setQualityPreset(preset: QualityPreset) {
        _state.value = _state.value.copy(qualityPreset = preset)
    }

    fun setFpsOption(fpsOption: FpsOption) {
        _state.value = _state.value.copy(selectedFps = fpsOption)
    }

    fun setBitrateOption(bitrateOption: BitrateOption) {
        _state.value = _state.value.copy(selectedBitrate = bitrateOption)
    }

    fun setSelectedScene(sceneId: String) {
        _state.value = _state.value.copy(selectedSceneId = sceneId)
    }

    fun toggleSourceVisibility(sceneId: String, sourceId: String) {
        val updatedScenes = _state.value.scenes.map { scene ->
            if (scene.id == sceneId) {
                val updatedSources = scene.sources.map { source ->
                    if (source.id == sourceId) {
                        source.copy(isVisible = !source.isVisible)
                    } else source
                }
                scene.copy(sources = updatedSources)
            } else scene
        }
        _state.value = _state.value.copy(scenes = updatedScenes)
    }

    fun updateAudioFilters(update: (AudioFilterSettings) -> AudioFilterSettings) {
        _state.value = _state.value.copy(audioFilters = update(_state.value.audioFilters))
    }

    fun setError(errorMsg: String?) {
        _state.value = _state.value.copy(error = errorMsg)
    }

    fun loadVideos(context: Context) {
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val files = moviesDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("mp4", "mkv", "webm")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        val videoList = files.map { file ->
            var durationMs = 0L
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = timeStr?.toLongOrNull() ?: 0L
                retriever.release()
            } catch (_: Exception) {}

            RecordedVideo(
                id = file.name,
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                durationMs = durationMs,
                timestamp = file.lastModified()
            )
        }
        _videos.value = videoList
    }

    fun deleteVideo(video: RecordedVideo, context: Context) {
        val file = File(video.path)
        if (file.exists()) {
            file.delete()
        }
        loadVideos(context)
    }
}
