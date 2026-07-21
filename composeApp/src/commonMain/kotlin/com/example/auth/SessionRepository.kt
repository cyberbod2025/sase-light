package com.example.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio de sesión con operaciones de dominio (no CRUD genérico).
 * Contrato base común a las implementaciones mock (demo) y real (Supabase);
 * los consumidores (LabViewModel, DI) solo dependen de este contrato.
 */
interface SessionRepository {
    /** Estado de autenticación observable. */
    val authState: StateFlow<AuthState>

    /** Cierra la sesión activa localmente (revocación remota: ver [CredentialSessionRepository]). */
    fun signOut()

    /** Restaura el estado inicial (sin sesión) para aislamiento entre pruebas. */
    fun resetForTests()
}

/**
 * Capacidad de sesión demo: exclusiva de implementaciones mock. El backend
 * real nunca la ofrece — así no existe una vía "demo" hacia datos reales.
 */
interface DemoSessionRepository : SessionRepository {
    /** Inicia una sesión demo determinista por identidad institucional. */
    fun signInAsDemo(identity: DemoStaffIdentity): AuthState
}

/**
 * Capacidad de autenticación real por credenciales del personal. La
 * implementación concreta vive en la capa de infraestructura (Supabase);
 * este contrato solo expone modelos de dominio y errores tipados.
 */
interface CredentialSessionRepository : SessionRepository {
    /** Autentica personal con email y contraseña. Denegaciones como valores, no excepciones. */
    suspend fun signInWithCredentials(email: String, password: String): StaffSignInResult

    /** Cierra sesión revocándola también en el backend. */
    suspend fun signOutWithRevocation()

    /** Intenta restaurar una sesión previa persistida por el backend. */
    suspend fun restoreSession(): AuthState
}

/**
 * Identidades demo sintéticas — sin correos reales, sin contraseñas, sin PII.
 * DOCENTE_TUTOR_DEMO demuestra la combinación de roles múltiples.
 */
enum class DemoStaffIdentity(
    val displayName: String,
    val roles: Set<InstitutionRole>
) {
    ADMIN_DEMO("PERSONAL DEMO ADMINISTRACION", setOf(InstitutionRole.ADMIN_INSTITUCIONAL)),
    DIRECCION_DEMO("PERSONAL DEMO DIRECCION", setOf(InstitutionRole.DIRECCION)),
    SECRETARIA_DEMO("PERSONAL DEMO SECRETARIA", setOf(InstitutionRole.SECRETARIA)),
    TRABAJO_SOCIAL_DEMO("PERSONAL DEMO TRABAJO SOCIAL", setOf(InstitutionRole.TRABAJO_SOCIAL)),
    MEDICO_DEMO("PERSONAL DEMO MEDICO ESCOLAR", setOf(InstitutionRole.MEDICO_ESCOLAR)),
    UDEII_DEMO("PERSONAL DEMO UDEII", setOf(InstitutionRole.UDEII)),
    DOCENTE_DEMO("PERSONAL DEMO DOCENTE", setOf(InstitutionRole.DOCENTE)),
    DOCENTE_TUTOR_DEMO(
        "PERSONAL DEMO DOCENTE TUTOR",
        setOf(InstitutionRole.DOCENTE, InstitutionRole.TUTOR)
    ),
    PREFECTURA_DEMO("PERSONAL DEMO PREFECTURA", setOf(InstitutionRole.PREFECTURA)),
    FAMILIA_DEMO("FAMILIA DEMO", setOf(InstitutionRole.FAMILIA))
}
