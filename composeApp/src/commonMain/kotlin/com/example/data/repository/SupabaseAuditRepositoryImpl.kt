package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.audit.InstitutionalAuditResult
import com.example.audit.InstitutionalAuditValidator
import com.example.data.SaseAudit
import com.example.data.auth.AuthSession
import com.example.data.auth.StaffRole
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Fila de `public.student_audit_events` (migracion 0004). */
@Serializable
internal data class AuditEventRow(
    @SerialName("institution_id") val institutionId: String,
    @SerialName("actor_profile_id") val actorProfileId: String,
    @SerialName("membership_id") val membershipId: String,
    @SerialName("active_role") val activeRole: String,
    val action: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("occurred_at") val occurredAt: String,
    val result: String,
    @SerialName("source_platform") val sourcePlatform: String,
)

internal fun InstitutionalAuditEvent.toRow(): AuditEventRow = AuditEventRow(
    institutionId = institutionId,
    actorProfileId = actorProfileId,
    membershipId = membershipId,
    activeRole = activeRole.name,
    action = action,
    entityType = entityType,
    entityId = entityId,
    occurredAt = timestamp,
    result = result.name,
    sourcePlatform = sourcePlatform,
)

/** `null` cuando la fila no representa un evento que este cliente entienda. */
internal fun AuditEventRow.toEventOrNull(): InstitutionalAuditEvent? {
    val role = StaffRole.entries.firstOrNull { it.name == activeRole } ?: return null
    val outcome = InstitutionalAuditResult.entries.firstOrNull { it.name == result } ?: return null
    return InstitutionalAuditEvent(
        institutionId = institutionId,
        actorProfileId = actorProfileId,
        membershipId = membershipId,
        activeRole = role,
        action = action,
        entityType = entityType,
        entityId = entityId,
        timestamp = occurredAt,
        result = outcome,
        sourcePlatform = sourcePlatform,
    )
}

/**
 * Bitacora institucional persistida en Supabase.
 *
 * Un evento solo se asienta si, ademas de ser valido, coincide con la sesion
 * activa: nadie audita a nombre de otra identidad, otra membresia u otra
 * institucion. La misma condicion la impone RLS del lado del servidor
 * (`student_audit_events_insert_own`); aqui se comprueba antes de gastar una
 * peticion y para que la negativa sea explicita.
 */
class SupabaseAuditRepositoryImpl(
    private val baseUrl: String,
    private val apiKey: String,
    private val sessionProvider: () -> AuthSession?,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AuditRepository {

    private val _audits = MutableStateFlow<List<SaseAudit>>(emptyList())
    override val audits: StateFlow<List<SaseAudit>> = _audits.asStateFlow()

    private companion object {
        const val COLUMNS = "institution_id,actor_profile_id,membership_id,active_role," +
            "action,entity_type,entity_id,occurred_at,result,source_platform"
        const val PAGE_SIZE = "200"
    }

    override suspend fun refresh(): Boolean {
        val session = sessionProvider() ?: return false

        val response = try {
            httpClient.get("$baseUrl/rest/v1/student_audit_events") {
                header("apikey", apiKey)
                header("Authorization", "Bearer ${session.accessToken}")
                url {
                    parameters.append("select", COLUMNS)
                    parameters.append("institution_id", "eq.${session.institutionId}")
                    parameters.append("order", "recorded_at.desc")
                    parameters.append("limit", PAGE_SIZE)
                }
            }
        } catch (e: Exception) {
            return false
        }

        if (response.status != HttpStatusCode.OK) return false

        val loaded = try {
            response.body<List<AuditEventRow>>()
                .filter { it.institutionId == session.institutionId }
                .mapNotNull { it.toEventOrNull() }
                .map { SaseAudit.fromInstitutionalEvent(it) }
        } catch (e: Exception) {
            return false
        }

        _audits.value = loaded
        return true
    }

    override suspend fun logAudit(event: InstitutionalAuditEvent): Boolean {
        val session = sessionProvider() ?: return false
        if (!InstitutionalAuditValidator.validate(event).isValid) return false
        if (!event.matchesSession(session)) return false

        val response = try {
            httpClient.post("$baseUrl/rest/v1/student_audit_events") {
                header("apikey", apiKey)
                header("Authorization", "Bearer ${session.accessToken}")
                header("Prefer", "return=minimal")
                contentType(ContentType.Application.Json)
                setBody(event.toRow())
            }
        } catch (e: Exception) {
            return false
        }

        if (response.status != HttpStatusCode.Created && response.status != HttpStatusCode.OK) {
            return false
        }

        _audits.value = listOf(SaseAudit.fromInstitutionalEvent(event)) + _audits.value
        return true
    }

    private fun InstitutionalAuditEvent.matchesSession(session: AuthSession): Boolean =
        institutionId == session.institutionId &&
            actorProfileId == session.profileId &&
            membershipId == session.membershipId
}
