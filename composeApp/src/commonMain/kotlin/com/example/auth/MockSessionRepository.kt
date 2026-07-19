package com.example.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación mock del repositorio de sesión: determinista, sin red,
 * sin contraseñas, sin cuentas reales. Cada instancia es independiente para
 * impedir contaminación entre pruebas (no es un singleton).
 */
class MockSessionRepository : SessionRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.NoSession)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override fun signInAsDemo(identity: DemoStaffIdentity): AuthState {
        val next = AuthState.Active(demoSession(identity))
        _authState.value = next
        return next
    }

    override fun signOut() {
        _authState.value = AuthState.NoSession
    }

    override fun resetForTests() {
        _authState.value = AuthState.NoSession
    }

    companion object {
        val DEMO_INSTITUTION_ID = InstitutionId("INST-DEMO-310")
        val DEMO_CYCLE_ID = SchoolCycleId("CICLO-DEMO-2026-2027")
        val DEMO_GROUP_ID = GroupId("GRUPO-DEMO-1A")

        private val GROUP_SCOPED_ROLES = setOf(
            InstitutionRole.DOCENTE,
            InstitutionRole.TUTOR,
            InstitutionRole.PREFECTURA
        )

        /**
         * Construye la sesión demo de forma determinista: misma identidad →
         * mismos identificadores. Personal de área opera con alcance de
         * institución; docentes/tutoría/prefectura con alcance de grupo demo;
         * FAMILIA solo con su propio registro (sin permisos institucionales).
         */
        fun demoSession(identity: DemoStaffIdentity): SessionContext {
            val slug = identity.name.lowercase().replace('_', '-')
            val userId = UserId("demo-user-$slug")
            val membershipId = MembershipId("demo-membership-$slug")
            val scopes: Set<AuthorizationScope> = when {
                identity == DemoStaffIdentity.FAMILIA_DEMO ->
                    setOf(AuthorizationScope.OwnRecord(userId))
                identity.roles.all { it in GROUP_SCOPED_ROLES } ->
                    setOf(
                        AuthorizationScope.Group(
                            institutionId = DEMO_INSTITUTION_ID,
                            cycleId = DEMO_CYCLE_ID,
                            groupId = DEMO_GROUP_ID
                        )
                    )
                else -> setOf(AuthorizationScope.Institution(DEMO_INSTITUTION_ID))
            }
            return SessionContext(
                userId = userId,
                displayName = identity.displayName,
                membership = InstitutionMembership(
                    id = membershipId,
                    userId = userId,
                    institutionId = DEMO_INSTITUTION_ID,
                    roleAssignments = identity.roles
                        .mapTo(mutableSetOf()) { RoleAssignment(membershipId, it) },
                    scopes = scopes,
                    active = true
                )
            )
        }
    }
}
