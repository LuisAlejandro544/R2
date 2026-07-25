package com.example.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    VERBOSE, INFO, WARN, ERROR, CRASH
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
)

object DebugLogger {

    private var entryId = 0L
    private val maxEntries = 500
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        // Install uncaught exception handler to capture crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("CRASH", "Excepción no capturada en el hilo ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        i("DebugLogger", "Sistema de trazabilidad de Debug iniciado correctamente.")
    }

    fun v(tag: String, message: String) = addLog(LogLevel.VERBOSE, tag, message)
    fun i(tag: String, message: String) = addLog(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.ERROR, tag, message, throwable)

    private fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val stackTrace = throwable?.let { Log.getStackTraceString(it) }
        
        // Log to standard Android logcat as well
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR, LogLevel.CRASH -> Log.e(tag, message, throwable)
        }

        val entry = LogEntry(
            id = ++entryId,
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message,
            throwable = stackTrace
        )

        synchronized(this) {
            val currentList = _logs.value.toMutableList()
            if (currentList.size >= maxEntries) {
                currentList.removeAt(0)
            }
            currentList.add(entry)
            _logs.value = currentList
        }
    }

    fun clearLogs() {
        synchronized(this) {
            _logs.value = emptyList()
        }
    }
}
