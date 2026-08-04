package com.example.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
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

/**
 * SupabaseAuthRepositoryImpl contra un MockEngine: ninguna llamada aqui toca
 * red real. Cubre el mapeo GoTrue+PostgREST -> AuthSession/AuthFailureReason
 * exactamente igual que StaffAuthTest cubre el mock en memoria.
 */
private const val TOKEN_PATH = "/auth/v1/token"
private const val MEMBERSHIPS_PATH = "/rest/v1/institutional_memberships"
private const val LOGOUT_PATH = "/auth/v1/logout"

private fun tokenBody(userId: String, email: String) = """
    {"access_token":"token-$userId","token_type":"bearer","user":{"id":"$userId","email":"$email"}}
""".trimIndent()

private fun membershipsBody(
    active: Boolean = true,
    profileActive: Boolean = true,
    fullName: String = "Demo Secretaria",
    displayName: String? = null,
    roleCodes: List<String> = listOf("SECRETARIA")
): String {
    val roles = roleCodes.joinToString(",") { """{"roles":{"code":"$it"}}""" }
    val displayNameJson = if (displayName != null) "\"$displayName\"" else "null"
    return """
        [{"active":$active,"profiles":{"full_name":"$fullName","active":$profileActive,"display_name":$displayNameJson},"membership_roles":[$roles]}]
    """.trimIndent()
}

private fun repositoryWith(
    tokenStatus: HttpStatusCode = HttpStatusCode.OK,
    tokenBody: String = tokenBody("user-1", "secretaria@example.invalid"),
    membershipsStatus: HttpStatusCode = HttpStatusCode.OK,
    membershipsBody: String = membershipsBody()
): SupabaseAuthRepositoryImpl {
    val engine = MockEngine { request ->
        when {
            request.url.encodedPath == TOKEN_PATH -> respond(
                content = ByteReadChannel(tokenBody),
                status = tokenStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
            request.url.encodedPath == MEMBERSHIPS_PATH -> respond(
                content = ByteReadChannel(membershipsBody),
                status = membershipsStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
            request.url.encodedPath == LOGOUT_PATH -> {
                assertEquals("local", request.url.parameters["scope"])
                assertEquals("Bearer token-user-1", request.headers[HttpHeaders.Authorization])
                respond(content = ByteReadChannel.Empty, status = HttpStatusCode.NoContent)
            }
            else -> error("Ruta no esperada en el test: ${request.url}")
        }
    }
    val client = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    return SupabaseAuthRepositoryImpl(
        baseUrl = "https://test.invalid",
        apiKey = "test-key",
        httpClient = client
    )
}

class SupabaseAuthRepositoryImplTest {

    @Test
    fun signInWithValidCredentialsMapsRoleAndOpensSession() = runTest {
        val repo = repositoryWith(
            tokenBody = tokenBody("user-42", "secretaria@example.invalid"),
            membershipsBody = membershipsBody(fullName = "Demo Secretaria", roleCodes = listOf("SECRETARIA"))
        )

        val result = repo.signIn("secretaria@example.invalid", "demo1234")

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("user-42", success.session.profile.id)
        assertEquals(StaffRole.SECRETARIA, success.session.profile.role)
        assertEquals("Demo Secretaria", success.session.profile.fullName)
        assertEquals(success.session, repo.session.value)
    }

    @Test
    fun displayNameOverridesFullNameWhenPresent() = runTest {
        val repo = repositoryWith(
            membershipsBody = membershipsBody(fullName = "Demo Secretaria", displayName = "Sec. Alterna")
        )

        val result = repo.signIn("secretaria@example.invalid", "demo1234")

        assertEquals("Sec. Alterna", assertIs<AuthResult.Success>(result).session.profile.fullName)
    }

    @Test
    fun invalidCredentialsDoNotOpenSession() = runTest {
        val repo = repositoryWith(
            tokenStatus = HttpStatusCode.BadRequest,
            tokenBody = """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""
        )

        val result = repo.signIn("secretaria@example.invalid", "incorrecta")

        assertEquals(AuthFailureReason.INVALID_CREDENTIALS, assertIs<AuthResult.Failure>(result).reason)
        assertNull(repo.session.value)
    }

    @Test
    fun noActiveMembershipIsNoStaffProfile() = runTest {
        val repo = repositoryWith(membershipsBody = "[]")

        val result = repo.signIn("familia@example.invalid", "demo1234")

        assertEquals(AuthFailureReason.NO_STAFF_PROFILE, assertIs<AuthResult.Failure>(result).reason)
        assertNull(repo.session.value)
    }

    @Test
    fun onlyFamiliaRoleCodeIsNoStaffProfile() = runTest {
        val repo = repositoryWith(membershipsBody = membershipsBody(roleCodes = listOf("FAMILIA")))

        val result = repo.signIn("familia@example.invalid", "demo1234")

        assertEquals(AuthFailureReason.NO_STAFF_PROFILE, assertIs<AuthResult.Failure>(result).reason)
    }

    @Test
    fun inactiveMembershipIsInactiveAccount() = runTest {
        val repo = repositoryWith(membershipsBody = membershipsBody(active = false))

        val result = repo.signIn("baja@example.invalid", "demo1234")

        assertEquals(AuthFailureReason.INACTIVE_ACCOUNT, assertIs<AuthResult.Failure>(result).reason)
        assertNull(repo.session.value)
    }

    @Test
    fun inactiveProfileIsInactiveAccount() = runTest {
        val repo = repositoryWith(membershipsBody = membershipsBody(profileActive = false))

        val result = repo.signIn("baja@example.invalid", "demo1234")

        assertEquals(AuthFailureReason.INACTIVE_ACCOUNT, assertIs<AuthResult.Failure>(result).reason)
    }

    @Test
    fun goTrueServerErrorIsNetworkFailure() = runTest {
        val repo = repositoryWith(tokenStatus = HttpStatusCode.InternalServerError, tokenBody = "oops")

        val result = repo.signIn("secretaria@example.invalid", "demo1234")

        assertEquals(AuthFailureReason.NETWORK, assertIs<AuthResult.Failure>(result).reason)
    }

    @Test
    fun postgrestServerErrorIsNetworkFailure() = runTest {
        val repo = repositoryWith(membershipsStatus = HttpStatusCode.InternalServerError, membershipsBody = "oops")

        val result = repo.signIn("secretaria@example.invalid", "demo1234")

        assertEquals(AuthFailureReason.NETWORK, assertIs<AuthResult.Failure>(result).reason)
    }

    @Test
    fun signOutClearsSession() = runTest {
        val repo = repositoryWith()
        repo.signIn("secretaria@example.invalid", "demo1234")

        repo.signOut()

        assertNull(repo.session.value)
    }

    @Test
    fun signOutWithoutSessionDoesNotTouchNetwork() = runTest {
        val repo = repositoryWith()

        repo.signOut()

        assertNull(repo.session.value)
    }
}
