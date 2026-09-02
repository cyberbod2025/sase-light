package com.example.data.repository

import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentPersistenceFailure
import com.example.data.StudentUpdateResult
import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
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
 * SupabaseStudentRepositoryImpl contra MockEngine: ninguna prueba toca la red
 * real ni el proyecto Supabase. Cubre el mapeo, el aislamiento por institucion
 * y que un rechazo del backend nunca se presente como exito.
 */
private const val STUDENTS_PATH = "/rest/v1/students"
private const val CREATE_RPC_PATH = "/rest/v1/rpc/create_student_core_and_identity"
private const val UPDATE_RPC_PATH = "/rest/v1/rpc/update_student_core_and_identity"
private const val BASE_URL = "https://proyecto-ficticio.supabase.invalid"

internal const val INSTITUTION_A = "11111111-1111-1111-1111-111111111111"
internal const val INSTITUTION_B = "22222222-2222-2222-2222-222222222222"

internal fun testSession(
    institutionId: String = INSTITUTION_A,
    profileId: String = "profile-secretaria",
    membershipId: String = "membership-1"
): AuthSession = AuthSession(
    profile = StaffProfile(
        id = profileId,
        email = "secretaria@example.invalid",
        fullName = "SECRETARIA DEMO",
        active = true
    ),
    institutional = InstitutionalSession.create(
        userId = profileId,
        profileId = profileId,
        membershipId = membershipId,
        institutionId = institutionId,
        institutionName = "INSTITUCIÓN FICTICIA STAGING",
        roleAssignments = listOf(
            InstitutionalRoleAssignment(roleId = "role-secretaria", role = StaffRole.SECRETARIA)
        ),
        activeRoleId = "role-secretaria",
        schoolCycleId = null,
        sessionStartedAt = 0L,
        expiresAt = null
    ),
    accessToken = "token-de-prueba"
)

private fun studentRow(
    id: String = "student-1",
    institutionId: String = INSTITUTION_A,
    fullName: String = "ALUMNA SINTETICA UNO",
    group: String = "1A",
    enrollmentId: String? = "S310-0001",
    curp: String = "SINT010101MDFABC01"
): String {
    val enrollment = if (enrollmentId == null) "null" else "\"$enrollmentId\""
    return """
        {"id":"$id","institution_id":"$institutionId","full_name":"$fullName",
         "student_group":"$group","enrollment_id":$enrollment,"curp":"$curp",
         "shift":"Vespertino","school_year":"2025-2026","status":"Activo",
         "pre_application_folio":"PS-0001"}
    """.trimIndent()
}

private class Recorder {
    val requests = mutableListOf<HttpRequestData>()
    var bodies = mutableListOf<String>()

    /** Cuerpo de la primera peticion que cumple [predicate] — requests/bodies estan en el mismo orden. */
    fun bodyFor(predicate: (HttpRequestData) -> Boolean): String = bodies[requests.indexOfFirst(predicate)]
}

/** Respuesta minima valida para el upsert de identidad sensible (todas sus columnas tienen default salvo student_id). */
private fun sensitiveIdentityRowJson(studentId: String = "student-1"): String =
    """{"student_id":"$studentId"}"""

/**
 * Respuesta de las RPC atomicas `create_student_core_and_identity` /
 * `update_student_core_and_identity` (migracion 0011): nucleo + identidad
 * sensible en una sola fila.
 */
private fun studentWithIdentityRowJson(
    id: String = "student-1",
    institutionId: String = INSTITUTION_A,
    fullName: String = "ALUMNA SINTETICA UNO",
    group: String = "1A",
    enrollmentId: String? = "S310-0001",
    curp: String = "SINT010101MDFABC01",
    tutorName: String = "",
    tutorRelation: String = ""
): String {
    val enrollment = if (enrollmentId == null) "null" else "\"$enrollmentId\""
    return """
        {"id":"$id","institution_id":"$institutionId","full_name":"$fullName",
         "student_group":"$group","enrollment_id":$enrollment,"curp":"$curp",
         "shift":"Vespertino","school_year":"2025-2026","status":"Activo",
         "pre_application_folio":"PS-0001","birth_date":"","birth_place":"",
         "address":"","zip_code":"","tutor_name":"$tutorName","tutor_relation":"$tutorRelation",
         "tutor_phone":"","tutor_email":"","emergency_contact_name":"",
         "emergency_contact_relation":"","emergency_contact_phone":"","emergency_contact_email":""}
    """.trimIndent()
}

private fun repositoryWith(
    session: AuthSession? = testSession(),
    recorder: Recorder = Recorder(),
    handler: (HttpRequestData) -> Pair<HttpStatusCode, String>
): Pair<SupabaseStudentRepositoryImpl, Recorder> {
    val engine = MockEngine { request ->
        recorder.requests += request
        recorder.bodies += runCatching {
            (request.body as? io.ktor.http.content.TextContent)?.text.orEmpty()
        }.getOrDefault("")
        val (status, body) = handler(request)
        respond(
            content = ByteReadChannel(body),
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    val client = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    return SupabaseStudentRepositoryImpl(
        baseUrl = BASE_URL,
        apiKey = "sb_publishable_ficticia",
        sessionProvider = { session },
        httpClient = client
    ) to recorder
}

class SupabaseStudentRepositoryImplTest {

    @Test
    fun refreshMapeaLaFilaJsonAlModeloStudent() = runTest {
        val (repository, _) = repositoryWith { request ->
            if (request.url.encodedPath == STUDENTS_PATH) HttpStatusCode.OK to "[${studentRow()}]"
            else HttpStatusCode.OK to "[]"
        }

        val result = repository.refresh()

        val loaded = assertIs<StudentSyncResult.Loaded>(result)
        val student = loaded.students.single()
        assertEquals("student-1", student.id)
        assertEquals("ALUMNA SINTETICA UNO", student.fullName)
        assertEquals("1A", student.group)
        assertEquals("S310-0001", student.enrollmentId)
        assertEquals("SINT010101MDFABC01", student.curp)
        assertEquals("Vespertino", student.shift)
        assertEquals("2025-2026", student.schoolYear)
        assertEquals("Activo", student.status)
        assertEquals("PS-0001", student.preApplicationFolio)
        assertEquals(loaded.students, repository.students.value)
    }

    @Test
    fun refreshTraduceMatriculaNulaACadenaVacia() = runTest {
        val (repository, _) = repositoryWith { request ->
            if (request.url.encodedPath == STUDENTS_PATH) HttpStatusCode.OK to "[${studentRow(enrollmentId = null)}]"
            else HttpStatusCode.OK to "[]"
        }

        val loaded = assertIs<StudentSyncResult.Loaded>(repository.refresh())
        assertEquals("", loaded.students.single().enrollmentId)
    }

    @Test
    fun refreshConsultaSoloLaInstitucionDeLaSesion() = runTest {
        val (repository, recorder) = repositoryWith { HttpStatusCode.OK to "[]" }

        repository.refresh()

        // refresh() consulta el nucleo y las tablas por area (identidad
        // sensible, observaciones, incidencias): todas deben filtrar por la
        // misma institucion de la sesion.
        assertTrue(recorder.requests.isNotEmpty())
        recorder.requests.forEach { request ->
            assertEquals("eq.$INSTITUTION_A", request.url.parameters["institution_id"])
            assertEquals("Bearer token-de-prueba", request.headers[HttpHeaders.Authorization])
        }
        val studentsRequest = recorder.requests.first { it.url.encodedPath == STUDENTS_PATH }
        assertEquals(STUDENTS_PATH, studentsRequest.url.encodedPath)
    }

    @Test
    fun refreshDescartaFilasDeOtraInstitucionAunqueElServidorLasDevuelva() = runTest {
        // Si una politica RLS se relajara por error, el cliente sigue sin
        // mostrar expedientes de otra escuela.
        val (repository, _) = repositoryWith { request ->
            if (request.url.encodedPath == STUDENTS_PATH) {
                HttpStatusCode.OK to
                    "[${studentRow(id = "propio")},${studentRow(id = "ajeno", institutionId = INSTITUTION_B)}]"
            } else {
                HttpStatusCode.OK to "[]"
            }
        }

        val loaded = assertIs<StudentSyncResult.Loaded>(repository.refresh())

        assertEquals(listOf("propio"), loaded.students.map { it.id })
    }

    @Test
    fun refreshFusionaLaIdentidadSensibleDeSuTablaPropia() = runTest {
        val (repository, _) = repositoryWith { request ->
            when {
                request.url.encodedPath == STUDENTS_PATH -> HttpStatusCode.OK to "[${studentRow()}]"
                request.url.encodedPath.endsWith("student_sensitive_identity") ->
                    HttpStatusCode.OK to "[${sensitiveIdentityRowJson()}]"
                else -> HttpStatusCode.OK to "[]"
            }
        }

        val loaded = assertIs<StudentSyncResult.Loaded>(repository.refresh())
        // sensitiveIdentityRowJson() solo fija student_id; el resto llega en
        // su valor neutro, pero la fusion en si (RLS respondio, no fallo) es
        // lo que separa Loaded de Partial.
        assertEquals("", loaded.students.single().tutorName)
    }

    @Test
    fun refreshEsIncompletoSiUnSubrecursoFallaPeroNoBorraElNucleoCargado() = runTest {
        // P1 de Codex, "Treat supplemental fetch failures as synchronization
        // failures": un fallo de transporte/servidor en identidad sensible
        // NO debe verse identico a "sin datos" (RLS). refresh() sigue
        // devolviendo el nucleo, pero marca el resultado como incompleto.
        val (repository, _) = repositoryWith { request ->
            when {
                request.url.encodedPath == STUDENTS_PATH -> HttpStatusCode.OK to "[${studentRow()}]"
                request.url.encodedPath.endsWith("student_sensitive_identity") ->
                    HttpStatusCode.InternalServerError to "{}"
                else -> HttpStatusCode.OK to "[]"
            }
        }

        val result = repository.refresh()

        val partial = assertIs<StudentSyncResult.Partial>(result)
        assertEquals(StudentPersistenceFailure.NETWORK, partial.reason)
        assertEquals("student-1", partial.students.single().id)
        assertEquals(partial.students, repository.students.value)
    }

    @Test
    fun refreshIncompletoPreservaLaIdentidadSensibleYaConocidaEnVezDeVaciarla() = runTest {
        // El caso critico: un refresh anterior SI trajo tutor/domicilio; un
        // refresh posterior donde ese subrecurso falla por red no debe
        // sustituirlos por valores neutros como si el backend hubiera
        // confirmado que ya no existen.
        var sensitiveShouldFail = false
        val (repository, _) = repositoryWith { request ->
            when {
                request.url.encodedPath == STUDENTS_PATH -> HttpStatusCode.OK to "[${studentRow()}]"
                request.url.encodedPath.endsWith("student_sensitive_identity") ->
                    if (sensitiveShouldFail) {
                        HttpStatusCode.InternalServerError to "{}"
                    } else {
                        HttpStatusCode.OK to """[{"student_id":"student-1","tutor_name":"MADRE SINTETICA"}]"""
                    }
                else -> HttpStatusCode.OK to "[]"
            }
        }

        val first = assertIs<StudentSyncResult.Loaded>(repository.refresh())
        assertEquals("MADRE SINTETICA", first.students.single().tutorName)

        sensitiveShouldFail = true
        val second = assertIs<StudentSyncResult.Partial>(repository.refresh())

        // El subrecurso fallo por servidor, no por RLS: el tutor ya conocido
        // se conserva, nunca se sustituye por "" como si ya no existiera.
        assertEquals("MADRE SINTETICA", second.students.single().tutorName)
    }

    @Test
    fun refreshSinSesionNoConsultaNada() = runTest {
        val (repository, recorder) = repositoryWith(session = null) {
            HttpStatusCode.OK to "[]"
        }

        val failed = assertIs<StudentSyncResult.Failed>(repository.refresh())

        assertEquals(StudentPersistenceFailure.NO_SESSION, failed.reason)
        assertTrue(recorder.requests.isEmpty(), "sin sesion no debe salir ninguna peticion")
    }

    @Test
    fun altaExitosaDevuelveElExpedienteConElIdAsignadoPorElServidor() = runTest {
        val (repository, recorder) = repositoryWith {
            HttpStatusCode.Created to "[${studentWithIdentityRowJson(id = "id-del-servidor")}]"
        }

        val result = repository.addStudent(
            Student(
                id = "",
                fullName = "ALUMNA SINTETICA UNO",
                group = "1A",
                enrollmentId = "S310-0001",
                curp = "sint010101mdfabc01"
            )
        )

        val added = assertIs<StudentAddResult.Added>(result)
        assertEquals("id-del-servidor", added.student.id)
        assertEquals(listOf(added.student), repository.students.value)

        val request = recorder.requests.single()
        assertEquals(CREATE_RPC_PATH, request.url.encodedPath)
        assertEquals(HttpMethod.Post, request.method)

        val body = recorder.bodies.single()
        // La institucion sale de la sesion y el id nunca lo fija el cliente:
        // el alta atomica (migracion 0011) ni siquiera acepta un parametro de id.
        assertTrue(body.contains("\"p_institution_id\":\"$INSTITUTION_A\""), body)
        assertTrue(!body.contains("\"id\":") && !body.contains("\"p_id\":"), "el cliente no debe enviar id: $body")
        assertTrue(body.contains("\"p_curp\":\"SINT010101MDFABC01\""), "la CURP se normaliza: $body")
    }

    @Test
    fun altaAtomicaPersisteYDevuelveLaIdentidadSensibleCapturada() = runTest {
        // Contrato del alta atomica (P1 de Codex, "Persist sensitive identity
        // during student creation"): lo que la UI captura para tutor debe
        // sobrevivir el viaje completo -- se manda en la RPC y se recibe de
        // vuelta en la MISMA respuesta (una sola escritura, no un alta
        // seguida de una relectura separada que pudiera quedarse a medias).
        val (repository, recorder) = repositoryWith {
            HttpStatusCode.Created to "[${studentWithIdentityRowJson(
                id = "id-del-servidor",
                tutorName = "MADRE SINTETICA",
                tutorRelation = "Madre"
            )}]"
        }

        val result = repository.addStudent(
            Student(
                id = "",
                fullName = "ALUMNA SINTETICA UNO",
                group = "1A",
                enrollmentId = "S310-0001",
                curp = "SINT010101MDFABC01",
                tutorName = "MADRE SINTETICA",
                tutorRelation = "Madre"
            )
        )

        val added = assertIs<StudentAddResult.Added>(result)
        assertEquals("MADRE SINTETICA", added.student.tutorName)
        assertEquals("Madre", added.student.tutorRelation)
        // Lo que quedo en memoria (equivalente a "releer del repositorio")
        // coincide exactamente con lo devuelto por el alta: no se perdio nada.
        assertEquals(added.student, repository.students.value.single())

        val body = recorder.bodies.single()
        assertTrue(body.contains("\"p_tutor_name\":\"MADRE SINTETICA\""), body)
        assertTrue(body.contains("\"p_tutor_relation\":\"Madre\""), body)
    }

    @Test
    fun altaRechazadaPorElServidorNoSeReportaComoGuardada() = runTest {
        val (repository, _) = repositoryWith { HttpStatusCode.Forbidden to """{"message":"denied"}""" }

        val result = repository.addStudent(
            Student(id = "", fullName = "ALUMNA", group = "1A", enrollmentId = "", curp = "SINT010101MDFABC01")
        )

        val failed = assertIs<StudentAddResult.Failed>(result)
        assertEquals(StudentPersistenceFailure.REJECTED, failed.reason)
        assertTrue(repository.students.value.isEmpty())
    }

    @Test
    fun altaConCurpDuplicadaIdentificaElExpedienteExistente() = runTest {
        val (repository, _) = repositoryWith { request ->
            if (request.method == HttpMethod.Post) {
                HttpStatusCode.Conflict to
                    """{"code":"23505","message":"duplicate key value violates unique constraint \"students_institution_curp_key\""}"""
            } else {
                HttpStatusCode.OK to "[${studentRow(id = "existente")}]"
            }
        }

        val result = repository.addStudent(
            Student(id = "", fullName = "ALUMNA", group = "1A", enrollmentId = "", curp = "SINT010101MDFABC01")
        )

        val duplicate = assertIs<StudentAddResult.DuplicateCurp>(result)
        assertEquals("SINT010101MDFABC01", duplicate.curp)
        assertEquals("existente", duplicate.existing.id)
    }

    @Test
    fun altaConMatriculaDuplicadaIdentificaElExpedienteExistente() = runTest {
        val (repository, _) = repositoryWith { request ->
            if (request.method == HttpMethod.Post) {
                HttpStatusCode.Conflict to
                    """{"code":"23505","message":"duplicate key value violates unique constraint \"students_institution_enrollment_key\""}"""
            } else {
                HttpStatusCode.OK to "[${studentRow(id = "existente")}]"
            }
        }

        val result = repository.addStudent(
            Student(id = "", fullName = "ALUMNA", group = "1A", enrollmentId = "S310-0001", curp = "SINT010101MDFABC01")
        )

        val duplicate = assertIs<StudentAddResult.DuplicateEnrollmentId>(result)
        assertEquals("S310-0001", duplicate.enrollmentId)
        assertEquals("existente", duplicate.existing.id)
    }

    @Test
    fun altaSinSesionFallaCerradaSinTocarLaRed() = runTest {
        val (repository, recorder) = repositoryWith(session = null) {
            HttpStatusCode.Created to "[${studentRow()}]"
        }

        val result = repository.addStudent(
            Student(id = "", fullName = "ALUMNA", group = "1A", enrollmentId = "", curp = "SINT010101MDFABC01")
        )

        assertEquals(
            StudentPersistenceFailure.NO_SESSION,
            assertIs<StudentAddResult.Failed>(result).reason
        )
        assertTrue(recorder.requests.isEmpty())
    }

    @Test
    fun actualizacionExitosaReemplazaElExpedienteEnMemoria() = runTest {
        val (repository, recorder) = repositoryWith { request ->
            when {
                request.url.encodedPath == STUDENTS_PATH && request.method == HttpMethod.Get ->
                    HttpStatusCode.OK to "[${studentRow(group = "1A")}]"
                request.method == HttpMethod.Get -> HttpStatusCode.OK to "[]"
                else -> HttpStatusCode.OK to "[${studentWithIdentityRowJson(group = "1B")}]"
            }
        }
        repository.refresh()

        val result = repository.updateStudent(
            Student(
                id = "student-1",
                fullName = "ALUMNA SINTETICA UNO",
                group = "1B",
                enrollmentId = "S310-0001",
                curp = "SINT010101MDFABC01"
            )
        )

        assertEquals("1B", assertIs<StudentUpdateResult.Updated>(result).student.group)
        assertEquals("1B", repository.students.value.single().group)

        // Una sola RPC atomica (migracion 0011), no un PATCH del nucleo
        // seguido de un upsert separado de identidad sensible.
        val rpcCall = recorder.requests.first { it.url.encodedPath == UPDATE_RPC_PATH }
        assertEquals(HttpMethod.Post, rpcCall.method)
        val body = recorder.bodyFor { it.url.encodedPath == UPDATE_RPC_PATH }
        assertTrue(body.contains("\"p_student_id\":\"student-1\""), body)
    }

    @Test
    fun actualizacionAtomicaNoActualizaLaCacheSiElServidorRechazaLaEscritura() = runTest {
        // Contrato de atomicidad (P1 de Codex, "Make core and sensitive
        // student updates atomic"): si la RPC no responde 200 (la funcion de
        // servidor aborto la transaccion completa), el cliente no debe dejar
        // ningun rastro de la escritura en memoria -- ni nucleo ni identidad
        // sensible a medias.
        val (repository, _) = repositoryWith { request ->
            when {
                request.url.encodedPath == STUDENTS_PATH && request.method == HttpMethod.Get ->
                    HttpStatusCode.OK to "[${studentRow(group = "1A")}]"
                request.method == HttpMethod.Get -> HttpStatusCode.OK to "[]"
                else -> HttpStatusCode.InternalServerError to "{\"message\":\"SASE_STUDENT_UPDATE_REJECTED\"}"
            }
        }
        repository.refresh()

        val result = repository.updateStudent(
            Student(
                id = "student-1",
                fullName = "ALUMNA SINTETICA UNO",
                group = "1B",
                enrollmentId = "S310-0001",
                curp = "SINT010101MDFABC01"
            )
        )

        assertIs<StudentUpdateResult.Failed>(result)
        // La cache sigue reflejando el estado previo a la RPC fallida.
        assertEquals("1A", repository.students.value.single().group)
    }

    @Test
    fun actualizacionQueNoAlcanzaNingunaFilaEsUnRechazoNoUnGuardado() = runTest {
        // PostgREST responde 200 con lista vacia cuando RLS oculta la fila o el
        // expediente pertenece a otra institucion.
        val (repository, _) = repositoryWith { HttpStatusCode.OK to "[]" }

        val result = repository.updateStudent(
            Student(id = "ajeno", fullName = "ALUMNA", group = "1A", enrollmentId = "", curp = "SINT010101MDFABC01")
        )

        assertEquals(
            StudentPersistenceFailure.REJECTED,
            assertIs<StudentUpdateResult.Failed>(result).reason
        )
    }

    @Test
    fun actualizacionSinSesionFallaCerrada() = runTest {
        val (repository, recorder) = repositoryWith(session = null) {
            HttpStatusCode.OK to "[${studentRow()}]"
        }

        val result = repository.updateStudent(
            Student(id = "student-1", fullName = "ALUMNA", group = "1A", enrollmentId = "", curp = "SINT010101MDFABC01")
        )

        assertEquals(
            StudentPersistenceFailure.NO_SESSION,
            assertIs<StudentUpdateResult.Failed>(result).reason
        )
        assertTrue(recorder.requests.isEmpty())
    }

    @Test
    fun errorDeRedSeDistingueDeUnRechazoDelServidor() = runTest {
        val engine = MockEngine { throw RuntimeException("sin conectividad") }
        val repository = SupabaseStudentRepositoryImpl(
            baseUrl = BASE_URL,
            apiKey = "sb_publishable_ficticia",
            sessionProvider = { testSession() },
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
        )
        val student = Student(
            id = "student-1",
            fullName = "ALUMNA",
            group = "1A",
            enrollmentId = "",
            curp = "SINT010101MDFABC01"
        )

        assertEquals(
            StudentPersistenceFailure.NETWORK,
            assertIs<StudentSyncResult.Failed>(repository.refresh()).reason
        )
        assertEquals(
            StudentPersistenceFailure.NETWORK,
            assertIs<StudentAddResult.Failed>(repository.addStudent(student)).reason
        )
        assertEquals(
            StudentPersistenceFailure.NETWORK,
            assertIs<StudentUpdateResult.Failed>(repository.updateStudent(student)).reason
        )
        assertTrue(repository.students.value.isEmpty())
    }

    @Test
    fun unServidorCaidoNoSeConfundeConUnRechazoDePermisos() = runTest {
        val (repository, _) = repositoryWith { HttpStatusCode.InternalServerError to "{}" }

        assertEquals(
            StudentPersistenceFailure.NETWORK,
            assertIs<StudentSyncResult.Failed>(repository.refresh()).reason
        )
    }

    @Test
    fun laMatriculaVaciaSePersisteComoNula() = runTest {
        val (repository, recorder) = repositoryWith {
            HttpStatusCode.Created to "[${studentWithIdentityRowJson(enrollmentId = null)}]"
        }

        repository.addStudent(
            Student(id = "", fullName = "ALUMNA", group = "1A", enrollmentId = "   ", curp = "SINT010101MDFABC01")
        )

        assertTrue(
            recorder.bodies.single().contains("\"p_enrollment_id\":null"),
            "una matricula en blanco debe viajar como null: ${recorder.bodies.single()}"
        )
    }

    @Test
    fun laActualizacionEnviaTodasLasColumnasDelNucleoIncluidasLasNulas() = runTest {
        // Con encodeDefaults=false un parametro ausente de la RPC queda
        // intacto en la columna correspondiente: si el payload omitiera los
        // nulos, seria imposible borrar una matricula o desligar un folio de
        // pre-solicitud.
        val (repository, recorder) = repositoryWith {
            HttpStatusCode.OK to "[${studentWithIdentityRowJson(enrollmentId = null)}]"
        }

        repository.updateStudent(
            Student(
                id = "student-1",
                fullName = "ALUMNA",
                group = "1A",
                enrollmentId = "",
                curp = "SINT010101MDFABC01",
                preApplicationFolio = null
            )
        )

        val body = recorder.bodyFor { it.url.encodedPath == UPDATE_RPC_PATH }
        listOf(
            "p_student_id", "p_full_name", "p_student_group", "p_enrollment_id",
            "p_curp", "p_shift", "p_school_year", "p_status", "p_pre_application_folio"
        ).forEach { column ->
            assertTrue(body.contains("\"$column\":"), "falta el parametro $column en $body")
        }
        assertTrue(body.contains("\"p_enrollment_id\":null"), body)
        assertTrue(body.contains("\"p_pre_application_folio\":null"), body)
    }

    @Test
    fun datosCorruptosDelServidorNoSePresentanComoListaVacia() = runTest {
        val (repository, _) = repositoryWith { HttpStatusCode.OK to "no-es-json" }

        assertIs<StudentSyncResult.Failed>(repository.refresh())
        assertNull(repository.students.value.firstOrNull())
    }
}
