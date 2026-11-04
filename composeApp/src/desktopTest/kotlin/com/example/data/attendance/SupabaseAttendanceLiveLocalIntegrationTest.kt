package com.example.data.attendance

import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assume
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas de integración del backend y migración 0030 contra la base local efímera de Supabase
 * (PostgreSQL 17.6 + PostgREST + GoTrue en Docker local).
 *
 * Utiliza [Assume.assumeTrue] para garantizar que en entornos sin Docker local las pruebas
 * se registren formalmente como OMITIDAS (skipped) en los reportes XML, y no como falsos positivos.
 */
class SupabaseAttendanceLiveLocalIntegrationTest {

    private val localBaseUrl = System.getProperty("sase.localSupabase.url") ?: "http://127.0.0.1:55321"
    private val localAnonKey = System.getProperty("sase.localSupabase.anonKey")
        ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"

    private val inst1Id = "11111111-1111-1111-1111-111111111111"
    private val inst2Id = "22222222-2222-2222-2222-222222222222"

    private val group1AId = "33333333-0000-4000-8000-000000000001"
    private val group2BId = "33333333-0000-4000-8000-000000000002"

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
    fun testLiveSupabaseAttendanceRlsPermissionsDoubleModulesAndReconnection() = runBlocking {
        Assume.assumeTrue(
            "OMITIDA: Pila local de Supabase no detectada en 127.0.0.1:55321 (requiere Docker)",
            isLocalSupabaseReachable()
        )

        val json = Json { ignoreUnknownKeys = true }
        var client1: HttpClient? = HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
        }

        try {
            val c1 = client1!!
            val doc1Session = signInGoTrue(c1, "docente1@example.invalid", StaffRole.DOCENTE, inst1Id)
            val doc2Session = signInGoTrue(c1, "docente2@example.invalid", StaffRole.DOCENTE, inst1Id)
            val sec2Session = signInGoTrue(c1, "secretaria2@example.invalid", StaffRole.SECRETARIA, inst2Id)

            // 1. Rechazo a rol anónimo en llamadas directas al backend
            val anonResponse = c1.post("$localBaseUrl/rest/v1/rpc/sase_open_or_schedule_class_session") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer $localAnonKey")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_group_id", group1AId)
                        put("p_date", "2026-10-15")
                        put("p_module_number", 1)
                    }
                )
            }
            assertTrue(
                anonResponse.status == HttpStatusCode.Unauthorized || anonResponse.status == HttpStatusCode.Forbidden,
                "El rol anon debe ser rechazado en RPCs con 401 o 403"
            )

            // 2. Docente asignado autorizado abre sesión de clase (Módulo 1)
            val openMod1Response = c1.post("$localBaseUrl/rest/v1/rpc/sase_open_or_schedule_class_session") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc1Session.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_group_id", group1AId)
                        put("p_date", "2026-10-15")
                        put("p_module_number", 1)
                        put("p_status", "PROGRAMADA")
                    }
                )
            }
            assertEquals(HttpStatusCode.OK, openMod1Response.status, "Docente asignado debe poder abrir sesión Módulo 1")
            val mod1Snapshot = json.parseToJsonElement(openMod1Response.body<String>()).jsonObject
            val sessionId1 = mod1Snapshot["session_id"]!!.jsonPrimitive.content
            assertNotNull(sessionId1)
            assertEquals("PROGRAMADA", mod1Snapshot["session_status"]!!.jsonPrimitive.content)

            // Alumnado vigente según fecha y grupo
            val roster = mod1Snapshot["roster"]!!.jsonArray
            assertTrue(roster.size >= 5, "El grupo 1° A debe tener su alumnado vigente en el snapshot")
            val student1Id = roster[0].jsonObject["student_id"]!!.jsonPrimitive.content
            val student2Id = roster[1].jsonObject["student_id"]!!.jsonPrimitive.content

            // 3. Docente no asignado rechazado en el mismo grupo
            val forbiddenDocResponse = c1.post("$localBaseUrl/rest/v1/rpc/sase_open_or_schedule_class_session") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc2Session.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_group_id", group1AId)
                        put("p_date", "2026-10-15")
                        put("p_module_number", 1)
                    }
                )
            }
            assertTrue(
                forbiddenDocResponse.status == HttpStatusCode.Forbidden ||
                forbiddenDocResponse.status == HttpStatusCode.InternalServerError ||
                forbiddenDocResponse.status == HttpStatusCode.BadRequest,
                "Docente 2 no asignado al grupo 1° A debe ser rechazado"
            )

            // 4. Usuario de otra institución rechazado (Aislamiento institucional)
            val foreignInstResponse = c1.post("$localBaseUrl/rest/v1/rpc/sase_open_or_schedule_class_session") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${sec2Session.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_group_id", group1AId)
                        put("p_date", "2026-10-15")
                        put("p_module_number", 1)
                    }
                )
            }
            assertTrue(
                foreignInstResponse.status == HttpStatusCode.Forbidden ||
                foreignInstResponse.status == HttpStatusCode.InternalServerError ||
                foreignInstResponse.status == HttpStatusCode.BadRequest,
                "Usuario de Institución 2 debe ser rechazado al intentar operar grupo de Institución 1"
            )

            // 5. Módulos dobles: abrir Módulo 2 en la misma fecha
            val openMod2Response = c1.post("$localBaseUrl/rest/v1/rpc/sase_open_or_schedule_class_session") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc1Session.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_group_id", group1AId)
                        put("p_date", "2026-10-15")
                        put("p_module_number", 2)
                        put("p_status", "PROGRAMADA")
                    }
                )
            }
            assertEquals(HttpStatusCode.OK, openMod2Response.status)
            val mod2Snapshot = json.parseToJsonElement(openMod2Response.body<String>()).jsonObject
            val sessionId2 = mod2Snapshot["session_id"]!!.jsonPrimitive.content
            assertTrue(sessionId1 != sessionId2, "Los módulos dobles deben generar sesiones independientes con IDs distintos")

            // 6. Guardado parcial: captura inicial para 2 de los 5 alumnos
            val partialSaveResponse = c1.post("$localBaseUrl/rest/v1/rpc/sase_save_class_attendance") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc1Session.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_session_id", sessionId1)
                        put("p_entries", buildJsonArray {
                            add(buildJsonObject {
                                put("student_id", student1Id)
                                put("presence", "PRESENTE")
                                put("punctuality", "A_TIEMPO")
                                put("minutes_late", 0)
                            })
                            add(buildJsonObject {
                                put("student_id", student2Id)
                                put("presence", "PRESENTE")
                                put("punctuality", "RETARDO")
                                put("minutes_late", 15)
                                put("justification", "Retardo en transporte escolar")
                            })
                        })
                    }
                )
            }
            assertEquals(HttpStatusCode.OK, partialSaveResponse.status, "El guardado parcial debe ser exitoso")
            val partialSnapshot = json.parseToJsonElement(partialSaveResponse.body<String>()).jsonObject
            val partialRoster = partialSnapshot["roster"]!!.jsonArray
            val student1Entry = partialRoster.first { it.jsonObject["student_id"]!!.jsonPrimitive.content == student1Id }.jsonObject
            val student2Entry = partialRoster.first { it.jsonObject["student_id"]!!.jsonPrimitive.content == student2Id }.jsonObject
            assertEquals("PRESENTE", student1Entry["presence"]!!.jsonPrimitive.content)
            assertEquals("A_TIEMPO", student1Entry["punctuality"]!!.jsonPrimitive.content)
            assertEquals("PRESENTE", student2Entry["presence"]!!.jsonPrimitive.content)
            assertEquals("RETARDO", student2Entry["punctuality"]!!.jsonPrimitive.content)
            assertEquals(15, student2Entry["minutes_late"]!!.jsonPrimitive.content.toInt())

            // 7. Persistencia tras cerrar y reconectar cliente HTTP
            client1?.close()
            client1 = null

            val client2 = HttpClient(CIO) {
                install(ContentNegotiation) { json(json) }
            }
            val doc1SessionReconnected = signInGoTrue(client2, "docente1@example.invalid", StaffRole.DOCENTE, inst1Id)

            val queryPersistedResponse = client2.post("$localBaseUrl/rest/v1/rpc/sase_open_or_schedule_class_session") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc1SessionReconnected.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_group_id", group1AId)
                        put("p_date", "2026-10-15")
                        put("p_module_number", 1)
                    }
                )
            }
            assertEquals(HttpStatusCode.OK, queryPersistedResponse.status)
            val persistedSnapshot = json.parseToJsonElement(queryPersistedResponse.body<String>()).jsonObject
            assertEquals(sessionId1, persistedSnapshot["session_id"]!!.jsonPrimitive.content)
            val reconnectedRoster = persistedSnapshot["roster"]!!.jsonArray
            val student2Reconnected = reconnectedRoster.first { it.jsonObject["student_id"]!!.jsonPrimitive.content == student2Id }.jsonObject
            assertEquals("RETARDO", student2Reconnected["punctuality"]!!.jsonPrimitive.content)
            assertEquals(15, student2Reconnected["minutes_late"]!!.jsonPrimitive.content.toInt())

            // 8. Corrección histórica / actualización sin duplicados
            val updateLateResponse = client2.post("$localBaseUrl/rest/v1/rpc/sase_save_class_attendance") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc1SessionReconnected.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_session_id", sessionId1)
                        put("p_entries", buildJsonArray {
                            add(buildJsonObject {
                                put("student_id", student2Id)
                                put("presence", "PRESENTE")
                                put("punctuality", "RETARDO")
                                put("minutes_late", 20)
                                put("justification", "Ajuste de retardo validado")
                            })
                        })
                    }
                )
            }
            assertEquals(HttpStatusCode.OK, updateLateResponse.status)
            val updatedSnapshot = json.parseToJsonElement(updateLateResponse.body<String>()).jsonObject
            val updatedRoster = updatedSnapshot["roster"]!!.jsonArray
            val student2Updated = updatedRoster.first { it.jsonObject["student_id"]!!.jsonPrimitive.content == student2Id }.jsonObject
            assertEquals(20, student2Updated["minutes_late"]!!.jsonPrimitive.content.toInt())

            // 9. Cancelación de sesión sin imputar faltas
            val cancelResponse = client2.post("$localBaseUrl/rest/v1/rpc/sase_update_class_session_status") {
                header("apikey", localAnonKey)
                header("Authorization", "Bearer ${doc1SessionReconnected.accessToken}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("p_session_id", sessionId2)
                        put("p_status", "CANCELADA")
                        put("p_notes", "Suspensión de labores por contingencia climática")
                    }
                )
            }
            assertEquals(HttpStatusCode.OK, cancelResponse.status)
            val cancelledSnapshot = json.parseToJsonElement(cancelResponse.body<String>()).jsonObject
            assertEquals("CANCELADA", cancelledSnapshot["session_status"]!!.jsonPrimitive.content)

            client2.close()
        } finally {
            client1?.close()
        }
    }
}
