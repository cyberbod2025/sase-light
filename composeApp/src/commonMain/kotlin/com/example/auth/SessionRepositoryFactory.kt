package com.example.auth

/** Backends de autenticación disponibles. MOCK es el predeterminado. */
enum class AuthBackend {
    MOCK,
    SUPABASE
}

/** Razón no sensible por la que un backend no puede proveerse. */
enum class BackendUnavailableReason {
    CONFIGURATION_MISSING,
    CONFIGURATION_INVALID
}

/**
 * Resultado explícito de la provisión: pedir SUPABASE sin configuración
 * produce [Unavailable] — jamás un fallback silencioso a mock.
 */
sealed interface SessionRepositoryProvision {
    data class Ready(
        val backend: AuthBackend,
        val repository: SessionRepository
    ) : SessionRepositoryProvision

    data class Unavailable(
        val backend: AuthBackend,
        val reason: BackendUnavailableReason
    ) : SessionRepositoryProvision
}

/**
 * Selección explícita del backend de sesión, sin estado global mutable:
 * cada invocación devuelve instancias nuevas e independientes.
 */
/**
 * Cableado real por defecto: gateways Supabase sobre un cliente perezoso.
 * Construirlo NO crea el cliente ni efectúa red; eso ocurre solo al usarlo.
 */
internal fun defaultSupabaseSessionRepository(
    configuration: SupabaseConfiguration
): SessionRepository {
    val gateways = SupabaseStaffGateways(SupabaseClientProvider(configuration))
    return SupabaseSessionRepository(authGateway = gateways, directoryGateway = gateways)
}

object SessionRepositoryFactory {

    fun create(
        backend: AuthBackend = AuthBackend.MOCK,
        configuration: SupabaseConfigurationState = SupabaseConfigurationState.NotConfigured,
        supabaseRepositoryBuilder: (SupabaseConfiguration) -> SessionRepository =
            ::defaultSupabaseSessionRepository
    ): SessionRepositoryProvision = when (backend) {
        AuthBackend.MOCK ->
            SessionRepositoryProvision.Ready(AuthBackend.MOCK, MockSessionRepository())

        AuthBackend.SUPABASE -> when (configuration) {
            is SupabaseConfigurationState.NotConfigured ->
                SessionRepositoryProvision.Unavailable(
                    AuthBackend.SUPABASE,
                    BackendUnavailableReason.CONFIGURATION_MISSING
                )

            is SupabaseConfigurationState.Invalid ->
                SessionRepositoryProvision.Unavailable(
                    AuthBackend.SUPABASE,
                    BackendUnavailableReason.CONFIGURATION_INVALID
                )

            is SupabaseConfigurationState.Configured ->
                SessionRepositoryProvision.Ready(
                    AuthBackend.SUPABASE,
                    supabaseRepositoryBuilder(configuration.configuration)
                )
        }
    }
}
