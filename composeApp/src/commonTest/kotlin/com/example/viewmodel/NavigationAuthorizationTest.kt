package com.example.viewmodel

import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.SaseArea
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun roleId(role: StaffRole): String = "role-${role.name.lowercase()}"

private fun sessionFor(
    role: StaffRole,
    active: Boolean = true,
    assignedRoles: List<StaffRole> = listOf(role)
): AuthSession {
    val assignments = assignedRoles.distinct().map { assignedRole ->
        InstitutionalRoleAssignment(roleId = roleId(assignedRole), role = assignedRole)
    }
    return AuthSession(
        profile = StaffProfile(
            id = "staff-test",
            email = "staff@example.invalid",
            fullName = "Staff De Prueba",
            active = active
        ),
        institutional = InstitutionalSession.create(
            userId = "user-test",
            profileId = "staff-test",
            membershipId = "membership-test",
            institutionId = "institution-test",
            institutionName = "Institución Ficticia de Prueba",
            roleAssignments = assignments,
            activeRoleId = roleId(role),
            schoolCycleId = "cycle-test",
            sessionStartedAt = 1_000L,
            expiresAt = 10_000L
        ),
        accessToken = "token-test"
    )
}

private val allScreens: List<Screen> = listOf(
    Screen.SessionHome,
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

    @Test
    fun screenAreaMapsSessionHomeAndEveryInstitutionalScreen() {
        assertEquals(SaseArea.SESSION, screenArea(Screen.SessionHome))
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
        assertNull(screenArea(Screen.PreApplicationFamilyPortal))
    }

    @Test
    fun canOpenScreenDeniesEveryScreenWithoutSession() {
        allScreens.forEach { screen ->
            assertFalse(canOpenScreen(session = null, screen = screen), "no debería autorizar $screen sin sesión")
        }
    }

    @Test
    fun canOpenScreenDeniesEveryScreenWithInactiveProfile() {
        val inactive = sessionFor(StaffRole.SECRETARIA, active = false)
        allScreens.forEach { screen ->
            assertFalse(canOpenScreen(inactive, screen), "perfil inactivo no debería autorizar $screen")
        }
    }

    @Test
    fun canOpenScreenDeniesFamilyPortalEvenWithActiveStaffSession() {
        assertFalse(canOpenScreen(sessionFor(StaffRole.SECRETARIA), Screen.PreApplicationFamilyPortal))
    }

    @Test
    fun everyActiveInstitutionalRoleCanOpenTheSafeUniversalHome() {
        StaffRole.entries.forEach { role ->
            val session = sessionFor(role)
            assertTrue(canOpenScreen(session, Screen.SessionHome), "$role debe poder abrir el home de sesión")
        }
    }

    @Test
    fun authorizedScreenForKeepsAnAuthorizedRequestedScreen() {
        val session = sessionFor(StaffRole.SECRETARIA)

        assertEquals(
            Screen.OfficialEnrollmentDashboard,
            authorizedScreenFor(session, Screen.OfficialEnrollmentDashboard)
        )
    }

    @Test
    fun authorizedScreenForReplacesUnauthorizedScreenWithSafeSessionHome() {
        val session = sessionFor(StaffRole.TRABAJO_SOCIAL)

        assertEquals(Screen.SessionHome, authorizedScreenFor(session, Screen.SecretaryDashboard))
    }

    @Test
    fun authorizedScreenForReturnsNullWithoutAnActiveSession() {
        assertNull(authorizedScreenFor(null, Screen.SecretaryDashboard))
        assertNull(
            authorizedScreenFor(
                sessionFor(StaffRole.SECRETARIA, active = false),
                Screen.SecretaryDashboard
            )
        )
    }

    @Test
    fun sidebarIsEmptyWithoutActiveSession() {
        assertEquals(emptyList(), visibleSidebarItems(null))
        assertEquals(emptyList(), visibleSidebarItems(sessionFor(StaffRole.SECRETARIA, active = false)))
    }

    @Test
    fun secretariaSidebarContainsOnlyItsAuthorizedDestinations() {
        assertEquals(
            listOf(
                "Inicio",
                "Expedientes",
                "Inscripciones",
                "Pre-Solicitudes",
                "Altas Oficiales",
                "Credenciales"
            ),
            visibleSidebarItems(sessionFor(StaffRole.SECRETARIA))
        )
    }

    @Test
    fun direccionSidebarExcludesPreApplicationsAndFamilyPortal() {
        assertEquals(
            listOf(
                "Inicio",
                "Expedientes",
                "Inscripciones",
                "Altas Oficiales",
                "Credenciales"
            ),
            visibleSidebarItems(sessionFor(StaffRole.DIRECCION))
        )
    }

    @Test
    fun scopedRolesSeeOnlySafeSessionHomeUntilTheyHaveSegmentedDestinations() {
        listOf(
            StaffRole.TRABAJO_SOCIAL,
            StaffRole.MEDICO_ESCOLAR,
            StaffRole.UDEII,
            StaffRole.DOCENTE
        ).forEach { role ->
            assertEquals(listOf("Inicio"), visibleSidebarItems(sessionFor(role)), "$role solo debe ver Inicio")
        }
    }

    @Test
    fun everyVisibleSidebarItemResolvesToAnAuthorizedScreen() {
        StaffRole.entries.forEach { role ->
            val session = sessionFor(role)
            visibleSidebarItems(session).forEach { item ->
                val destination = assertNotNull(secretarySidebarDestination(item), "$item debe tener destino")
                assertTrue(canOpenScreen(session, destination), "$role debe poder abrir $item")
            }
        }
    }

    @Test
    fun secretariaCanOpenItsOperationalScreensAndSessionHome() {
        val session = sessionFor(StaffRole.SECRETARIA)
        assertTrue(canOpenScreen(session, Screen.SessionHome))
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
    fun direccionCanOpenItsOperationalScreensAndSessionHome() {
        val session = sessionFor(StaffRole.DIRECCION)
        assertTrue(canOpenScreen(session, Screen.SessionHome))
        assertTrue(canOpenScreen(session, Screen.SecretaryDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecordsDashboard))
        assertTrue(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
        assertTrue(canOpenScreen(session, Screen.EnrollmentDashboard))
        assertTrue(canOpenScreen(session, Screen.OfficialEnrollmentDashboard))
        assertTrue(canOpenScreen(session, Screen.CredentialPreview(studentId = "STU-001")))
        assertTrue(canOpenScreen(session, Screen.StudentCredentialDashboard))
    }

    @Test
    fun scopedRolesCannotOpenTheUnsegmentedStudentRecord() {
        listOf(StaffRole.TRABAJO_SOCIAL, StaffRole.MEDICO_ESCOLAR, StaffRole.UDEII, StaffRole.DOCENTE).forEach { role ->
            val session = sessionFor(role)
            assertTrue(canOpenScreen(session, Screen.SessionHome))
            assertFalse(canOpenScreen(session, Screen.StudentRecordsDashboard))
            assertFalse(canOpenScreen(session, Screen.StudentRecord(studentId = "STU-001")))
            assertFalse(canOpenScreen(session, Screen.SecretaryDashboard))
        }
    }

    @Test
    fun homeScreenForEveryRoleIsTheSafeSessionHome() {
        StaffRole.entries.forEach { role ->
            assertEquals(Screen.SessionHome, homeScreenFor(role))
            assertEquals(Screen.SessionHome, homeScreenFor(sessionFor(role)))
        }
    }

    @Test
    fun homeScreenForSessionFailsClosedWithoutActiveSession() {
        assertNull(homeScreenFor(session = null))
        assertNull(homeScreenFor(sessionFor(StaffRole.SECRETARIA, active = false)))
    }

    @Test
    fun switchingAssignedRoleRecomputesNavigationFromEffectiveSessionPermissions() {
        val secretaria = sessionFor(
            role = StaffRole.SECRETARIA,
            assignedRoles = listOf(StaffRole.SECRETARIA, StaffRole.DOCENTE)
        )
        val docente = secretaria.switchToAssignedRole(roleId(StaffRole.DOCENTE))

        assertEquals(StaffRole.SECRETARIA, secretaria.activeRole)
        assertTrue(canOpenScreen(secretaria, Screen.StudentRecordsDashboard))
        assertEquals(StaffRole.DOCENTE, docente.activeRole)
        assertFalse(canOpenScreen(docente, Screen.StudentRecordsDashboard))
        assertEquals(listOf("Inicio"), visibleSidebarItems(docente))
        assertEquals(Screen.SessionHome, authorizedScreenFor(docente, Screen.StudentRecordsDashboard))
    }
}
