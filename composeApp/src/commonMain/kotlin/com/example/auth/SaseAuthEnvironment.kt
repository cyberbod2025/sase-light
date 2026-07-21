package com.example.auth

/**
 * Lectura de variables de entorno por plataforma. Existe para que la
 * configuración de Supabase NUNCA viva en el repositorio: ni URL, ni clave.
 */
expect object PlatformEnvironment {
    fun read(name: String): String?
}

/**
 * Selección del backend de sesión a partir del entorno.
 *
 * Variables (se definen fuera del repositorio; `.env*` ya está ignorado):
 *   SASE_AUTH_BACKEND     = MOCK | SUPABASE   (ausente ⇒ MOCK)
 *   SASE_SUPABASE_URL     = https://<proyecto>.supabase.co
 *   SASE_SUPABASE_ANON_KEY= clave publicable (jamás service_role)
 *
 * Sin configuración explícita el backend es MOCK. Pedir SUPABASE sin
 * configuración válida produce Unavailable, nunca un fallback silencioso.
 */
object SaseAuthEnvironment {

    const val BACKEND_VARIABLE = "SASE_AUTH_BACKEND"
    const val URL_VARIABLE = "SASE_SUPABASE_URL"
    const val ANON_KEY_VARIABLE = "SASE_SUPABASE_ANON_KEY"

    fun backend(read: (String) -> String? = PlatformEnvironment::read): AuthBackend =
        when (read(BACKEND_VARIABLE)?.trim()?.uppercase()) {
            "SUPABASE" -> AuthBackend.SUPABASE
            else -> AuthBackend.MOCK
        }

    fun configuration(read: (String) -> String? = PlatformEnvironment::read): SupabaseConfigurationState =
        SupabaseConfigurationState.from(read(URL_VARIABLE), read(ANON_KEY_VARIABLE))

    fun provision(read: (String) -> String? = PlatformEnvironment::read): SessionRepositoryProvision =
        SessionRepositoryFactory.create(
            backend = backend(read),
            configuration = configuration(read)
        )
}
