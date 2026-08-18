package com.example.data.attendance

import com.example.data.auth.StaffRole
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MockAttendanceRepositoryImplTest {

    private val date = "2026-08-18"

    @Test
    fun teacherOnlySeesAssignedGroups() = runTest {
        val repository = MockAttendanceRepositoryImpl()

        val assigned = repository.groupsForTeacher(assignedTeacherSession())
        val other = repository.groupsForTeacher(unassignedTeacherSession())

        assertEquals(
            listOf(MockAttendanceData.GROUP_1A_ID),
            assertIs<AttendanceResult.Success<List<TeacherGroup>>>(assigned).value.map { it.id }
        )
        assertEquals(
            listOf(MockAttendanceData.GROUP_2B_ID),
            assertIs<AttendanceResult.Success<List<TeacherGroup>>>(other).value.map { it.id }
        )
    }

    @Test
    fun nonTeacherRoleGetsNoGroups() = runTest {
        val repository = MockAttendanceRepositoryImpl()

        val result = repository.groupsForTeacher(
            attendanceSession(MockAttendanceData.TEACHER_1A_MEMBERSHIP_ID, role = StaffRole.SECRETARIA)
        )

        assertEquals(
            AttendanceFailureReason.NOT_A_TEACHER,
            assertIs<AttendanceResult.Failure>(result).reason
        )
    }

    @Test
    fun openingAGroupNotAssignedIsRejected() = runTest {
        val repository = MockAttendanceRepositoryImpl()

        val result = repository.openClassSession(
            session = unassignedTeacherSession(),
            groupId = MockAttendanceData.GROUP_1A_ID,
            date = date
        )

        assertEquals(
            AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP,
            assertIs<AttendanceResult.Failure>(result).reason
        )
    }

    @Test
    fun openingTwiceRecoversTheSameSessionAndStartsAllPresent() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val session = assignedTeacherSession()

        val first = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, date)
        ).value
        val second = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, date)
        ).value

        assertEquals(first.sessionId, second.sessionId)
        assertEquals(5, first.entries.size)
        assertTrue(first.entries.all { it.status == AttendanceStatus.PRESENTE })
        assertFalse(first.hasSavedCapture)
    }

    @Test
    fun savingTheWholeRosterPersistsEvenWithoutExceptions() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val session = assignedTeacherSession()
        val opened = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, date)
        ).value

        // Nadie fue marcado: el docente deja a todo el grupo en PRESENTE.
        val saved = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.saveClassAttendance(
                session = session,
                classSessionId = opened.sessionId,
                entries = opened.entries.map { it.student.id to it.status }
            )
        ).value

        assertTrue(saved.entries.all { it.recorded })
        assertTrue(saved.hasSavedCapture)

        val reopened = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, date)
        ).value
        assertTrue(reopened.hasSavedCapture)
        assertEquals(saved.sessionId, reopened.sessionId)
    }

    @Test
    fun partialRosterIsRejected() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val session = assignedTeacherSession()
        val opened = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, date)
        ).value

        val result = repository.saveClassAttendance(
            session = session,
            classSessionId = opened.sessionId,
            entries = listOf(opened.entries.first().student.id to AttendanceStatus.AUSENTE)
        )

        assertEquals(
            AttendanceFailureReason.INCOMPLETE_ROSTER,
            assertIs<AttendanceResult.Failure>(result).reason
        )
    }

    @Test
    fun anotherTeacherCannotSaveIntoAnExistingSession() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val owner = assignedTeacherSession()
        val opened = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(owner, MockAttendanceData.GROUP_1A_ID, date)
        ).value

        val result = repository.saveClassAttendance(
            session = unassignedTeacherSession(),
            classSessionId = opened.sessionId,
            entries = opened.entries.map { it.student.id to it.status }
        )

        assertEquals(
            AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP,
            assertIs<AttendanceResult.Failure>(result).reason
        )
    }

    @Test
    fun modifyingAnExistingCaptureUpdatesWithoutDuplicating() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val session = assignedTeacherSession()
        val opened = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, date)
        ).value
        val target = opened.entries.first().student.id

        repository.saveClassAttendance(
            session,
            opened.sessionId,
            opened.entries.map { it.student.id to it.status }
        )
        val updated = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.saveClassAttendance(
                session,
                opened.sessionId,
                opened.entries.map { entry ->
                    entry.student.id to
                        if (entry.student.id == target) AttendanceStatus.RETARDO else entry.status
                }
            )
        ).value

        assertEquals(5, updated.entries.size)
        assertEquals(1, updated.entries.count { it.student.id == target })
        assertEquals(AttendanceStatus.RETARDO, updated.statusOf(target))
    }

    @Test
    fun capturesOfDifferentDatesDoNotShareSession() = runTest {
        val repository = MockAttendanceRepositoryImpl()
        val session = assignedTeacherSession()

        val today = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, "2026-08-18")
        ).value
        val tomorrow = assertIs<AttendanceResult.Success<ClassAttendanceSnapshot>>(
            repository.openClassSession(session, MockAttendanceData.GROUP_1A_ID, "2026-08-19")
        ).value

        assertTrue(today.sessionId != tomorrow.sessionId)
    }
}
