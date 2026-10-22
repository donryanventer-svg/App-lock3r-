package com.example.data

data class AppInfo(
    val packageName: String,
    val name: String,
    val isLocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val isSensitive: Boolean = false,
    val category: String = "General"
)
