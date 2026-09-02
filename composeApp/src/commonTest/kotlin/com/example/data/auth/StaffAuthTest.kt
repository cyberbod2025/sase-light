package com.example.data.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaffAuthTest {

    @Test
    fun signInWithValidCredentialsOpensSession() = runTest {
        val repo = MockAuthRepositoryImpl()
        val result = repo.signIn("secretaria@example.invalid", "demo1234")
        assertTrue(result is AuthResult.Success)
        assertEquals(StaffRole.SECRETARIA, result.session.activeRole)
        assertEquals("membership-secretary-demo", result.session.membershipId)
        assertEquals(MockStaffDirectory.INSTITUTION_ID, result.session.institutionId)
        assertEquals(result.session, repo.session.value)
    }

    @Test
    fun emailIsNormalizedBeforeLookup() = runTest {
        val repo = MockAuthRepositoryImpl()
        val result = repo.signIn("  SECRETARIA@Example.Invalid ", "demo1234")
        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun wrongPasswordDoesNotOpenSession() = runTest {
        val repo = MockAuthRepositoryImpl()
        val result = repo.signIn("secretaria@example.invalid", "incorrecta")
        assertEquals(AuthFailureReason.INVALID_CREDENTIALS, (result as AuthResult.Failure).reason)
        assertNull(repo.session.value)
    }

    @Test
    fun unknownEmailIsIndistinguishableFromWrongPassword() = runTest {
        val repo = MockAuthRepositoryImpl()
        val result = repo.signIn("nadie@example.invalid", "demo1234")
        assertEquals(AuthFailureReason.INVALID_CREDENTIALS, (result as AuthResult.Failure).reason)
    }

    @Test
    fun inactiveStaffCannotSignIn() = runTest {
        val repo = MockAuthRepositoryImpl()
        val result = repo.signIn("baja@example.invalid", "demo1234")
        assertEquals(AuthFailureReason.INACTIVE_ACCOUNT, (result as AuthResult.Failure).reason)
        assertNull(repo.session.value)
    }

    @Test
    fun signOutClearsSession() = runTest {
        val repo = MockAuthRepositoryImpl()
        repo.signIn("direccion@example.invalid", "demo1234")
        repo.signOut()
        assertNull(repo.session.value)
    }

    @Test
    fun secretariaDrivesEnrollmentButNotClinicalAreas() {
        assertTrue(StaffPermissions.canAccess(StaffRole.SECRETARIA, SaseArea.ALTA_OFICIAL))
        assertTrue(StaffPermissions.canAccess(StaffRole.SECRETARIA, SaseArea.CREDENCIAL))
        assertFalse(StaffPermissions.canAccess(StaffRole.SECRETARIA, SaseArea.SALUD))
        assertFalse(StaffPermissions.canAccess(StaffRole.SECRETARIA, SaseArea.UDEII))
    }

    @Test
    fun clinicalAndSocialAreasStayCompartmentalized() {
        assertFalse(StaffPermissions.canAccess(StaffRole.MEDICO_ESCOLAR, SaseArea.TRABAJO_SOCIAL))
        assertFalse(StaffPermissions.canAccess(StaffRole.TRABAJO_SOCIAL, SaseArea.SALUD))
        assertFalse(StaffPermissions.canAccess(StaffRole.DOCENTE, SaseArea.SALUD))
        assertFalse(StaffPermissions.canAccess(StaffRole.UDEII, SaseArea.TRABAJO_SOCIAL))
    }

    @Test
    fun directionSeesIndicatorsAndNoOneElseDoes() {
        assertTrue(StaffPermissions.canAccess(StaffRole.DIRECCION, SaseArea.INDICADORES))
        StaffRole.entries.filter { it != StaffRole.DIRECCION }.forEach {
            assertFalse(StaffPermissions.canAccess(it, SaseArea.INDICADORES), "$it no debe ver indicadores")
        }
    }

    @Test
    fun noSessionGrantsNothing() {
        SaseArea.entries.forEach { assertFalse(StaffPermissions.canAccess(null, it)) }
    }

    /**
     * Debe coincidir EXACTAMENTE con el catalogo real de permisos (rol ->
     * permiso) introspeccionado en el proyecto Supabase de staging el
     * 2026-08-12 (ver comentario en StaffPermissions.actionMatrix y las
     * migraciones 0004/0005/0010). Si esta lista cambia, StaffPermissions
     * debe cambiar junto con ella y viceversa -- es la defensa contra que el
     * cliente vuelva a ofrecer una accion que RLS rechaza en silencio
     * (P1 de Codex, "Align connected action permissions with RLS").
     */
    @Test
    fun laMatrizDeAccionesNoOfreceEscrituraQueElCatalogoRealDeDireccionNoTiene() {
        // DIRECCION en el catalogo real: VIEW_AUDIT, VIEW_ENROLLMENT,
        // VIEW_INCIDENTS, VIEW_STUDENT_BASE, VIEW_STUDENT_SENSITIVE_IDENTITY.
        // Ningun EDIT_*. ESCALATE_CASE y LOG_STUDENT_RECORD_EVENT no escriben
        // en ninguna tabla protegida por EDIT_* (solo student_audit_events,
        // gobernada por membership_roles real via 0009), asi que son la
        // unica excepcion legitima.
        assertFalse(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.CREATE_STUDENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.UPDATE_STUDENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.ADD_OBSERVATION))
        assertFalse(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.REPORT_INCIDENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.ADVANCE_INCIDENT))
        assertTrue(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.ESCALATE_CASE))
        assertTrue(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.LOG_STUDENT_RECORD_EVENT))
    }

    @Test
    fun secretariaTieneExactamenteLasAccionesQueEditStudentIdentityYEditIncidentsLeDan() {
        // EDIT_STUDENT_IDENTITY (catalogo real, 0004/0005) cubre nucleo,
        // identidad sensible y observaciones. EDIT_INCIDENTS lo otorga la
        // migracion 0010 (escrita, pendiente de aplicar) especificamente a
        // SECRETARIA -- ver esa migracion para el porque.
        StaffAction.entries.forEach { action ->
            assertTrue(
                StaffPermissions.canPerform(StaffRole.SECRETARIA, action),
                "SECRETARIA deberia poder $action"
            )
        }
    }

    @Test
    fun rolesSinPermisoEnElCatalogoRealNoPuedenEjecutarNingunaAccionDeEscritura() {
        // TRABAJO_SOCIAL/MEDICO_ESCOLAR/UDEII/DOCENTE no tienen ningun
        // EDIT_STUDENT_IDENTITY/EDIT_INCIDENTS en el catalogo real hoy (sus
        // EDIT_* propios -- EDIT_MEDICAL/EDIT_BAP/EDIT_SOCIAL_CONTEXT -- no
        // corresponden a ninguna StaffAction todavia cableada en el
        // ViewModel), asi que el cliente no debe ofrecerles ninguna.
        listOf(StaffRole.TRABAJO_SOCIAL, StaffRole.MEDICO_ESCOLAR, StaffRole.UDEII, StaffRole.DOCENTE)
            .forEach { role ->
                StaffAction.entries.forEach { action ->
                    assertFalse(
                        StaffPermissions.canPerform(role, action),
                        "$role no deberia poder $action"
                    )
                }
            }
    }

    @Test
    fun unaSesionInactivaNoPuedeEjecutarNingunaAccionAunqueElRolPudiera() {
        val institutional = InstitutionalSession.create(
            userId = "user-secretaria",
            profileId = "profile-secretaria",
            membershipId = "membership-secretaria",
            institutionId = "institution-x",
            institutionName = "Institución ficticia de prueba",
            roleAssignments = listOf(
                InstitutionalRoleAssignment("role-secretaria", StaffRole.SECRETARIA)
            ),
            activeRoleId = "role-secretaria",
            schoolCycleId = "cycle-test",
            sessionStartedAt = 1_000L,
            expiresAt = 2_000L
        )
        val inactiveSession = AuthSession(
            profile = StaffProfile("profile-secretaria", "s@example.invalid", "S", active = false),
            institutional = institutional,
            accessToken = "t"
        )
        StaffAction.entries.forEach { action ->
            assertFalse(StaffPermissions.canPerform(inactiveSession, action))
        }
    }

    @Test
    fun sinSesionNingunaAccionSeAutoriza() {
        StaffAction.entries.forEach { action ->
            assertFalse(StaffPermissions.canPerform(null, action))
        }
    }

    @Test
    fun inactiveSessionGrantsNothing() {
        val institutional = InstitutionalSession.create(
            userId = "user-x",
            profileId = "profile-x",
            membershipId = "membership-x",
            institutionId = "institution-x",
            institutionName = "Institución ficticia de prueba",
            roleAssignments = listOf(
                InstitutionalRoleAssignment("role-direccion", StaffRole.DIRECCION)
            ),
            activeRoleId = "role-direccion",
            schoolCycleId = "cycle-test",
            sessionStartedAt = 1_000L,
            expiresAt = 2_000L
        )
        val session = AuthSession(
            profile = StaffProfile("profile-x", "x@example.invalid", "X", active = false),
            institutional = institutional,
            accessToken = "t"
        )
        SaseArea.entries.forEach { assertFalse(StaffPermissions.canAccess(session, it)) }
    }
}
