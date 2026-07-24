package com.example.viewmodel

import com.example.data.auth.AuthSession
import com.example.data.auth.SaseArea
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun sessionFor(role: StaffRole, active: Boolean = true): AuthSession = AuthSession(
    profile = StaffProfile(
        id = "staff-test",
        email = "staff@example.invalid",
        fullName = "Staff De Prueba",
        role = role,
        active = active
    ),
    accessToken = "token-test"
)

private val allScreens: List<Screen> = listOf(
    Screen.SecretaryDashboard,
    Screen.StudentRecordsDashboard,
    Screen.EnrollmentDashboard,
    Screen.StudentRecord(studentId = "STU-001"),
    Screen.PreApplicationFamilyPortal,
    Screen.SecretariaPreApplicationDashboard,
    Screen.OfficialEnrollmentDashboard,
    Screen.CredentialPreview(studentId = "STU-001"),
    Screen.StudentCredentialDashboard
)

class NavigationAuthorizationTest {

    // --- screenArea: cada variante de Screen ---

    @Test
    fun screenAreaMapsEveryInstitutionalScreenToItsArea() {
        assertEquals(SaseArea.SECRETARIA, screenArea(Screen.SecretaryDashboard))
        assertEquals(SaseArea.EXPEDIENTE, screenArea(Screen.StudentRecordsDashboard))
        assertEquals(SaseArea.EXPEDIENTE, screenArea(Screen.StudentRecord(studentId = "STU-001")))
        assertEquals(SaseArea.ALTA_OFICIAL, screenArea(Screen.EnrollmentDashboard))
        assertEquals(SaseArea.PRE_SOLICITUD, screenArea(Screen.SecretariaPreApplicationDashboard))
        assertEquals(SaseArea.ALTA_OFICIAL, screenArea(Screen.OfficialEnrollmentDashboard))
        assertEquals(SaseArea.CREDENCIAL, screenArea(Screen.CredentialPreview(studentId = "STU-001")))
        assertEquals(SaseArea.CREDENCIAL, screenArea(Screen.StudentCredentialDashboard))
    }

    @Test
    fun screenAreaDeniesFamilyPortalExplicitlyInsteadOfDefaultingToAccess() {
        // Caso pendiente: el portal familiar deberia ser una ruta publica sin
        // sesion de staff, pero esa via no existe todavia. Mientras tanto no
        // tiene area institucional: null es explicito, no un olvido.
        assertNull(screenArea(Screen.PreApplicationFamilyPortal))
    }

    // --- canOpenScreen: sesion ausente ---

    @Test
    fun canOpenScreenDeniesEveryScreenWithoutSession() {
        allScreens.forEach { screen ->
            assertFalse(canOpenScreen(session = null, screen = screen), "no deberia autorizar $screen sin sesion")
        }
    }

    // --- canOpenScreen: perfil inactivo ---

    @Test
    fun canOpenScreenDeniesEveryScreenWithInactiveProfile() {
        val inactive = sessionFor(StaffRole.SECRETARIA, active = false)
        allScreens.forEach { screen ->
            assertFalse(canOpenScreen(inactive, screen), "perfil inactivo no deberia autorizar $screen")
        }
    }

    // --- canOpenScreen: portal familiar bajo el gating de staff ---

    @Test
    fun canOpenScreenDeniesFamilyPortalCaseEvenWithActiveStaffSession() {
        // No fingimos que ya es accesible sin sesion: hoy simplemente no
        // tiene politica de staff bajo la que autorizarse.
        val session = sessionFor(StaffRole.SECRETARIA)
        assertFalse(canOpenScreen(session, Screen.PreApplicationFamilyPortal))
    }

    // --- canOpenScreen: rol autorizado / no autorizado por rol ---

    @Test
    fun secretariaCanOpenAllItsAuthorizedScreens() {
        val session = sessionFor(StaffRole.SECRETARIA)
        assertTrue(canOpenScreen(session, Screen.SecretaryDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecordsDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
        assertTrue(canOpenScreen(session, Screen.EnrollmentDashboard))
        assertTrue(canOpenScreen(session, Screen.SecretariaPreApplicationDashboard))
        assertTrue(canOpenScreen(session, Screen.OfficialEnrollmentDashboard))
        assertTrue(canOpenScreen(session, Screen.CredentialPreview(studentId = "STU-001")))
        assertTrue(canOpenScreen(session, Screen.StudentCredentialDashboard))
    }

    @Test
    fun direccionCannotOpenPreApplicationDashboard() {
        val session = sessionFor(StaffRole.DIRECCION)
        assertFalse(canOpenScreen(session, Screen.SecretariaPreApplicationDashboard))
    }

    @Test
    fun direccionCanOpenSecretaryEnrollmentAndCredentialScreens() {
        val session = sessionFor(StaffRole.DIRECCION)
        assertTrue(canOpenScreen(session, Screen.SecretaryDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecordsDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
        assertTrue(canOpenScreen(session, Screen.EnrollmentDashboard))
        assertTrue(canOpenScreen(session, Screen.OfficialEnrollmentDashboard))
        assertTrue(canOpenScreen(session, Screen.CredentialPreview(studentId = "STU-001")))
        assertTrue(canOpenScreen(session, Screen.StudentCredentialDashboard))
    }

    @Test
    fun trabajoSocialOnlyOpensExpedienteScreens() {
        val session = sessionFor(StaffRole.TRABAJO_SOCIAL)
        assertTrue(canOpenScreen(session, Screen.StudentRecordsDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
        assertFalse(canOpenScreen(session, Screen.SecretaryDashboard))
        assertFalse(canOpenScreen(session, Screen.EnrollmentDashboard))
        assertFalse(canOpenScreen(session, Screen.SecretariaPreApplicationDashboard))
        assertFalse(canOpenScreen(session, Screen.OfficialEnrollmentDashboard))
        assertFalse(canOpenScreen(session, Screen.CredentialPreview(studentId = "STU-001")))
        assertFalse(canOpenScreen(session, Screen.StudentCredentialDashboard))
    }

    @Test
    fun udeiiOnlyOpensExpedienteScreens() {
        val session = sessionFor(StaffRole.UDEII)
        assertTrue(canOpenScreen(session, Screen.StudentRecordsDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
        assertFalse(canOpenScreen(session, Screen.SecretaryDashboard))
        assertFalse(canOpenScreen(session, Screen.EnrollmentDashboard))
        assertFalse(canOpenScreen(session, Screen.OfficialEnrollmentDashboard))
        assertFalse(canOpenScreen(session, Screen.CredentialPreview(studentId = "STU-001")))
    }

    @Test
    fun docenteOnlyOpensExpedienteScreens() {
        val session = sessionFor(StaffRole.DOCENTE)
        assertTrue(canOpenScreen(session, Screen.StudentRecordsDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
        assertFalse(canOpenScreen(session, Screen.SecretaryDashboard))
        assertFalse(canOpenScreen(session, Screen.CredentialPreview(studentId = "STU-001")))
    }

    @Test
    fun medicoEscolarOpensNoInstitutionalScreenYet() {
        // Su unica area es SALUD, que hoy no tiene ninguna Screen mapeada.
        val session = sessionFor(StaffRole.MEDICO_ESCOLAR)
        allScreens.filterNot { it is Screen.PreApplicationFamilyPortal }.forEach { screen ->
            assertFalse(
                canOpenScreen(session, screen),
                "Medico Escolar no deberia abrir $screen todavia (falta pantalla de Salud)"
            )
        }
    }

    // --- homeScreenFor: cada StaffRole ---

    @Test
    fun homeScreenForDireccionAndSecretariaIsSecretaryDashboard() {
        assertEquals(Screen.SecretaryDashboard, homeScreenFor(StaffRole.DIRECCION))
        assertEquals(Screen.SecretaryDashboard, homeScreenFor(StaffRole.SECRETARIA))
    }

    @Test
    fun homeScreenForClinicalAndTeachingRolesIsStudentRecordsDashboard() {
        assertEquals(Screen.StudentRecordsDashboard, homeScreenFor(StaffRole.TRABAJO_SOCIAL))
        assertEquals(Screen.StudentRecordsDashboard, homeScreenFor(StaffRole.UDEII))
        assertEquals(Screen.StudentRecordsDashboard, homeScreenFor(StaffRole.DOCENTE))
    }

    @Test
    fun homeScreenForMedicoEscolarIsNullUntilSaludScreenExists() {
        assertNull(homeScreenFor(StaffRole.MEDICO_ESCOLAR))
    }

    @Test
    fun everyNonNullHomeScreenIsActuallyOpenableByThatRole() {
        StaffRole.entries.forEach { role ->
            val home = homeScreenFor(role) ?: return@forEach
            assertTrue(
                canOpenScreen(sessionFor(role), home),
                "$role deberia poder abrir su propio home $home"
            )
        }
    }
}
