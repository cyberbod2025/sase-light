package com.example.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Proveedor perezoso del cliente Supabase: el cliente solo se construye en el
 * primer uso real, nunca al arrancar la aplicación ni durante pruebas que no
 * lo consumen. No es un singleton global: cada provider posee su cliente.
 * Solo se instalan Auth y Postgrest (sin Storage, Realtime ni Functions).
 */
class SupabaseClientProvider(
    private val configuration: SupabaseConfiguration
) {
    private val lazyClient = lazy {
        createSupabaseClient(
            supabaseUrl = configuration.url,
            supabaseKey = configuration.anonKey
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    /** El primer acceso construye el cliente (sin efectuar llamadas de red). */
    val client: SupabaseClient
        get() = lazyClient.value

    /** Visible para pruebas: garantiza que nada creó el cliente antes de tiempo. */
    val isClientCreated: Boolean
        get() = lazyClient.isInitialized()

    /** Cierre correcto de recursos si el cliente llegó a crearse. */
    suspend fun close() {
        if (lazyClient.isInitialized()) {
            lazyClient.value.close()
        }
    }
}
