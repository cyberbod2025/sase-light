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
        assertEquals(StaffRole.SECRETARIA, result.session.profile.role)
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

    @Test
    fun inactiveSessionGrantsNothing() {
        val session = AuthSession(
            profile = StaffProfile("x", "x@example.invalid", "X", StaffRole.DIRECCION, active = false),
            accessToken = "t"
        )
        SaseArea.entries.forEach { assertFalse(StaffPermissions.canAccess(session, it)) }
    }
}
