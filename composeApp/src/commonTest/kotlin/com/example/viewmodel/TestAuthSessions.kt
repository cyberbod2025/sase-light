package com.example.viewmodel

import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthSession
import com.example.data.auth.StaffProfile
import com.example.data.auth.StaffRole
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

internal fun testSessionFor(role: StaffRole, active: Boolean = true): AuthSession = AuthSession(
    profile = StaffProfile(
        id = "staff-test-shared",
        email = "staff-shared@example.invalid",
        fullName = "Staff Compartido Test",
        role = role,
        active = active
    ),
    accessToken = "token-shared-test"
)

internal fun labViewModelWithRole(role: StaffRole): LabViewModel =
    LabViewModel(authRepository = FixedSessionAuthRepository(testSessionFor(role)))
