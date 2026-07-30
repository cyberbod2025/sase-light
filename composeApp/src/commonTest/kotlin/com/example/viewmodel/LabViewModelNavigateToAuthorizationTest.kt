package com.example.viewmodel

import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private class FixedSessionAuthRepository(
        fixedSession: AuthSession?
    ) : AuthRepository {
        override val session: StateFlow<AuthSession?> = MutableStateFlow(fixedSession).asStateFlow()

        override suspend fun signIn(email: String, password: String): AuthResult =
            throw UnsupportedOperationException("FixedSessionAuthRepository solo fija sesion, no inicia sesion")

        override suspend fun signOut() {
            throw UnsupportedOperationException("FixedSessionAuthRepository no cierra sesion")
        }
    }

    private fun sessionFor(role: StaffRole, active: Boolean = true): AuthSession = AuthSession(
        profile = StaffProfile(
            id = "staff-nav-test",
            email = "staff-nav@example.invalid",
            fullName = "Staff Navegacion Test",
            role = role,
            active = active
        ),
        accessToken = "token-nav-test"
    )

    private fun viewModelWithSession(session: AuthSession?): LabViewModel =
        LabViewModel(authRepository = FixedSessionAuthRepository(session))

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

        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)
    }

    @Test
    fun noSessionDoesNotNavigate() {
        val vm = viewModelWithSession(session = null)

        vm.navigateTo(Screen.StudentRecordsDashboard)

        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)
    }

    @Test
    fun inactiveProfileDoesNotNavigate() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA, active = false))

        vm.navigateTo(Screen.StudentRecordsDashboard)

        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)
    }

    @Test
    fun preApplicationFamilyPortalDoesNotOpenThroughThisFlow() {
        // No se concede acceso especial: screenArea sigue null para esta ruta
        // incluso con una sesion de staff activa y autorizada en otras areas.
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))

        vm.navigateTo(Screen.PreApplicationFamilyPortal)

        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)
    }

    @Test
    fun rejectedNavigationPreservesThePreviousScreen() {
        val vm = viewModelWithSession(sessionFor(StaffRole.TRABAJO_SOCIAL))
        vm.navigateTo(Screen.StudentRecordsDashboard)
        assertIs<Screen.StudentRecordsDashboard>(vm.currentScreen.value)

        // Trabajo Social no tiene ALTA_OFICIAL: el intento se ignora en silencio.
        vm.navigateTo(Screen.OfficialEnrollmentDashboard)

        assertIs<Screen.StudentRecordsDashboard>(vm.currentScreen.value)
    }

    @Test
    fun navigateBackReturnsToTheAuthenticatedRoleHome() {
        val vm = viewModelWithSession(sessionFor(StaffRole.TRABAJO_SOCIAL))
        vm.navigateTo(Screen.StudentRecord(studentId = "STU-001"))

        vm.navigateBack()

        assertIs<Screen.StudentRecordsDashboard>(vm.currentScreen.value)
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

        assertIs<Screen.SecretaryDashboard>(vm.currentScreen.value)
    }

    @Test
    fun navigateFromSecretarySidebarDelegatesToNavigateToWhenAuthorized() {
        val vm = viewModelWithSession(sessionFor(StaffRole.SECRETARIA))

        vm.navigateFromSecretarySidebar("Pre-Solicitudes")

        assertIs<Screen.SecretariaPreApplicationDashboard>(vm.currentScreen.value)
    }
}
