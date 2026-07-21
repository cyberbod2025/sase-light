package com.example.auth

import platform.Foundation.NSProcessInfo

actual object PlatformEnvironment {
    actual fun read(name: String): String? =
        (NSProcessInfo.processInfo.environment[name] as? String)?.takeIf { it.isNotBlank() }
}
