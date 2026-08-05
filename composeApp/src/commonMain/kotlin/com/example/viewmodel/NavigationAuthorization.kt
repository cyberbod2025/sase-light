package com.example.viewmodel

import com.example.data.auth.AuthSession
import com.example.data.auth.SaseArea
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole

/**
 * Politica pura Screen -> SaseArea -> AuthSession. `canOpenScreen` gatea
 * `LabViewModel.navigateTo` (M3), `visibleSidebarItems` gatea que items del
 * sidebar se muestran (M1), y [authorizedScreenFor] impide renderizar una
 * pantalla no autorizada en SaseAppContent (M4). Toda decision parte de
 * [AuthSession]: el rol activo y sus permisos efectivos viajan juntos en la
 * sesion institucional; no existe un selector de rol paralelo en la UI.
 */

/**
 * Area institucional que protege cada Screen. `null` es una decision
 * explicita, no una omision: hoy no existe una via de acceso de staff para
 * esa pantalla.
 */
fun screenArea(screen: Screen): SaseArea? = when (screen) {
    is Screen.SessionHome -> SaseArea.SESSION
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
 * No consulta ningun selector de rol paralelo a la sesion.
 */
fun canOpenScreen(session: AuthSession?, screen: Screen): Boolean {
    val area = screenArea(screen) ?: return false
    return StaffPermissions.canAccess(session, area)
}

/**
 * Resuelve la unica pantalla que la UI puede renderizar. Conserva el destino
 * solicitado cuando esta autorizado; en caso contrario usa el home del rol.
 * Si no hay sesion activa o el rol aun no tiene home, retorna null para que la
 * UI falle cerrada sin mostrar contenido institucional.
 */
fun authorizedScreenFor(session: AuthSession?, requestedScreen: Screen): Screen? {
    val activeSession = session?.takeIf { it.profile.active } ?: return null
    if (canOpenScreen(session, requestedScreen)) return requestedScreen
    return homeScreenFor(activeSession)
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

/**
 * Home seguro para cualquier rol institucional. No presupone acceso a
 * expedientes, secretaria, salud u otra area operativa.
 */
fun homeScreenFor(role: StaffRole): Screen? =
    Screen.SessionHome.takeIf { StaffPermissions.canAccess(role, SaseArea.SESSION) }

/**
 * Variante autoritativa durante ejecucion: parte de la sesion efectiva y
 * valida el permiso SESSION que corresponde a su rol activo. Un perfil
 * inactivo o una sesion inconsistente falla cerrada.
 */
fun homeScreenFor(session: AuthSession?): Screen? {
    val activeSession = session?.takeIf { it.profile.active } ?: return null
    val home = homeScreenFor(activeSession.activeRole) ?: return null
    return home.takeIf { canOpenScreen(activeSession, it) }
}
