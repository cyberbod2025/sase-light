package com.example

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getPlatformName(): String = "Android ${android.os.Build.VERSION.SDK_INT}"

@SuppressLint("SimpleDateFormat")
actual fun formatTimestamp(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
}
