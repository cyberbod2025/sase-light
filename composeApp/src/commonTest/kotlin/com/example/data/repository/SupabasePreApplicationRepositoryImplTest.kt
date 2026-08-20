package com.example.data.repository

import com.example.data.auth.FamilySession
import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SupabasePreApplicationRepositoryImpl contra MockEngine: ninguna prueba toca
 * la red real ni el proyecto Supabase. Cubre el mapeo, el doble actor
 * (personal via REST+RLS, familia via las RPC `security definer` de la
 * migracion 0015 -- sin JWT, identificada solo por el token de acceso),
 * duplicados, sesion obsoleta y que un rechazo del backend nunca se
 * presente como exito -- mismo criterio que SupabaseStudentRepositoryImplTest.
 */
private const val BASE_URL = "https://proyecto-ficticio.supabase.invalid"
private const val CREATE_RPC_PATH = "/rest/v1/rpc/create_pre_application_with_children"
private const val UPDATE_RPC_PATH = "/rest/v1/rpc/update_pre_application_with_children"
private const val GET_RPC_PATH = "/rest/v1/rpc/get_pre_application_with_children"
private const val PARENT_PATH = "/rest/v1/pre_applications"

private fun familySession(folio: String = "PRE-TEST-01", token: String = "family-token-1") =
    FamilySession(folio = folio, accessToken = token)

private fun draftPreApplication(folio: String = "PRE-TEST-01", curp: String = "TEST010101HDFABC01"): PreApplication =
    MockPreApplicationData.preApplications.first().copy(
        folio = folio,
        status = PreApplicationStatus.BORRADOR,
        submittedAt = null,
        alumnoCurp = curp,
        readinessStatus = ReadinessStatus.PENDING,
        readyAt = null,
        readinessNotes = ""
    )

private fun parentRowJson(folio: String, institutionId: String = "inst-1", curp: String = "TEST010101HDFABC01"): String = """
    {"folio":"$folio","institution_id":"$institutionId","status":"ENVIADA",
     "submitted_at":"HOY","tramite":"NUEVO INGRESO","ciclo_escolar":"2026-2027","grado_solicitado":1,
     "alumno_nombre_completo":"ALUMNO PRUEBA","alumno_curp":"$curp","alumno_fecha_nacimiento":"01/ENE/2010",
     "alumno_sexo":"H","alumno_nacionalidad":"MEXICANA","alumno_entidad_nacimiento":"CDMX",
     "alumno_domicilio":"DOMICILIO PRUEBA","alumno_telefono_casa":"5500000000","escuela_procedencia":"",
     "promedio_grado_anterior":9.0,"persona_tramite_nombre":"MADRE PRUEBA","persona_tramite_parentesco":"MADRE",
     "persona_tramite_telefono":"5500000000","persona_tramite_identificacion":"INE","persona_tramite_contacto_principal":true,
     "ficha_servicio_medico":"","ficha_numero_afiliacion":null,"ficha_tipo_sangre":null,"ficha_alergias":"",
     "ficha_padecimientos":"","ficha_medicamentos":"","ficha_restriccion_fisica":"","ficha_usa_lentes":false,
     "ficha_dificultad_visual_auditiva":"","ficha_salud_bucal":"","ficha_cartilla_vacunacion":false,
     "contexto_vive_con_quien":"","contexto_tipo_familia":"","contexto_hijo_unico":false,
     "contexto_lugar_entre_hermanos":0,"contexto_hermanos_en_escuela":false,"contexto_integrantes_hogar":0,
     "contexto_sosten_economico":"","contexto_ingreso_rangos":"","contexto_tipo_vivienda":"",
     "contexto_servicios_basicos":false,"contexto_internet":false,"contexto_dispositivo_tareas":"",
     "contexto_beca_apoyo":"","contexto_transporte":"","contexto_dificultad_materiales":false,
     "contexto_atiende_avisos":"","contexto_horario_comunicacion":"","contexto_puede_acudir_citatorios":false,
     "udeii_antecedente_apoyo":"","udeii_terapia_lenguaje":false,"udeii_apoyo_psicologico":false,
     "udeii_apoyo_pedagogico":false,"udeii_documentos_disponibles":"","udeii_informe_escuela_anterior":false,
     "udeii_evaluacion_psicopedagogica":false,"udeii_plan_intervencion":false,"udeii_portafolio":false,
     "udeii_observaciones_familiares":"","consentimiento_aviso_privacidad":true,"consentimiento_uso_datos_expediente":true,
     "consentimiento_foto_alumno":true,"consentimiento_foto_credencial":true,"consentimiento_foto_autorizados":true,
     "consentimiento_comunicacion_whatsapp":true,"consentimiento_reglamento_interno":true,"consentimiento_marco_convivencia":true,
     "consentimiento_corresponsabilidad_familiar":true,"observaciones_secretaria":"","motivo_correccion":"",
     "readiness_status":"PENDING","ready_at":null,"readiness_notes":""}
""".trimIndent()

/** Respuesta de `get_pre_application_with_children`: fila padre + 3 colecciones vacias. */
private fun withChildrenJson(parentJson: String): String =
    """{"record":$parentJson,"responsables":[],"autorizados":[],"documentos":[]}"""

/** Respuesta de `create_pre_application_with_children`: folio + token, unica vez que el token vuelve. */
private fun createResponseJson(folio: String, accessToken: String = "family-token-1"): String =
    """{"folio":"$folio","access_token":"$accessToken"}"""

private fun clientFor(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

class SupabasePreApplicationRepositoryImplTest {

    @Test
    fun submitWithoutAnySessionUsesAnonymousCreate() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == CREATE_RPC_PATH) {
                respond(createResponseJson("PRE-TEST-01"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { null },
            familySessionProvider = { null },
            httpClient = clientFor(engine)
        )
        val result = repo.submit(draftPreApplication())
        val submitted = assertIs<PreApplicationSubmitResult.Submitted>(result)
        assertEquals("PRE-TEST-01", submitted.preApplication.folio)
        assertEquals("family-token-1", submitted.accessToken)
    }

    @Test
    fun submitDuplicateCurpByStaffResolvesExistingRecord() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == CREATE_RPC_PATH) {
                respond(
                    content = ByteReadChannel("""{"message":"duplicate key value violates unique constraint \"pre_applications_institution_curp_key\""}"""),
                    status = HttpStatusCode.Conflict,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else if (request.url.encodedPath == PARENT_PATH) {
                respond(
                    content = ByteReadChannel("[${parentRowJson("PRE-EXISTING")}]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { testSession() },
            familySessionProvider = { null },
            httpClient = clientFor(engine)
        )
        val result = repo.submit(draftPreApplication())
        val duplicate = assertIs<PreApplicationSubmitResult.DuplicateCurp>(result)
        assertEquals("TEST010101HDFABC01", duplicate.curp)
    }

    /**
     * La familia no tiene via de lectura por CURP (RLS/RPCs solo la
     * autorizan sobre su propio folio+token, que en un conflicto de alta
     * todavia no conoce): un 409 se reporta como fallo simple, nunca como
     * DuplicateCurp con datos de otra pre-solicitud.
     */
    @Test
    fun submitDuplicateCurpByFamilyNeverLeaksExistingRecord() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath == CREATE_RPC_PATH) {
                respond(
                    content = ByteReadChannel("""{"message":"duplicate key value violates unique constraint \"pre_applications_institution_curp_key\""}"""),
                    status = HttpStatusCode.Conflict,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { null },
            familySessionProvider = { familySession() },
            httpClient = clientFor(engine)
        )
        val result = repo.submit(draftPreApplication())
        val failed = assertIs<PreApplicationSubmitResult.Failed>(result)
        assertEquals(PreApplicationPersistenceFailure.REJECTED, failed.reason)
    }

    @Test
    fun updateRejectedByServerNeverReportsSuccess() = runTest {
        // El servidor rechaza -- 0 filas afectadas o token/permiso invalido
        // -- la RPC lanza excepcion, PostgREST responde 400. El cliente
        // nunca debe traducir esto en Updated.
        val engine = MockEngine { request ->
            if (request.url.encodedPath == UPDATE_RPC_PATH) {
                respond(
                    content = ByteReadChannel("""{"message":"SASE_PRE_APPLICATION_UPDATE_REJECTED"}"""),
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { null },
            familySessionProvider = { familySession(folio = "PRE-AJENO", token = "otro-token") },
            httpClient = clientFor(engine)
        )
        // Token/folio que no corresponden a la fila que se intenta actualizar.
        val result = repo.update(draftPreApplication(folio = "PRE-AJENO"))
        val failed = assertIs<PreApplicationUpdateResult.Failed>(result)
        assertEquals(PreApplicationPersistenceFailure.REJECTED, failed.reason)
    }

    @Test
    fun obsoleteSessionMutationResponseIsDiscarded() = runTest {
        var currentToken = "token-A"
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == CREATE_RPC_PATH -> {
                    // La sesion cambia MIENTRAS esta peticion esta en vuelo.
                    currentToken = "token-B"
                    respond(
                        content = ByteReadChannel(createResponseJson("PRE-RACE", "token-A")),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(
                    withChildrenJson(parentRowJson("PRE-RACE")),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { null },
            familySessionProvider = { familySession(folio = "PRE-RACE", token = currentToken) },
            httpClient = clientFor(engine)
        )
        val result = repo.submit(draftPreApplication(folio = "PRE-RACE"))
        val submitted = assertIs<PreApplicationSubmitResult.Submitted>(result)
        assertEquals("PRE-RACE", submitted.preApplication.folio)
        assertTrue(repo.preApplications.value.any { it.folio == "PRE-RACE" })
    }

    @Test
    fun refreshForStaffFiltersByInstitutionQueryParameter() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            when {
                request.url.encodedPath == PARENT_PATH -> respond(
                    content = ByteReadChannel("[${parentRowJson("PRE-INST-1")}]"),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { testSession() },
            familySessionProvider = { null },
            httpClient = clientFor(engine)
        )
        repo.refresh()
        val parentRequest = requests.first { it.url.encodedPath == PARENT_PATH }
        assertEquals("eq.$INSTITUTION_A", parentRequest.url.parameters["institution_id"])
    }

    /**
     * La familia nunca hace un GET directo a `pre_applications` (RLS no la
     * autoriza sin JWT): su unica via de lectura es la RPC
     * `get_pre_application_with_children`, con el folio+token de su propia
     * sesion como parametros explicitos, no como filtro de query string.
     */
    @Test
    fun refreshForFamilyUsesTokenRpcNotDirectRest() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond(
                withChildrenJson(parentRowJson("PRE-TEST-01")),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { null },
            familySessionProvider = { familySession() },
            httpClient = clientFor(engine)
        )
        val result = repo.refresh()
        assertIs<PreApplicationSyncResult.Loaded>(result)
        assertTrue(requests.none { it.url.encodedPath == PARENT_PATH })
        val rpcRequest = requests.first { it.url.encodedPath == GET_RPC_PATH }
        assertEquals(HttpMethod.Post, rpcRequest.method)
    }

    @Test
    fun clearEmptiesLocalStateWithoutTouchingNetwork() = runTest {
        val engine = MockEngine { request ->
            respond(
                withChildrenJson(parentRowJson("PRE-X")),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val repo = SupabasePreApplicationRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "anon-key",
            staffSessionProvider = { null },
            familySessionProvider = { familySession(folio = "PRE-X") },
            httpClient = clientFor(engine)
        )
        repo.refresh()
        assertTrue(repo.preApplications.value.isNotEmpty())
        repo.clear()
        assertTrue(repo.preApplications.value.isEmpty())
    }
}
