package com.example.environment

/** Runtime mode selected explicitly by the application host. */
enum class AppEnvironmentMode {
    DEMO_LOCAL,
    SUPABASE_STAGING,
    PRODUCTION,
}

/**
 * Supabase values supplied by the application host.
 *
 * This model never reads environment variables and never contains defaults, so
 * connected modes cannot silently fall back to demo or embedded credentials.
 */
data class ExternalSupabaseConfiguration(
    val url: String,
    val publishableKey: String,
) {
    init {
        require(url.isNotBlank()) { "Supabase URL must not be blank." }
        require(url.startsWith("https://") && url.none { it.isWhitespace() }) {
            "Supabase URL must be an absolute HTTPS URL without whitespace."
        }
        require(publishableKey.isNotBlank()) { "Supabase publishable key must not be blank." }
    }
}

/** Visible markers that keep local demo data unambiguously separate from real data. */
data class DemoLocalConfiguration(
    val institutionDisplayName: String,
    val institutionIsFictitious: Boolean,
    val usesOnlySyntheticData: Boolean,
    val demoUsersEnabled: Boolean,
) {
    init {
        require(institutionDisplayName.isNotBlank()) { "Demo institution name must not be blank." }
        require(institutionIsFictitious) { "A local demo institution must be explicitly fictitious." }
        require(usesOnlySyntheticData) { "A local demo must use only synthetic data." }
    }
}

/**
 * Pure, immutable application configuration.
 *
 * Connected environments always carry externally supplied Supabase values.
 * Production cannot contain demo configuration or enable demo users.
 */
class AppEnvironment private constructor(
    val mode: AppEnvironmentMode,
    val appVersion: String,
    val supabase: ExternalSupabaseConfiguration?,
    val demo: DemoLocalConfiguration?,
) {
    val isDemo: Boolean
        get() = demo != null

    val demoUsersEnabled: Boolean
        get() = demo?.demoUsersEnabled == true

    val usesSyntheticData: Boolean
        get() = mode != AppEnvironmentMode.PRODUCTION

    init {
        require(appVersion.isNotBlank()) { "App version must not be blank." }

        when (mode) {
            AppEnvironmentMode.DEMO_LOCAL -> {
                requireNotNull(demo) { "DEMO_LOCAL requires explicit demo configuration." }
                require(supabase == null) { "DEMO_LOCAL must not contain Supabase configuration." }
            }

            AppEnvironmentMode.SUPABASE_STAGING -> {
                requireNotNull(supabase) {
                    "SUPABASE_STAGING requires external URL and publishable key configuration."
                }
                require(demo == null) { "SUPABASE_STAGING must not contain demo configuration." }
            }

            AppEnvironmentMode.PRODUCTION -> {
                requireNotNull(supabase) {
                    "PRODUCTION requires external URL and publishable key configuration."
                }
                require(demo == null) {
                    "PRODUCTION prohibits demo configuration and demo users."
                }
            }
        }
    }

    companion object {
        const val ENVIRONMENT_KEY: String = "SASE_APP_ENVIRONMENT"
        const val APP_VERSION_KEY: String = "SASE_APP_VERSION"
        const val SUPABASE_URL_KEY: String = "SASE_SUPABASE_URL"
        const val SUPABASE_PUBLISHABLE_KEY: String = "SASE_SUPABASE_PUBLISHABLE_KEY"

        const val DEMO_INSTITUTION_NAME: String =
            "INSTITUCIÓN ESCOLAR FICTICIA SASE — DEMO LOCAL SIN DATOS REALES"

        /** The only factory that enables demo users and synthetic demo data. */
        fun demoLocal(appVersion: String): AppEnvironment =
            AppEnvironment(
                mode = AppEnvironmentMode.DEMO_LOCAL,
                appVersion = requireConfiguredValue(APP_VERSION_KEY, appVersion),
                supabase = null,
                demo = DemoLocalConfiguration(
                    institutionDisplayName = DEMO_INSTITUTION_NAME,
                    institutionIsFictitious = true,
                    usesOnlySyntheticData = true,
                    demoUsersEnabled = true,
                ),
            )

        /** Parses only the supplied values; absence or invalidity never selects a fallback. */
        fun from(values: Map<String, String>): AppEnvironment = AppEnvironmentParser.parse(values)

        internal fun connected(
            mode: AppEnvironmentMode,
            appVersion: String,
            supabase: ExternalSupabaseConfiguration,
        ): AppEnvironment =
            AppEnvironment(
                mode = mode,
                appVersion = appVersion,
                supabase = supabase,
                demo = null,
            )
    }
}

class AppEnvironmentConfigurationException(message: String) : IllegalArgumentException(message)

/** Pure parser for host-provided string configuration. */
object AppEnvironmentParser {
    fun parse(values: Map<String, String>): AppEnvironment {
        val rawMode = values.required(AppEnvironment.ENVIRONMENT_KEY)
        val mode = AppEnvironmentMode.entries.firstOrNull { it.name == rawMode }
            ?: throw AppEnvironmentConfigurationException(
                "Invalid ${AppEnvironment.ENVIRONMENT_KEY} value '$rawMode'. " +
                    "Expected one of ${AppEnvironmentMode.entries.joinToString { it.name }}. " +
                    "No fallback will be applied.",
            )
        val appVersion = values.required(AppEnvironment.APP_VERSION_KEY)

        return when (mode) {
            AppEnvironmentMode.DEMO_LOCAL -> AppEnvironment.demoLocal(appVersion)
            AppEnvironmentMode.SUPABASE_STAGING,
            AppEnvironmentMode.PRODUCTION -> AppEnvironment.connected(
                mode = mode,
                appVersion = appVersion,
                supabase = ExternalSupabaseConfiguration(
                    url = values.required(AppEnvironment.SUPABASE_URL_KEY),
                    publishableKey = values.required(AppEnvironment.SUPABASE_PUBLISHABLE_KEY),
                ),
            )
        }
    }
}

private fun Map<String, String>.required(key: String): String =
    requireConfiguredValue(key, this[key])

private fun requireConfiguredValue(key: String, value: String?): String {
    if (value == null) {
        throw AppEnvironmentConfigurationException(
            "Missing required configuration: $key. No fallback will be applied.",
        )
    }

    return value.trim().takeIf { it.isNotEmpty() }
        ?: throw AppEnvironmentConfigurationException(
            "Invalid configuration: $key must not be blank. No fallback will be applied.",
        )
}
