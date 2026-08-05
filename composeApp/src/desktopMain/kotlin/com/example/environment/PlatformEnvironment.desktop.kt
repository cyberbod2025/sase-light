package com.example.environment

actual fun platformAppEnvironmentValues(): Map<String, String> = buildMap {
    put(
        AppEnvironment.ENVIRONMENT_KEY,
        hostValue("sase.environment", AppEnvironment.ENVIRONMENT_KEY) ?: AppEnvironmentMode.DEMO_LOCAL.name
    )
    put(
        AppEnvironment.APP_VERSION_KEY,
        hostValue("sase.appVersion", AppEnvironment.APP_VERSION_KEY) ?: "1.0"
    )
    hostValue("sase.supabaseUrl", AppEnvironment.SUPABASE_URL_KEY)?.let {
        put(AppEnvironment.SUPABASE_URL_KEY, it)
    }
    hostValue("sase.supabasePublishableKey", AppEnvironment.SUPABASE_PUBLISHABLE_KEY)?.let {
        put(AppEnvironment.SUPABASE_PUBLISHABLE_KEY, it)
    }
}

private fun hostValue(systemProperty: String, environmentVariable: String): String? =
    System.getProperty(systemProperty)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environmentVariable)?.takeIf { it.isNotBlank() }
