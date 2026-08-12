package com.example.data.repository

import com.example.data.IncidentStatus
import com.example.data.SaseIncident
import com.example.data.SaseObservation
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

/** Domicilio, tutor y contacto de emergencia (migracion 0005). Tabla propia, RLS separado del nucleo. */
@Serializable
internal data class SensitiveIdentityRow(
    @SerialName("student_id") val studentId: String,
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("birth_place") val birthPlace: String = "",
    val address: String = "",
    @SerialName("zip_code") val zipCode: String = "",
    @SerialName("tutor_name") val tutorName: String = "",
    @SerialName("tutor_relation") val tutorRelation: String = "",
    @SerialName("tutor_phone") val tutorPhone: String = "",
    @SerialName("tutor_email") val tutorEmail: String = "",
    @SerialName("emergency_contact_name") val emergencyContactName: String = "",
    @SerialName("emergency_contact_relation") val emergencyContactRelation: String = "",
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String = "",
    @SerialName("emergency_contact_email") val emergencyContactEmail: String = "",
)

internal fun Student.withSensitiveIdentity(row: SensitiveIdentityRow?): Student = if (row == null) this else copy(
    birthDate = row.birthDate,
    birthPlace = row.birthPlace,
    address = row.address,
    zipCode = row.zipCode,
    tutorName = row.tutorName,
    tutorRelation = row.tutorRelation,
    tutorPhone = row.tutorPhone,
    tutorEmail = row.tutorEmail,
    emergencyContactName = row.emergencyContactName,
    emergencyContactRelation = row.emergencyContactRelation,
    emergencyContactPhone = row.emergencyContactPhone,
    emergencyContactEmail = row.emergencyContactEmail,
)

/**
 * Preserva la identidad sensible de una fuente anterior en memoria (no de
 * una fila fresca del servidor). Uso exclusivo de [SupabaseStudentRepositoryImpl.refresh]
 * cuando el subrecurso `student_sensitive_identity` falla: refresh() nunca
 * debe sustituir un domicilio/tutor ya conocido por valores neutros solo
 * porque esa peticion en particular fallo (P1 de Codex, "Treat supplemental
 * fetch failures as synchronization failures").
 */
internal fun Student.withSensitiveIdentityFrom(source: Student): Student = copy(
    birthDate = source.birthDate,
    birthPlace = source.birthPlace,
    address = source.address,
    zipCode = source.zipCode,
    tutorName = source.tutorName,
    tutorRelation = source.tutorRelation,
    tutorPhone = source.tutorPhone,
    tutorEmail = source.tutorEmail,
    emergencyContactName = source.emergencyContactName,
    emergencyContactRelation = source.emergencyContactRelation,
    emergencyContactPhone = source.emergencyContactPhone,
    emergencyContactEmail = source.emergencyContactEmail,
)

/**
 * Fila combinada que devuelven las RPC atomicas `create_student_core_and_identity`
 * / `update_student_core_and_identity` (migracion 0011): nucleo + identidad
 * sensible en una sola respuesta, porque ambas tablas se escribieron en la
 * misma transaccion de servidor.
 */
@Serializable
internal data class StudentWithIdentityRow(
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
    @SerialName("birth_date") val birthDate: String = "",
    @SerialName("birth_place") val birthPlace: String = "",
    val address: String = "",
    @SerialName("zip_code") val zipCode: String = "",
    @SerialName("tutor_name") val tutorName: String = "",
    @SerialName("tutor_relation") val tutorRelation: String = "",
    @SerialName("tutor_phone") val tutorPhone: String = "",
    @SerialName("tutor_email") val tutorEmail: String = "",
    @SerialName("emergency_contact_name") val emergencyContactName: String = "",
    @SerialName("emergency_contact_relation") val emergencyContactRelation: String = "",
    @SerialName("emergency_contact_phone") val emergencyContactPhone: String = "",
    @SerialName("emergency_contact_email") val emergencyContactEmail: String = "",
)

internal fun StudentWithIdentityRow.toStudent(): Student = Student(
    id = id,
    fullName = fullName,
    group = studentGroup,
    enrollmentId = enrollmentId.orEmpty(),
    curp = curp,
    shift = shift,
    schoolYear = schoolYear,
    status = status,
    preApplicationFolio = preApplicationFolio,
    birthDate = birthDate,
    birthPlace = birthPlace,
    address = address,
    zipCode = zipCode,
    tutorName = tutorName,
    tutorRelation = tutorRelation,
    tutorPhone = tutorPhone,
    tutorEmail = tutorEmail,
    emergencyContactName = emergencyContactName,
    emergencyContactRelation = emergencyContactRelation,
    emergencyContactPhone = emergencyContactPhone,
    emergencyContactEmail = emergencyContactEmail,
)

/** Cuerpo de `rpc/create_student_core_and_identity` (migracion 0011). */
@Serializable
internal data class CreateStudentIdentityRpcPayload(
    @SerialName("p_institution_id") val institutionId: String,
    @SerialName("p_full_name") val fullName: String,
    @SerialName("p_student_group") val studentGroup: String,
    @SerialName("p_enrollment_id") val enrollmentId: String?,
    @SerialName("p_curp") val curp: String,
    @SerialName("p_shift") val shift: String,
    @SerialName("p_school_year") val schoolYear: String,
    @SerialName("p_status") val status: String,
    @SerialName("p_pre_application_folio") val preApplicationFolio: String?,
    @SerialName("p_birth_date") val birthDate: String,
    @SerialName("p_birth_place") val birthPlace: String,
    @SerialName("p_address") val address: String,
    @SerialName("p_zip_code") val zipCode: String,
    @SerialName("p_tutor_name") val tutorName: String,
    @SerialName("p_tutor_relation") val tutorRelation: String,
    @SerialName("p_tutor_phone") val tutorPhone: String,
    @SerialName("p_tutor_email") val tutorEmail: String,
    @SerialName("p_emergency_contact_name") val emergencyContactName: String,
    @SerialName("p_emergency_contact_relation") val emergencyContactRelation: String,
    @SerialName("p_emergency_contact_phone") val emergencyContactPhone: String,
    @SerialName("p_emergency_contact_email") val emergencyContactEmail: String,
)

/** Cuerpo de `rpc/update_student_core_and_identity` (migracion 0011). */
@Serializable
internal data class UpdateStudentIdentityRpcPayload(
    @SerialName("p_student_id") val studentId: String,
    @SerialName("p_full_name") val fullName: String,
    @SerialName("p_student_group") val studentGroup: String,
    @SerialName("p_enrollment_id") val enrollmentId: String?,
    @SerialName("p_curp") val curp: String,
    @SerialName("p_shift") val shift: String,
    @SerialName("p_school_year") val schoolYear: String,
    @SerialName("p_status") val status: String,
    @SerialName("p_pre_application_folio") val preApplicationFolio: String?,
    @SerialName("p_birth_date") val birthDate: String,
    @SerialName("p_birth_place") val birthPlace: String,
    @SerialName("p_address") val address: String,
    @SerialName("p_zip_code") val zipCode: String,
    @SerialName("p_tutor_name") val tutorName: String,
    @SerialName("p_tutor_relation") val tutorRelation: String,
    @SerialName("p_tutor_phone") val tutorPhone: String,
    @SerialName("p_tutor_email") val tutorEmail: String,
    @SerialName("p_emergency_contact_name") val emergencyContactName: String,
    @SerialName("p_emergency_contact_relation") val emergencyContactRelation: String,
    @SerialName("p_emergency_contact_phone") val emergencyContactPhone: String,
    @SerialName("p_emergency_contact_email") val emergencyContactEmail: String,
)

internal fun Student.toCreateRpcPayload(institutionId: String): CreateStudentIdentityRpcPayload =
    CreateStudentIdentityRpcPayload(
        institutionId = institutionId,
        fullName = fullName.trim(),
        studentGroup = group.trim(),
        enrollmentId = enrollmentId.trim().takeIf { it.isNotEmpty() },
        curp = curp.trim().uppercase(),
        shift = shift.trim(),
        schoolYear = schoolYear.trim(),
        status = status.trim(),
        preApplicationFolio = preApplicationFolio?.trim()?.takeIf { it.isNotEmpty() },
        birthDate = birthDate.trim(),
        birthPlace = birthPlace.trim(),
        address = address.trim(),
        zipCode = zipCode.trim(),
        tutorName = tutorName.trim(),
        tutorRelation = tutorRelation.trim(),
        tutorPhone = tutorPhone.trim(),
        tutorEmail = tutorEmail.trim(),
        emergencyContactName = emergencyContactName.trim(),
        emergencyContactRelation = emergencyContactRelation.trim(),
        emergencyContactPhone = emergencyContactPhone.trim(),
        emergencyContactEmail = emergencyContactEmail.trim(),
    )

internal fun Student.toUpdateRpcPayload(): UpdateStudentIdentityRpcPayload =
    UpdateStudentIdentityRpcPayload(
        studentId = id,
        fullName = fullName.trim(),
        studentGroup = group.trim(),
        enrollmentId = enrollmentId.trim().takeIf { it.isNotEmpty() },
        curp = curp.trim().uppercase(),
        shift = shift.trim(),
        schoolYear = schoolYear.trim(),
        status = status.trim(),
        preApplicationFolio = preApplicationFolio?.trim()?.takeIf { it.isNotEmpty() },
        birthDate = birthDate.trim(),
        birthPlace = birthPlace.trim(),
        address = address.trim(),
        zipCode = zipCode.trim(),
        tutorName = tutorName.trim(),
        tutorRelation = tutorRelation.trim(),
        tutorPhone = tutorPhone.trim(),
        tutorEmail = tutorEmail.trim(),
        emergencyContactName = emergencyContactName.trim(),
        emergencyContactRelation = emergencyContactRelation.trim(),
        emergencyContactPhone = emergencyContactPhone.trim(),
        emergencyContactEmail = emergencyContactEmail.trim(),
    )

/** Observaciones institucionales (migracion 0005 + 0008). Historial inmutable, sin UPDATE/DELETE. */
@Serializable
internal data class ObservationRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("author_name") val authorName: String = "",
    val category: String = "",
    @SerialName("observation_text") val observationText: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class ObservationWritePayload(
    @SerialName("student_id") val studentId: String,
    @SerialName("institution_id") val institutionId: String,
    @SerialName("author_profile_id") val authorProfileId: String,
    @SerialName("author_name") val authorName: String,
    val category: String,
    @SerialName("observation_text") val observationText: String,
)

internal fun ObservationRow.toSaseObservation(): SaseObservation = SaseObservation(
    text = observationText,
    author = authorName,
    date = createdAt,
    category = category,
)

/** Incidencias escolares (migracion 0005, esquema corregido en 0007). */
@Serializable
internal data class IncidentRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("incident_type") val incidentType: String,
    @SerialName("incident_date") val incidentDate: String = "",
    val status: String,
    @SerialName("reporter_name") val reporterName: String = "",
    @SerialName("reported_by_profile_id") val reportedByProfileId: String? = null,
    @SerialName("agreement_notes") val agreementNotes: String? = null,
    @SerialName("follow_ups") val followUps: List<String> = emptyList(),
)

@Serializable
internal data class IncidentInsertPayload(
    @SerialName("student_id") val studentId: String,
    @SerialName("institution_id") val institutionId: String,
    @SerialName("incident_type") val incidentType: String,
    @SerialName("incident_date") val incidentDate: String,
    val status: String,
    @SerialName("reporter_name") val reporterName: String,
    @SerialName("reported_by_profile_id") val reportedByProfileId: String?,
    @SerialName("agreement_notes") val agreementNotes: String?,
)

@Serializable
internal data class IncidentUpdatePayload(
    val status: String,
    @SerialName("agreement_notes") val agreementNotes: String?,
    @SerialName("follow_ups") val followUps: List<String>,
)

internal fun IncidentRow.toSaseIncident(): SaseIncident = SaseIncident(
    date = incidentDate,
    type = incidentType,
    reporter = reporterName,
    status = status,
    id = id,
    reportedByStaffId = reportedByProfileId,
    agreementNotes = agreementNotes,
    followUps = followUps,
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
 *  - Identidad sensible (domicilio/tutor/contacto), observaciones e
 *    incidencias viven en tablas propias con su propio RLS (migraciones
 *    0005-0008): un campo que el actor no tiene permiso de ver simplemente no
 *    llega en la fila (RLS filtra, no lanza error) y el modelo lo deja en su
 *    valor neutro — nunca se fabrica.
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
        const val SENSITIVE_COLUMNS = "student_id,birth_date,birth_place,address,zip_code," +
            "tutor_name,tutor_relation,tutor_phone,tutor_email,emergency_contact_name," +
            "emergency_contact_relation,emergency_contact_phone,emergency_contact_email"
        const val OBSERVATION_COLUMNS = "id,student_id,author_name,category,observation_text,created_at"
        const val INCIDENT_COLUMNS = "id,student_id,incident_type,incident_date,status," +
            "reporter_name,reported_by_profile_id,agreement_notes,follow_ups"
        const val CURP_CONSTRAINT = "students_institution_curp_key"
        const val ENROLLMENT_CONSTRAINT = "students_institution_enrollment_key"
    }

    override suspend fun refresh(): StudentSyncResult {
        val session = sessionProvider() ?: return StudentSyncResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )

        val coreRows = try {
            val response = httpClient.get("$baseUrl/rest/v1/students") {
                authHeaders(session)
                url {
                    parameters.append("select", COLUMNS)
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("order", "full_name.asc")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                return StudentSyncResult.Failed(response.status.toFailure())
            }
            response.body<List<StudentRow>>()
                // Cinturon y tirantes: descarta cualquier fila de otra
                // institucion que llegara pese al filtro y a RLS.
                .filter { it.institutionId == session.institutionId }
        } catch (e: Exception) {
            return StudentSyncResult.Failed(StudentPersistenceFailure.NETWORK)
        }

        // Las tablas por area pueden devolver vacio si el actor no tiene el
        // permiso correspondiente (RLS filtra filas, no lanza error): eso es
        // el comportamiento correcto, no un fallo de refresh(). Un fallo de
        // transporte/servidor es distinto: NO se convierte en "sin datos" —
        // se preserva lo que ya habia en memoria para esa seccion y el
        // resultado se reporta como incompleto (P1 de Codex, "Treat
        // supplemental fetch failures as synchronization failures").
        val previousById = _students.value.associateBy { it.id }
        var incompleteReason: StudentPersistenceFailure? = null

        val sensitiveOutcome = fetchByInstitution<SensitiveIdentityRow>(
            session, "student_sensitive_identity", SENSITIVE_COLUMNS
        )
        val sensitiveByStudent = when (sensitiveOutcome) {
            is FetchOutcome.Ok -> sensitiveOutcome.rows.associateBy { it.studentId }
            is FetchOutcome.Error -> { incompleteReason = sensitiveOutcome.reason; null }
        }

        val observationsOutcome = fetchByInstitution<ObservationRow>(
            session, "student_observations", OBSERVATION_COLUMNS, order = "created_at.desc"
        )
        val observationsByStudent = when (observationsOutcome) {
            is FetchOutcome.Ok -> observationsOutcome.rows.groupBy { it.studentId }
            is FetchOutcome.Error -> { incompleteReason = observationsOutcome.reason; null }
        }

        val incidentsOutcome = fetchByInstitution<IncidentRow>(
            session, "student_incidents", INCIDENT_COLUMNS, order = "created_at.desc"
        )
        val incidentsByStudent = when (incidentsOutcome) {
            is FetchOutcome.Ok -> incidentsOutcome.rows.groupBy { it.studentId }
            is FetchOutcome.Error -> { incompleteReason = incidentsOutcome.reason; null }
        }

        val loaded = coreRows.map { row ->
            val previous = previousById[row.id]
            val base = row.toStudent()
            val withSensitive = if (sensitiveByStudent != null) {
                base.withSensitiveIdentity(sensitiveByStudent[row.id])
            } else {
                previous?.let { base.withSensitiveIdentityFrom(it) } ?: base
            }
            withSensitive.copy(
                observations = if (observationsByStudent != null) {
                    observationsByStudent[row.id].orEmpty().map { it.toSaseObservation() }
                } else {
                    previous?.observations.orEmpty()
                },
                schoolIncidents = if (incidentsByStudent != null) {
                    incidentsByStudent[row.id].orEmpty().map { it.toSaseIncident() }
                } else {
                    previous?.schoolIncidents.orEmpty()
                }
            )
        }

        // El resultado solo se publica si la sesion activa sigue siendo
        // aquella bajo la que se pidieron estos datos. Si cambio mientras
        // este refresh estaba en vuelo (p.ej. A cierra sesion y B inicia
        // sesion antes de que la respuesta de A llegue), publicarlo
        // repoblaria el StateFlow compartido con expedientes de la sesion
        // obsoleta de A durante la sesion de B (P1 de Codex, "Ignore refresh
        // results from obsolete sessions").
        if (sessionProvider() != session) {
            return StudentSyncResult.Failed(StudentPersistenceFailure.NO_SESSION)
        }

        _students.value = loaded
        return incompleteReason?.let { StudentSyncResult.Partial(loaded, it) }
            ?: StudentSyncResult.Loaded(loaded)
    }

    private sealed class FetchOutcome<out T> {
        data class Ok<T>(val rows: List<T>) : FetchOutcome<T>()
        data class Error(val reason: StudentPersistenceFailure) : FetchOutcome<Nothing>()
    }

    private suspend inline fun <reified T> fetchByInstitution(
        session: AuthSession,
        table: String,
        columns: String,
        order: String? = null
    ): FetchOutcome<T> = try {
        val response = httpClient.get("$baseUrl/rest/v1/$table") {
            authHeaders(session)
            url {
                parameters.append("select", columns)
                parameters.append("institution_id", "eq.${session.institutionId}")
                if (order != null) parameters.append("order", order)
            }
        }
        if (response.status == HttpStatusCode.OK) {
            FetchOutcome.Ok(response.body<List<T>>())
        } else {
            FetchOutcome.Error(response.status.toFailure())
        }
    } catch (e: Exception) {
        FetchOutcome.Error(StudentPersistenceFailure.NETWORK)
    }

    /**
     * Alta atomica del nucleo + identidad sensible via `rpc/create_student_core_and_identity`
     * (migracion 0011): una sola invocacion de servidor, una sola transaccion.
     * Nunca se reporta un alta como exitosa con tutor/domicilio a medio
     * escribir (P1 de Codex, "Persist sensitive identity during student
     * creation") ni con nucleo escrito pero identidad ausente.
     */
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
            httpClient.post("$baseUrl/rest/v1/rpc/create_student_core_and_identity") {
                authHeaders(session)
                contentType(ContentType.Application.Json)
                setBody(student.toCreateRpcPayload(session.institutionId))
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
            response.body<List<StudentWithIdentityRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentAddResult.Failed(StudentPersistenceFailure.REJECTED)

        val added = created.toStudent()
        _students.value = (_students.value.filterNot { it.id == added.id } + added)
            .sortedBy { it.fullName }
        return StudentAddResult.Added(added)
    }

    /**
     * Actualizacion atomica del nucleo + identidad sensible via
     * `rpc/update_student_core_and_identity` (migracion 0011): una sola
     * invocacion de servidor, una sola transaccion. Si RLS rechaza el UPDATE
     * del nucleo (expediente inexistente/de otra institucion) o el upsert de
     * identidad sensible falla por cualquier motivo, la funcion aborta
     * COMPLETA en el servidor — nunca queda un nucleo actualizado con
     * identidad sensible vieja, ni al reves (P1 de Codex, "Make core and
     * sensitive student updates atomic"). El cliente ya no intenta simular
     * esa atomicidad con dos llamadas HTTP.
     */
    override suspend fun updateStudent(student: Student): StudentUpdateResult {
        val session = sessionProvider() ?: return StudentUpdateResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )

        val response = try {
            httpClient.post("$baseUrl/rest/v1/rpc/update_student_core_and_identity") {
                authHeaders(session)
                contentType(ContentType.Application.Json)
                setBody(student.toUpdateRpcPayload())
            }
        } catch (e: Exception) {
            return StudentUpdateResult.Failed(StudentPersistenceFailure.NETWORK)
        }

        if (response.status != HttpStatusCode.OK) {
            return StudentUpdateResult.Failed(response.status.toFailure())
        }

        // La RPC devuelve lista vacia (o la funcion aborta con un error, ya
        // cubierto arriba) cuando ninguna fila del nucleo coincidio —
        // expediente inexistente o de otra institucion—. Eso es un rechazo,
        // nunca un "guardado", y en ese caso el servidor no escribio nada.
        val updatedRow = try {
            response.body<List<StudentWithIdentityRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)

        val cached = _students.value.firstOrNull { it.id == student.id }
        val updated = updatedRow.toStudent()
            .copy(
                observations = cached?.observations.orEmpty(),
                schoolIncidents = cached?.schoolIncidents.orEmpty()
            )
        _students.value = _students.value
            .map { if (it.id == updated.id) updated else it }
            .sortedBy { it.fullName }
        return StudentUpdateResult.Updated(updated)
    }

    override suspend fun addObservation(studentId: String, observation: SaseObservation): StudentUpdateResult {
        val session = sessionProvider() ?: return StudentUpdateResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )
        val response = try {
            httpClient.post("$baseUrl/rest/v1/student_observations") {
                authHeaders(session)
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                url { parameters.append("select", OBSERVATION_COLUMNS) }
                setBody(
                    ObservationWritePayload(
                        studentId = studentId,
                        institutionId = session.institutionId,
                        authorProfileId = session.profileId,
                        authorName = observation.author,
                        category = observation.category,
                        observationText = observation.text
                    )
                )
            }
        } catch (e: Exception) {
            return StudentUpdateResult.Failed(StudentPersistenceFailure.NETWORK)
        }
        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            return StudentUpdateResult.Failed(response.status.toFailure())
        }
        val created = try {
            response.body<List<ObservationRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)

        return mergeIntoCachedStudent(studentId) { it.copy(observations = listOf(created.toSaseObservation()) + it.observations) }
    }

    override suspend fun addIncident(
        studentId: String,
        type: String,
        description: String,
        date: String,
        reportedByStaffId: String,
        reportedByName: String
    ): StudentUpdateResult {
        val session = sessionProvider() ?: return StudentUpdateResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )
        val response = try {
            httpClient.post("$baseUrl/rest/v1/student_incidents") {
                authHeaders(session)
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                url { parameters.append("select", INCIDENT_COLUMNS) }
                setBody(
                    IncidentInsertPayload(
                        studentId = studentId,
                        institutionId = session.institutionId,
                        incidentType = type,
                        incidentDate = date,
                        status = IncidentStatus.ABIERTA.label,
                        reporterName = reportedByName,
                        reportedByProfileId = reportedByStaffId,
                        agreementNotes = description.takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            return StudentUpdateResult.Failed(StudentPersistenceFailure.NETWORK)
        }
        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            return StudentUpdateResult.Failed(response.status.toFailure())
        }
        val created = try {
            response.body<List<IncidentRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)

        return mergeIntoCachedStudent(studentId) { it.copy(schoolIncidents = listOf(created.toSaseIncident()) + it.schoolIncidents) }
    }

    override suspend fun advanceIncident(studentId: String, updated: SaseIncident): StudentUpdateResult {
        val session = sessionProvider() ?: return StudentUpdateResult.Failed(
            StudentPersistenceFailure.NO_SESSION
        )
        val response = try {
            httpClient.patch("$baseUrl/rest/v1/student_incidents") {
                authHeaders(session)
                header("Prefer", "return=representation")
                contentType(ContentType.Application.Json)
                url {
                    parameters.append("id", "eq.${updated.id}")
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("select", INCIDENT_COLUMNS)
                }
                setBody(
                    IncidentUpdatePayload(
                        status = updated.status,
                        agreementNotes = updated.agreementNotes,
                        followUps = updated.followUps
                    )
                )
            }
        } catch (e: Exception) {
            return StudentUpdateResult.Failed(StudentPersistenceFailure.NETWORK)
        }
        if (response.status != HttpStatusCode.OK) {
            return StudentUpdateResult.Failed(response.status.toFailure())
        }
        val savedRow = try {
            response.body<List<IncidentRow>>().firstOrNull()
        } catch (e: Exception) {
            null
        } ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)
        val saved = savedRow.toSaseIncident()

        return mergeIntoCachedStudent(studentId) { student ->
            student.copy(schoolIncidents = student.schoolIncidents.map { if (it.id == saved.id) saved else it })
        }
    }

    private fun mergeIntoCachedStudent(studentId: String, transform: (Student) -> Student): StudentUpdateResult {
        val current = _students.value.firstOrNull { it.id == studentId }
            ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)
        val updated = transform(current)
        _students.value = _students.value.map { if (it.id == studentId) updated else it }
        return StudentUpdateResult.Updated(updated)
    }

    override fun clear() {
        _students.value = emptyList()
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
