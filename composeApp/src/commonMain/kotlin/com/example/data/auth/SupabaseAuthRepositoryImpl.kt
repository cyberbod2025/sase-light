package com.example.data.auth

import com.example.currentEpochMillis
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
    @SerialName("expires_in") val expiresIn: Long = 3_600,
    val user: GoTrueUser
)

@Serializable
internal data class RoleCodeRow(val id: String? = null, val code: String)

@Serializable
internal data class MembershipRoleRow(
    @SerialName("role_id") val roleId: String? = null,
    val roles: RoleCodeRow
)

@Serializable
internal data class InstitutionRow(val name: String)

@Serializable
internal data class ProfileRow(
    @SerialName("full_name") val fullName: String,
    val active: Boolean,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
internal data class MembershipRow(
    val id: String,
    @SerialName("institution_id") val institutionId: String,
    val active: Boolean,
    val institutions: InstitutionRow,
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
    private val baseUrl: String,
    private val apiKey: String,
    private val nowMillis: () -> Long = ::currentEpochMillis,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    private var pendingIdentity: PendingSupabaseIdentity? = null

    override suspend fun signIn(email: String, password: String): AuthResult {
        // Nunca conservar una sesión o selección pendiente anterior mientras
        // se valida otra identidad, incluso si la nueva petición falla.
        _session.value = null
        pendingIdentity = null
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
        if (membership == null) {
            revokeRejectedToken(tokenResponse.accessToken)
            return AuthResult.Failure(AuthFailureReason.NO_MEMBERSHIP)
        }

        if (!membership.active || !membership.profiles.active) {
            revokeRejectedToken(tokenResponse.accessToken)
            return AuthResult.Failure(AuthFailureReason.INACTIVE_ACCOUNT)
        }

        val assignments = membership.membershipRoles.mapNotNull { row ->
            roleCodeToStaffRole(row.roles.code)?.let { role ->
                InstitutionalRoleAssignment(
                    roleId = row.roleId ?: row.roles.id ?: row.roles.code,
                    role = role
                )
            }
        }.distinctBy { it.roleId }
        if (assignments.isEmpty()) {
            revokeRejectedToken(tokenResponse.accessToken)
            return AuthResult.Failure(AuthFailureReason.NO_ROLE)
        }

        val profile = StaffProfile(
            id = tokenResponse.user.id,
            email = tokenResponse.user.email ?: normalized,
            fullName = membership.profiles.displayName?.takeIf { it.isNotBlank() }
                ?: membership.profiles.fullName,
            active = true
        )
        val pending = PendingSupabaseIdentity(
            token = tokenResponse,
            membership = membership,
            profile = profile,
            assignments = assignments
        )
        pendingIdentity = pending
        return if (assignments.size == 1) {
            activate(pending, assignments.single().roleId)
        } else {
            AuthResult.RoleSelectionRequired(
                RoleSelectionContext(
                    institutionName = membership.institutions.name,
                    availableRoles = assignments
                )
            )
        }
    }

    override suspend fun selectRole(roleId: String): AuthResult {
        val pending = pendingIdentity
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        return activate(pending, roleId)
    }

    override suspend fun switchRole(roleId: String): AuthResult {
        val active = _session.value
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        val switched = runCatching { active.switchToAssignedRole(roleId) }.getOrNull()
            ?: return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        _session.value = switched
        return AuthResult.Success(switched)
    }

    override suspend fun signOut() {
        val accessToken = _session.value?.accessToken ?: pendingIdentity?.token?.accessToken
        if (accessToken != null) {
            runCatching { requestLogout(accessToken) }
        }
        // El cliente siempre deja de exponer la sesion local, incluso si el
        // backend no esta disponible. Nunca se conserva una identidad previa.
        _session.value = null
        pendingIdentity = null
    }

    private fun activate(pending: PendingSupabaseIdentity, roleId: String): AuthResult {
        if (pending.assignments.none { it.roleId == roleId }) {
            return AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
        }
        val startedAt = nowMillis()
        val institutional = InstitutionalSession.create(
            userId = pending.token.user.id,
            profileId = pending.profile.id,
            membershipId = pending.membership.id,
            institutionId = pending.membership.institutionId,
            institutionName = pending.membership.institutions.name,
            roleAssignments = pending.assignments,
            activeRoleId = roleId,
            schoolCycleId = null,
            sessionStartedAt = startedAt,
            expiresAt = startedAt + pending.token.expiresIn * 1_000L
        )
        val session = AuthSession(
            profile = pending.profile,
            institutional = institutional,
            accessToken = pending.token.accessToken
        )
        pendingIdentity = null
        _session.value = session
        return AuthResult.Success(session)
    }

    private suspend fun revokeRejectedToken(accessToken: String) {
        runCatching { requestLogout(accessToken) }
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
                    "id,institution_id,active,institutions(name)," +
                        "profiles(full_name,active,display_name)," +
                        "membership_roles(role_id,roles(id,code))"
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

    private data class PendingSupabaseIdentity(
        val token: GoTrueTokenResponse,
        val membership: MembershipRow,
        val profile: StaffProfile,
        val assignments: List<InstitutionalRoleAssignment>
    )
}
