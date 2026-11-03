package com.example

import android.annotation.SuppressLint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getPlatformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"

@SuppressLint("SimpleDateFormat")
actual fun formatTimestamp(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

actual fun exportTextFileToDisk(fileName: String, content: String): String {
    val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val baseDir = File(System.getProperty("java.io.tmpdir") ?: "/data/local/tmp", "sase-exports")
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    val targetFile = File(baseDir, sanitizedName)
    targetFile.writeText(content, Charsets.UTF_8)
    return targetFile.absolutePath
}
