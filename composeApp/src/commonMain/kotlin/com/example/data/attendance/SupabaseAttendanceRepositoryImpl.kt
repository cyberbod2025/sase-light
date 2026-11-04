package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.StaffRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
internal data class RemoteSchoolGroupRow(
    val id: String,
    val name: String,
    @SerialName("grade_label") val gradeLabel: String,
    @SerialName("school_cycle_id") val schoolCycleId: String,
    @SerialName("institution_id") val institutionId: String
)

/**
 * Adaptador de persistencia remota para Asistencia y Puntualidad contra Supabase.
 *
 * POLÍTICA DE SEGURIDAD Y DESPLIEGUE:
 * En entornos conectados donde la migración 0030 no ha sido formalmente verificada
 * en el catálogo remoto, [remoteMigrationVerified] permanece en `false` por omisión,
 * bloqueando de forma segura (fail-closed) cualquier operación remota con
 * [AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY], sin degradarse silenciosamente a mocks.
 */
class SupabaseAttendanceRepositoryImpl(
    private val baseUrl: String,
    private val apiKey: String,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
    private val remoteMigrationVerified: Boolean = false
) : AttendanceRepository {

    override val isAvailableInCurrentEnvironment: Boolean
        get() = remoteMigrationVerified

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>> {
        if (!isAvailableInCurrentEnvironment) {
            return AttendanceResult.Failure(AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY)
        }
        if (session.activeRole != StaffRole.DOCENTE && session.activeRole != StaffRole.DIRECCION) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        return try {
            val response: HttpResponse = httpClient.get("$baseUrl/rest/v1/school_groups") {
                authHeaders(session)
                url {
                    parameters.append("select", "id,name,grade_label,school_cycle_id,institution_id")
                    parameters.append("order", "name.asc")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                return AttendanceResult.Failure(reasonForStatus(response.status, response.bodyAsText()))
            }
            val rows: List<RemoteSchoolGroupRow> = response.body()
            AttendanceResult.Success(
                rows.map {
                    TeacherGroup(
                        id = it.id,
                        name = it.name,
                        gradeLabel = it.gradeLabel,
                        schoolCycleId = it.schoolCycleId,
                        institutionId = it.institutionId
                    )
                }
            )
        } catch (e: Exception) {
            println("[SupabaseAttendanceRepositoryImpl] groupsForTeacher failed: ${e.message}")
            AttendanceResult.Failure(AttendanceFailureReason.NETWORK)
        }
    }

    override suspend fun scheduleOrOpenSession(
        session: AuthSession,
        groupId: String,
        date: String,
        moduleNumber: Int,
        status: SessionStatus,
        notes: String?
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (!isAvailableInCurrentEnvironment) {
            return AttendanceResult.Failure(AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY)
        }
        if (session.activeRole != StaffRole.DOCENTE && session.activeRole != StaffRole.DIRECCION) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        if (groupId.isBlank() || date.isBlank() || moduleNumber !in 1..8) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val payload = buildJsonObject {
            put("p_group_id", groupId)
            put("p_date", date)
            put("p_module_number", moduleNumber)
            put("p_status", status.name)
            notes?.let { put("p_notes", it) }
        }
        return callSnapshotRpc(session, "sase_open_or_schedule_class_session", payload)
    }

    override suspend fun updateSessionStatus(
        session: AuthSession,
        sessionId: String,
        status: SessionStatus,
        notes: String?
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (!isAvailableInCurrentEnvironment) {
            return AttendanceResult.Failure(AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY)
        }
        if (sessionId.isBlank()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val payload = buildJsonObject {
            put("p_session_id", sessionId)
            put("p_status", status.name)
            notes?.let { put("p_notes", it) }
        }
        return callSnapshotRpc(session, "sase_update_class_session_status", payload)
    }

    override suspend fun saveAttendance(
        session: AuthSession,
        sessionId: String,
        entries: List<AttendanceRecordEntry>
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (!isAvailableInCurrentEnvironment) {
            return AttendanceResult.Failure(AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY)
        }
        if (sessionId.isBlank()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val payload = buildJsonObject {
            put("p_session_id", sessionId)
            put(
                "p_entries",
                buildJsonArray {
                    for (entry in entries) {
                        add(
                            buildJsonObject {
                                put("student_id", entry.studentId)
                                entry.presence?.let { put("presence", it.name) }
                                put("punctuality", entry.punctuality.name)
                                put("minutes_late", entry.minutesLate)
                                entry.justification?.let { put("justification", it) }
                            }
                        )
                    }
                }
            )
        }
        return callSnapshotRpc(session, "sase_save_class_attendance", payload)
    }

    override suspend fun getGroupAttendanceReport(
        session: AuthSession,
        groupId: String,
        startDate: String,
        endDate: String,
        thresholds: AttendanceThresholds
    ): AttendanceResult<ClassAttendanceReport> {
        if (!isAvailableInCurrentEnvironment) {
            return AttendanceResult.Failure(AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY)
        }
        if (groupId.isBlank() || startDate.isBlank() || endDate.isBlank()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val payload = buildJsonObject {
            put("p_group_id", groupId)
            put("p_start_date", startDate)
            put("p_end_date", endDate)
            put("p_min_attendance_rate", thresholds.minAttendanceRatePct)
            put("p_max_tardiness", thresholds.maxTardinessEvents)
        }
        return try {
            val response: HttpResponse = httpClient.post("$baseUrl/rest/v1/rpc/sase_get_group_attendance_report") {
                authHeaders(session)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val text = response.bodyAsText()
            if (response.status != HttpStatusCode.OK) {
                AttendanceResult.Failure(reasonForStatus(response.status, text))
            } else {
                parseReport(text, thresholds)
            }
        } catch (e: Exception) {
            println("[SupabaseAttendanceRepositoryImpl] getGroupAttendanceReport failed: ${e.message}")
            AttendanceResult.Failure(AttendanceFailureReason.NETWORK)
        }
    }

    private suspend fun callSnapshotRpc(
        session: AuthSession,
        function: String,
        payload: JsonObject
    ): AttendanceResult<ClassAttendanceSnapshot> = try {
        val response: HttpResponse = httpClient.post("$baseUrl/rest/v1/rpc/$function") {
            authHeaders(session)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        val text = response.bodyAsText()
        if (response.status != HttpStatusCode.OK) {
            AttendanceResult.Failure(reasonForStatus(response.status, text))
        } else {
            parseSnapshot(text)
        }
    } catch (e: Exception) {
        println("[SupabaseAttendanceRepositoryImpl] callSnapshotRpc($function) failed: ${e.message}")
        AttendanceResult.Failure(AttendanceFailureReason.NETWORK)
    }

    private fun parseSnapshot(body: String): AttendanceResult<ClassAttendanceSnapshot> {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val rawRoster = root.getValue("roster").jsonArray
            val entries = rawRoster.map { element ->
                val row = element.jsonObject
                val presence = AttendancePresence.fromCode(row["presence"]?.jsonPrimitive?.content)
                val punctuality = PunctualityStatus.fromCode(row["punctuality"]?.jsonPrimitive?.content)
                val minutesLate = row["minutes_late"]?.jsonPrimitive?.intOrNull ?: 0
                val justification = row["justification"]?.jsonPrimitive?.content
                val isRecorded = row["recorded"]?.jsonPrimitive?.content?.toBoolean() ?: false

                AttendanceStudentEntry(
                    student = GroupStudent(
                        id = row.getValue("student_id").jsonPrimitive.content,
                        fullName = row.getValue("full_name").jsonPrimitive.content,
                        listNumber = row.getValue("list_number").jsonPrimitive.int
                    ),
                    presence = presence,
                    punctuality = punctuality,
                    minutesLate = minutesLate,
                    justification = justification,
                    isRecorded = isRecorded
                )
            }
            AttendanceResult.Success(
                ClassAttendanceSnapshot(
                    sessionId = root.getValue("session_id").jsonPrimitive.content,
                    groupId = root.getValue("group_id").jsonPrimitive.content,
                    groupName = root.getValue("group_name").jsonPrimitive.content,
                    gradeLabel = root.getValue("grade_label").jsonPrimitive.content,
                    date = root.getValue("session_date").jsonPrimitive.content,
                    moduleNumber = root["module_number"]?.jsonPrimitive?.intOrNull ?: 1,
                    sessionStatus = SessionStatus.fromCode(root["session_status"]?.jsonPrimitive?.content ?: "PROGRAMADA"),
                    notes = root["notes"]?.jsonPrimitive?.content,
                    entries = entries.sortedBy { it.student.listNumber }
                )
            )
        } catch (e: Exception) {
            println("[SupabaseAttendanceRepositoryImpl] parseSnapshot failed: ${e.message}")
            AttendanceResult.Failure(AttendanceFailureReason.UNEXPECTED)
        }
    }

    private fun parseReport(body: String, thresholds: AttendanceThresholds): AttendanceResult<ClassAttendanceReport> {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val rawMetrics = root.getValue("student_metrics").jsonArray
            val metrics = rawMetrics.map { element ->
                val row = element.jsonObject
                StudentAttendanceMetric(
                    studentId = row.getValue("student_id").jsonPrimitive.content,
                    fullName = row.getValue("full_name").jsonPrimitive.content,
                    assignedGroup = row.getValue("assigned_group").jsonPrimitive.content,
                    listNumber = row.getValue("list_number").jsonPrimitive.int,
                    applicableSessions = row.getValue("applicable_sessions").jsonPrimitive.int,
                    attendedSessions = row.getValue("attended_sessions").jsonPrimitive.int,
                    absentSessions = row.getValue("absent_sessions").jsonPrimitive.int,
                    pendingSessions = row.getValue("pending_sessions").jsonPrimitive.int,
                    tardinessEvents = row.getValue("tardiness_events").jsonPrimitive.int,
                    attendanceRatePct = row.getValue("attendance_rate_pct").jsonPrimitive.content.toDouble(),
                    hasAttendanceAlert = row.getValue("has_attendance_alert").jsonPrimitive.content.toBoolean(),
                    hasTardinessAlert = row.getValue("has_tardiness_alert").jsonPrimitive.content.toBoolean()
                )
            }
            val totalAtt = root.getValue("total_attendance_events").jsonPrimitive.int
            val totalAbs = root.getValue("total_absence_events").jsonPrimitive.int
            val totalApplicable = root["total_applicable_opportunities"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: metrics.sumOf { it.applicableSessions }
            val totalCaptured = root["total_captured_records"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: (totalAtt + totalAbs)
            val totalPending = root["total_pending_records"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: metrics.sumOf { it.pendingSessions }
            val coverage = root["coverage_pct"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: if (totalApplicable > 0) ((totalCaptured.toDouble() / totalApplicable.toDouble()) * 1000.0).toInt() / 10.0 else 100.0
            val isComplete = totalApplicable > 0 && totalCaptured == totalApplicable

            AttendanceResult.Success(
                ClassAttendanceReport(
                    groupId = root.getValue("group_id").jsonPrimitive.content,
                    groupName = root.getValue("group_name").jsonPrimitive.content,
                    periodStart = root.getValue("period_start").jsonPrimitive.content,
                    periodEnd = root.getValue("period_end").jsonPrimitive.content,
                    totalSessionsScheduled = root.getValue("total_sessions_scheduled").jsonPrimitive.int,
                    sessionsHeld = root.getValue("sessions_held").jsonPrimitive.int,
                    sessionsCancelled = root.getValue("sessions_cancelled").jsonPrimitive.int,
                    sessionsNonTeaching = root.getValue("sessions_non_teaching").jsonPrimitive.int,
                    totalApplicableOpportunities = totalApplicable,
                    totalCapturedRecords = totalCaptured,
                    totalAttendanceEvents = totalAtt,
                    totalAbsenceEvents = totalAbs,
                    totalPendingRecords = totalPending,
                    coveragePct = coverage,
                    isCompleteCoverage = isComplete,
                    totalTardinessEvents = root.getValue("total_tardiness_events").jsonPrimitive.int,
                    distinctStudentsWithTardiness = root.getValue("distinct_students_with_tardiness").jsonPrimitive.int,
                    overallAttendanceRatePct = root.getValue("overall_attendance_rate_pct").jsonPrimitive.content.toDouble(),
                    thresholds = thresholds,
                    studentMetrics = metrics.sortedBy { it.listNumber }
                )
            )
        } catch (e: Exception) {
            println("[SupabaseAttendanceRepositoryImpl] parseReport failed: ${e.message}")
            AttendanceResult.Failure(AttendanceFailureReason.UNEXPECTED)
        }
    }

    private fun reasonForStatus(status: HttpStatusCode, body: String): AttendanceFailureReason = when {
        body.contains("SASE_ATTENDANCE_DUPLICATE_SESSION") -> AttendanceFailureReason.DUPLICATE_SESSION
        body.contains("SASE_ATTENDANCE_FORBIDDEN") -> AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP
        body.contains("SASE_ATTENDANCE_INVALID") -> AttendanceFailureReason.INVALID_DATA
        body.contains("SASE_ATTENDANCE_NOT_FOUND") -> AttendanceFailureReason.SESSION_NOT_FOUND
        status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden -> AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP
        status == HttpStatusCode.NotFound -> AttendanceFailureReason.SESSION_NOT_FOUND
        else -> AttendanceFailureReason.UNEXPECTED
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeaders(session: AuthSession) {
        header("apikey", apiKey)
        header("Authorization", "Bearer ${session.accessToken}")
    }
}
