package com.example.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Configuración de conexión a Supabase. Solo transporta la URL del proyecto y
 * la clave publicable (anon); las claves de servicio se rechazan en la
 * validación. Nunca contiene valores reales en el repositorio: los valores se
 * inyectan explícitamente en tiempo de ejecución.
 */
data class SupabaseConfiguration(
    val url: String,
    val anonKey: String
) {
    /** Representación segura: jamás imprime la clave. */
    override fun toString(): String = "SupabaseConfiguration(url=$url, anonKey=***)"
}

/** Problemas de validación de configuración, sin exponer valores. */
enum class SupabaseConfigurationProblem {
    MISSING_URL,
    MISSING_KEY,
    MALFORMED_URL,
    INSECURE_URL,
    SERVICE_ROLE_KEY_REJECTED
}

/**
 * Estado explícito de la configuración: la ausencia o invalidez son estados
 * de primera clase — nunca provocan una llamada de red ni un fallback mudo.
 */
sealed interface SupabaseConfigurationState {
    data object NotConfigured : SupabaseConfigurationState

    data class Invalid(val problem: SupabaseConfigurationProblem) : SupabaseConfigurationState

    data class Configured(val configuration: SupabaseConfiguration) : SupabaseConfigurationState

    companion object {
        /**
         * Construye el estado desde valores crudos (p. ej. variables de
         * entorno). null/vacío ⇒ NotConfigured; valores presentes pero
         * inválidos ⇒ Invalid; nunca lanza excepciones.
         */
        fun from(url: String?, anonKey: String?): SupabaseConfigurationState {
            val trimmedUrl = url?.trim().orEmpty()
            val trimmedKey = anonKey?.trim().orEmpty()
            if (trimmedUrl.isEmpty() && trimmedKey.isEmpty()) return NotConfigured
            if (trimmedUrl.isEmpty()) return Invalid(SupabaseConfigurationProblem.MISSING_URL)
            if (trimmedKey.isEmpty()) return Invalid(SupabaseConfigurationProblem.MISSING_KEY)
            if (!trimmedUrl.startsWith("https://")) {
                return Invalid(SupabaseConfigurationProblem.INSECURE_URL)
            }
            if (trimmedUrl.length <= "https://".length || !trimmedUrl.removePrefix("https://").contains('.')) {
                return Invalid(SupabaseConfigurationProblem.MALFORMED_URL)
            }
            if (looksLikeServiceRoleKey(trimmedKey)) {
                return Invalid(SupabaseConfigurationProblem.SERVICE_ROLE_KEY_REJECTED)
            }
            return Configured(SupabaseConfiguration(trimmedUrl, trimmedKey))
        }

        /**
         * Guardia best-effort: una clave JWT cuyo payload declara
         * role=service_role jamás debe llegar a un cliente. Si la clave no es
         * un JWT legible, no se asume nada (las claves publicables nuevas no
         * son JWT).
         */
        @OptIn(ExperimentalEncodingApi::class)
        internal fun looksLikeServiceRoleKey(key: String): Boolean {
            val segments = key.split('.')
            if (segments.size != 3) return false
            return try {
                val payload = Base64.UrlSafe.decode(padBase64(segments[1])).decodeToString()
                payload.contains("\"role\"") && payload.contains("service_role")
            } catch (_: Throwable) {
                false
            }
        }

        private fun padBase64(value: String): String {
            val remainder = value.length % 4
            return if (remainder == 0) value else value + "=".repeat(4 - remainder)
        }
    }
}
