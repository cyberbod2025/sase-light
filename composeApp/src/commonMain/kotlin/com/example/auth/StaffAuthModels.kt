package com.example.auth

/**
 * Errores tipados de autenticación del personal. Razones no sensibles: nunca
 * transportan mensajes internos del SDK, tokens ni credenciales.
 */
enum class StaffAuthFailure {
    INVALID_CREDENTIALS,
    CONFIGURATION_MISSING,
    NETWORK_UNAVAILABLE,
    MEMBERSHIP_NOT_FOUND,
    MEMBERSHIP_INACTIVE,
    PROFILE_UNAVAILABLE,
    UNEXPECTED_FAILURE
}

/** Resultado explícito del inicio de sesión con credenciales. */
sealed interface StaffSignInResult {
    data class Success(val session: SessionContext) : StaffSignInResult
    data class Failure(val failure: StaffAuthFailure) : StaffSignInResult
}

/** Perfil mínimo del personal (espejo de `public.profiles`). */
data class StaffProfile(
    val userId: UserId,
    val displayName: String
)

/** Resultado del gateway de autenticación: solo identidad, jamás tokens. */
sealed interface StaffAuthGatewayResult {
    data class Authenticated(val userId: UserId) : StaffAuthGatewayResult
    data class Failed(val failure: StaffAuthFailure) : StaffAuthGatewayResult
}

/**
 * Frontera de infraestructura hacia el proveedor de identidad (Supabase Auth).
 * Expone únicamente modelos de dominio: los tipos del SDK no salen de la
 * implementación concreta.
 */
interface StaffAuthGateway {
    suspend fun signIn(email: String, password: String): StaffAuthGatewayResult

    suspend fun signOut()

    /** Identidad de una sesión previamente persistida por el proveedor, si existe. */
    suspend fun restoreUserId(): UserId?
}

/**
 * Frontera de infraestructura hacia el directorio institucional
 * (profiles / memberships / roles / scopes de la migración de Fase 1).
 * Los roles NUNCA provienen del cliente: se leen del directorio almacenado.
 */
interface StaffDirectoryGateway {
    suspend fun fetchProfile(userId: UserId): StaffProfile?

    suspend fun fetchMembership(userId: UserId): InstitutionMembership?
}
