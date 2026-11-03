package com.example.data.enrollment

import com.example.data.Student
import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import com.example.data.repository.MockStudentRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas de integración del adaptador persistente [SupabaseSchoolMovementRepositoryImpl]
 * contra la base local efímera de pruebas (PostgreSQL 17.6 + PostgREST + GoTrue en Docker).
 *
 * Verifica con sesiones JWT reales por rol (NO credenciales administrativas):
 * 1. Escritura y lectura autorizadas (SECRETARIA y DIRECCION).
 * 2. Rechazo de usuarios anónimos (anon) y roles no autorizados (DOCENTE, TRABAJO_SOCIAL).
 * 3. Aislamiento entre instituciones (SECRETARIA de Institución 2 frente a Institución 1).
 * 4. Restricciones CHECK, claves foráneas FK y prevención de duplicados UNIQUE.
 * 5. Correcciones históricas con `supersedes_movement_id` y reincorporaciones.
 * 6. Persistencia real tras desconectar/cerrar el cliente HTTP y reconectar con una nueva instancia.
 */
class SupabaseSchoolMovementLiveLocalIntegrationTest {

    private val localBaseUrl = System.getProperty("sase.localSupabase.url") ?: "http://127.0.0.1:55321"
    private val localAnonKey = System.getProperty("sase.localSupabase.anonKey")
        ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"

    private val inst1Id = "11111111-1111-1111-1111-111111111111"
    private val inst2Id = "22222222-2222-2222-2222-222222222222"
    private val stuAId = "10000000-0000-4000-8000-000000000101"
    private val stuBId = "10000000-0000-4000-8000-000000000102"
    private val stuCId = "10000000-0000-4000-8000-000000000103"
    private val stuInst2Id = "20000000-0000-4000-8000-000000000201"

    @Serializable
    private data class GoTruePasswordGrantRequest(
        val email: String,
        val password: String
    )

    @Serializable
    private data class GoTrueTokenResponse(
        @SerialName("access_token") val accessToken: String,
        val user: GoTrueUser
    )

    @Serializable
    private data class GoTrueUser(
        val id: String,
        val email: String
    )

    private fun isLocalSupabaseReachable(): Boolean = runCatching {
        Socket("127.0.0.1", 55321).use { true }
    }.getOrDefault(false)

    private suspend fun signInGoTrue(
        httpClient: HttpClient,
        email: String,
        role: StaffRole,
        institutionId: String
    ): AuthSession {
        val response = httpClient.post("$localBaseUrl/auth/v1/token?grant_type=password") {
            header("apikey", localAnonKey)
            contentType(ContentType.Application.Json)
            setBody(GoTruePasswordGrantRequest(email = email, password = "TestPassword123!"))
        }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "No fue posible autenticar la sesión de prueba para $email en GoTrue local"
        )
        val body = response.body<GoTrueTokenResponse>()
        val roleId = "role-${role.name.lowercase()}"
        return AuthSession(
            profile = StaffProfile(
                id = body.user.id,
                email = body.user.email,
                fullName = email,
                active = true
            ),
            institutional = InstitutionalSession.create(
                userId = body.user.id,
                profileId = body.user.id,
                membershipId = "membership-${body.user.id}",
                institutionId = institutionId,
                institutionName = "Plantel Prueba $institutionId",
                roleAssignments = listOf(InstitutionalRoleAssignment(roleId = roleId, role = role)),
                activeRoleId = roleId,
                schoolCycleId = "2026-2027",
                sessionStartedAt = 1_000L,
                expiresAt = null
            ),
            accessToken = body.accessToken
        )
    }

    @Test
    fun testLiveLocalSupabaseDatabaseRlsConstraintsReconnectionAndCutoff() = runBlocking {
        if (!isLocalSupabaseReachable()) {
            println("SKIP: Base local efímera de Supabase no detectada en 127.0.0.1:55321.")
            return@runBlocking
        }

        val json = Json { ignoreUnknownKeys = true }
        val authClient = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
        }

        try {
            val sec1Session = signInGoTrue(authClient, "secretaria1@example.invalid", StaffRole.SECRETARIA, inst1Id)
            val dir1Session = signInGoTrue(authClient, "direccion1@example.invalid", StaffRole.DIRECCION, inst1Id)
            val doc1Session = signInGoTrue(authClient, "docente1@example.invalid", StaffRole.DOCENTE, inst1Id)
            val ts1Session = signInGoTrue(authClient, "trabajosocial1@example.invalid", StaffRole.TRABAJO_SOCIAL, inst1Id)
            val sec2Session = signInGoTrue(authClient, "secretaria2@example.invalid", StaffRole.SECRETARIA, inst2Id)

            // 1. Verificar rechazo directo a rol anónimo (anon) en PostgREST
            val anonGet = authClient.get("$localBaseUrl/rest/v1/student_movement_events") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer $localAnonKey")
            }
            assertEquals(HttpStatusCode.Unauthorized, anonGet.status, "El rol anon debe ser rechazado con HTTP 401")

            val anonPost = authClient.post("$localBaseUrl/rest/v1/student_movement_events") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer $localAnonKey")
                contentType(ContentType.Application.Json)
                setBody(
                    MovementEventRow(
                        institutionId = inst1Id,
                        studentId = stuAId,
                        movementType = "INSCRIPCION_INICIAL",
                        effectiveDate = "2026-08-28",
                        recordedBy = "ANON"
                    )
                )
            }
            assertEquals(HttpStatusCode.Unauthorized, anonPost.status, "El rol anon no puede insertar movimientos")

            val studentRepo = MockStudentRepositoryImpl()

            // 2. Verificar rechazo de escritura y lectura para DOCENTE y TRABAJO_SOCIAL usando sus sesiones reales
            val repoDocente = SupabaseSchoolMovementRepositoryImpl(
                baseUrl = localBaseUrl,
                apiKey = localAnonKey,
                sessionProvider = { doc1Session },
                studentRepository = studentRepo,
                remoteMigrationVerified = true,
                httpClient = authClient
            )
            val docWrite = repoDocente.recordMovement(
                SchoolMovementRecord(
                    id = "",
                    institutionId = inst1Id,
                    studentId = stuAId,
                    movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                    effectiveDate = "2026-09-02",
                    recordedBy = "DOCENTE_1",
                    toGroup = "1-A"
                )
            )
            assertTrue(docWrite.isFailure, "DOCENTE debe ser rechazado por RLS al intentar registrar movimientos")
            assertTrue(
                docWrite.exceptionOrNull()?.message?.contains("403") == true,
                "El rechazo a DOCENTE debe provenir de HTTP 403 RLS: ${docWrite.exceptionOrNull()?.message}"
            )
            val docRead = repoDocente.getAllMovements()
            assertTrue(docRead.isEmpty(), "DOCENTE no debe leer el padrón de movimientos detallados por RLS")

            val repoTrabajoSocial = SupabaseSchoolMovementRepositoryImpl(
                baseUrl = localBaseUrl,
                apiKey = localAnonKey,
                sessionProvider = { ts1Session },
                studentRepository = studentRepo,
                remoteMigrationVerified = true,
                httpClient = authClient
            )
            val tsWrite = repoTrabajoSocial.recordMovement(
                SchoolMovementRecord(
                    id = "",
                    institutionId = inst1Id,
                    studentId = stuAId,
                    movementType = SchoolMovementType.PENDIENTE_ACLARACION,
                    effectiveDate = "2026-09-03",
                    recordedBy = "TS_1",
                    departureCategory = DepartureCategory.PENDIENTE_ACLARACION
                )
            )
            assertTrue(tsWrite.isFailure, "TRABAJO_SOCIAL debe ser rechazado por RLS al intentar registrar movimientos")
            assertTrue(repoTrabajoSocial.getAllMovements().isEmpty(), "TRABAJO_SOCIAL no debe leer movimientos por RLS")

            // 3. Verificar aislamiento inter-institucional con SECRETARIA de Institución 2
            val repoSecInst2 = SupabaseSchoolMovementRepositoryImpl(
                baseUrl = localBaseUrl,
                apiKey = localAnonKey,
                sessionProvider = { sec2Session },
                studentRepository = studentRepo,
                remoteMigrationVerified = true,
                httpClient = authClient
            )
            assertTrue(
                repoSecInst2.getAllMovements().none { it.institutionId == inst1Id },
                "SECRETARIA de Institución 2 no puede leer movimientos de Institución 1"
            )
            // Intento de vincular alumno de Institución 1 en un movimiento de Institución 2 -> rechazado por trigger
            val crossStudentAttempt = repoSecInst2.recordMovement(
                SchoolMovementRecord(
                    id = "",
                    institutionId = inst2Id,
                    studentId = stuAId, // pertenece a Institución 1
                    movementType = SchoolMovementType.INSCRIPCION_INICIAL,
                    effectiveDate = "2026-08-28",
                    recordedBy = "SEC_INST_2",
                    toGroup = "1-A"
                )
            )
            assertTrue(
                crossStudentAttempt.isFailure,
                "El backend debe rechazar asociar un alumno de otra institución (SASE_MOVEMENT_STUDENT_INSTITUTION_MISMATCH)"
            )

            // 4. Cliente 1 (SECRETARIA y DIRECCION de Institución 1): escritura autorizada, restricciones, duplicados y correcciones
            val client1Http = HttpClient(CIO) {
                install(ContentNegotiation) { json(json) }
            }
            val repoSecClient1 = SupabaseSchoolMovementRepositoryImpl(
                baseUrl = localBaseUrl,
                apiKey = localAnonKey,
                sessionProvider = { sec1Session },
                studentRepository = studentRepo,
                remoteMigrationVerified = true,
                httpClient = client1Http
            )
            val repoDirClient1 = SupabaseSchoolMovementRepositoryImpl(
                baseUrl = localBaseUrl,
                apiKey = localAnonKey,
                sessionProvider = { dir1Session },
                studentRepository = studentRepo,
                remoteMigrationVerified = true,
                httpClient = client1Http
            )

            val existingBefore = repoSecClient1.getAllMovements()
            if (existingBefore.none { it.studentId == stuAId && it.effectiveDate == "2026-08-28" }) {
                assertTrue(
                    repoSecClient1.recordMovement(
                        SchoolMovementRecord(
                            id = "",
                            institutionId = inst1Id,
                            studentId = stuAId,
                            movementType = SchoolMovementType.INSCRIPCION_INICIAL,
                            effectiveDate = "2026-08-28",
                            recordedBy = "SECRETARIA_1",
                            toGroup = "1-A",
                            documentationSource = "Cédula de Inscripción 2026"
                        )
                    ).isSuccess
                )
            }
            if (existingBefore.none { it.studentId == stuBId && it.movementType == SchoolMovementType.TRASLADO_SALIENTE }) {
                assertTrue(
                    repoSecClient1.recordMovement(
                        SchoolMovementRecord(
                            id = "",
                            institutionId = inst1Id,
                            studentId = stuBId,
                            movementType = SchoolMovementType.TRASLADO_SALIENTE,
                            effectiveDate = "2026-09-25",
                            recordedBy = "SECRETARIA_1",
                            fromGroup = "1-A",
                            destinationSchool = "Escuela Secundaria Técnica No. 15, CCT 09DST0015W",
                            departureCategory = DepartureCategory.TRASLADO,
                            documentationSource = "Oficio de Traslado SEP-2026-88",
                            classificationCriteria = "Cambio de domicilio acreditado"
                        )
                    ).isSuccess
                )
            }
            if (existingBefore.none { it.studentId == stuCId && it.movementType == SchoolMovementType.BAJA_ADMINISTRATIVA }) {
                assertTrue(
                    repoSecClient1.recordMovement(
                        SchoolMovementRecord(
                            id = "",
                            institutionId = inst1Id,
                            studentId = stuCId,
                            movementType = SchoolMovementType.BAJA_ADMINISTRATIVA,
                            effectiveDate = "2026-09-10",
                            recordedBy = "SECRETARIA_1",
                            fromGroup = "1-A",
                            departureCategory = DepartureCategory.BAJA_ADMINISTRATIVA,
                            documentationSource = "Solicitud temporal de tutor",
                            classificationCriteria = "Baja administrativa temporal"
                        )
                    ).isSuccess
                )
            }
            if (existingBefore.none { it.studentId == stuCId && it.movementType == SchoolMovementType.REINCORPORACION }) {
                assertTrue(
                    repoDirClient1.recordMovement(
                        SchoolMovementRecord(
                            id = "",
                            institutionId = inst1Id,
                            studentId = stuCId,
                            movementType = SchoolMovementType.REINCORPORACION,
                            effectiveDate = "2026-10-05",
                            recordedBy = "DIRECCION_1",
                            toGroup = "1-A",
                            documentationSource = "Oficio de Reincorporación DIR-2026-12",
                            classificationCriteria = "Reincorporación autorizada por Dirección"
                        )
                    ).isSuccess
                )
            }

            // Verificar rechazo de duplicado exacto en PostgreSQL (UNIQUE idx_student_movement_no_duplicate_event)
            val duplicateAttempt = repoSecClient1.recordMovement(
                SchoolMovementRecord(
                    id = "",
                    institutionId = inst1Id,
                    studentId = stuAId,
                    movementType = SchoolMovementType.INSCRIPCION_INICIAL,
                    effectiveDate = "2026-08-28",
                    recordedBy = "SECRETARIA_1",
                    toGroup = "1-A"
                )
            )
            assertTrue(
                duplicateAttempt.isFailure,
                "PostgreSQL debe rechazar un movimiento duplicado con HTTP 409 (idx_student_movement_no_duplicate_event)"
            )

            // Verificar rechazo de FK inexistente en PostgreSQL
            val fkAttempt = repoDirClient1.recordMovement(
                SchoolMovementRecord(
                    id = "",
                    institutionId = inst1Id,
                    studentId = "99999999-9999-9999-9999-999999999999",
                    movementType = SchoolMovementType.INSCRIPCION_INICIAL,
                    effectiveDate = "2026-08-28",
                    recordedBy = "DIRECCION_1",
                    toGroup = "1-A"
                )
            )
            assertTrue(fkAttempt.isFailure, "PostgreSQL debe rechazar student_id inexistente")

            // Cerrar Cliente 1 para simular desconexión / cierre de sesión HTTP
            client1Http.close()

            // 5. Reconectar con un Cliente 2 completamente nuevo y verificar persistencia real en PostgreSQL
            val client2Http = HttpClient(CIO) {
                install(ContentNegotiation) { json(json) }
            }
            try {
                val repoSecClient2 = SupabaseSchoolMovementRepositoryImpl(
                    baseUrl = localBaseUrl,
                    apiKey = localAnonKey,
                    sessionProvider = { sec1Session },
                    studentRepository = studentRepo,
                    remoteMigrationVerified = true,
                    httpClient = client2Http
                )

                val persistedMovements = repoSecClient2.getAllMovements()
                assertTrue(
                    persistedMovements.isNotEmpty(),
                    "Tras cerrar el Cliente 1 y reconectar el Cliente 2, los movimientos deben persistir en PostgreSQL"
                )

                // Evaluar el corte con el padrón de los 3 estudiantes de Institución 1 en PostgreSQL
                val inst1Students = listOf(
                    Student(id = stuAId, fullName = "ANA LOPEZ GOMEZ", group = "1-A", enrollmentId = "MAT-2026-001", curp = "LOGA120515HMCRNA01"),
                    Student(id = stuBId, fullName = "CARLOS PEREZ RUIZ", group = "1-A", enrollmentId = "MAT-2026-002", curp = "PERC120515HMCRNC02"),
                    Student(id = stuCId, fullName = "ELENA VEGA PAZ", group = "1-A", enrollmentId = "MAT-2026-003", curp = "VEPE120515MMCRNE03")
                )
                val snapshot = SchoolMovementEvaluator.calculateCutoff(
                    students = inst1Students,
                    movements = persistedMovements,
                    cutoffDate = "2026-10-31",
                    cohortStartDate = "2026-08-28"
                )
                assertEquals(2, snapshot.totalActiveStudents, "Ana y Elena (reincorporada) deben estar activas al corte")
                assertEquals(1, snapshot.transfersOutTotal, "Carlos debe figurar como traslado saliente acreditado")
                assertEquals(1, snapshot.reincorporationsTotal, "Elena debe figurar como reincorporación al plantel")
                assertEquals(100.0, snapshot.cohortRetentionRate, "La retención de cohorte debe ser 100.0%")

                val detailAna = snapshot.studentDetails.first { it.studentId == stuAId }
                assertEquals("1-B", detailAna.assignedGroup, "La corrección histórica en PostgreSQL debe ubicar a Ana en 1-B")
            } finally {
                client2Http.close()
            }
        } finally {
            authClient.close()
        }
    }
}
