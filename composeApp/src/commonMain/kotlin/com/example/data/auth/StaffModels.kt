package com.example.data.auth

/**
 * Roles institucionales de la Secundaria 310. Cada rol ve solo lo que su
 * funcion requiere; la matriz de acceso vive en [StaffPermissions].
 */
enum class StaffRole {
    DIRECCION,
    SECRETARIA,
    TRABAJO_SOCIAL,
    MEDICO_ESCOLAR,
    UDEII,
    DOCENTE
}

/**
 * Perfil de personal. No contiene datos de alumnos ni de familias: es la
 * primera etapa del backend real (staff antes que datos de menores).
 */
data class StaffProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val role: StaffRole,
    val active: Boolean = true
)

data class AuthSession(
    val profile: StaffProfile,
    val accessToken: String
)

sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Failure(val reason: AuthFailureReason) : AuthResult()
}

enum class AuthFailureReason {
    INVALID_CREDENTIALS,
    INACTIVE_ACCOUNT,
    NO_STAFF_PROFILE,
    NETWORK
}
