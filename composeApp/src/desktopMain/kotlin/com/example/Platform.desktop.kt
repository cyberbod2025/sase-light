package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getPlatformName(): String = "Desktop (${System.getProperty("os.name")})"

actual fun getApiKey(): String = System.getenv("GEMINI_API_KEY") ?: ""

actual fun formatTimestamp(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}
