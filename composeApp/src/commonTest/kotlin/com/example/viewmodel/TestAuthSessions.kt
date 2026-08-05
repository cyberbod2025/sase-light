package com.example.viewmodel

import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthSession
import com.example.data.auth.InstitutionalRoleAssignment
import com.example.data.auth.InstitutionalSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
import com.example.environment.AppEnvironment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AuthRepository de prueba que fija una sesion sin pasar por signIn/coroutinas.
 * Compartido por las suites que necesitan un LabViewModel con sesion activa
 * para ejercitar codigo detras de la compuerta de canOpenScreen (M2/M3).
 */
internal class FixedSessionAuthRepository(
    fixedSession: AuthSession?
) : AuthRepository {
    override val session: StateFlow<AuthSession?> = MutableStateFlow(fixedSession).asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult =
        throw UnsupportedOperationException("FixedSessionAuthRepository solo fija sesion, no inicia sesion")

    override suspend fun signOut() {
        throw UnsupportedOperationException("FixedSessionAuthRepository no cierra sesion")
    }
}

internal fun testSessionFor(role: StaffRole, active: Boolean = true): AuthSession {
    val roleId = "role-test-${role.name.lowercase()}"
    val profileId = "profile-test-${role.name.lowercase()}"
    return AuthSession(
        profile = StaffProfile(
            id = profileId,
            email = "staff-shared@example.invalid",
            fullName = "Staff Compartido Test",
            active = active
        ),
        institutional = InstitutionalSession.create(
            userId = "user-test-${role.name.lowercase()}",
            profileId = profileId,
            membershipId = "membership-test-${role.name.lowercase()}",
            institutionId = "institution-test-fictitious",
            institutionName = "INSTITUCIÓN FICTICIA DE PRUEBAS",
            roleAssignments = listOf(InstitutionalRoleAssignment(roleId = roleId, role = role)),
            activeRoleId = roleId,
            schoolCycleId = "cycle-test-2026-2027",
            sessionStartedAt = 1_000L,
            expiresAt = null
        ),
        accessToken = "token-shared-test"
    )
}

internal fun labViewModelWithRole(role: StaffRole): LabViewModel =
    LabViewModel(
        appEnvironment = AppEnvironment.demoLocal("test"),
        authRepository = FixedSessionAuthRepository(testSessionFor(role))
    )
