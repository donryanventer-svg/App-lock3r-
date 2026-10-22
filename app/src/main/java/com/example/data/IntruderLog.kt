package com.example.data

data class IntruderLog(
    val id: Long = System.currentTimeMillis(),
    val appName: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attempts: Int = 1,
    val fakeCrashTriggered: Boolean = false
)
