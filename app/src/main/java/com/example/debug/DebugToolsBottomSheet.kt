package com.example.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolsBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        PerformanceMonitor.startMonitoring(scope)
        onDispose {
            PerformanceMonitor.stopMonitoring()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF10141E),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF9100).copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug",
                            tint = Color(0xFFFF9100),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Panel Debug & Diagnóstico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Exclusivo para compilaciones DEBUG",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9100)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            val tabs = listOf("📊 Consumos CPU/RAM", "📋 Logs & Traza", "💥 Pruebas", "ℹ️ Info Build")
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF181E2E),
                contentColor = Color(0xFF00E676)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF00E676) else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> PerformanceMetricsTab()
                1 -> LogsViewerTab(context)
                2 -> FaultSimulatorTab(context)
                3 -> BuildInfoTab(context)
            }
        }
    }
}

@Composable
fun PerformanceMetricsTab() {
    val stats by PerformanceMonitor.stats.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // General Hardware Gauges Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181E2E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "USO GLOBAL DE RECURSOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D2FF)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // CPU Usage Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Uso Estimado de CPU:", fontSize = 12.sp, color = Color.White)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f %%", stats.cpuUsagePercent),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.cpuUsagePercent > 50f) Color(0xFFFF5252) else Color(0xFF00E676)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (stats.cpuUsagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (stats.cpuUsagePercent > 50f) Color(0xFFFF5252) else Color(0xFF00E676),
                        trackColor = Color(0xFF262C3D)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Heap Memory Bar
                    val heapProgress = if (stats.maxHeapMb > 0) stats.usedHeapMb / stats.maxHeapMb else 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Memoria Heap JVM:", fontSize = 12.sp, color = Color.White)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f MB / %.1f MB", stats.usedHeapMb, stats.maxHeapMb),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D2FF)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { heapProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(0xFF00D2FF),
                        trackColor = Color(0xFF262C3D)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Memoria Nativa (C++/Rust): ${String.format(Locale.getDefault(), "%.1f MB", stats.nativeHeapMb)}",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Hilos Activos: ${stats.activeThreadsCount}",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "DESGLOSE DE CONSUMO POR SUBSISTEMA / COMPONENTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(stats.topConsumers) { consumer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181E2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = consumer.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF262C3D)
                        ) {
                            Text(
                                text = consumer.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (consumer.status.contains("ACTIVO") || consumer.status.contains("EJECUCIÓN") || consumer.status.contains("VISIBLE") || consumer.status.contains("TRANSMITIENDO")) Color(0xFF00E676) else Color.Gray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = consumer.description,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CPU Estimado: ${String.format(Locale.getDefault(), "%.1f %%", consumer.estimatedCpuPercent)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF9100)
                        )
                        Text(
                            text = "RAM Estimada: ${String.format(Locale.getDefault(), "%.1f MB", consumer.estimatedRamMb)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00D2FF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogsViewerTab(context: Context) {
    val allLogs by DebugLogger.logs.collectAsState()
    var filterLevel by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(allLogs, filterLevel, searchQuery) {
        allLogs.filter { log ->
            val matchesLevel = filterLevel == null || log.level == filterLevel
            val matchesSearch = searchQuery.isEmpty() ||
                    log.tag.contains(searchQuery, ignoreCase = true) ||
                    log.message.contains(searchQuery, ignoreCase = true)
            matchesLevel && matchesSearch
        }.reversed()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar log...", fontSize = 11.sp, color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E676),
                    unfocusedBorderColor = Color(0xFF262C3D),
                    focusedContainerColor = Color(0xFF181E2E),
                    unfocusedContainerColor = Color(0xFF181E2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
            )

            IconButton(
                onClick = {
                    val fullText = filteredLogs.joinToString("\n") { "[${it.timestamp}][${it.level}][${it.tag}] ${it.message}" }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Debug Logs", fullText))
                    Toast.makeText(context, "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF181E2E), CircleShape)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { DebugLogger.clearLogs() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF181E2E), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Level Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                Pair("TODOS", null),
                Pair("INFO", LogLevel.INFO),
                Pair("WARN", LogLevel.WARN),
                Pair("ERROR", LogLevel.ERROR)
            )

            filterOptions.forEach { (label, level) ->
                val isSelected = filterLevel == level
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF00E676) else Color(0xFF181E2E),
                    modifier = Modifier.clickable { filterLevel = level }
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Log List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay registros de logs correspondientes.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs) { log ->
                    val levelColor = when (log.level) {
                        LogLevel.VERBOSE -> Color.Gray
                        LogLevel.INFO -> Color(0xFF00D2FF)
                        LogLevel.WARN -> Color(0xFFFFD600)
                        LogLevel.ERROR, LogLevel.CRASH -> Color(0xFFFF5252)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A28)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "[${log.timestamp}] [${log.tag}]",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Text(
                                    text = log.level.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = levelColor
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.message,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            if (log.throwable != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.throwable,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFF8A80)
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
fun FaultSimulatorTab(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "SIMULADOR DE EVENTOS Y PRUEBAS TÉCNICAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Button(
            onClick = {
                DebugLogger.w("TestSimulator", "Advertencia de prueba generada manualmente.", Throwable("Advertencia de prueba"))
                Toast.makeText(context, "Log WARN de prueba agregado", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3212)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFD600))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generar Advertencia (WARN Log)", color = Color(0xFFFFD600), fontSize = 12.sp)
        }

        Button(
            onClick = {
                DebugLogger.e("TestSimulator", "Error controlado de prueba ejecutado.", RuntimeException("Error simulado de prueba"))
                Toast.makeText(context, "Log ERROR de prueba agregado", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFFFF5252))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simular Error / Exception (ERROR Log)", color = Color(0xFFFF5252), fontSize = 12.sp)
        }

        Button(
            onClick = {
                System.gc()
                DebugLogger.i("MemoryDebug", "Solicitud manual de Garbage Collection ejecutada.")
                Toast.makeText(context, "Garbage Collection ejecutado", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181E2E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF00D2FF))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Forzar Libaración de Memoria (System.gc)", color = Color(0xFF00D2FF), fontSize = 12.sp)
        }
    }
}

@Composable
fun BuildInfoTab(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "INFORMACIÓN DEL ENTORNO DE COMPILACIÓN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        val items = listOf(
            "Paquete (Package ID)" to context.packageName,
            "Tipo de Build" to "DEBUG (Entorno de Desarrollo)",
            "Dispositivo Modelo" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Versión de Android" to "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Arquitectura ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
            "SubSistemas Activos" to "C++ Oboe Audio + Rust Video Pipeline"
        )

        items.forEach { (label, value) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181E2E)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, fontSize = 11.sp, color = Color.Gray)
                    Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
