package com.example

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDate

actual fun getPlatformName(): String = "iOS"

actual fun formatTimestamp(pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    return formatter.stringFromDate(NSDate())
}
