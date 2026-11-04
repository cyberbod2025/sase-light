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

        val totalApplicableOpportunities = studentMetrics.sumOf { it.applicableSessions }
        val totalAttendanceEvents = studentMetrics.sumOf { it.attendedSessions }
        val totalAbsenceEvents = studentMetrics.sumOf { it.absentSessions }
        val totalCapturedRecords = totalAttendanceEvents + totalAbsenceEvents
        val totalPendingRecords = studentMetrics.sumOf { it.pendingSessions }
        val totalTardinessEvents = studentMetrics.sumOf { it.tardinessEvents }
        val distinctStudentsWithTardiness = studentMetrics.count { it.tardinessEvents > 0 }

        val coveragePct = if (totalApplicableOpportunities > 0) {
            ((totalCapturedRecords.toDouble() / totalApplicableOpportunities.toDouble()) * 1000.0).toInt() / 10.0
        } else {
            100.0
        }
        val isCompleteCoverage = totalApplicableOpportunities > 0 && totalCapturedRecords == totalApplicableOpportunities

        val overallRate = if (totalCapturedRecords > 0) {
            (totalAttendanceEvents.toDouble() / totalCapturedRecords.toDouble()) * 100.0
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
                totalApplicableOpportunities = totalApplicableOpportunities,
                totalCapturedRecords = totalCapturedRecords,
                totalAttendanceEvents = totalAttendanceEvents,
                totalAbsenceEvents = totalAbsenceEvents,
                totalPendingRecords = totalPendingRecords,
                coveragePct = coveragePct,
                isCompleteCoverage = isCompleteCoverage,
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
        val movements = movementsProvider?.invoke().orEmpty()
        if (movements.isEmpty()) return MockAttendanceData.defaultRosters[groupId].orEmpty()

        val allStudents = (MockAttendanceData.defaultRosters[MockAttendanceData.GROUP_1A_ID].orEmpty() +
                           MockAttendanceData.defaultRosters[MockAttendanceData.GROUP_2B_ID].orEmpty()).distinctBy { it.id }

        return allStudents.filter { student ->
            isStudentActiveInGroupOnDate(student.id, groupId, date)
        }
    }

    private fun resolveAllStudentsForGroupInPeriod(
        groupId: String,
        startDate: String,
        endDate: String
    ): List<GroupStudent> {
        val allStudents = (MockAttendanceData.defaultRosters[MockAttendanceData.GROUP_1A_ID].orEmpty() +
                           MockAttendanceData.defaultRosters[MockAttendanceData.GROUP_2B_ID].orEmpty()).distinctBy { it.id }
        val movements = movementsProvider?.invoke().orEmpty()
        if (movements.isEmpty()) {
            return MockAttendanceData.defaultRosters[groupId].orEmpty()
        }

        return allStudents.filter { student ->
            isStudentActiveInGroupOnDate(student.id, groupId, startDate) ||
            isStudentActiveInGroupOnDate(student.id, groupId, endDate) ||
            movements.any { move ->
                move.studentId == student.id &&
                move.effectiveDate in startDate..endDate &&
                isStudentActiveInGroupOnDate(student.id, groupId, move.effectiveDate)
            }
        }
    }

    private fun isStudentActiveInGroupOnDate(
        studentId: String,
        groupId: String,
        date: String
    ): Boolean {
        val movements = movementsProvider?.invoke().orEmpty()
        val defaultList1A = MockAttendanceData.defaultRosters[MockAttendanceData.GROUP_1A_ID].orEmpty().map { it.id }
        val defaultList2B = MockAttendanceData.defaultRosters[MockAttendanceData.GROUP_2B_ID].orEmpty().map { it.id }

        val originalGroupId = when {
            studentId in defaultList1A -> MockAttendanceData.GROUP_1A_ID
            studentId in defaultList2B -> MockAttendanceData.GROUP_2B_ID
            else -> groupId
        }

        if (movements.isEmpty()) {
            return originalGroupId == groupId
        }

        val supersededIds = movements
            .filter { it.isCorrection && !it.supersedesMovementId.isNullOrBlank() }
            .mapNotNull { it.supersedesMovementId }
            .toSet()

        val allMovesForStudent = movements
            .filter { it.studentId == studentId && it.id !in supersededIds }
            .sortedWith(SchoolMovementEvaluator.DETERMINISTIC_ORDER)

        if (allMovesForStudent.isEmpty()) {
            return originalGroupId == groupId
        }

        val studentMovesUntilDate = allMovesForStudent
            .filter { it.effectiveDate <= date }

        if (studentMovesUntilDate.isEmpty()) {
            return false // Ingreso futuro a la fecha evaluada
        }

        var currentActive = false
        var currentGroupId: String = originalGroupId

        for (move in studentMovesUntilDate) {
            when (move.movementType) {
                SchoolMovementType.INSCRIPCION_INICIAL,
                SchoolMovementType.ALTA_EXTEMPORANEA,
                SchoolMovementType.REINCORPORACION -> {
                    currentActive = true
                    val target = move.toGroup.orEmpty()
                    if (target.isNotBlank()) {
                        currentGroupId = if (target.contains("2-B") || target.contains("2B") || target.contains("2° B")) {
                            MockAttendanceData.GROUP_2B_ID
                        } else {
                            MockAttendanceData.GROUP_1A_ID
                        }
                    }
                }
                SchoolMovementType.CAMBIO_DE_GRUPO -> {
                    currentActive = true
                    val target = move.toGroup.orEmpty()
                    if (target.isNotBlank()) {
                        currentGroupId = if (target.contains("2-B") || target.contains("2B") || target.contains("2° B")) {
                            MockAttendanceData.GROUP_2B_ID
                        } else {
                            MockAttendanceData.GROUP_1A_ID
                        }
                    }
                }
                SchoolMovementType.TRASLADO_SALIENTE,
                SchoolMovementType.BAJA_ADMINISTRATIVA,
                SchoolMovementType.ABANDONO_CONFIRMADO,
                SchoolMovementType.PENDIENTE_ACLARACION -> {
                    currentActive = false
                }
            }
        }

        return currentActive && currentGroupId == groupId
    }
}
