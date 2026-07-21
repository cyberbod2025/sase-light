package com.example.auth

actual object PlatformEnvironment {
    actual fun read(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
}
