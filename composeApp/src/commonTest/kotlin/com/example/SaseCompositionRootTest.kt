package com.example

import com.example.environment.AppEnvironment
import com.example.environment.AppEnvironmentMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaseCompositionRootTest {
    @Test
    fun demoLocalEnablesOnlyExplicitDemoRepository() {
        val bootstrap = SaseCompositionRoot.create(AppEnvironment.demoLocal("test"))
        val ready = assertIs<SaseBootstrap.Ready>(bootstrap)

        assertEquals(AppEnvironmentMode.DEMO_LOCAL, ready.environment.mode)
        assertTrue(ready.viewModel.demoAccessAvailable)
    }

    @Test
    fun stagingRequiresExternalConfigurationAndNeverFallsBackToDemo() {
        val bootstrap = SaseCompositionRoot.create(
            mapOf(
                AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.SUPABASE_STAGING.name,
                AppEnvironment.APP_VERSION_KEY to "test"
            )
        )

        assertIs<SaseBootstrap.ConfigurationFailure>(bootstrap)
    }

    @Test
    fun stagingWithExternalValuesHasNoDemoAccess() {
        val bootstrap = SaseCompositionRoot.create(
            mapOf(
                AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.SUPABASE_STAGING.name,
                AppEnvironment.APP_VERSION_KEY to "test",
                AppEnvironment.SUPABASE_URL_KEY to "https://project-ref.supabase.co",
                AppEnvironment.SUPABASE_PUBLISHABLE_KEY to "publishable-test-key"
            )
        )
        val ready = assertIs<SaseBootstrap.Ready>(bootstrap)

        assertFalse(ready.viewModel.demoAccessAvailable)
    }

    @Test
    fun productionFailsClosedUntilPersistenceAndRlsAreValidated() {
        val environment = AppEnvironment.from(
            mapOf(
                AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.PRODUCTION.name,
                AppEnvironment.APP_VERSION_KEY to "test",
                AppEnvironment.SUPABASE_URL_KEY to "https://project-ref.supabase.co",
                AppEnvironment.SUPABASE_PUBLISHABLE_KEY to "publishable-test-key"
            )
        )

        val failure = assertIs<SaseBootstrap.ConfigurationFailure>(
            SaseCompositionRoot.create(environment)
        )
        assertTrue(failure.message.contains("RLS"))
    }

    @Test
    fun productionWithoutExternalConfigurationFailsWithoutDemoFallback() {
        val failure = SaseCompositionRoot.create(
            mapOf(
                AppEnvironment.ENVIRONMENT_KEY to AppEnvironmentMode.PRODUCTION.name,
                AppEnvironment.APP_VERSION_KEY to "test"
            )
        )

        assertIs<SaseBootstrap.ConfigurationFailure>(failure)
    }
}
