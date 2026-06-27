package com.example

expect fun getPlatformName(): String

expect fun getApiKey(): String

expect fun formatTimestamp(pattern: String): String
