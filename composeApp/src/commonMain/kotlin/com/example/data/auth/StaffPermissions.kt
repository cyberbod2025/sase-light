package com.example.data.auth

/**
 * Areas funcionales de SASE. El acceso se decide por area, no por pantalla,
 * para que la matriz sobreviva a cambios de UI.
 */
enum class SaseArea {
    SESSION,
    PRE_SOLICITUD,
    SECRETARIA,
    ALTA_OFICIAL,
    EXPEDIENTE,
    CREDENCIAL,
    INDICADORES,
    TRABAJO_SOCIAL,
    SALUD,
    UDEII,
    DOCENCIA
}

/** Acciones de escritura que requieren una autorizacion adicional a la ruta. */
enum class StaffAction {
    CREATE_STUDENT,
    UPDATE_STUDENT,
    ADD_OBSERVATION,
    REPORT_INCIDENT,
    ADVANCE_INCIDENT,
    ESCALATE_CASE
}

/**
 * Matriz de acceso rol -> areas. Es la fuente de verdad en cliente; la base
 * de datos la replica con RLS (ver supabase/migrations).
 */
object StaffPermissions {

    private val matrix: Map<StaffRole, Set<SaseArea>> = mapOf(
        StaffRole.DIRECCION to setOf(
            SaseArea.SESSION,
            SaseArea.INDICADORES,
            SaseArea.EXPEDIENTE,
            SaseArea.SECRETARIA,
            SaseArea.ALTA_OFICIAL,
            SaseArea.CREDENCIAL
        ),
        StaffRole.SECRETARIA to setOf(
            SaseArea.SESSION,
            SaseArea.PRE_SOLICITUD,
            SaseArea.SECRETARIA,
            SaseArea.ALTA_OFICIAL,
            SaseArea.EXPEDIENTE,
            SaseArea.CREDENCIAL
        ),
        // Hasta que existan vistas segmentadas por campo, estos roles no
        // reciben EXPEDIENTE: la pantalla actual incluye salud, familia y
        // seguimiento disciplinario en un solo documento.
        StaffRole.TRABAJO_SOCIAL to setOf(SaseArea.SESSION, SaseArea.TRABAJO_SOCIAL),
        StaffRole.MEDICO_ESCOLAR to setOf(SaseArea.SESSION, SaseArea.SALUD),
        StaffRole.UDEII to setOf(SaseArea.SESSION, SaseArea.UDEII),
        StaffRole.DOCENTE to setOf(SaseArea.SESSION, SaseArea.DOCENCIA)
    )

    private val actionMatrix: Map<StaffRole, Set<StaffAction>> = mapOf(
        StaffRole.DIRECCION to StaffAction.entries.toSet(),
        StaffRole.SECRETARIA to setOf(
            StaffAction.CREATE_STUDENT,
            StaffAction.UPDATE_STUDENT,
            StaffAction.ADD_OBSERVATION,
            StaffAction.REPORT_INCIDENT,
            StaffAction.ADVANCE_INCIDENT,
            StaffAction.ESCALATE_CASE
        ),
        StaffRole.TRABAJO_SOCIAL to emptySet(),
        StaffRole.MEDICO_ESCOLAR to emptySet(),
        StaffRole.UDEII to emptySet(),
        StaffRole.DOCENTE to emptySet()
    )

    fun areasFor(role: StaffRole): Set<SaseArea> = matrix[role].orEmpty()

    fun canAccess(role: StaffRole, area: SaseArea): Boolean = area in areasFor(role)

    fun canAccess(session: AuthSession?, area: SaseArea): Boolean {
        val profile = session?.profile ?: return false
        return profile.active && area in session.permissions
    }

    fun canPerform(role: StaffRole, action: StaffAction): Boolean =
        action in actionMatrix[role].orEmpty()

    fun canPerform(session: AuthSession?, action: StaffAction): Boolean {
        val activeSession = session ?: return false
        return activeSession.profile.active && canPerform(activeSession.activeRole, action)
    }
}
