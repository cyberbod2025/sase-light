package com.example

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate
import platform.Foundation.NSTemporaryDirectory

actual fun getPlatformName(): String = "iOS"

actual fun formatTimestamp(pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    return formatter.stringFromDate(NSDate())
}

actual fun currentEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1_000.0).toLong()

actual fun exportTextFileToDisk(fileName: String, content: String): String {
    val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    return "${NSTemporaryDirectory()}$sanitizedName"
}
