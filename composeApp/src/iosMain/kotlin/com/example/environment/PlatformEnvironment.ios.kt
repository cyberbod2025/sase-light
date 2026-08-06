package com.example.environment

actual fun platformAppEnvironmentValues(): Map<String, String> = mapOf(
    AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.DEMO_LOCAL.name,
    AppEnvironment.APP_VERSION_KEY to "1.0"
)
