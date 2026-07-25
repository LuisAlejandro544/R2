package com.example

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class FloatingBubbleOverlay(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    fun show() {
        if (!Settings.canDrawOverlays(context)) return
        if (overlayView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = ServiceLifecycleOwner().apply { onCreate() }

        val typeParam = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            typeParam,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 250
        }

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                MyApplicationTheme {
                    FloatingBubbleContent(
                        onStopRecording = {
                            val intent = Intent(context, ScreenRecordService::class.java).apply {
                                action = ScreenRecordService.ACTION_STOP
                            }
                            context.startService(intent)
                        },
                        onTogglePause = {
                            val isPaused = ScreenRecordRepository.state.value.isPaused
                            val actionStr = if (isPaused) ScreenRecordService.ACTION_RESUME else ScreenRecordService.ACTION_PAUSE
                            val intent = Intent(context, ScreenRecordService::class.java).apply {
                                action = actionStr
                            }
                            context.startService(intent)
                        },
                        onToggleMute = {
                            val intent = Intent(context, ScreenRecordService::class.java).apply {
                                action = ScreenRecordService.ACTION_TOGGLE_MUTE
                            }
                            context.startService(intent)
                        },
                        onToggleCameraPip = {
                            val intent = Intent(context, ScreenRecordService::class.java).apply {
                                action = ScreenRecordService.ACTION_TOGGLE_FACECAM
                            }
                            context.startService(intent)
                        }
                    )
                }
            }

            setOnTouchListener { v, event ->
                val lp = windowParams ?: return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = lp.x
                        initialY = lp.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            isDragging = true
                        }
                        if (isDragging) {
                            lp.x = initialX + dx
                            lp.y = initialY + dy
                            windowManager?.updateViewLayout(overlayView, lp)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            v.performClick()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        overlayView = view
        try {
            windowManager?.addView(view, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        try {
            if (overlayView != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun FloatingBubbleContent(
    onStopRecording: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleCameraPip: () -> Unit
) {
    val state by ScreenRecordRepository.state.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "bubblePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubblePulseScale"
    )

    val mins = state.recordingDurationSeconds / 60
    val secs = state.recordingDurationSeconds % 60
    val timerStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xEE161A26),
        shadowElevation = 10.dp,
        modifier = Modifier.padding(4.dp)
    ) {
        if (!isExpanded) {
            // Collapsed Bubble Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(if (!state.isPaused) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (state.isPaused) Color.Yellow else Color(0xFFFF3B30))
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = timerStr,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            // Expanded Control Panel
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pause / Resume Button
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF232B3E))
                        .testTag("bubble_pause_btn")
                ) {
                    Icon(
                        imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (state.isPaused) "Reanudar" else "Pausar",
                        tint = if (state.isPaused) Color(0xFFFFD600) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Stop Recording Button
                IconButton(
                    onClick = onStopRecording,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFF3B30), Color(0xFFD32F2F)))
                        )
                        .testTag("bubble_stop_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Detener",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Mute / Unmute Button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF232B3E))
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Micrófono",
                        tint = if (state.isMuted) Color(0xFFFF5252) else Color(0xFF00D2FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Facecam PiP Toggle Button
                IconButton(
                    onClick = onToggleCameraPip,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF232B3E))
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Facecam",
                        tint = if (state.enableCameraPip) Color(0xFFFFAB91) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close / Collapse Panel
                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B202E))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Minimizar",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
