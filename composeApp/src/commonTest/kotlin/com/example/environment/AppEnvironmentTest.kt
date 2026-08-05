package com.example.environment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppEnvironmentTest {
    @Test
    fun demoLocalFactoryIsExplicitlyFictitiousAndSynthetic() {
        val environment = AppEnvironment.demoLocal(appVersion = "1.0.0-test")

        assertEquals(AppEnvironmentMode.DEMO_LOCAL, environment.mode)
        assertEquals("1.0.0-test", environment.appVersion)
        assertEquals(AppEnvironment.DEMO_INSTITUTION_NAME, environment.demo?.institutionDisplayName)
        assertTrue(environment.demo?.institutionIsFictitious == true)
        assertTrue(environment.isDemo)
        assertTrue(environment.demoUsersEnabled)
        assertTrue(environment.usesSyntheticData)
        assertNull(environment.supabase)
    }

    @Test
    fun parserBuildsDemoOnlyWhenItIsExplicitlyRequested() {
        val environment = AppEnvironment.from(
            mapOf(
                AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.DEMO_LOCAL.name,
                AppEnvironment.APP_VERSION_KEY to "1.0.0-test",
            ),
        )

        assertEquals(AppEnvironmentMode.DEMO_LOCAL, environment.mode)
        assertTrue(environment.isDemo)
    }

    @Test
    fun validStagingUsesOnlyExternalSupabaseConfiguration() {
        val environment = AppEnvironment.from(
            connectedValues(mode = AppEnvironmentMode.SUPABASE_STAGING),
        )

        assertEquals(AppEnvironmentMode.SUPABASE_STAGING, environment.mode)
        assertEquals("https://external.example.invalid", environment.supabase?.url)
        assertEquals("publishable-key-placeholder", environment.supabase?.publishableKey)
        assertFalse(environment.isDemo)
        assertFalse(environment.demoUsersEnabled)
        assertTrue(environment.usesSyntheticData)
        assertNull(environment.demo)
    }

    @Test
    fun productionRequiresExternalConfigurationAndCannotBeDemo() {
        val environment = AppEnvironment.from(
            connectedValues(mode = AppEnvironmentMode.PRODUCTION),
        )

        assertEquals(AppEnvironmentMode.PRODUCTION, environment.mode)
        assertEquals("1.0.0-test", environment.appVersion)
        assertFalse(environment.isDemo)
        assertFalse(environment.demoUsersEnabled)
        assertFalse(environment.usesSyntheticData)
        assertNull(environment.demo)
    }

    @Test
    fun stagingWithoutExternalConfigurationFailsInsteadOfFallingBack() {
        val error = assertFailsWith<AppEnvironmentConfigurationException> {
            AppEnvironment.from(
                mapOf(
                    AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.SUPABASE_STAGING.name,
                    AppEnvironment.APP_VERSION_KEY to "1.0.0-test",
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains(AppEnvironment.SUPABASE_URL_KEY))
        assertTrue(error.message.orEmpty().contains("No fallback"))
    }

    @Test
    fun stagingWithoutPublishableKeyFailsInsteadOfFallingBack() {
        val error = assertFailsWith<AppEnvironmentConfigurationException> {
            AppEnvironment.from(
                mapOf(
                    AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.SUPABASE_STAGING.name,
                    AppEnvironment.APP_VERSION_KEY to "1.0.0-test",
                    AppEnvironment.SUPABASE_URL_KEY to "https://external.example.invalid",
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains(AppEnvironment.SUPABASE_PUBLISHABLE_KEY))
        assertTrue(error.message.orEmpty().contains("No fallback"))
    }

    @Test
    fun productionWithoutExternalConfigurationFailsInsteadOfFallingBack() {
        val error = assertFailsWith<AppEnvironmentConfigurationException> {
            AppEnvironment.from(
                mapOf(
                    AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.PRODUCTION.name,
                    AppEnvironment.APP_VERSION_KEY to "1.0.0-test",
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains(AppEnvironment.SUPABASE_URL_KEY))
        assertTrue(error.message.orEmpty().contains("No fallback"))
    }

    @Test
    fun missingModeFailsInsteadOfSelectingDemo() {
        val error = assertFailsWith<AppEnvironmentConfigurationException> {
            AppEnvironment.from(
                mapOf(AppEnvironment.APP_VERSION_KEY to "1.0.0-test"),
            )
        }

        assertTrue(error.message.orEmpty().contains(AppEnvironment.ENVIRONMENT_KEY))
        assertTrue(error.message.orEmpty().contains("No fallback"))
    }

    @Test
    fun unknownModeFailsInsteadOfSelectingDemo() {
        val error = assertFailsWith<AppEnvironmentConfigurationException> {
            AppEnvironment.from(
                mapOf(
                    AppEnvironment.ENVIRONMENT_KEY to "UNKNOWN",
                    AppEnvironment.APP_VERSION_KEY to "1.0.0-test",
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains("UNKNOWN"))
        assertTrue(error.message.orEmpty().contains("No fallback"))
    }

    private fun connectedValues(mode: AppEnvironmentMode): Map<String, String> =
        mapOf(
            AppEnvironment.ENVIRONMENT_KEY to mode.name,
            AppEnvironment.APP_VERSION_KEY to "1.0.0-test",
            AppEnvironment.SUPABASE_URL_KEY to "https://external.example.invalid",
            AppEnvironment.SUPABASE_PUBLISHABLE_KEY to "publishable-key-placeholder",
        )
}
