package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MockAttendanceRepositoryTest {

    private fun createSession(
        membershipId: String,
        role: StaffRole
    ): AuthSession = AuthSession(
        profile = StaffProfile(
            id = "profile-$membershipId",
            email = "staff-$membershipId@example.invalid",
            fullName = "Personal $membershipId",
            active = true
        ),
        institutional = InstitutionalSession.create(
            userId = "user-$membershipId",
            profileId = "profile-$membershipId",
            membershipId = membershipId,
            institutionId = "INST-001",
            institutionName = "Institución Ficticia",
            roleAssignments = listOf(
                InstitutionalRoleAssignment(roleId = "role-${role.name.lowercase()}", role = role)
            ),
            activeRoleId = "role-${role.name.lowercase()}",
            schoolCycleId = "cycle-demo",
            sessionStartedAt = 1000L,
            expiresAt = 10000L
        ),
        accessToken = "token-test"
    )

    @Test
    fun docenteAsignadoPuedeConsultarSusGrupos() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val teacher1 = createSession(MockAttendanceData.TEACHER_1A_MEMBERSHIP_ID, StaffRole.DOCENTE)

        val res = repo.groupsForTeacher(teacher1)
        val groups = assertIs<AttendanceResult.Success<List<TeacherGroup>>>(res).value

        assertEquals(1, groups.size)
        assertEquals(MockAttendanceData.GROUP_1A_ID, groups.first().id)
        assertEquals("1° A", groups.first().name)
    }

    @Test
    fun docenteNoPuedeAbrirGrupoNoAsignado() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val teacher2 = createSession(MockAttendanceData.TEACHER_2B_MEMBERSHIP_ID, StaffRole.DOCENTE)

        // Docente de 2° B intenta abrir 1° A
        val res = repo.scheduleOrOpenSession(teacher2, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 1)
        val failure = assertIs<AttendanceResult.Failure>(res)
        assertEquals(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP, failure.reason)
    }

    @Test
    fun rolNoDocenteEsRechazadoAlIntentarGestionarLista() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val secretary = createSession("sec-01", StaffRole.SECRETARIA)

        val res = repo.groupsForTeacher(secretary)
        val failure = assertIs<AttendanceResult.Failure>(res)
        assertEquals(AttendanceFailureReason.NOT_A_TEACHER, failure.reason)
    }

    @Test
    fun validacionDeDatosRechazaParametrosInvalidos() = runBlocking {
        val repo = MockAttendanceRepositoryImpl()
        val teacher = createSession(MockAttendanceData.TEACHER_1A_MEMBERSHIP_ID, StaffRole.DOCENTE)

        // Fecha en blanco
        val resBlankDate = repo.scheduleOrOpenSession(teacher, MockAttendanceData.GROUP_1A_ID, "", 1)
        assertEquals(AttendanceFailureReason.INVALID_DATA, assertIs<AttendanceResult.Failure>(resBlankDate).reason)

        // Módulo fuera de rango (0 o 9)
        val resInvalidMod = repo.scheduleOrOpenSession(teacher, MockAttendanceData.GROUP_1A_ID, "2026-10-15", 9)
        assertEquals(AttendanceFailureReason.INVALID_DATA, assertIs<AttendanceResult.Failure>(resInvalidMod).reason)
    }
}
