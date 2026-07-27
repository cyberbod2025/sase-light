package com.example.viewmodel

import com.example.data.auth.AuthSession
import com.example.data.auth.SaseArea
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole

/**
 * Politica pura Screen -> SaseArea -> StaffRole. `canOpenScreen` gatea
 * `LabViewModel.navigateTo` (M3) y `visibleSidebarItems` gatea que items del
 * sidebar se muestran (M1). SaseAppContent y el selector mock (AppRole)
 * todavia no consumen esta politica.
 */

/**
 * Area institucional que protege cada Screen. `null` es una decision
 * explicita, no una omision: hoy no existe una via de acceso de staff para
 * esa pantalla.
 */
fun screenArea(screen: Screen): SaseArea? = when (screen) {
    is Screen.SecretaryDashboard -> SaseArea.SECRETARIA
    is Screen.StudentRecordsDashboard -> SaseArea.EXPEDIENTE
    is Screen.StudentRecord -> SaseArea.EXPEDIENTE
    is Screen.EnrollmentDashboard -> SaseArea.ALTA_OFICIAL
    is Screen.SecretariaPreApplicationDashboard -> SaseArea.PRE_SOLICITUD
    is Screen.OfficialEnrollmentDashboard -> SaseArea.ALTA_OFICIAL
    is Screen.CredentialPreview -> SaseArea.CREDENCIAL
    is Screen.StudentCredentialDashboard -> SaseArea.CREDENCIAL
    // Portal de la familia: pensado como acceso publico (URL/QR) sin sesion
    // de staff, pero SaseAppContent todavia lo gatea igual que el resto de
    // Screen detras del login. No tiene area institucional de staff; se
    // niega aqui explicitamente para no fingir una autorizacion que la
    // compuerta real no otorga todavia (ver canOpenScreenDeniesFamilyPortalCase).
    is Screen.PreApplicationFamilyPortal -> null
}

/**
 * Autoriza una Screen exclusivamente a partir de la sesion autenticada: sin
 * sesion, con perfil inactivo, o sin area mapeada en [screenArea], se niega.
 * No consulta AppRole en ningun caso.
 */
fun canOpenScreen(session: AuthSession?, screen: Screen): Boolean {
    val area = screenArea(screen) ?: return false
    return StaffPermissions.canAccess(session, area)
}

/**
 * Orden canonico y claves de autorizacion del sidebar de Secretaria.
 */
val secretarySidebarItemNames: List<String> = listOf(
    "Inicio",
    "Expedientes",
    "Inscripciones",
    "Portal Familia",
    "Pre-Solicitudes",
    "Altas Oficiales",
    "Credenciales"
)

/**
 * Items del sidebar autorizados para la sesion actual: se resuelven a su
 * Screen destino (via [secretarySidebarDestination]) y se filtran con
 * [canOpenScreen]. "Portal Familia" no tiene area de staff ([screenArea]
 * retorna null para [Screen.PreApplicationFamilyPortal]) y por tanto nunca
 * aparece aqui, para ningun rol: coincide con que su navegacion ya esta
 * bloqueada por el guard de M3, no es una restriccion nueva de este item.
 */
fun visibleSidebarItems(session: AuthSession?): List<String> =
    secretarySidebarItemNames.filter { item ->
        secretarySidebarDestination(item)?.let { screen -> canOpenScreen(session, screen) } ?: false
    }

// Candidatas a pantalla de inicio, en orden de prioridad. Un rol recibe la
// primera cuya area le pertenezca segun StaffPermissions.areasFor. Lista
// deliberadamente corta hoy: crecera cuando existan pantallas dedicadas a
// otras areas (p. ej. una futura pantalla de Salud para MEDICO_ESCOLAR).
private val homeScreenCandidates: List<Screen> = listOf(
    Screen.SecretaryDashboard,
    Screen.StudentRecordsDashboard
)

/**
 * Pantalla de inicio autorizada para un rol. `null` es una decision
 * explicita: el rol no tiene ninguna Screen candidata dentro de sus areas
 * permitidas (hoy es el caso de MEDICO_ESCOLAR, cuya unica area es SALUD y
 * que todavia no tiene pantalla propia).
 */
fun homeScreenFor(role: StaffRole): Screen? {
    val areas = StaffPermissions.areasFor(role)
    return homeScreenCandidates.firstOrNull { candidate -> screenArea(candidate) in areas }
}
