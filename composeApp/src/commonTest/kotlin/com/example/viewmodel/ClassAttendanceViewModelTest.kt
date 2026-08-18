package com.example.viewmodel

import com.example.data.attendance.AttendanceFailureReason
import com.example.data.attendance.AttendanceRepository
import com.example.data.attendance.AttendanceResult
import com.example.data.attendance.AttendanceStatus
import com.example.data.attendance.ClassAttendanceSnapshot
import com.example.data.attendance.MockAttendanceData
import com.example.data.attendance.MockAttendanceRepositoryImpl
import com.example.data.attendance.TeacherGroup
import com.example.data.attendance.assignedTeacherSession
import com.example.data.attendance.attendanceSession
import com.example.data.attendance.unassignedTeacherSession
import com.example.data.auth.AuthSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClassAttendanceViewModelTest {

    private val date = "2026-08-18"

    private fun viewModelFor(
        repository: AttendanceRepository,
        session: AuthSession,
        scope: TestScope
    ): ClassAttendanceViewModel = ClassAttendanceViewModel(
        repository = repository,
        session = session,
        coroutineScope = scope,
        today = { date }
    )

    @Test
    fun loadsOnlyAssignedGroups() = runTest {
        val viewModel = viewModelFor(MockAttendanceRepositoryImpl(), assignedTeacherSession(), this)

        viewModel.loadGroups()
        advanceUntilIdle()

        assertEquals(listOf(MockAttendanceData.GROUP_1A_ID), viewModel.state.value.groups.map { it.id })
        assertFalse(viewModel.state.value.showsEmptyGroupsState)
    }

    @Test
    fun showsUsefulEmptyStateWhenTeacherHasNoGroups() = runTest {
        val viewModel = viewModelFor(
            MockAttendanceRepositoryImpl(),
            attendanceSession("membership-teacher-without-groups"),
            this
        )

        viewModel.loadGroups()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showsEmptyGroupsState)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun openingAGroupStartsAllPresentAndFlagsUnsavedChanges() = runTest {
        val viewModel = viewModelFor(MockAttendanceRepositoryImpl(), assignedTeacherSession(), this)

        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(5, state.roster.size)
        assertEquals(5, state.presentCount)
        // Aun sin tocar nada, la captura no existe todavia en la base.
        assertTrue(state.hasUnsavedChanges)
        assertEquals("Cambios sin guardar", unsavedChangesLabel(state))
        assertEquals("Guardar asistencia", classAttendanceSaveLabel(state))
        assertTrue(state.canSave)
    }

    @Test
    fun markingAnExceptionOnlyChangesThatStudent() = runTest {
        val viewModel = viewModelFor(MockAttendanceRepositoryImpl(), assignedTeacherSession(), this)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        val target = viewModel.state.value.roster[2].student.id

        viewModel.setStatus(target, AttendanceStatus.AUSENTE)

        val state = viewModel.state.value
        assertEquals(AttendanceStatus.AUSENTE, state.roster.first { it.student.id == target }.status)
        assertEquals(4, state.presentCount)
        assertEquals(1, state.absentCount)
        assertEquals("Presentes 4 · Ausentes 1 · Retardos 0", classAttendanceSummary(state))
    }

    @Test
    fun savingSendsTheWholeRosterNotOnlyTheModifiedStudents() = runTest {
        val spy = RecordingAttendanceRepository(MockAttendanceRepositoryImpl())
        val viewModel = viewModelFor(spy, assignedTeacherSession(), this)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        val target = viewModel.state.value.roster.first().student.id
        viewModel.setStatus(target, AttendanceStatus.RETARDO)

        viewModel.save()
        advanceUntilIdle()

        val sent = assertNotNull(spy.lastSavedEntries)
        assertEquals(5, sent.size)
        assertEquals(1, sent.count { it.second == AttendanceStatus.RETARDO })
        assertEquals(4, sent.count { it.second == AttendanceStatus.PRESENTE })
    }

    @Test
    fun savingWithoutAnyExceptionStillPersistsTheWholeGroup() = runTest {
        val spy = RecordingAttendanceRepository(MockAttendanceRepositoryImpl())
        val viewModel = viewModelFor(spy, assignedTeacherSession(), this)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(5, assertNotNull(spy.lastSavedEntries).size)
        val state = viewModel.state.value
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.canSave)
        assertEquals("Asistencia guardada", classAttendanceSaveLabel(state))
        assertEquals("Asistencia guardada para $date.", state.successMessage)
    }

    @Test
    fun secondTapWhileSavingDoesNotSubmitTwice() = runTest {
        val scope = TestScope(StandardTestDispatcher(testScheduler))
        val spy = RecordingAttendanceRepository(MockAttendanceRepositoryImpl())
        val viewModel = viewModelFor(spy, assignedTeacherSession(), scope)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()

        viewModel.save()
        // Sin dejar avanzar la corrutina: el segundo toque cae con saving = true.
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, spy.saveCallCount)
    }

    @Test
    fun failedSaveKeepsTheDraftAndShowsARecoverableMessage() = runTest {
        val failing = FailingSaveRepository(MockAttendanceRepositoryImpl())
        val viewModel = viewModelFor(failing, assignedTeacherSession(), this)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        val target = viewModel.state.value.roster.first().student.id
        viewModel.setStatus(target, AttendanceStatus.AUSENTE)

        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AttendanceFailureReason.NETWORK, state.error)
        assertEquals(AttendanceStatus.AUSENTE, state.roster.first { it.student.id == target }.status)
        assertTrue(state.hasUnsavedChanges)
        assertFalse(state.saving)
        assertNotNull(attendanceErrorMessage(state.error))
    }

    @Test
    fun reopeningAfterSavingRecoversTheCaptureWithoutUnsavedChanges() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val viewModel = viewModelFor(repository, assignedTeacherSession(), this)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        val target = viewModel.state.value.roster[1].student.id
        viewModel.setStatus(target, AttendanceStatus.RETARDO)
        viewModel.save()
        advanceUntilIdle()

        viewModel.closeGroup()
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AttendanceStatus.RETARDO, state.roster.first { it.student.id == target }.status)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun unassignedTeacherOpeningAForeignGroupGetsAControlledFailure() = runTest {
        val viewModel = viewModelFor(MockAttendanceRepositoryImpl(), unassignedTeacherSession(), this)

        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.snapshot)
        assertEquals(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP, state.error)
        assertNotNull(attendanceErrorMessage(state.error))
    }

    @Test
    fun markAllPresentRestoresTheInitialState() = runTest {
        val viewModel = viewModelFor(MockAttendanceRepositoryImpl(), assignedTeacherSession(), this)
        viewModel.openGroup(MockAttendanceData.GROUP_1A_ID)
        advanceUntilIdle()
        viewModel.setStatus(viewModel.state.value.roster.first().student.id, AttendanceStatus.AUSENTE)

        viewModel.markAllPresent()

        assertEquals(5, viewModel.state.value.presentCount)
    }
}

/** Envoltura que captura exactamente lo que la UI envio al guardar. */
private class RecordingAttendanceRepository(
    private val delegate: AttendanceRepository
) : AttendanceRepository {
    var lastSavedEntries: List<Pair<String, AttendanceStatus>>? = null
        private set
    var saveCallCount: Int = 0
        private set

    override suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>> =
        delegate.groupsForTeacher(session)

    override suspend fun openClassSession(
        session: AuthSession,
        groupId: String,
        date: String
    ): AttendanceResult<ClassAttendanceSnapshot> = delegate.openClassSession(session, groupId, date)

    override suspend fun saveClassAttendance(
        session: AuthSession,
        classSessionId: String,
        entries: List<Pair<String, AttendanceStatus>>
    ): AttendanceResult<ClassAttendanceSnapshot> {
        saveCallCount += 1
        lastSavedEntries = entries
        return delegate.saveClassAttendance(session, classSessionId, entries)
    }
}

private class FailingSaveRepository(
    private val delegate: AttendanceRepository
) : AttendanceRepository {
    override suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>> =
        delegate.groupsForTeacher(session)

    override suspend fun openClassSession(
        session: AuthSession,
        groupId: String,
        date: String
    ): AttendanceResult<ClassAttendanceSnapshot> = delegate.openClassSession(session, groupId, date)

    override suspend fun saveClassAttendance(
        session: AuthSession,
        classSessionId: String,
        entries: List<Pair<String, AttendanceStatus>>
    ): AttendanceResult<ClassAttendanceSnapshot> =
        AttendanceResult.Failure(AttendanceFailureReason.NETWORK)
}
