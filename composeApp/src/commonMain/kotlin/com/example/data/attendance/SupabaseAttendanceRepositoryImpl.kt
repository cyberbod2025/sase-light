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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
internal data class SchoolGroupRow(
    val id: String,
    val name: String,
    @SerialName("grade_label") val gradeLabel: String,
    @SerialName("school_cycle_id") val schoolCycleId: String
)

/**
 * AttendanceRepository real contra Supabase, con el mismo enfoque que
 * [com.example.data.auth.SupabaseAuthRepositoryImpl]: Ktor + PostgREST directo
 * (sin el SDK supabase-kt, que exige un toolchain superior al de este
 * proyecto) y `httpClient` inyectable para poder probar sin red.
 *
 * Reparto de responsabilidades, deliberado:
 *  - LECTURA de grupos: `select` normal sobre `school_groups`, filtrado por la
 *    politica RLS `school_groups_select_assigned_teacher`. El cliente no manda
 *    ningun filtro de autorizacion; si lo intentara, la RLS mandaria igual.
 *  - ESCRITURA: unicamente RPC `security definer` atomica
 *    (`sase_open_class_session`, `sase_save_class_attendance`). Las tablas de
 *    asistencia no otorgan INSERT/UPDATE a `authenticated`, asi que un cliente
 *    no puede fabricar un guardado parcial ni saltarse la validacion de roster
 *    completo.
 */
class SupabaseAttendanceRepositoryImpl(
    private val baseUrl: String,
    private val apiKey: String,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AttendanceRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>> {
        if (session.activeRole != StaffRole.DOCENTE) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        return try {
            val response: HttpResponse = httpClient.get("$baseUrl/rest/v1/school_groups") {
                authHeaders(session)
                url {
                    parameters.append("select", "id,name,grade_label,school_cycle_id")
                    parameters.append("order", "name.asc")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                return AttendanceResult.Failure(reasonForStatus(response.status, response.bodyAsText()))
            }
            val rows: List<SchoolGroupRow> = response.body()
            AttendanceResult.Success(
                rows.map {
                    TeacherGroup(
                        id = it.id,
                        name = it.name,
                        gradeLabel = it.gradeLabel,
                        schoolCycleId = it.schoolCycleId
                    )
                }
            )
        } catch (e: Exception) {
            AttendanceResult.Failure(AttendanceFailureReason.NETWORK)
        }
    }

    override suspend fun openClassSession(
        session: AuthSession,
        groupId: String,
        date: String
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (session.activeRole != StaffRole.DOCENTE) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        if (groupId.isBlank() || date.isBlank()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val payload = buildJsonObject {
            put("p_group_id", groupId)
            put("p_date", date)
        }
        return callSnapshotRpc(session, "sase_open_class_session", payload)
    }

    override suspend fun saveClassAttendance(
        session: AuthSession,
        classSessionId: String,
        entries: List<Pair<String, AttendanceStatus>>
    ): AttendanceResult<ClassAttendanceSnapshot> {
        if (session.activeRole != StaffRole.DOCENTE) {
            return AttendanceResult.Failure(AttendanceFailureReason.NOT_A_TEACHER)
        }
        if (classSessionId.isBlank() || entries.isEmpty()) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        val payload = buildJsonObject {
            put("p_session_id", classSessionId)
            put(
                "p_entries",
                buildJsonArray {
                    entries.forEach { (studentId, status) ->
                        add(
                            buildJsonObject {
                                put("student_id", studentId)
                                put("status", status.name)
                            }
                        )
                    }
                }
            )
        }
        return callSnapshotRpc(session, "sase_save_class_attendance", payload)
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
        AttendanceResult.Failure(AttendanceFailureReason.NETWORK)
    }

    private fun parseSnapshot(body: String): AttendanceResult<ClassAttendanceSnapshot> {
        return try {
        val root = json.parseToJsonElement(body) as JsonObject
        val rawRoster = root.getValue("roster").jsonArray
        val entries = rawRoster.mapNotNull { element ->
            val row = element as JsonObject
            val status = AttendanceStatus.fromCode(row.getValue("status").jsonPrimitive.content)
            status?.let {
                AttendanceEntry(
                    student = GroupStudent(
                        id = row.getValue("student_id").jsonPrimitive.content,
                        fullName = row.getValue("full_name").jsonPrimitive.content,
                        listNumber = row.getValue("list_number").jsonPrimitive.content.toInt()
                    ),
                    status = it,
                    recorded = row.getValue("recorded").jsonPrimitive.content.toBoolean()
                )
            }
        }
        // Un estado desconocido nunca se descarta en silencio: el roster
        // incompleto se trata como respuesta invalida.
        if (entries.size != rawRoster.size) {
            return AttendanceResult.Failure(AttendanceFailureReason.INVALID_DATA)
        }
        AttendanceResult.Success(
            ClassAttendanceSnapshot(
                sessionId = root.getValue("session_id").jsonPrimitive.content,
                groupId = root.getValue("group_id").jsonPrimitive.content,
                groupName = root.getValue("group_name").jsonPrimitive.content,
                gradeLabel = root.getValue("grade_label").jsonPrimitive.content,
                date = root.getValue("session_date").jsonPrimitive.content,
                entries = entries.sortedBy { it.student.listNumber }
            )
        )
        } catch (e: Exception) {
            AttendanceResult.Failure(AttendanceFailureReason.UNEXPECTED)
        }
    }

    /**
     * Traduce el fallo del servidor a vocabulario de dominio. Los codigos
     * `SASE_ATTENDANCE_*` los levanta 0004_class_attendance_slice.sql; el
     * cliente nunca muestra el texto crudo de Postgres.
     */
    private fun reasonForStatus(status: HttpStatusCode, body: String): AttendanceFailureReason = when {
        body.contains("SASE_ATTENDANCE_INCOMPLETE_ROSTER") -> AttendanceFailureReason.INCOMPLETE_ROSTER
        body.contains("SASE_ATTENDANCE_DUPLICATE_STUDENT") -> AttendanceFailureReason.INVALID_DATA
        body.contains("SASE_ATTENDANCE_STUDENT_NOT_IN_GROUP") -> AttendanceFailureReason.INVALID_DATA
        body.contains("SASE_ATTENDANCE_ENTRIES_INVALID") -> AttendanceFailureReason.INVALID_DATA
        body.contains("SASE_ATTENDANCE_ARGS_REQUIRED") -> AttendanceFailureReason.INVALID_DATA
        body.contains("SASE_ATTENDANCE_FORBIDDEN") -> AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP
        status == HttpStatusCode.Unauthorized -> AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP
        status == HttpStatusCode.Forbidden -> AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP
        status == HttpStatusCode.NotFound -> AttendanceFailureReason.SESSION_NOT_FOUND
        else -> AttendanceFailureReason.UNEXPECTED
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeaders(session: AuthSession) {
        header("apikey", apiKey)
        header("Authorization", "Bearer ${session.accessToken}")
    }
}
