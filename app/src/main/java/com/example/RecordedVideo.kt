package com.example

data class RecordedVideo(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val timestamp: Long
)
