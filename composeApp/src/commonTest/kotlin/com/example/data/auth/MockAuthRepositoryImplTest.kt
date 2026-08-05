package com.example.data.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockAuthRepositoryImplTest {

    @Test
    fun demoButtonsOpenOnlyTheirExpectedInstitutionalRole() = runTest {
        val expectedRoles = listOf(
            StaffRole.DIRECCION,
            StaffRole.SECRETARIA,
            StaffRole.DOCENTE
        )

        expectedRoles.forEach { expectedRole ->
            val repo = MockAuthRepositoryImpl(nowMillis = { STARTED_AT })

            assertTrue(repo.demoAccessAvailable)
            val result = repo.signInDemo(expectedRole)
            assertTrue(result is AuthResult.Success)
            assertEquals(expectedRole, result.session.activeRole)
            assertEquals(MockStaffDirectory.INSTITUTION_ID, result.session.institutionId)
            assertEquals(MockStaffDirectory.INSTITUTION_NAME, result.session.institutionName)
            assertEquals(result.session, repo.session.value)
        }
    }

    @Test
    fun rolesWithoutDemoButtonFailClosed() = runTest {
        val repo = MockAuthRepositoryImpl(nowMillis = { STARTED_AT })

        listOf(StaffRole.TRABAJO_SOCIAL, StaffRole.MEDICO_ESCOLAR, StaffRole.UDEII).forEach { role ->
            val result = repo.signInDemo(role)
            assertEquals(AuthFailureReason.DEMO_UNAVAILABLE, (result as AuthResult.Failure).reason)
            assertNull(repo.session.value)
        }
    }

    @Test
    fun multiRoleCredentialsRequireExplicitAssignedRoleSelection() = runTest {
        val repo = MockAuthRepositoryImpl(nowMillis = { STARTED_AT })

        val signIn = repo.signIn("direccion@example.invalid", "demo1234")

        assertTrue(signIn is AuthResult.RoleSelectionRequired)
        assertEquals(
            listOf(StaffRole.DIRECCION, StaffRole.SECRETARIA),
            signIn.context.availableRoles.map { it.role }
        )
        assertNull(repo.session.value)

        val selected = repo.selectRole("role-secretaria")
        assertTrue(selected is AuthResult.Success)
        assertEquals(StaffRole.SECRETARIA, selected.session.activeRole)
        assertEquals("role-secretaria", selected.session.activeRoleId)
    }

    @Test
    fun selectingOrSwitchingToAnUnassignedRoleFailsClosed() = runTest {
        val repo = MockAuthRepositoryImpl(nowMillis = { STARTED_AT })
        repo.signIn("direccion@example.invalid", "demo1234")

        val invalidSelection = repo.selectRole("role-docente")
        assertEquals(
            AuthFailureReason.ROLE_NOT_ASSIGNED,
            (invalidSelection as AuthResult.Failure).reason
        )
        assertNull(repo.session.value)

        val direction = repo.selectRole("role-direccion")
        assertTrue(direction is AuthResult.Success)
        val switched = repo.switchRole("role-secretaria")
        assertTrue(switched is AuthResult.Success)
        assertEquals(StaffRole.SECRETARIA, switched.session.activeRole)

        val invalidSwitch = repo.switchRole("role-docente")
        assertEquals(
            AuthFailureReason.ROLE_NOT_ASSIGNED,
            (invalidSwitch as AuthResult.Failure).reason
        )
        assertEquals(StaffRole.SECRETARIA, repo.session.value?.activeRole)
    }

    @Test
    fun credentialsWithoutMembershipOrRoleAreRejected() = runTest {
        val repo = MockAuthRepositoryImpl(nowMillis = { STARTED_AT })

        val withoutMembership = repo.signIn("sinmembresia@example.invalid", "demo1234")
        assertEquals(
            AuthFailureReason.NO_MEMBERSHIP,
            (withoutMembership as AuthResult.Failure).reason
        )
        assertNull(repo.session.value)

        val withoutRole = repo.signIn("sinrol@example.invalid", "demo1234")
        assertEquals(AuthFailureReason.NO_ROLE, (withoutRole as AuthResult.Failure).reason)
        assertNull(repo.session.value)
    }

    @Test
    fun issuedDemoSessionExpiresAtTheEightHourBoundary() = runTest {
        var now = STARTED_AT
        val repo = MockAuthRepositoryImpl(nowMillis = { now })
        val result = repo.signInDemo(StaffRole.SECRETARIA)
        assertTrue(result is AuthResult.Success)
        assertEquals(STARTED_AT + EIGHT_HOURS_MILLIS, result.session.expiresAt)
        assertFalse(result.session.isExpired(STARTED_AT + EIGHT_HOURS_MILLIS - 1L))

        now += EIGHT_HOURS_MILLIS

        assertTrue(repo.session.value?.isExpired(now) == true)
    }

    private companion object {
        const val STARTED_AT = 1_700_000_000_000L
        const val EIGHT_HOURS_MILLIS = 8L * 60L * 60L * 1_000L
    }
}
