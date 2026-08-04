package com.example.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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

@Serializable
internal data class GoTrueTokenRequest(val email: String, val password: String)

@Serializable
internal data class GoTrueUser(val id: String, val email: String? = null)

@Serializable
internal data class GoTrueTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: GoTrueUser
)

@Serializable
internal data class RoleCodeRow(val code: String)

@Serializable
internal data class MembershipRoleRow(val roles: RoleCodeRow)

@Serializable
internal data class ProfileRow(
    @SerialName("full_name") val fullName: String,
    val active: Boolean,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
internal data class MembershipRow(
    val active: Boolean,
    val profiles: ProfileRow,
    @SerialName("membership_roles") val membershipRoles: List<MembershipRoleRow> = emptyList()
)

/**
 * Puente roles.code (dato, servidor) -> StaffRole (enum, cliente). `null` es
 * una decision explicita: codigos sin StaffRole equivalente (p.ej. "FAMILIA")
 * no otorgan ninguna pantalla de staff.
 */
internal fun roleCodeToStaffRole(code: String): StaffRole? = when (code) {
    "DIRECCION" -> StaffRole.DIRECCION
    "SECRETARIA" -> StaffRole.SECRETARIA
    "TRABAJO_SOCIAL" -> StaffRole.TRABAJO_SOCIAL
    "MEDICO_ESCOLAR" -> StaffRole.MEDICO_ESCOLAR
    "UDEII" -> StaffRole.UDEII
    "DOCENTE" -> StaffRole.DOCENTE
    else -> null
}

/**
 * AuthRepository real contra el proyecto Supabase "SASE-Light"
 * (plyjvvpkaafnkxmmqkbh): GoTrue para signIn/signOut y PostgREST para
 * resolver la membresia/rol institucional despues de autenticarse.
 *
 * REST directo (sin el SDK supabase-kt): su ultima version estable exige
 * Kotlin 2.3.x y Android minSdk 26, por encima de lo que este proyecto fija
 * hoy (Kotlin 2.1.20, minSdk 24) — forzar ese salto de toolchain solo para un
 * cliente HTTP habria sido un cambio de mucho mayor alcance que el propio
 * dependencia. Ktor + kotlinx.serialization si son compatibles con el
 * toolchain actual y cubren exactamente lo necesario: dos llamadas REST bien
 * documentadas y estables (GoTrue token, PostgREST institutional_memberships).
 *
 * httpClient es inyectable para poder probar con MockEngine sin red real
 * (ver SupabaseAuthRepositoryImplTest) — ningun test de este repositorio toca
 * la red de verdad.
 */
class SupabaseAuthRepositoryImpl(
    private val baseUrl: String = SupabaseConfig.URL,
    private val apiKey: String = SupabaseConfig.PUBLISHABLE_KEY,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult {
        val normalized = email.trim().lowercase()

        val tokenResponse = try {
            requestToken(normalized, password)
        } catch (e: Exception) {
            return AuthResult.Failure(AuthFailureReason.NETWORK)
        } ?: return AuthResult.Failure(AuthFailureReason.INVALID_CREDENTIALS)

        val membership = try {
            fetchActiveMembership(tokenResponse.accessToken, tokenResponse.user.id)
        } catch (e: Exception) {
            return AuthResult.Failure(AuthFailureReason.NETWORK)
        }
            ?: return AuthResult.Failure(AuthFailureReason.NO_STAFF_PROFILE)

        if (!membership.active || !membership.profiles.active) {
            return AuthResult.Failure(AuthFailureReason.INACTIVE_ACCOUNT)
        }

        val staffRole = membership.membershipRoles
            .firstNotNullOfOrNull { roleCodeToStaffRole(it.roles.code) }
            ?: return AuthResult.Failure(AuthFailureReason.NO_STAFF_PROFILE)

        val profile = StaffProfile(
            id = tokenResponse.user.id,
            email = tokenResponse.user.email ?: normalized,
            fullName = membership.profiles.displayName?.takeIf { it.isNotBlank() }
                ?: membership.profiles.fullName,
            role = staffRole,
            active = true
        )
        val newSession = AuthSession(profile = profile, accessToken = tokenResponse.accessToken)
        _session.value = newSession
        return AuthResult.Success(newSession)
    }

    override suspend fun signOut() {
        val accessToken = _session.value?.accessToken
        try {
            if (accessToken != null) {
                requestLogout(accessToken)
            }
        } finally {
            // El cliente siempre deja de exponer la sesion local, incluso si
            // el backend no esta disponible. Nunca se conserva una identidad
            // anterior mientras se procesa un nuevo acceso.
            _session.value = null
        }
    }

    /**
     * Cierra unicamente la sesion actual. Supabase conserva los access tokens
     * como JWT validos hasta su expiracion, pero elimina la sesion/refresh
     * token asociados a este dispositivo.
     */
    private suspend fun requestLogout(accessToken: String) {
        val response: HttpResponse = httpClient.post("$baseUrl/auth/v1/logout") {
            header("apikey", apiKey)
            header("Authorization", "Bearer $accessToken")
            url { parameters.append("scope", "local") }
        }
        if (response.status != HttpStatusCode.NoContent) {
            error("GoTrue respondio ${response.status} en /auth/v1/logout")
        }
    }

    /** null = credenciales invalidas (400/401/403 de GoTrue); excepcion = fallo de red/servidor. */
    private suspend fun requestToken(email: String, password: String): GoTrueTokenResponse? {
        val response: HttpResponse = httpClient.post("$baseUrl/auth/v1/token") {
            url { parameters.append("grant_type", "password") }
            header("apikey", apiKey)
            contentType(ContentType.Application.Json)
            setBody(GoTrueTokenRequest(email = email, password = password))
        }
        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> null
            else -> error("GoTrue respondio ${response.status} en /auth/v1/token")
        }
    }

    /** null = sin membresia institucional activa (perfil "familia" o sin institucion). */
    private suspend fun fetchActiveMembership(accessToken: String, userId: String): MembershipRow? {
        val response: HttpResponse = httpClient.get("$baseUrl/rest/v1/institutional_memberships") {
            header("apikey", apiKey)
            header("Authorization", "Bearer $accessToken")
            url {
                parameters.append(
                    "select",
                    "active,profiles(full_name,active,display_name),membership_roles(roles(code))"
                )
                parameters.append("profile_id", "eq.$userId")
                parameters.append("active", "eq.true")
                parameters.append("limit", "1")
            }
        }
        if (response.status != HttpStatusCode.OK) {
            error("PostgREST respondio ${response.status} en /rest/v1/institutional_memberships")
        }
        val rows: List<MembershipRow> = response.body()
        return rows.firstOrNull()
    }
}
