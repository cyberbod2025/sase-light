package com.example.data.repository

import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentPersistenceFailure
import com.example.data.StudentUpdateResult
import com.example.data.auth.AuthSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Proyeccion del nucleo institucional persistido (migracion 0004). */
@Serializable
internal data class StudentRow(
    val id: String,
    @SerialName("institution_id") val institutionId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_group") val studentGroup: String = "",
    @SerialName("enrollment_id") val enrollmentId: String? = null,
    val curp: String,
    val shift: String = "",
    @SerialName("school_year") val schoolYear: String = "",
    val status: String = "",
    @SerialName("pre_application_folio") val preApplicationFolio: String? = null,
)

/**
 * Cuerpo de alta/actualizacion. `id` nunca viaja: lo asigna el servidor, de
 * modo que el cliente no puede fijar la identidad de un expediente.
 *
 * Ningun campo lleva valor por omision a proposito: con `encodeDefaults = false`
 * —el ajuste por defecto de kotlinx.serialization— una propiedad igual a su
 * default no se serializa, y en un PATCH de PostgREST una columna ausente
 * queda intacta. Sin esto, borrar una matricula seria imposible.
 */
@Serializable
internal data class StudentWritePayload(
    @SerialName("institution_id") val institutionId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_group") val studentGroup: String,
    @SerialName("enrollment_id") val enrollmentId: String?,
    val curp: String,
    val shift: String,
    @SerialName("school_year") val schoolYear: String,
    val status: String,
    @SerialName("pre_application_folio") val preApplicationFolio: String?,
)

internal fun StudentRow.toStudent(): Student = Student(
    id = id,
    fullName = fullName,
    group = studentGroup,
    enrollmentId = enrollmentId.orEmpty(),
    curp = curp,
    shift = shift,
    schoolYear = schoolYear,
    status = status,
    preApplicationFolio = preApplicationFolio,
)

internal fun Student.toWritePayload(institutionId: String): StudentWritePayload = StudentWritePayload(
    institutionId = institutionId,
    fullName = fullName.trim(),
    studentGroup = group.trim(),
    // Una matricula vacia se persiste como NULL: el indice parcial admite
    // muchos expedientes sin matricula, pero ninguna matricula repetida.
    enrollmentId = enrollmentId.trim().takeIf { it.isNotEmpty() },
    curp = curp.trim().uppercase(),
    shift = shift.trim(),
    schoolYear = schoolYear.trim(),
    status = status.trim(),
    preApplicationFolio = preApplicationFolio?.trim()?.takeIf { it.isNotEmpty() },
)

/**
 * Expedientes persistidos en Supabase via PostgREST.
 *
 * Reglas que esta clase no delega en la interfaz:
 *  - Sin [AuthSession] no se lee ni se escribe nada (falla cerrado).
 *  - Toda consulta y toda mutacion filtran por la institucion de la sesion,
 *    ademas de lo que ya impone RLS: si una politica se relajara por error, el
 *    cliente sigue sin cruzar instituciones.
 *  - `institution_id` proviene SIEMPRE de la sesion, nunca del modelo recibido.
 *
 * `httpClient` es inyectable para probar con MockEngine; ninguna prueba de este
 * repositorio toca la red real.
 */
class SupabaseStudentRepositoryImpl(
    private val baseUrl: String,
    private val apiKey: String,
    private val sessionProvider: () -> AuthSession?,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : StudentRepository {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    override val students: StateFlow<List<Student>> = _students.asStateFlow()

    private companion object {
        const val COLUMNS = "id,institution_id,full_name,student_group,enrollment_id," +
            "curp,shift,school_year,status,pre_application_folio"
        const val CURP_CONSTRAINT = "students_institution_curp_key"
        const val ENROLLMENT_CONSTRAINT = "students_institution_enrollment_key"
    }

    override suspend fun refresh(): StudentSyncResult {
        val session = sessionProvider() ?: return StudentSyncResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )

        val response = try {
            httpClient.get("$baseUrl/rest/v1/students") {
                authHeaders(session)
                url {
                    parameters.append("select", COLUMNS)
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("order", "full_name.asc")
                }
            }
        } catch (e: Exception) {
            return StudentSyncResult.Failed(StudentPersistenceFailure.NETWORK)
        }

        if (response.status != HttpStatusCode.OK) {
            return StudentSyncResult.Failed(response.status.toFailure())
        }

        val loaded = try {
            response.body<List<StudentRow>>()
                // Cinturon y tirantes: descarta cualquier fila de otra
                // institucion que llegara pese al filtro y a RLS.
                .filter { it.institutionId == session.institutionId }
                .map { it.toStudent() }
        } catch (e: Exception) {
            return StudentSyncResult.Failed(StudentPersistenceFailure.NETWORK)
        }

        _students.value = loaded
        return StudentSyncResult.Loaded(loaded)
    }

    override suspend fun addStudent(student: Student): StudentAddResult {
        val session = sessionProvider() ?: return StudentAddResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )
        if (student.fullName.isBlank()) {
            return StudentAddResult.InvalidData("El nombre del alumno es obligatorio.")
        }
        if (student.curp.isBlank()) {
            return StudentAddResult.InvalidData("La CURP es obligatoria.")
        }

        val response = try {
            httpClient.post("$baseUrl/rest/v1/students") {
                authHeaders(session)
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                url { parameters.append("select", COLUMNS) }
                setBody(student.toWritePayload(session.institutionId))
            }
        } catch (e: Exception) {
            return StudentAddResult.Failed(StudentPersistenceFailure.NETWORK)
        }

        if (response.status == HttpStatusCode.Conflict) {
            return conflictResult(session, student, response)
        }
        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            return StudentAddResult.Failed(response.status.toFailure())
        }

        val created = try {
            response.body<List<StudentRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentAddResult.Failed(StudentPersistenceFailure.REJECTED)

        val added = created.toStudent()
        _students.value = (_students.value.filterNot { it.id == added.id } + added)
            .sortedBy { it.fullName }
        return StudentAddResult.Added(added)
    }

    override suspend fun updateStudent(student: Student): StudentUpdateResult {
        val session = sessionProvider() ?: return StudentUpdateResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )

        val response = try {
            httpClient.patch("$baseUrl/rest/v1/students") {
                authHeaders(session)
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("id", "eq.${student.id}")
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("select", COLUMNS)
                }
                setBody(student.toWritePayload(session.institutionId))
            }
        } catch (e: Exception) {
            return StudentUpdateResult.Failed(StudentPersistenceFailure.NETWORK)
        }

        if (response.status != HttpStatusCode.OK) {
            return StudentUpdateResult.Failed(response.status.toFailure())
        }

        // PostgREST responde 200 con lista vacia cuando ninguna fila coincidio
        // —expediente inexistente o de otra institucion—. Eso es un rechazo,
        // nunca un "guardado".
        val updatedRow = try {
            response.body<List<StudentRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)

        val updated = updatedRow.toStudent()
        _students.value = _students.value
            .map { if (it.id == updated.id) updated else it }
            .sortedBy { it.fullName }
        return StudentUpdateResult.Updated(updated)
    }

    /**
     * Traduce un 409 al duplicado concreto. Si el expediente en conflicto no
     * puede leerse, se reporta un rechazo en vez de inventar un existente.
     */
    private suspend fun conflictResult(
        session: AuthSession,
        student: Student,
        response: HttpResponse
    ): StudentAddResult {
        val detail = try {
            response.bodyAsText()
        } catch (e: Exception) {
            ""
        }

        return when {
            detail.contains(CURP_CONSTRAINT) -> {
                val curp = student.curp.trim().uppercase()
                findOne(session, "curp", "eq.$curp")
                    ?.let { StudentAddResult.DuplicateCurp(curp = curp, existing = it) }
                    ?: StudentAddResult.Failed(StudentPersistenceFailure.REJECTED)
            }

            detail.contains(ENROLLMENT_CONSTRAINT) -> {
                val enrollmentId = student.enrollmentId.trim()
                findOne(session, "enrollment_id", "eq.$enrollmentId")
                    ?.let {
                        StudentAddResult.DuplicateEnrollmentId(
                            enrollmentId = enrollmentId,
                            existing = it
                        )
                    }
                    ?: StudentAddResult.Failed(StudentPersistenceFailure.REJECTED)
            }

            else -> StudentAddResult.Failed(StudentPersistenceFailure.REJECTED)
        }
    }

    private suspend fun findOne(
        session: AuthSession,
        column: String,
        filter: String
    ): Student? = try {
        val response = httpClient.get("$baseUrl/rest/v1/students") {
            authHeaders(session)
            url {
                parameters.append("select", COLUMNS)
                parameters.append("institution_id", "eq.${session.institutionId}")
                parameters.append(column, filter)
                parameters.append("limit", "1")
            }
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<List<StudentRow>>()
                .firstOrNull { it.institutionId == session.institutionId }
                ?.toStudent()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authHeaders(session: AuthSession) {
        header("apikey", apiKey)
        header("Authorization", "Bearer ${session.accessToken}")
    }
}

/**
 * 4xx es una decision del servidor (RLS, permiso, validacion); 5xx y los fallos
 * de transporte son operaciones que no llegaron a completarse.
 */
internal fun HttpStatusCode.toFailure(): StudentPersistenceFailure =
    if (value in 400..499) StudentPersistenceFailure.REJECTED
    else StudentPersistenceFailure.NETWORK
