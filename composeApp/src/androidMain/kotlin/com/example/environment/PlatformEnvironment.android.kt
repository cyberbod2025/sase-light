package com.example.environment

import com.example.BuildConfig

actual fun platformAppEnvironmentValues(): Map<String, String> = buildMap {
    put(AppEnvironment.ENVIRONMENT_KEY, BuildConfig.SASE_APP_ENVIRONMENT)
    put(AppEnvironment.APP_VERSION_KEY, BuildConfig.VERSION_NAME)
    BuildConfig.SASE_SUPABASE_URL.takeIf { it.isNotBlank() }?.let {
        put(AppEnvironment.SUPABASE_URL_KEY, it)
    }
    BuildConfig.SASE_SUPABASE_PUBLISHABLE_KEY.takeIf { it.isNotBlank() }?.let {
        put(AppEnvironment.SUPABASE_PUBLISHABLE_KEY, it)
    }
}
