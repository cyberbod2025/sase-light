package com.example

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getPlatformName(): String = "Desktop (${System.getProperty("os.name")})"

actual fun formatTimestamp(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

actual fun exportTextFileToDisk(fileName: String, content: String): String {
    val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val exportDirProp = System.getProperty("sase.export.dir")?.takeIf { it.isNotBlank() }
    val baseDir = if (exportDirProp != null) {
        File(exportDirProp)
    } else {
        File(System.getProperty("java.io.tmpdir"), "sase-exports")
    }
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }
    val targetFile = File(baseDir, sanitizedName)
    targetFile.writeText(content, Charsets.UTF_8)
    return targetFile.absolutePath
}
