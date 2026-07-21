package com.example.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Implementación Supabase de los gateways de autenticación y directorio.
 * Los tipos del SDK y los DTOs viven exclusivamente aquí; hacia afuera solo
 * salen modelos de dominio. Nunca se registran tokens ni credenciales, y los
 * roles/scopes se leen siempre del directorio almacenado (RLS de Fase 1) —
 * jamás de metadata controlada por el cliente.
 */
class SupabaseStaffGateways(
    private val provider: SupabaseClientProvider
) : StaffAuthGateway, StaffDirectoryGateway {

    override suspend fun signIn(email: String, password: String): StaffAuthGatewayResult = try {
        provider.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val user = provider.client.auth.currentUserOrNull()
        if (user == null) {
            StaffAuthGatewayResult.Failed(StaffAuthFailure.UNEXPECTED_FAILURE)
        } else {
            StaffAuthGatewayResult.Authenticated(UserId(user.id))
        }
    } catch (_: RestException) {
        StaffAuthGatewayResult.Failed(StaffAuthFailure.INVALID_CREDENTIALS)
    } catch (_: HttpRequestException) {
        StaffAuthGatewayResult.Failed(StaffAuthFailure.NETWORK_UNAVAILABLE)
    } catch (_: Throwable) {
        StaffAuthGatewayResult.Failed(StaffAuthFailure.UNEXPECTED_FAILURE)
    }

    override suspend fun signOut() {
        try {
            provider.client.auth.signOut()
        } catch (_: Throwable) {
            // La revocación remota es best-effort; el estado local ya se limpió.
        }
    }

    override suspend fun restoreUserId(): UserId? = try {
        provider.client.auth.awaitInitialization()
        provider.client.auth.currentUserOrNull()?.let { UserId(it.id) }
    } catch (_: Throwable) {
        null
    }

    override suspend fun fetchProfile(userId: UserId): StaffProfile? =
        provider.client.from("profiles")
            .select(columns = Columns.list("id", "display_name")) {
                filter { eq("id", userId.value) }
            }
            .decodeList<ProfileRow>()
            .singleOrNull()
            ?.let { StaffProfile(UserId(it.id), it.displayName) }

    override suspend fun fetchMembership(userId: UserId): InstitutionMembership? {
        val row = provider.client.from("institutional_memberships")
            .select { filter { eq("profile_id", userId.value) } }
            .decodeList<MembershipRow>()
            // Fase 2: una sola institución por persona; ambigüedad ⇒ deny-safe.
            .singleOrNull()
            ?: return null

        val membershipId = MembershipId(row.id)
        val institutionId = InstitutionId(row.institutionId)

        val roleRows = provider.client.from("membership_roles")
            .select(columns = Columns.raw("roles(code)")) {
                filter { eq("membership_id", row.id) }
            }
            .decodeList<MembershipRoleRow>()

        val scopeRows = provider.client.from("membership_scopes")
            .select { filter { eq("membership_id", row.id) } }
            .decodeList<MembershipScopeRow>()

        val roleAssignments = roleRows
            .mapNotNull { roleRow -> parseRole(roleRow.role.code) }
            .mapTo(mutableSetOf()) { RoleAssignment(membershipId, it) }
        val scopes = scopeRows
            .mapNotNullTo(mutableSetOf()) { it.toDomainScope(institutionId) }

        return InstitutionMembership(
            id = membershipId,
            userId = userId,
            institutionId = institutionId,
            roleAssignments = roleAssignments,
            scopes = scopes,
            active = row.active
        )
    }

    /** Un código desconocido no concede nada (deny-by-default). */
    private fun parseRole(code: String): InstitutionRole? =
        InstitutionRole.entries.firstOrNull { it.name == code.trim().uppercase() }

    @Serializable
    private data class ProfileRow(
        val id: String,
        @SerialName("display_name") val displayName: String
    )

    @Serializable
    private data class MembershipRow(
        val id: String,
        @SerialName("profile_id") val profileId: String,
        @SerialName("institution_id") val institutionId: String,
        val active: Boolean
    )

    @Serializable
    private data class MembershipRoleRow(
        @SerialName("roles") val role: RoleRef
    ) {
        @Serializable
        data class RoleRef(val code: String)
    }

    @Serializable
    private data class MembershipScopeRow(
        @SerialName("scope_type") val scopeType: String,
        @SerialName("scope_key") val scopeKey: String? = null
    ) {
        /**
         * Convención de claves: GROUP usa "cicloId/grupoId". Una clave
         * ilegible descarta el scope (deny-safe), nunca amplía acceso.
         */
        fun toDomainScope(institutionId: InstitutionId): AuthorizationScope? = when (scopeType) {
            "INSTITUTION" -> AuthorizationScope.Institution(institutionId)
            "SCHOOL_CYCLE" -> scopeKey?.let {
                AuthorizationScope.SchoolCycle(institutionId, SchoolCycleId(it))
            }
            "GROUP" -> scopeKey?.split('/', limit = 2)
                ?.takeIf { it.size == 2 && it.none(String::isBlank) }
                ?.let { (cycle, group) ->
                    AuthorizationScope.Group(institutionId, SchoolCycleId(cycle), GroupId(group))
                }
            "STUDENT" -> scopeKey?.let {
                AuthorizationScope.Student(institutionId, StudentId(it))
            }
            "OWN_RECORD" -> scopeKey?.let { AuthorizationScope.OwnRecord(UserId(it)) }
            else -> null
        }
    }
}
