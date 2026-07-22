package com.example.data.auth

/**
 * Areas funcionales de SASE. El acceso se decide por area, no por pantalla,
 * para que la matriz sobreviva a cambios de UI.
 */
enum class SaseArea {
    PRE_SOLICITUD,
    SECRETARIA,
    ALTA_OFICIAL,
    EXPEDIENTE,
    CREDENCIAL,
    INDICADORES,
    TRABAJO_SOCIAL,
    SALUD,
    UDEII
}

/**
 * Matriz de acceso rol -> areas. Es la fuente de verdad en cliente; la base
 * de datos la replica con RLS (ver supabase/migrations).
 */
object StaffPermissions {

    private val matrix: Map<StaffRole, Set<SaseArea>> = mapOf(
        StaffRole.DIRECCION to setOf(
            SaseArea.INDICADORES,
            SaseArea.EXPEDIENTE,
            SaseArea.SECRETARIA,
            SaseArea.ALTA_OFICIAL,
            SaseArea.CREDENCIAL
        ),
        StaffRole.SECRETARIA to setOf(
            SaseArea.PRE_SOLICITUD,
            SaseArea.SECRETARIA,
            SaseArea.ALTA_OFICIAL,
            SaseArea.EXPEDIENTE,
            SaseArea.CREDENCIAL
        ),
        StaffRole.TRABAJO_SOCIAL to setOf(SaseArea.EXPEDIENTE, SaseArea.TRABAJO_SOCIAL),
        StaffRole.MEDICO_ESCOLAR to setOf(SaseArea.SALUD),
        StaffRole.UDEII to setOf(SaseArea.EXPEDIENTE, SaseArea.UDEII),
        StaffRole.DOCENTE to setOf(SaseArea.EXPEDIENTE)
    )

    fun areasFor(role: StaffRole): Set<SaseArea> = matrix[role].orEmpty()

    fun canAccess(role: StaffRole, area: SaseArea): Boolean = area in areasFor(role)

    fun canAccess(session: AuthSession?, area: SaseArea): Boolean {
        val profile = session?.profile ?: return false
        return profile.active && canAccess(profile.role, area)
    }
}
