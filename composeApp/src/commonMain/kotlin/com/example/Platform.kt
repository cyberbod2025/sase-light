package com.example

expect fun getPlatformName(): String

expect fun formatTimestamp(pattern: String): String

expect fun currentEpochMillis(): Long
