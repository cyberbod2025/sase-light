package com.example.viewmodel

import com.example.data.auth.AuthSession
import com.example.data.auth.StaffRole
import com.example.environment.AppEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * M2: navigateTo() consulta canOpenScreen(session.value, destino) antes de
 * mutar currentScreen. Una navegacion no autorizada conserva la pantalla
 * vigente, sin excepcion y sin redirigir. Solo la sesion autenticada decide.
 */
class LabViewModelNavigateToAuthorizationTest {

    // Fija una sesion directamente (sin signIn/coroutinas): estas pruebas son
    // de la politica de gating, no del flujo de login (ya cubierto en
    // LabViewModelAuthTest). Tambien permite fijar un perfil inactivo, algo
    // que el signIn real de MockAuthRepositoryImpl nunca deja pasar.
    // FixedSessionAuthRepository/testSessionFor viven en TestAuthSessions.kt.

    private fun sessionFor(role: StaffRole, active: Boolean = true): AuthSession =
        testSessionFor(role = role, active = active)

    // Dispatchers.Unconfined: estas pruebas no son runTest (no hay
    // testScheduler); revalidateSession() puede expirar la sesion inactiva y
    // lanzar authScope.launch { ... } de verdad (expireSession -> signOut),
    // lo que con el MainScope() por defecto fallaria por falta de
    // Dispatchers.Main en un test JVM plano.
    private fun viewModelWithSession(session: AuthSession?): LabViewModel =
        LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("test"),
            authRepository = FixedSessionAuthRepository(session),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

    @Test
    fun authorizedSessionNavigatesToDestination() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))

        vm.navigateTo(Screen.StudentRecordsDashboard)

        assertIs<Screen.StudentRecordsDashboard>(vm.currentScreen.value)
    }

    @Test
    fun unauthorizedRoleDoesNotNavigate() {
        // Trabajo Social no tiene el area PRE_SOLICITUD en StaffPermissions.matrix.
        val vm = viewModelWithSession(sessionFor(StaffRole.TRABAJO_SOCIAL))

        vm.navigateTo(Screen.SecretariaPreApplicationDashboard)

        assertIs<Screen.SessionHome>(vm.currentScreen.value)
    }

    @Test
    fun noSessionDoesNotNavigate() {
        val vm = viewModelWithSession(session = null)

        vm.navigateTo(Screen.StudentRecordsDashboard)

        assertIs<Screen.SessionHome>(vm.currentScreen.value)
    }

    @Test
    fun inactiveProfileDoesNotNavigate() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA, active = false))

        vm.navigateTo(Screen.StudentRecordsDashboard)

        assertIs<Screen.SessionHome>(vm.currentScreen.value)
    }

    @Test
    fun preApplicationFamilyPortalDoesNotOpenThroughThisFlow() {
        // No se concede acceso especial: screenArea sigue null para esta ruta
        // incluso con una sesion de staff activa y autorizada en otras areas.
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))

        vm.navigateTo(Screen.PreApplicationFamilyPortal)

        assertIs<Screen.SessionHome>(vm.currentScreen.value)
    }

    @Test
    fun rejectedNavigationPreservesThePreviousScreen() {
        val vm = viewModelWithSession(sessionFor(StaffRole.DIRECCION))
        vm.navigateTo(Screen.SecretaryDashboard)
        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)

        // Dirección no tiene PRE_SOLICITUD: el intento se ignora en silencio.
        vm.navigateTo(Screen.SecretariaPreApplicationDashboard)

        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)
    }

    @Test
    fun navigateBackReturnsToTheAuthenticatedRoleHome() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))
        vm.navigateTo(Screen.StudentRecord(studentId = "STU-001"))

        vm.navigateBack()

        assertIs<Screen.SessionHome>(vm.currentScreen.value)
    }

    @Test
    fun navigationBetweenTwoAuthorizedDestinationsStillWorks() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))

        vm.navigateTo(Screen.StudentRecordsDashboard)
        assertIs<Screen.StudentRecordsDashboard>(vm.currentScreen.value)

        vm.navigateTo(Screen.OfficialEnrollmentDashboard)
        assertIs<Screen.OfficialEnrollmentDashboard>(vm.currentScreen.value)
    }

    @Test
    fun navigateFromSecretarySidebarIsBlockedForUnauthorizedRole() {
        val vm = viewModelWithSession(sessionFor(StaffRole.TRABAJO_SOCIAL))

        vm.navigateFromSecretarySidebar("Pre-Solicitudes")

        assertIs<Screen.SessionHome>(vm.currentScreen.value)
    }

    @Test
    fun navigateFromSecretarySidebarDelegatesToNavigateToWhenAuthorized() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))

        vm.navigateFromSecretarySidebar("Pre-Solicitudes")

        assertIs<Screen.SecretariaPreApplicationDashboard>(vm.currentScreen.value)
    }
}
