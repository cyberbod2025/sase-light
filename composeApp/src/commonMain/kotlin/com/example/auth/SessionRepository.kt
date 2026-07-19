package com.example.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio de sesión con operaciones de dominio (no CRUD genérico).
 * En esta fase solo existe la implementación mock; una implementación real
 * (Supabase Auth) se conectará detrás de esta misma interfaz en fases
 * posteriores sin tocar a los consumidores.
 */
interface SessionRepository {
    /** Estado de autenticación observable. */
    val authState: StateFlow<AuthState>

    /** Inicia una sesión demo determinista por identidad institucional. */
    fun signInAsDemo(identity: DemoStaffIdentity): AuthState

    /** Cierra la sesión activa. */
    fun signOut()

    /** Restaura el estado inicial (sin sesión) para aislamiento entre pruebas. */
    fun resetForTests()
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
