package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.StaffRole

/**
 * Repositorio de asistencia en memoria, exclusivo de `DEMO_LOCAL`.
 *
 * Aplica LAS MISMAS invariantes que la base real (0004_class_attendance_slice.sql):
 * una sesion por grupo + docente + fecha, un registro por sesion + alumno, y
 * rechazo de guardados parciales. Asi el modo demo no puede "funcionar" con
 * reglas mas laxas que el backend.
 */
class MockAttendanceRepositoryImpl : AttendanceRepository {

    private data class SessionKey(
        val groupId: String,
        val membershipId: String,
        val date: String
    )

    private class StoredSession(
        val id: String,
        val key: SessionKey,
        val statuses: MutableMap<String, AttendanceStatus> = mutableMapOf()
    )

    private val sessions = mutableMapOf<SessionKey, StoredSession>()
    private val sessionsById = mutableMapOf<String, StoredSession>()

    override suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>> {
        if (session.activeRole != StaffRole.DOCENTE) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        val assigned = MockAttendanceData.assignments[session.membershipId].orEmpty()
        return AttendanceResult.Success(
            MockAttendanceData.groups.filter { it.id in assigned }
        )
    }

    override suspend fun openClassSession(
        session: AuthSession,
        groupId: String,
        date: String
    ): AttendanceResult<ClassAttendanceSnapshot> {
        val group = authorizedGroup(session, groupId)
            ?: return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)
        if (date.isBlank()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }

        val key = SessionKey(groupId, session.membershipId, date)
        val stored = sessions.getOrPut(key) {
            StoredSession(id = "class-session-${sessions.size + 1}", key = key)
                .also { sessionsById[it.id] = it }
        }
        return AttendanceResult.Success(snapshot(stored, group, date))
    }

    override suspend fun saveClassAttendance(
        session: AuthSession,
        classSessionId: String,
        entries: List<Pair<String, AttendanceStatus>>
    ): AttendanceResult<ClassAttendanceSnapshot> {
        // Sesion inexistente y sesion ajena responden igual: nunca se revela la
        // existencia de la sesion de otro docente.
        val stored = sessionsById[classSessionId]
            ?: return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)
        if (stored.key.membershipId != session.membershipId) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)
        }
        val group = authorizedGroup(session, stored.key.groupId)
            ?: return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)

        val roster = MockAttendanceData.rosters[group.id].orEmpty()
        val submitted = entries.map { it.first }
        if (submitted.size != submitted.toSet().size) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        if (submitted.any { id -> roster.none { it.id == id } }) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        // El roster completo es obligatorio: guardar solo el diff no persiste
        // una captura donde el docente dejo a todos en PRESENTE.
        if (submitted.toSet() != roster.map { it.id }.toSet()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INCOMPLETE_ROSTER)
        }

        entries.forEach { (studentId, status) -> stored.statuses[studentId] = status }
        return AttendanceResult.Success(snapshot(stored, group, stored.key.date))
    }

    override suspend fun resetDemoData() {
        sessions.clear()
        sessionsById.clear()
    }

    private fun authorizedGroup(session: AuthSession, groupId: String): TeacherGroup? {
        if (session.activeRole != StaffRole.DOCENTE) return null
        val assigned = MockAttendanceData.assignments[session.membershipId].orEmpty()
        if (groupId !in assigned) return null
        return MockAttendanceData.groups.firstOrNull { it.id == groupId }
    }

    private fun snapshot(
        stored: StoredSession,
        group: TeacherGroup,
        date: String
    ): ClassAttendanceSnapshot = ClassAttendanceSnapshot(
        sessionId = stored.id,
        groupId = group.id,
        groupName = group.name,
        gradeLabel = group.gradeLabel,
        date = date,
        entries = MockAttendanceData.rosters[group.id].orEmpty()
            .sortedBy { it.listNumber }
            .map { student ->
                val recorded = stored.statuses[student.id]
                AttendanceEntry(
                    student = student,
                    status = recorded ?: AttendanceStatus.PRESENTE,
                    recorded = recorded != null
                )
            }
    )
}
