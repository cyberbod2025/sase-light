package com.example.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SessionRepositoryFactoryTest {

    @Test
    fun defaultBackendIsMock() {
        val provision = SessionRepositoryFactory.create()
        val ready = assertIs<SessionRepositoryProvision.Ready>(provision)
        assertEquals(AuthBackend.MOCK, ready.backend)
        assertIs<MockSessionRepository>(ready.repository)
        assertIs<AuthState.NoSession>(ready.repository.authState.value)
    }

    @Test
    fun explicitMockBackendProvidesMock() {
        val ready = assertIs<SessionRepositoryProvision.Ready>(
            SessionRepositoryFactory.create(backend = AuthBackend.MOCK)
        )
        assertIs<MockSessionRepository>(ready.repository)
    }

    @Test
    fun supabaseWithoutConfigurationIsUnavailableNotSilentFallback() {
        val provision = SessionRepositoryFactory.create(
            backend = AuthBackend.SUPABASE,
            configuration = SupabaseConfigurationState.NotConfigured
        )
        val unavailable = assertIs<SessionRepositoryProvision.Unavailable>(provision)
        assertEquals(AuthBackend.SUPABASE, unavailable.backend)
        assertEquals(BackendUnavailableReason.CONFIGURATION_MISSING, unavailable.reason)
    }

    @Test
    fun supabaseWithInvalidConfigurationIsUnavailable() {
        val provision = SessionRepositoryFactory.create(
            backend = AuthBackend.SUPABASE,
            configuration = SupabaseConfigurationState.Invalid(
                SupabaseConfigurationProblem.INSECURE_URL
            )
        )
        val unavailable = assertIs<SessionRepositoryProvision.Unavailable>(provision)
        assertEquals(BackendUnavailableReason.CONFIGURATION_INVALID, unavailable.reason)
    }

    @Test
    fun supabaseWithConfigurationProvidesInjectedRepository() {
        val configured = SupabaseConfigurationState.Configured(
            SupabaseConfiguration("https://demo.supabase.co", "sb_publishable_demo_key")
        )
        val injected = MockSessionRepository()
        var builderReceived: SupabaseConfiguration? = null
        val provision = SessionRepositoryFactory.create(
            backend = AuthBackend.SUPABASE,
            configuration = configured,
            supabaseRepositoryBuilder = { config ->
                builderReceived = config
                injected
            }
        )
        val ready = assertIs<SessionRepositoryProvision.Ready>(provision)
        assertEquals(AuthBackend.SUPABASE, ready.backend)
        assertSame(injected, ready.repository)
        assertEquals(configured.configuration, builderReceived)
    }

    @Test
    fun defaultSupabaseWiringDoesNotCreateClientEagerly() {
        // Construir la provisión real no debe crear el cliente ni tocar red.
        val provider = SupabaseClientProvider(
            SupabaseConfiguration("https://demo.supabase.co", "sb_publishable_demo_key")
        )
        assertTrue(!provider.isClientCreated)
        val gateways = SupabaseStaffGateways(provider)
        val repository = SupabaseSessionRepository(gateways, gateways)
        assertIs<AuthState.NoSession>(repository.authState.value)
        assertTrue(!provider.isClientCreated, "El cliente no debe existir antes de usarse")
    }

    @Test
    fun eachProvisionIsIndependent() {
        val first = assertIs<SessionRepositoryProvision.Ready>(SessionRepositoryFactory.create())
        val second = assertIs<SessionRepositoryProvision.Ready>(SessionRepositoryFactory.create())
        assertNotSame(first.repository, second.repository)

        (first.repository as MockSessionRepository).signInAsDemo(DemoStaffIdentity.SECRETARIA_DEMO)
        assertIs<AuthState.Active>(first.repository.authState.value)
        assertIs<AuthState.NoSession>(second.repository.authState.value)
    }
}
