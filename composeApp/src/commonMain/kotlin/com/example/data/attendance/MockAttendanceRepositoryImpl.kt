package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.StaffRole
import com.example.data.enrollment.SchoolMovementEvaluator
import com.example.data.enrollment.SchoolMovementRecord
import com.example.data.enrollment.SchoolMovementType
import com.example.formatTimestamp

/**
 * Implementación en memoria del repositorio de asistencia para `DEMO_LOCAL` y pruebas automatizadas.
 *
 * Aplica reglas estrictas de:
 * 1. Autorización por rol institucional y asignación docente.
 * 2. Unicidad de sesión por (grupo, docente, fecha, módulo).
 * 3. Distinción entre sesión programada, cancelada y no lectiva.
 * 4. Captura pendiente, parcial y completa sin transformar pendientes en inasistencias.
 * 5. Evaluación de vigencia del alumno según movimientos escolares fechados.
 * 6. Conteo diferenciado de eventos de retardo vs alumnos distintos con retardo.
 */
class MockAttendanceRepositoryImpl(
    private val movementsProvider: (() -> List<SchoolMovementRecord>)? = null,
    private val customRosterProvider: ((groupId: String, date: String) -> List<GroupStudent>)? = null
) : AttendanceRepository {

    private data class SessionKey(
        val groupId: String,
        val teacherMembershipId: String,
        val date: String,
        val moduleNumber: Int
    )

    private class StoredSession(
        val id: String,
        val key: SessionKey,
        var status: SessionStatus,
        var notes: String?,
        val records: MutableMap<String, AttendanceRecordEntry> = mutableMapOf()
    )

    private val sessionsByKey = mutableMapOf<SessionKey, StoredSession>()
    private val sessionsById = mutableMapOf<String, StoredSession>()

    override suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>> {
        if (session.activeRole != StaffRole.DOCENTE && session.activeRole != StaffRole.DIRECCION) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        val assigned = if (session.activeRole == StaffRole.DIRECCION) {
            MockAttendanceData.groups.map { it.id }
        } else {
            MockAttendanceData.teacherAssignments[session.membershipId].orEmpty()
        }
        return AttendanceResult.Success(
            MockAttendanceData.groups.filter { it.id in assigned }
        )
    }

    override suspend fun scheduleOrOpenSession(
        session: AuthSession,
        groupId: String,
        date: String,
        moduleNumber: Int,
        status: SessionStatus,
        notes: String?
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (groupId.isBlank() || date.isBlank() || moduleNumber !in 1..8) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val group = checkGroupAuthorization(session, groupId)
            ?: return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)

        val key = SessionKey(
            groupId = groupId,
            teacherMembershipId = session.membershipId,
            date = date,
            moduleNumber = moduleNumber
        )

        val stored = sessionsByKey.getOrPut(key) {
            val newId = "session-${groupId}-$date-m$moduleNumber"
            StoredSession(
                id = newId,
                key = key,
                status = status,
                notes = notes
            ).also { sessionsById[newId] = it }
        }

        return AttendanceResult.Success(buildSnapshot(stored, group))
    }

    override suspend fun updateSessionStatus(
        session: AuthSession,
        sessionId: String,
        status: SessionStatus,
        notes: String?
    ): AttendanceResult<ClassAttendanceSnapshot> {
        val stored = sessionsById[sessionId]
            ?: return AttendanceResult.Failure(AttendanceFailureReason.SESSION_NOT_FOUND)
        val group = checkGroupAuthorization(session, stored.key.groupId)
            ?: return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)

        stored.status = status
        if (notes != null) {
            stored.notes = notes
        }
        return AttendanceResult.Success(buildSnapshot(stored, group))
    }

    override suspend fun saveAttendance(
        session: AuthSession,
        sessionId: String,
        entries: List<AttendanceRecordEntry>
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (sessionId.isBlank()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val stored = sessionsById[sessionId]
            ?: return AttendanceResult.Failure(AttendanceFailureReason.SESSION_NOT_FOUND)
        val group = checkGroupAuthorization(session, stored.key.groupId)
            ?: return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)

        val activeRoster = resolveRosterForGroupAndDate(group.id, stored.key.date)
        val activeIds = activeRoster.map { it.id }.toSet()

        for (entry in entries) {
            if (entry.studentId !in activeIds) {
                return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
            }
            if (entry.presence == AttendancePresence.AUSENTE && entry.punctuality != PunctualityStatus.NO_APLICA) {
                return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
            }
            if (entry.punctuality == PunctualityStatus.RETARDO && entry.presence != AttendancePresence.PRESENTE) {
                return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
            }
        }

        val nowIso = formatTimestamp("yyyy-MM-dd'T'HH:mm:ss'Z'")
        for (entry in entries) {
            stored.records[entry.studentId] = entry.copy(
                recordedAt = nowIso,
                recordedBy = session.profile.email
            )
        }

        return AttendanceResult.Success(buildSnapshot(stored, group))
    }

    override suspend fun getGroupAttendanceReport(
        session: AuthSession,
        groupId: String,
        startDate: String,
        endDate: String,
        thresholds: AttendanceThresholds
    ): AttendanceResult<ClassAttendanceReport> {
        if (groupId.isBlank() || startDate.isBlank() || endDate.isBlank() || startDate > endDate) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val isAuthorized = session.activeRole in setOf(
            StaffRole.DOCENTE,
            StaffRole.DIRECCION,
            StaffRole.SECRETARIA
        )
        if (!isAuthorized) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP)
        }
        val group = MockAttendanceData.groups.firstOrNull { it.id == groupId }
            ?: return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)

        // Obtener sesiones para este grupo en el periodo
        val groupSessions = sessionsByKey.values
            .filter { it.key.groupId == groupId && it.key.date in startDate..endDate }
            .sortedWith(compareBy({ it.key.date }, { it.key.moduleNumber }))

        val scheduledCount = groupSessions.size
        val heldSessions = groupSessions.filter { it.status == SessionStatus.PROGRAMADA }
        val cancelledCount = groupSessions.count { it.status == SessionStatus.CANCELADA }
        val nonTeachingCount = groupSessions.count { it.status == SessionStatus.NO_LECTIVA }

        // Resolver estudiantes que alguna vez estuvieron o están en este grupo
        val allStudentsInGroup = resolveAllStudentsForGroupInPeriod(groupId, startDate, endDate)

        val studentMetrics = allStudentsInGroup.map { student ->
            // Determinar sesiones aplicables a este estudiante según movimientos escolares
            val applicableSessions = heldSessions.filter { session ->
                isStudentActiveInGroupOnDate(student.id, groupId, session.key.date)
            }

            var attendedCount = 0
            var absentCount = 0
            var pendingCount = 0
            var tardinessCount = 0

            for (session in applicableSessions) {
                val record = session.records[student.id]
                when (record?.presence) {
                    AttendancePresence.PRESENTE -> {
                        attendedCount++
                        if (record.punctuality == PunctualityStatus.RETARDO) {
                            tardinessCount++
                        }
                    }
                    AttendancePresence.AUSENTE -> {
                        absentCount++
                    }
                    null -> {
                        // Captura pendiente: NO se cuenta como falta
                        pendingCount++
                    }
                }
            }

            val decidedSessions = attendedCount + absentCount
            val attendanceRate = if (applicableSessions.isNotEmpty()) {
                val base = if (decidedSessions > 0) decidedSessions else applicableSessions.size
                (attendedCount.toDouble() / base.toDouble()) * 100.0
            } else {
                100.0
            }
            val roundedRate = (attendanceRate * 10.0).toInt() / 10.0

            StudentAttendanceMetric(
                studentId = student.id,
                fullName = student.fullName,
                assignedGroup = group.name,
                listNumber = student.listNumber,
                applicableSessions = applicableSessions.size,
                attendedSessions = attendedCount,
                absentSessions = absentCount,
                pendingSessions = pendingCount,
                tardinessEvents = tardinessCount,
                attendanceRatePct = roundedRate,
                hasAttendanceAlert = roundedRate < thresholds.minAttendanceRatePct && decidedSessions > 0,
                hasTardinessAlert = tardinessCount >= thresholds.maxTardinessEvents
            )
        }

        val totalAttendanceEvents = studentMetrics.sumOf { it.attendedSessions }
        val totalAbsenceEvents = studentMetrics.sumOf { it.absentSessions }
        val totalTardinessEvents = studentMetrics.sumOf { it.tardinessEvents }
        val distinctStudentsWithTardiness = studentMetrics.count { it.tardinessEvents > 0 }

        val totalDecided = totalAttendanceEvents + totalAbsenceEvents
        val overallRate = if (totalDecided > 0) {
            (totalAttendanceEvents.toDouble() / totalDecided.toDouble()) * 100.0
        } else {
            100.0
        }

        return AttendanceResult.Success(
            ClassAttendanceReport(
                groupId = groupId,
                groupName = group.name,
                periodStart = startDate,
                periodEnd = endDate,
                totalSessionsScheduled = scheduledCount,
                sessionsHeld = heldSessions.size,
                sessionsCancelled = cancelledCount,
                sessionsNonTeaching = nonTeachingCount,
                totalAttendanceEvents = totalAttendanceEvents,
                totalAbsenceEvents = totalAbsenceEvents,
                totalTardinessEvents = totalTardinessEvents,
                distinctStudentsWithTardiness = distinctStudentsWithTardiness,
                overallAttendanceRatePct = (overallRate * 10.0).toInt() / 10.0,
                thresholds = thresholds,
                studentMetrics = studentMetrics.sortedBy { it.listNumber }
            )
        )
    }

    override suspend fun resetDemoData() {
        sessionsByKey.clear()
        sessionsById.clear()
    }

    private fun checkGroupAuthorization(session: AuthSession, groupId: String): TeacherGroup? {
        if (session.activeRole == StaffRole.DIRECCION) {
            return MockAttendanceData.groups.firstOrNull { it.id == groupId }
        }
        if (session.activeRole != StaffRole.DOCENTE) return null
        val assigned = MockAttendanceData.teacherAssignments[session.membershipId].orEmpty()
        if (groupId !in assigned) return null
        return MockAttendanceData.groups.firstOrNull { it.id == groupId }
    }

    private fun buildSnapshot(
        stored: StoredSession,
        group: TeacherGroup
    ): ClassAttendanceSnapshot {
        val roster = resolveRosterForGroupAndDate(group.id, stored.key.date)
        val entries = roster.map { student ->
            val record = stored.records[student.id]
            AttendanceStudentEntry(
                student = student,
                presence = record?.presence,
                punctuality = record?.punctuality ?: PunctualityStatus.NO_APLICA,
                minutesLate = record?.minutesLate ?: 0,
                justification = record?.justification,
                isRecorded = record != null && record.presence != null
            )
        }
        return ClassAttendanceSnapshot(
            sessionId = stored.id,
            groupId = group.id,
            groupName = group.name,
            gradeLabel = group.gradeLabel,
            date = stored.key.date,
            moduleNumber = stored.key.moduleNumber,
            sessionStatus = stored.status,
            notes = stored.notes,
            entries = entries.sortedBy { it.student.listNumber }
        )
    }

    private fun resolveRosterForGroupAndDate(groupId: String, date: String): List<GroupStudent> {
        customRosterProvider?.let { return it(groupId, date) }
        val defaultList = MockAttendanceData.defaultRosters[groupId].orEmpty()
        val movements = movementsProvider?.invoke().orEmpty()
        if (movements.isEmpty()) return defaultList

        return defaultList.filter { student ->
            isStudentActiveInGroupOnDate(student.id, groupId, date)
        }
    }

    private fun resolveAllStudentsForGroupInPeriod(
        groupId: String,
        startDate: String,
        endDate: String
    ): List<GroupStudent> {
        val defaultList = MockAttendanceData.defaultRosters[groupId].orEmpty()
        val movements = movementsProvider?.invoke().orEmpty()
        if (movements.isEmpty()) return defaultList

        // Incluye a cualquier alumno que haya estado activo al menos un día en el periodo
        return defaultList.filter { student ->
            val moves = movements.filter { it.studentId == student.id }
            if (moves.isEmpty()) return@filter true
            // Evaluar si estuvo activo en alguna fecha del rango
            moves.any { it.effectiveDate <= endDate }
        }
    }

    private fun isStudentActiveInGroupOnDate(
        studentId: String,
        groupId: String,
        date: String
    ): Boolean {
        val movements = movementsProvider?.invoke().orEmpty()
        if (movements.isEmpty()) return true

        val supersededIds = movements
            .filter { it.isCorrection && !it.supersedesMovementId.isNullOrBlank() }
            .mapNotNull { it.supersedesMovementId }
            .toSet()

        val allMovesForStudent = movements
            .filter { it.studentId == studentId && it.id !in supersededIds }

        if (allMovesForStudent.isEmpty()) return true

        val studentMovesUntilDate = allMovesForStudent
            .filter { it.effectiveDate <= date }
            .sortedWith(SchoolMovementEvaluator.DETERMINISTIC_ORDER)

        if (studentMovesUntilDate.isEmpty()) return false // Aún no ingresa a la fecha

        val lastMove = studentMovesUntilDate.last()
        return when (lastMove.movementType) {
            SchoolMovementType.INSCRIPCION_INICIAL,
            SchoolMovementType.ALTA_EXTEMPORANEA,
            SchoolMovementType.REINCORPORACION -> true
            SchoolMovementType.CAMBIO_DE_GRUPO -> {
                val targetGroup = lastMove.toGroup.orEmpty()
                targetGroup.contains("A") && groupId.contains("1a", ignoreCase = true) ||
                targetGroup.contains("B") && groupId.contains("2b", ignoreCase = true)
            }
            SchoolMovementType.TRASLADO_SALIENTE,
            SchoolMovementType.BAJA_ADMINISTRATIVA,
            SchoolMovementType.ABANDONO_CONFIRMADO -> false
            SchoolMovementType.PENDIENTE_ACLARACION -> false
        }
    }
}
