package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecordScreen(
    viewModel: ScreenRecordViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val videos by viewModel.videos.collectAsState()

    var selectedVideoForPlayback by remember { mutableStateOf<RecordedVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<RecordedVideo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    // MediaProjection screen capture intent launcher
    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.startServiceWithResult(context, result.resultCode, result.data!!)
        } else {
            ScreenRecordRepository.setError("Se canceló el permiso para capturar la pantalla.")
        }
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchScreenCapture(context) { intent -> projectionLauncher.launch(intent) }
        } else {
            ScreenRecordRepository.setError("Permiso de micrófono denegado.")
        }
    }

    // Notification permission launcher
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        checkAudioAndStart(context, state.recordAudio, audioPermissionLauncher) { intent ->
            projectionLauncher.launch(intent)
        }
    }

    // Camera permission launcher for Facecam PiP
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleEnableCameraPip(true)
        } else {
            viewModel.toggleEnableCameraPip(false)
            ScreenRecordRepository.setError("Permiso de cámara denegado")
        }
    }

    fun startRecordingFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            checkAudioAndStart(context, state.recordAudio, audioPermissionLauncher) { intent ->
                projectionLauncher.launch(intent)
            }
        }
    }

    var showDebugSheet by remember { mutableStateOf(false) }
    val debugSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDebugSheet) {
        com.example.debug.DebugToolsBottomSheet(
            sheetState = debugSheetState,
            onDismiss = { showDebugSheet = false }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F121C),
        floatingActionButton = {
            if (com.example.BuildConfig.DEBUG) {
                Surface(
                    onClick = { showDebugSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF9100),
                    shadowElevation = 8.dp,
                    modifier = Modifier.testTag("debug_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug Panel",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEBUG",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Bar / Header
            HeaderBar(
                isRecording = state.isRecording,
                onOpenDebug = if (com.example.BuildConfig.DEBUG) { { showDebugSheet = true } } else null
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error banner if any
            state.error?.let { errorMsg ->
                ErrorBanner(
                    message = errorMsg,
                    onDismiss = { viewModel.clearError() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Record Hero Card
                item {
                    RecordingHeroCard(
                        isRecording = state.isRecording,
                        isPaused = state.isPaused,
                        isMuted = state.isMuted,
                        durationSeconds = state.recordingDurationSeconds,
                        onToggleRecording = {
                            if (state.isRecording) {
                                viewModel.stopRecording(context)
                            } else {
                                startRecordingFlow()
                            }
                        },
                        onTogglePause = {
                            if (state.isPaused) viewModel.resumeRecording(context)
                            else viewModel.pauseRecording(context)
                        },
                        onToggleMute = {
                            viewModel.toggleMuteRecording(context)
                        }
                    )
                }

                // Architecture Engine Info Card
                item {
                    MobileObsArchitectureCard(
                        recordInternalAudio = state.recordInternalAudio,
                        enableCameraPip = state.enableCameraPip
                    )
                }

                // Scenes & Sources System Card (Rust Scene Graph)
                item {
                    ScenesAndSourcesCard(
                        scenes = state.scenes,
                        selectedSceneId = state.selectedSceneId,
                        isRecording = state.isRecording,
                        onSelectScene = { viewModel.selectScene(it) },
                        onToggleSource = { sceneId, sourceId -> viewModel.toggleSourceVisibility(sceneId, sourceId) }
                    )
                }

                // Advanced Audio DSP Filters Card (C++ Oboe Engine)
                item {
                    AdvancedAudioFiltersCard(
                        audioFilters = state.audioFilters,
                        isRecording = state.isRecording,
                        onUpdateFilters = { update -> viewModel.updateAudioFilters(update) }
                    )
                }

                // Options Card
                item {
                    SettingsCard(
                        selectedAudioMode = state.audioMode,
                        enableCameraPip = state.enableCameraPip,
                        enableNoiseGate = state.enableNoiseGate,
                        enableFloatingBubble = state.enableFloatingBubble,
                        selectedQuality = state.qualityPreset,
                        selectedFps = state.selectedFps,
                        selectedBitrate = state.selectedBitrate,
                        isRecording = state.isRecording,
                        onSelectAudioMode = { viewModel.selectAudioMode(it) },
                        onToggleCameraPip = { enable ->
                            if (enable) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.toggleEnableCameraPip(true)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                viewModel.toggleEnableCameraPip(false)
                            }
                        },
                        onToggleNoiseGate = { viewModel.toggleEnableNoiseGate(it) },
                        onToggleFloatingBubble = { viewModel.toggleFloatingBubble(it) },
                        onSelectQuality = { viewModel.selectQualityPreset(it) },
                        onSelectFps = { viewModel.selectFpsOption(it) },
                        onSelectBitrate = { viewModel.selectBitrateOption(it) }
                    )
                }

                // Recorded Videos Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tus Grabaciones",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF22283A)
                        ) {
                            Text(
                                text = "${videos.size} videos",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00D2FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (videos.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    items(
                        items = videos,
                        key = { it.id }
                    ) { video ->
                        VideoItemCard(
                            video = video,
                            onPlay = { selectedVideoForPlayback = video },
                            onExternalPlay = { viewModel.playVideoExternal(context, video) },
                            onShare = { viewModel.shareVideo(context, video) },
                            onDelete = { videoToDelete = video }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Video Player Dialog
    selectedVideoForPlayback?.let { video ->
        VideoPlayerDialog(
            video = video,
            onDismiss = { selectedVideoForPlayback = null },
            onShare = { viewModel.shareVideo(context, video) },
            onTrim = { vid, startMs, endMs ->
                viewModel.trimVideoLossless(context, vid, startMs, endMs) { success ->
                    if (success) {
                        selectedVideoForPlayback = null
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("¿Eliminar grabación?", color = Color.White) },
            text = { Text("Se eliminará permanentemente '${video.name}'.", color = Color.LightGray) },
            containerColor = Color(0xFF1E2436),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(context, video)
                        videoToDelete = null
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

private fun checkAudioAndStart(
    context: Context,
    recordAudio: Boolean,
    audioPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onReady: (Intent) -> Unit
) {
    if (recordAudio && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
        launchScreenCapture(context, onReady)
    }
}

private fun launchScreenCapture(context: Context, onReady: (Intent) -> Unit) {
    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    onReady(manager.createScreenCaptureIntent())
}

@Composable
fun HeaderBar(
    isRecording: Boolean,
    onOpenDebug: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF3B30), Color(0xFFFF7A00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Grabar Pantalla",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Grabadora rápida de video",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenDebug != null) {
                Surface(
                    onClick = onOpenDebug,
                    shape = CircleShape,
                    color = Color(0xFF3B2A10)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug",
                        tint = Color(0xFFFF9100),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isRecording) Color(0xFF3B1214) else Color(0xFF162B28)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color(0xFFFF3B30) else Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "GRABANDO" else "LISTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) Color(0xFFFF3B30) else Color(0xFF00E676)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B181A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF3B30)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecordingHeroCard(
    isRecording: Boolean,
    isPaused: Boolean,
    isMuted: Boolean,
    durationSeconds: Long,
    onToggleRecording: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF181D2D)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulse Outer Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) Color(0xFFFFD600).copy(alpha = 0.25f)
                                else Color(0xFFFF3B30).copy(alpha = 0.25f)
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) {
                                if (isPaused) Brush.linearGradient(listOf(Color(0xFFFFD600), Color(0xFFFF9100)))
                                else Brush.linearGradient(listOf(Color(0xFFFF3B30), Color(0xFFD32F2F)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF00D2FF), Color(0xFF0072FF)))
                            }
                        )
                        .clickable { onToggleRecording() }
                        .testTag("record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRecording) "Detener grabación" else "Iniciar grabación",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Text & Status Badge
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            val timerFormatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isRecording) timerFormatted else "00:00",
                    fontSize = 36.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) (if (isPaused) Color(0xFFFFD600) else Color(0xFFFF3B30)) else Color.White
                )

                if (isPaused) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF3B3212)
                    ) {
                        Text(
                            text = "PAUSADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD600),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Extra Control Buttons when recording is active
            if (isRecording) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause / Resume Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF222B3D),
                        modifier = Modifier.clickable { onTogglePause() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else androidx.compose.material.icons.Icons.Default.Pause,
                                contentDescription = if (isPaused) "Reanudar" else "Pausar",
                                tint = if (isPaused) Color(0xFFFFD600) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPaused) "Reanudar" else "Pausar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPaused) Color(0xFFFFD600) else Color.White
                            )
                        }
                    }

                    // Mute Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF222B3D),
                        modifier = Modifier.clickable { onToggleMute() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Micrófono",
                                tint = if (isMuted) Color(0xFFFF5252) else Color(0xFF00D2FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMuted) "Silenciado" else "Mic On",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMuted) Color(0xFFFF5252) else Color(0xFF00D2FF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = if (isRecording) "Grabando en segundo plano con burbuja flotante" else "Toca el botón azul para iniciar la grabación",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsCard(
    selectedAudioMode: AudioMode,
    enableCameraPip: Boolean,
    enableNoiseGate: Boolean,
    enableFloatingBubble: Boolean,
    selectedQuality: QualityPreset,
    selectedFps: FpsOption,
    selectedBitrate: BitrateOption,
    isRecording: Boolean,
    onSelectAudioMode: (AudioMode) -> Unit,
    onToggleCameraPip: (Boolean) -> Unit,
    onToggleNoiseGate: (Boolean) -> Unit,
    onToggleFloatingBubble: (Boolean) -> Unit,
    onSelectQuality: (QualityPreset) -> Unit,
    onSelectFps: (FpsOption) -> Unit,
    onSelectBitrate: (BitrateOption) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Ajustes de Grabación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Audio Mode Selector Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF00D2FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fuente de Audio (Motor Oboe C++)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Audio Modes Options Grid/List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioMode.entries.forEach { mode ->
                    val isSelected = selectedAudioMode == mode
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isRecording) { onSelectAudioMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF202738) else Color(0xFF131724),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00D2FF) else Color(0xFF262C3D)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = if (!isRecording) { { onSelectAudioMode(mode) } } else null,
                                enabled = !isRecording,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00D2FF),
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.LightGray
                                    )
                                    if (mode == AudioMode.DUAL) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF1B2F2A)
                                        ) {
                                            Text(
                                                text = "Recomendado",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF80CBC4),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF262C3D), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Camera PiP Facecam Toggle Row (Rust Engine)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (enableCameraPip) Color(0xFFFFAB91) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cámara Rostro / Facecam (Rust)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF381F19)
                            ) {
                                Text(
                                    text = "PiP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFAB91),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (enableCameraPip) "Superpone tu rostro sobre la pantalla" else "Desactivada",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Switch(
                    checked = enableCameraPip,
                    onCheckedChange = onToggleCameraPip,
                    enabled = !isRecording,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFFAB91)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Noise Gate / Noise Suppression Toggle Row (Oboe C++)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (enableNoiseGate) Color(0xFF00D2FF) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Supresión de Ruido (C++ Oboe)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF132A38)
                            ) {
                                Text(
                                    text = "DSP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D2FF),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (enableNoiseGate) "Filtra el ruido de fondo por debajo de -35dB" else "Filtro desactivado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Switch(
                    checked = enableNoiseGate,
                    onCheckedChange = onToggleNoiseGate,
                    enabled = !isRecording,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00D2FF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating Control Bubble Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlipToFront,
                        contentDescription = null,
                        tint = if (enableFloatingBubble) Color(0xFF00E676) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Burbuja Flotante de Control",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF133821)
                            ) {
                                Text(
                                    text = "FLOTANTE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (enableFloatingBubble) "Burbuja interactiva para pausar/detener fuera de la app" else "Desactivada",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Switch(
                    checked = enableFloatingBubble,
                    onCheckedChange = { checked ->
                        if (checked && !Settings.canDrawOverlays(context)) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        onToggleFloatingBubble(checked)
                    },
                    enabled = !isRecording,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00E676)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF262C3D), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Resolution Selection Row
            Column {
                Text(
                    text = "Resolución de Video",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QualityPreset.entries.forEach { preset ->
                        val selected = selectedQuality == preset
                        FilterChip(
                            selected = selected,
                            onClick = { if (!isRecording) onSelectQuality(preset) },
                            enabled = !isRecording,
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D2FF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF22283A),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // FPS Selection Row
            Column {
                Text(
                    text = "Cuadros por segundo (FPS)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FpsOption.entries.forEach { fpsOption ->
                        val selected = selectedFps == fpsOption
                        FilterChip(
                            selected = selected,
                            onClick = { if (!isRecording) onSelectFps(fpsOption) },
                            enabled = !isRecording,
                            label = { Text(fpsOption.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D2FF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF22283A),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bitrate Selection Row
            Column {
                Text(
                    text = "Tasa de bits (Bitrate)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BitrateOption.entries.forEach { bitrateOption ->
                        val selected = selectedBitrate == bitrateOption
                        FilterChip(
                            selected = selected,
                            onClick = { if (!isRecording) onSelectBitrate(bitrateOption) },
                            enabled = !isRecording,
                            label = { Text(bitrateOption.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D2FF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF22283A),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItemCard(
    video: RecordedVideo,
    onPlay: () -> Unit,
    onExternalPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(video.timestamp) {
        SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault()).format(Date(video.timestamp))
    }
    val sizeMb = remember(video.sizeBytes) {
        String.format(Locale.getDefault(), "%.1f MB", video.sizeBytes / (1024f * 1024f))
    }
    val durationStr = remember(video.durationMs) {
        if (video.durationMs <= 0) "Video" else {
            val totalSecs = video.durationMs / 1000
            val m = totalSecs / 60
            val s = totalSecs % 60
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail Placeholder Badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF232A40)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = Color(0xFF00D2FF),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = " • $sizeMb",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00D2FF)
                    )
                }
            }

            // Quick Actions
            Row {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = Color.LightGray
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22283A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sin grabaciones aún",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Presiona el botón de inicio para realizar tu primera grabación de pantalla.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MobileObsArchitectureCard(
    recordInternalAudio: Boolean = true,
    enableCameraPip: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131724))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "⚡ Motor Nativo Mobile OBS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D2FF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArchitectureBadge(
                    tag = "Kotlin UI",
                    detail = "MediaProjection",
                    bgColor = Color(0xFF231C3D),
                    textColor = Color(0xFFB39DDB),
                    modifier = Modifier.weight(1f)
                )
                ArchitectureBadge(
                    tag = "C++ Oboe",
                    detail = if (recordInternalAudio) "Mezcla Dual Active" else "Low-Latency Mic",
                    bgColor = Color(0xFF1B2F2A),
                    textColor = Color(0xFF80CBC4),
                    modifier = Modifier.weight(1f)
                )
                ArchitectureBadge(
                    tag = "Rust Engine",
                    detail = if (enableCameraPip) "Facecam PiP Active" else "Zero-Copy Video",
                    bgColor = Color(0xFF381F19),
                    textColor = Color(0xFFFFAB91),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ArchitectureBadge(
    tag: String,
    detail: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScenesAndSourcesCard(
    scenes: List<ObsScene>,
    selectedSceneId: String,
    isRecording: Boolean,
    onSelectScene: (String) -> Unit,
    onToggleSource: (String, String) -> Unit
) {
    val currentScene = scenes.find { it.id == selectedSceneId } ?: scenes.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFFFFAB91),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sistema de Escenas y Fuentes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF381F19)
                            ) {
                                Text(
                                    text = "RUST",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFAB91),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Composición gráfica por capas y fuentes independientes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scene Tabs Selector
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scenes.forEach { scene ->
                    val isSelected = scene.id == selectedSceneId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectScene(scene.id) },
                        label = {
                            Text(
                                text = scene.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFAB91),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF22283A),
                            labelColor = Color.White
                        )
                    )
                }
            }

            currentScene?.let { scene ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = scene.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Capas y Fuentes activas (${scene.sources.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D2FF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    scene.sources.forEach { source ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF131724),
                            border = BorderStroke(1.dp, Color(0xFF262C3D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (source.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (source.isVisible) Color(0xFF00E676) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = source.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (source.isVisible) Color.White else Color.Gray
                                        )
                                        Text(
                                            text = "${source.type.label} • Capa Z: ${source.zIndex}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Switch(
                                    checked = source.isVisible,
                                    onCheckedChange = { onToggleSource(scene.id, source.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF00E676)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedAudioFiltersCard(
    audioFilters: AudioFilterSettings,
    isRecording: Boolean,
    onUpdateFilters: ((AudioFilterSettings) -> AudioFilterSettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF00D2FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtros de Audio Avanzados (C++ DSP)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF132A38)
                            ) {
                                Text(
                                    text = "OBOE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D2FF),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Compresor, Gain Booster y Ecualización de 3 Bandas",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compresor Dinámico Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compresor Dinámico (C++ DSP)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Controla picos de volumen cuando gritas o hay explosiones en el juego",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = audioFilters.enableCompressor,
                    onCheckedChange = { enable -> onUpdateFilters { it.copy(enableCompressor = enable) } },
                    enabled = !isRecording,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00D2FF)
                    )
                )
            }

            if (audioFilters.enableCompressor) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Umbral Compresor: ${audioFilters.compressorThresholdDb.toInt()} dB (Ratio ${audioFilters.compressorRatio.toInt()}:1)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00D2FF)
                )
                Slider(
                    value = audioFilters.compressorThresholdDb,
                    onValueChange = { thresh -> onUpdateFilters { it.copy(compressorThresholdDb = thresh) } },
                    valueRange = -30f..-6f,
                    enabled = !isRecording,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00D2FF),
                        activeTrackColor = Color(0xFF00D2FF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF262C3D), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Gain Booster Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Amplificador de Ganancia / Gain Booster",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Aumenta la claridad de voces suaves sin distorsión (+0dB a +12dB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = audioFilters.enableGainBooster,
                    onCheckedChange = { enable -> onUpdateFilters { it.copy(enableGainBooster = enable) } },
                    enabled = !isRecording,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00D2FF)
                    )
                )
            }

            if (audioFilters.enableGainBooster) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ganancia de Salida: +${String.format(Locale.getDefault(), "%.1f", audioFilters.gainBoostDb)} dB",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00D2FF)
                )
                Slider(
                    value = audioFilters.gainBoostDb,
                    onValueChange = { gain -> onUpdateFilters { it.copy(gainBoostDb = gain) } },
                    valueRange = 0f..12f,
                    enabled = !isRecording,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00D2FF),
                        activeTrackColor = Color(0xFF00D2FF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF262C3D), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3-Band Equalizer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ecualizador de 3 Bandas (C++ Filter)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Ajuste fino de Graves, Medios y Agudos para micrófono y juego",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = audioFilters.enableEqualizer,
                    onCheckedChange = { enable -> onUpdateFilters { it.copy(enableEqualizer = enable) } },
                    enabled = !isRecording,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00D2FF)
                    )
                )
            }

            if (audioFilters.enableEqualizer) {
                Spacer(modifier = Modifier.height(8.dp))

                // Low / Bass
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Graves (Bass): ${String.format(Locale.getDefault(), "%+.1f", audioFilters.eqLowGain)} dB", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Slider(
                    value = audioFilters.eqLowGain,
                    onValueChange = { low -> onUpdateFilters { it.copy(eqLowGain = low) } },
                    valueRange = -6f..6f,
                    enabled = !isRecording,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF9800), activeTrackColor = Color(0xFFFF9800))
                )

                // Mid
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Medios (Voice): ${String.format(Locale.getDefault(), "%+.1f", audioFilters.eqMidGain)} dB", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Slider(
                    value = audioFilters.eqMidGain,
                    onValueChange = { mid -> onUpdateFilters { it.copy(eqMidGain = mid) } },
                    valueRange = -6f..6f,
                    enabled = !isRecording,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676))
                )

                // High / Treble
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agudos (Treble): ${String.format(Locale.getDefault(), "%+.1f", audioFilters.eqHighGain)} dB", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Slider(
                    value = audioFilters.eqHighGain,
                    onValueChange = { high -> onUpdateFilters { it.copy(eqHighGain = high) } },
                    valueRange = -6f..6f,
                    enabled = !isRecording,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00D2FF), activeTrackColor = Color(0xFF00D2FF))
                )
            }
        }
    }
}

