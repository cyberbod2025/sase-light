package com.example.data.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AuthIdentityIsolationTest {
    @Test
    fun failedLoginClearsPreviousPendingRoleSelection() = runTest {
        val repository = MockAuthRepositoryImpl(nowMillis = { 1_000L })

        assertIs<AuthResult.RoleSelectionRequired>(
            repository.signIn("direccion@example.invalid", "demo1234")
        )
        val failure = assertIs<AuthResult.Failure>(
            repository.signIn("inexistente@example.invalid", "incorrecta")
        )
        assertEquals(AuthFailureReason.INVALID_CREDENTIALS, failure.reason)

        val staleSelection = assertIs<AuthResult.Failure>(
            repository.selectRole("role-direccion")
        )
        assertEquals(AuthFailureReason.ROLE_NOT_ASSIGNED, staleSelection.reason)
        assertNull(repository.session.value)
    }

    @Test
    fun logoutAllowsCleanRelogin() = runTest {
        val repository = MockAuthRepositoryImpl(nowMillis = { 1_000L })
        assertIs<AuthResult.Success>(repository.signInDemo(StaffRole.SECRETARIA))

        repository.signOut()
        assertNull(repository.session.value)

        val relogin = assertIs<AuthResult.Success>(repository.signInDemo(StaffRole.DOCENTE))
        assertEquals(StaffRole.DOCENTE, relogin.session.activeRole)
    }
}
