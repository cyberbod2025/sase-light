package com.example.integration

import com.example.data.attendance.AttendanceResult
import com.example.data.attendance.AttendanceStatus
import com.example.data.attendance.ClassAttendanceSnapshot
import com.example.data.attendance.MockAttendanceData
import com.example.data.attendance.MockAttendanceRepositoryImpl
import com.example.data.auth.AuthResult
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.SaseArea
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole
import com.example.environment.AppEnvironment
import com.example.viewmodel.ClassAttendanceViewModel
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.canOpenScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Recorrido completo de la rebanada docente sobre las piezas reales de la app:
 * login (MockAuthRepositoryImpl) -> rol activo DOCENTE -> autorizacion de
 * pantalla -> grupo -> asistencia -> guardar -> refrescar -> modificar ->
 * cerrar sesion -> volver a entrar -> recuperar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClassAttendanceGoldenPathTest {

    private val date = "2026-08-18"

    @Test
    fun teacherCapturesAttendanceAndRecoversItAfterSigningInAgain() = runTest {
        val authRepository = MockAuthRepositoryImpl()
        val attendanceRepository = MockAttendanceRepositoryImpl()
        val viewModel = LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("test"),
            authRepository = authRepository,
            attendanceRepository = attendanceRepository,
            coroutineScope = this
        )

        // 1. Login real del docente demo.
        viewModel.signIn("docente@example.invalid", "demo1234")
        advanceUntilIdle()
        val session = assertNotNull(viewModel.session.value)

        // 2. Rol activo explicito de DOCENTE y area DOCENCIA autorizada.
        assertEquals(StaffRole.DOCENTE, session.activeRole)
        assertTrue(StaffPermissions.canAccess(session, SaseArea.DOCENCIA))
        assertTrue(canOpenScreen(session, Screen.TeacherAttendance))
        assertFalse(canOpenScreen(session, Screen.SecretaryDashboard))

        viewModel.navigateTo(Screen.TeacherAttendance)
        assertEquals(Screen.TeacherAttendance, viewModel.currentScreen.value)

        // 3. Solo ve los grupos asignados.
        val attendance = ClassAttendanceViewModel(
            repository = viewModel.attendanceRepository,
            session = session,
            coroutineScope = this,
            today = { date }
        )
        attendance.loadGroups()
        advanceUntilIdle()
        assertEquals(listOf(MockAttendanceData.GROUP_1A_ID), attendance.state.value.groups.map { it.id })

        // 4. Abre el grupo: lista completa y "todos presentes" por omision.
        attendance.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        assertEquals(5, attendance.state.value.roster.size)
        assertEquals(5, attendance.state.value.presentCount)

        // 5. Marca excepciones y guarda en una sola operacion.
        val absent = attendance.state.value.roster[0].student.id
        val late = attendance.state.value.roster[3].student.id
        attendance.setStatus(absent, AttendanceStatus.AUSENTE)
        attendance.setStatus(late, AttendanceStatus.RETARDO)
        assertTrue(attendance.state.value.hasUnsavedChanges)

        attendance.save()
        advanceUntilIdle()
        assertFalse(attendance.state.value.hasUnsavedChanges)
        val savedSessionId = assertNotNull(attendance.state.value.snapshot).sessionId

        // 6. Refresca: recupera lo capturado sin duplicar la sesion.
        attendance.reloadOpenGroup()
        advanceUntilIdle()
        val reloaded = assertNotNull(attendance.state.value.snapshot)
        assertEquals(savedSessionId, reloaded.sessionId)
        assertEquals(AttendanceStatus.AUSENTE, reloaded.statusOf(absent))
        assertEquals(AttendanceStatus.RETARDO, reloaded.statusOf(late))

        // 7. Modifica sin duplicar registros.
        attendance.setStatus(absent, AttendanceStatus.PRESENTE)
        attendance.save()
        advanceUntilIdle()
        val modified = assertNotNull(attendance.state.value.snapshot)
        assertEquals(savedSessionId, modified.sessionId)
        assertEquals(5, modified.entries.size)
        assertEquals(1, modified.entries.count { it.student.id == absent })
        assertEquals(AttendanceStatus.PRESENTE, modified.statusOf(absent))

        // 8. Cierra sesion y vuelve a entrar: la informacion sigue ahi.
        viewModel.signOut()
        advanceUntilIdle()
        assertEquals(null, viewModel.session.value)

        viewModel.signIn("docente@example.invalid", "demo1234")
        advanceUntilIdle()
        val secondSession = assertNotNull(viewModel.session.value)

        val afterRelogin = ClassAttendanceViewModel(
            repository = viewModel.attendanceRepository,
            session = secondSession,
            coroutineScope = this,
            today = { date }
        )
        afterRelogin.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        val recovered = assertNotNull(afterRelogin.state.value.snapshot)
        assertEquals(savedSessionId, recovered.sessionId)
        assertEquals(AttendanceStatus.RETARDO, recovered.statusOf(late))
        assertFalse(afterRelogin.state.value.hasUnsavedChanges)
    }

    @Test
    fun anotherTeacherNeverSeesTheCaptureOfTheAssignedTeacher() = runTest {
        val authRepository = MockAuthRepositoryImpl()
        val attendanceRepository = MockAttendanceRepositoryImpl()
        val viewModel = LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("test"),
            authRepository = authRepository,
            attendanceRepository = attendanceRepository,
            coroutineScope = this
        )

        viewModel.signIn("docente@example.invalid", "demo1234")
        advanceUntilIdle()
        val assigned = assertNotNull(viewModel.session.value)
        val opened = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            attendanceRepository.openClassSession(assigned, MockAttendanceData.GROUP_1A_ID, date)
        ).value

        viewModel.signOut()
        advanceUntilIdle()
        val secondLogin = authRepository.signIn("docente2@example.invalid", "demo1234")
        val intruder = assertIs<AuthResult.Success>(secondLogin).session

        // No ve el grupo…
        val visible = assertIs<AttendanceResult.Success<List<*>>>(
            attendanceRepository.groupsForTeacher(intruder)
        ).value
        assertFalse(visible.any { it.toString().contains(MockAttendanceData.GROUP_1A_ID) })

        // …ni puede abrirlo, ni escribir en la sesion existente.
        assertIs<AttendanceResult.Failure>(
            attendanceRepository.openClassSession(intruder, MockAttendanceData.GROUP_1A_ID, date)
        )
        assertIs<AttendanceResult.Failure>(
            attendanceRepository.saveClassAttendance(
                session = intruder,
                classSessionId = opened.sessionId,
                entries = opened.entries.map { it.student.id to AttendanceStatus.AUSENTE }
            )
        )
    }
}
