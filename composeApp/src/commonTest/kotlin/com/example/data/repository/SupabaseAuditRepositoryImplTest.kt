package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.audit.InstitutionalAuditResult
import com.example.data.auth.AuthSession
import com.example.data.auth.StaffRole
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val AUDIT_PATH = "/rest/v1/student_audit_events"
private const val AUDIT_BASE_URL = "https://proyecto-ficticio.supabase.invalid"

private fun auditRepositoryWith(
    session: AuthSession? = testSession(),
    requests: MutableList<HttpRequestData> = mutableListOf(),
    bodies: MutableList<String> = mutableListOf(),
    handler: (HttpRequestData) -> Pair<HttpStatusCode, String>
): SupabaseAuditRepositoryImpl {
    val engine = MockEngine { request ->
        requests += request
        bodies += (request.body as? io.ktor.http.content.TextContent)?.text.orEmpty()
        val (status, body) = handler(request)
        respond(
            content = ByteReadChannel(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    return SupabaseAuditRepositoryImpl(
        baseUrl = AUDIT_BASE_URL,
        apiKey = "sb_publishable_ficticia",
        sessionProvider = { session },
        httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    )
}

private fun validEvent(
    institutionId: String = INSTITUTION_A,
    actorProfileId: String = "profile-secretaria",
    membershipId: String = "membership-1"
): InstitutionalAuditEvent = InstitutionalAuditEvent(
    institutionId = institutionId,
    actorProfileId = actorProfileId,
    membershipId = membershipId,
    activeRole = StaffRole.SECRETARIA,
    action = "student.updated",
    entityType = "student",
    entityId = "student-1",
    timestamp = "2026-08-05 10:00:00",
    result = InstitutionalAuditResult.AUTHORIZED,
    sourcePlatform = "desktop"
)

private fun auditRowJson(institutionId: String = INSTITUTION_A, action: String = "student.updated") = """
    {"institution_id":"$institutionId","actor_profile_id":"profile-secretaria",
     "membership_id":"membership-1","active_role":"SECRETARIA","action":"$action",
     "entity_type":"student","entity_id":"student-1","occurred_at":"2026-08-05 10:00:00",
     "result":"AUTHORIZED","source_platform":"desktop"}
""".trimIndent()

class SupabaseAuditRepositoryImplTest {

    @Test
    fun eventoValidoQuedaAsentadoConSuContextoInstitucional() = runTest {
        val bodies = mutableListOf<String>()
        val repository = auditRepositoryWith(bodies = bodies) {
            HttpStatusCode.Created to ""
        }

        assertTrue(repository.logAudit(validEvent()))

        val stored = repository.audits.value.single()
        assertEquals("student.updated", stored.action)
        val event = assertNotNull(stored.institutionalEvent)
        assertEquals(INSTITUTION_A, event.institutionId)
        assertEquals(StaffRole.SECRETARIA, event.activeRole)

        val body = bodies.single()
        assertTrue(body.contains("\"actor_profile_id\":\"profile-secretaria\""), body)
        assertTrue(body.contains("\"membership_id\":\"membership-1\""), body)
        assertTrue(body.contains("\"active_role\":\"SECRETARIA\""), body)
        assertTrue(body.contains("\"result\":\"AUTHORIZED\""), body)
    }

    @Test
    fun eventoInvalidoSeRechazaAntesDeSalirALaRed() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = auditRepositoryWith(requests = requests) {
            HttpStatusCode.Created to ""
        }

        // Una CURP completa dentro del evento es contenido prohibido.
        val rejected = repository.logAudit(
            validEvent().copy(entityId = "SINT010101MDFABC01")
        )

        assertFalse(rejected)
        assertTrue(requests.isEmpty(), "un evento invalido no debe generar peticion")
        assertTrue(repository.audits.value.isEmpty())
    }

    @Test
    fun nadieAuditaANombreDeOtraIdentidadOInstitucion() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = auditRepositoryWith(requests = requests) {
            HttpStatusCode.Created to ""
        }

        assertFalse(repository.logAudit(validEvent(institutionId = INSTITUTION_B)))
        assertFalse(repository.logAudit(validEvent(actorProfileId = "otro-perfil")))
        assertFalse(repository.logAudit(validEvent(membershipId = "otra-membresia")))
        assertTrue(requests.isEmpty())
        assertTrue(repository.audits.value.isEmpty())
    }

    @Test
    fun sinSesionNoSeRegistraNadaNiSeConsultaNada() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = auditRepositoryWith(session = null, requests = requests) {
            HttpStatusCode.Created to ""
        }

        assertFalse(repository.logAudit(validEvent()))
        assertFalse(repository.refresh())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun unRechazoDelServidorNoSeContabilizaComoBitacoraAsentada() = runTest {
        val repository = auditRepositoryWith { HttpStatusCode.Forbidden to """{"message":"denied"}""" }

        assertFalse(repository.logAudit(validEvent()))
        assertTrue(repository.audits.value.isEmpty(), "sin confirmacion no hay proyeccion local")
    }

    @Test
    fun unFalloDeRedNoInventaUnaBitacoraLocal() = runTest {
        val engine = MockEngine { throw RuntimeException("sin conectividad") }
        val repository = SupabaseAuditRepositoryImpl(
            baseUrl = AUDIT_BASE_URL,
            apiKey = "sb_publishable_ficticia",
            sessionProvider = { testSession() },
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        )

        assertFalse(repository.logAudit(validEvent()))
        assertFalse(repository.refresh())
        assertTrue(repository.audits.value.isEmpty())
    }

    @Test
    fun laBitacoraSeConsultaSoloParaLaInstitucionDeLaSesion() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = auditRepositoryWith(requests = requests) {
            HttpStatusCode.OK to "[${auditRowJson()}]"
        }

        assertTrue(repository.refresh())

        val request = requests.single()
        assertEquals(AUDIT_PATH, request.url.encodedPath)
        assertEquals("eq.$INSTITUTION_A", request.url.parameters["institution_id"])
        assertEquals("student.updated", repository.audits.value.single().action)
    }

    @Test
    fun laBitacoraDescartaFilasDeOtraInstitucion() = runTest {
        val repository = auditRepositoryWith {
            HttpStatusCode.OK to
                "[${auditRowJson(action = "propia")},${auditRowJson(institutionId = INSTITUTION_B, action = "ajena")}]"
        }

        assertTrue(repository.refresh())

        assertEquals(listOf("propia"), repository.audits.value.map { it.action })
    }
}
