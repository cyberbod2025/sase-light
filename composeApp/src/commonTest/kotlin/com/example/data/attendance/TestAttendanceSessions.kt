package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.MockStaffDirectory
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole

/**
 * Sesiones institucionales sinteticas para la rebanada de asistencia. Reutilizan
 * el modelo de sesion real (InstitutionalSession/AuthSession) — no existe un
 * modelo de identidad paralelo para pruebas.
 */
internal fun attendanceSession(
    membershipId: String,
    role: StaffRole = StaffRole.DOCENTE,
    active: Boolean = true
): AuthSession {
    val roleId = "role-${role.name.lowercase()}"
    val profileId = "profile-$membershipId"
    return AuthSession(
        profile = StaffProfile(
            id = profileId,
            email = "$membershipId@example.invalid",
            fullName = "Personal Demo $membershipId",
            active = active
        ),
        institutional = InstitutionalSession.create(
            userId = "user-$membershipId",
            profileId = profileId,
            membershipId = membershipId,
            institutionId = MockStaffDirectory.INSTITUTION_ID,
            institutionName = MockStaffDirectory.INSTITUTION_NAME,
            roleAssignments = listOf(InstitutionalRoleAssignment(roleId = roleId, role = role)),
            activeRoleId = roleId,
            schoolCycleId = MockStaffDirectory.SCHOOL_CYCLE_ID,
            sessionStartedAt = 1_000L,
            expiresAt = null
        ),
        accessToken = "token-$membershipId"
    )
}

/** Docente asignado al grupo 1° A. */
internal fun assignedTeacherSession(): AuthSession =
    attendanceSession(MockAttendanceData.TEACHER_1A_MEMBERSHIP_ID)

/** Docente que NO atiende el grupo 1° A (solo 2° B). */
internal fun unassignedTeacherSession(): AuthSession =
    attendanceSession(MockAttendanceData.TEACHER_2B_MEMBERSHIP_ID)
