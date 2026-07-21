package com.example.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SaseAuthEnvironmentTest {

    private fun env(vararg pairs: Pair<String, String?>): (String) -> String? {
        val values = pairs.toMap()
        return { values[it] }
    }

    @Test
    fun emptyEnvironmentKeepsMockBackend() {
        val provision = SaseAuthEnvironment.provision(env())
        val ready = assertIs<SessionRepositoryProvision.Ready>(provision)
        assertEquals(AuthBackend.MOCK, ready.backend)
    }

    @Test
    fun unknownBackendValueFallsBackToMockNotSupabase() {
        assertEquals(
            AuthBackend.MOCK,
            SaseAuthEnvironment.backend(env(SaseAuthEnvironment.BACKEND_VARIABLE to "produccion"))
        )
    }

    @Test
    fun supabaseWithoutConfigurationIsUnavailable() {
        val provision = SaseAuthEnvironment.provision(
            env(SaseAuthEnvironment.BACKEND_VARIABLE to "supabase")
        )
        val unavailable = assertIs<SessionRepositoryProvision.Unavailable>(provision)
        assertEquals(BackendUnavailableReason.CONFIGURATION_MISSING, unavailable.reason)
    }

    @Test
    fun supabaseWithInsecureUrlIsUnavailable() {
        val provision = SaseAuthEnvironment.provision(
            env(
                SaseAuthEnvironment.BACKEND_VARIABLE to "SUPABASE",
                SaseAuthEnvironment.URL_VARIABLE to "http://demo.supabase.co",
                SaseAuthEnvironment.ANON_KEY_VARIABLE to "sb_publishable_demo_key"
            )
        )
        assertEquals(
            BackendUnavailableReason.CONFIGURATION_INVALID,
            assertIs<SessionRepositoryProvision.Unavailable>(provision).reason
        )
    }

    @Test
    fun configuredSupabaseIsProvidedWithoutTouchingTheNetwork() {
        val provision = SaseAuthEnvironment.provision(
            env(
                SaseAuthEnvironment.BACKEND_VARIABLE to "SUPABASE",
                SaseAuthEnvironment.URL_VARIABLE to "https://demo.supabase.co",
                SaseAuthEnvironment.ANON_KEY_VARIABLE to "sb_publishable_demo_key"
            )
        )
        val ready = assertIs<SessionRepositoryProvision.Ready>(provision)
        assertEquals(AuthBackend.SUPABASE, ready.backend)
        assertIs<AuthState.NoSession>(ready.repository.authState.value)
    }

    @Test
    fun configurationIsReadFromTheEnvironmentNeverFromTheRepository() {
        // Los nombres de variable son el único acoplamiento; sin ellas no hay
        // URL ni clave en ningún sitio del código.
        assertEquals(
            SupabaseConfigurationState.NotConfigured,
            SaseAuthEnvironment.configuration(env())
        )
    }
}
