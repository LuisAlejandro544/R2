package com.example

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import java.io.File

import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun VideoPlayerDialog(
    video: RecordedVideo,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onTrim: (RecordedVideo, Long, Long) -> Unit
) {
    val context = LocalContext.current
    var isTrimmingMode by remember { mutableStateOf(false) }
    var isProcessingTrim by remember { mutableStateOf(false) }

    val totalDurationMs = if (video.durationMs > 0) video.durationMs else 10000L
    var sliderRange by remember(video) { mutableStateOf(0f..totalDurationMs.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1E2E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Reproduciendo e inspeccionando video",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = false)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.fromFile(File(video.path)))
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isTrimmingMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF22283A))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Recorte de Video Sin Pérdida (Rust / MediaMuxer)",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF00D2FF),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inicio: ${(sliderRange.start / 1000f).toInt()}s  —  Fin: ${(sliderRange.endInclusive / 1000f).toInt()}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        RangeSlider(
                            value = sliderRange,
                            onValueChange = { range ->
                                if (range.endInclusive - range.start >= 1000f) {
                                    sliderRange = range
                                }
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00D2FF),
                                activeTrackColor = Color(0xFF00D2FF),
                                inactiveTrackColor = Color.DarkGray
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isTrimmingMode = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    isProcessingTrim = true
                                    val startMs = sliderRange.start.toLong()
                                    val endMs = sliderRange.endInclusive.toLong()
                                    onTrim(video, startMs, endMs)
                                    isProcessingTrim = false
                                    isTrimmingMode = false
                                },
                                enabled = !isProcessingTrim,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00D2FF),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isProcessingTrim) "Procesando..." else "Guardar Recorte")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF00D2FF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir", color = Color(0xFF00D2FF))
                    }

                    OutlinedButton(
                        onClick = { isTrimmingMode = !isTrimmingMode },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recortar", color = Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}
