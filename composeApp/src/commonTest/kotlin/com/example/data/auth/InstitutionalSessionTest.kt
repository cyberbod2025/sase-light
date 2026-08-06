package com.example.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstitutionalSessionTest {

    private val secretaria = InstitutionalRoleAssignment(
        roleId = "role-secretaria",
        role = StaffRole.SECRETARIA
    )
    private val direccion = InstitutionalRoleAssignment(
        roleId = "role-direccion",
        role = StaffRole.DIRECCION
    )

    @Test
    fun singleRoleSessionExposesOnlyItsAssignedRole() {
        val session = newSession(
            roleAssignments = listOf(secretaria),
            activeRoleId = secretaria.roleId
        )

        assertEquals(setOf(secretaria.roleId), session.assignedRoleIds)
        assertEquals(setOf(StaffRole.SECRETARIA), session.assignedRoles)
        assertEquals(secretaria.roleId, session.activeRoleId)
        assertEquals(StaffRole.SECRETARIA, session.activeRole)
    }

    @Test
    fun multiRoleSessionCanSwitchToAnotherAssignedRole() {
        val session = newSession(
            roleAssignments = listOf(secretaria, direccion),
            activeRoleId = secretaria.roleId
        )

        val switched = session.switchToAssignedRole(direccion.roleId)

        assertEquals(direccion.roleId, switched.activeRoleId)
        assertEquals(StaffRole.DIRECCION, switched.activeRole)
        assertEquals(setOf(secretaria.roleId, direccion.roleId), switched.assignedRoleIds)
    }

    @Test
    fun switchingToUnassignedRoleIsRejected() {
        val session = newSession(
            roleAssignments = listOf(secretaria),
            activeRoleId = secretaria.roleId
        )

        assertFailsWith<IllegalArgumentException> {
            session.switchToAssignedRole("role-direccion")
        }
    }

    @Test
    fun switchingRoleRebuildsPermissionsFromStaffPermissions() {
        val session = newSession(
            roleAssignments = listOf(secretaria, direccion),
            activeRoleId = secretaria.roleId
        )

        val switched = session.switchToAssignedRole(direccion.roleId)

        assertEquals(StaffPermissions.areasFor(StaffRole.DIRECCION), switched.permissions)
        assertTrue(SaseArea.INDICADORES in switched.permissions)
        assertFalse(SaseArea.PRE_SOLICITUD in switched.permissions)
    }

    @Test
    fun expirationUsesTheProvidedCurrentEpochMillis() {
        val expiringSession = newSession(
            roleAssignments = listOf(secretaria),
            activeRoleId = secretaria.roleId,
            expiresAt = 2_000L
        )
        val sessionWithoutExpiration = newSession(
            roleAssignments = listOf(secretaria),
            activeRoleId = secretaria.roleId
        )

        assertFalse(expiringSession.isExpired(nowEpochMillis = 1_999L))
        assertTrue(expiringSession.isExpired(nowEpochMillis = 2_000L))
        assertFalse(sessionWithoutExpiration.isExpired(nowEpochMillis = Long.MAX_VALUE))
    }

    private fun newSession(
        roleAssignments: List<InstitutionalRoleAssignment>,
        activeRoleId: String,
        expiresAt: Long? = null
    ): InstitutionalSession = InstitutionalSession.create(
        userId = "user-1",
        profileId = "profile-1",
        membershipId = "membership-1",
        institutionId = "institution-fictitious-test",
        institutionName = "INSTITUCIÓN ESCOLAR FICTICIA DE PRUEBA",
        roleAssignments = roleAssignments,
        activeRoleId = activeRoleId,
        schoolCycleId = "2026-2027",
        sessionStartedAt = 1_000L,
        expiresAt = expiresAt
    )
}
