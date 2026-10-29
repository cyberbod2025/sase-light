package com.example.data.enrollment

import com.example.data.auth.AuthSession
import com.example.data.repository.StudentRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class MovementEventRow(
    val id: String? = null,
    @SerialName("institution_id") val institutionId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("movement_type") val movementType: String,
    @SerialName("effective_date") val effectiveDate: String,
    @SerialName("recorded_at") val recordedAt: String? = null,
    @SerialName("recorded_by") val recordedBy: String,
    @SerialName("from_group") val fromGroup: String? = null,
    @SerialName("to_group") val toGroup: String? = null,
    @SerialName("destination_school") val destinationSchool: String? = null,
    @SerialName("departure_category") val departureCategory: String? = null,
    @SerialName("documentation_source") val documentationSource: String? = null,
    @SerialName("classification_criteria") val classificationCriteria: String? = null,
    @SerialName("is_correction") val isCorrection: Boolean = false,
    @SerialName("supersedes_movement_id") val supersedesMovementId: String? = null,
    @SerialName("correction_reason") val correctionReason: String? = null,
    val notes: String? = null
)

internal fun SchoolMovementRecord.toRow(sessionInstitutionId: String): MovementEventRow = MovementEventRow(
    id = id.ifBlank { null },
    institutionId = sessionInstitutionId,
    studentId = studentId,
    movementType = movementType.name,
    effectiveDate = effectiveDate,
    recordedAt = recordedAt.ifBlank { null },
    recordedBy = recordedBy,
    fromGroup = fromGroup,
    toGroup = toGroup,
    destinationSchool = destinationSchool,
    departureCategory = departureCategory?.name,
    documentationSource = documentationSource,
    classificationCriteria = classificationCriteria,
    isCorrection = isCorrection,
    supersedesMovementId = supersedesMovementId,
    correctionReason = correctionReason,
    notes = notes
)

internal fun MovementEventRow.toDomain(): SchoolMovementRecord? {
    val mType = SchoolMovementType.entries.firstOrNull { it.name == movementType } ?: return null
    val depCat = departureCategory?.let { cat -> DepartureCategory.entries.firstOrNull { it.name == cat } }
    return SchoolMovementRecord(
        id = id.orEmpty(),
        institutionId = institutionId,
        studentId = studentId,
        movementType = mType,
        effectiveDate = effectiveDate,
        recordedAt = recordedAt.orEmpty(),
        recordedBy = recordedBy,
        fromGroup = fromGroup,
        toGroup = toGroup,
        destinationSchool = destinationSchool,
        departureCategory = depCat,
        documentationSource = documentationSource,
        classificationCriteria = classificationCriteria,
        isCorrection = isCorrection,
        supersedesMovementId = supersedesMovementId,
        correctionReason = correctionReason,
        notes = notes
    )
}

/**
 * Adaptador persistente de movimientos escolares conectado a Supabase Staging.
 * Exige sesión activa y valida pertenencia institucional.
 */
class SupabaseSchoolMovementRepositoryImpl(
    private val baseUrl: String,
    private val apiKey: String,
    private val sessionProvider: () -> AuthSession?,
    private val studentRepository: StudentRepository,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : SchoolMovementRepository {

    override suspend fun recordMovement(movement: SchoolMovementRecord): Result<SchoolMovementRecord> = runCatching {
        val session = sessionProvider() ?: error("No hay sesión activa para registrar el movimiento escolar.")
        SchoolMovementEvaluator.validateMovement(movement)

        val row = movement.toRow(session.institutionId)
        val response = httpClient.post("$baseUrl/rest/v1/student_movement_events") {
            header("apikey", apiKey)
            header("Authorization", "Bearer ${session.accessToken}")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(row)
        }

        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            error("Error al registrar movimiento en Supabase: HTTP ${response.status.value}")
        }

        val returned = response.body<List<MovementEventRow>>().firstOrNull()?.toDomain()
            ?: movement
        returned
    }

    override suspend fun getMovementsForStudent(studentId: String): List<SchoolMovementRecord> {
        val session = sessionProvider() ?: error("No hay sesión activa para consultar movimientos escolares.")

        val response = try {
            httpClient.get("$baseUrl/rest/v1/student_movement_events") {
                header("apikey", apiKey)
                header("Authorization", "Bearer ${session.accessToken}")
                url {
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("student_id", "eq.$studentId")
                    parameters.append("order", "effective_date.asc,recorded_at.asc,id.asc")
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error de conexión al consultar movimientos del estudiante en Supabase: ${e.message}", e)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Error al consultar movimientos del estudiante en Supabase: HTTP ${response.status.value}")
        }

        return try {
            response.body<List<MovementEventRow>>().mapNotNull { it.toDomain() }
        } catch (e: Exception) {
            throw IllegalStateException("Error al deserializar movimientos del estudiante desde Supabase: ${e.message}", e)
        }
    }

    override suspend fun getAllMovements(): List<SchoolMovementRecord> {
        val session = sessionProvider() ?: error("No hay sesión activa para consultar movimientos escolares.")

        val response = try {
            httpClient.get("$baseUrl/rest/v1/student_movement_events") {
                header("apikey", apiKey)
                header("Authorization", "Bearer ${session.accessToken}")
                url {
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("order", "effective_date.asc,recorded_at.asc,id.asc")
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error de conexión al consultar movimientos institucionales en Supabase: ${e.message}", e)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Error al consultar movimientos institucionales en Supabase: HTTP ${response.status.value}")
        }

        return try {
            response.body<List<MovementEventRow>>().mapNotNull { it.toDomain() }
        } catch (e: Exception) {
            throw IllegalStateException("Error al deserializar movimientos institucionales desde Supabase: ${e.message}", e)
        }
    }

    override suspend fun calculateCutoff(
        cutoffDate: String,
        cohortStartDate: String
    ): EnrollmentCutoffSnapshot {
        val students = studentRepository.students.value
        val movements = getAllMovements()
        return SchoolMovementEvaluator.calculateCutoff(
            students = students,
            movements = movements,
            cutoffDate = cutoffDate,
            cohortStartDate = cohortStartDate
        )
    }

    override fun clear() {
        throw UnsupportedOperationException("La operación de limpieza de almacén no está permitida en persistencia remota conectada a Supabase.")
    }
}
