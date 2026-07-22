package com.example.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementacion demo en memoria. Credenciales y correos son placeholder
 * (`example.invalid`), nunca personal real de la escuela.
 */
class MockAuthRepositoryImpl(
    private val staff: List<MockStaffCredential> = MockStaffDirectory.DEFAULT
) : AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult {
        val normalized = email.trim().lowercase()
        val match = staff.firstOrNull { it.profile.email == normalized }
            ?: return AuthResult.Failure(AuthFailureReason.INVALID_CREDENTIALS)
        if (match.password != password) {
            return AuthResult.Failure(AuthFailureReason.INVALID_CREDENTIALS)
        }
        if (!match.profile.active) {
            return AuthResult.Failure(AuthFailureReason.INACTIVE_ACCOUNT)
        }
        val session = AuthSession(match.profile, accessToken = "mock-token-${match.profile.id}")
        _session.value = session
        return AuthResult.Success(session)
    }

    override suspend fun signOut() {
        _session.value = null
    }
}

data class MockStaffCredential(
    val profile: StaffProfile,
    val password: String
)

object MockStaffDirectory {
    val DEFAULT: List<MockStaffCredential> = listOf(
        credential("staff-01", "direccion@example.invalid", "Demo Direccion", StaffRole.DIRECCION),
        credential("staff-02", "secretaria@example.invalid", "Demo Secretaria", StaffRole.SECRETARIA),
        credential("staff-03", "trabajosocial@example.invalid", "Demo Trabajo Social", StaffRole.TRABAJO_SOCIAL),
        credential("staff-04", "medico@example.invalid", "Demo Medico Escolar", StaffRole.MEDICO_ESCOLAR),
        credential("staff-05", "udeii@example.invalid", "Demo UDEII", StaffRole.UDEII),
        credential("staff-06", "docente@example.invalid", "Demo Docente", StaffRole.DOCENTE),
        credential("staff-07", "baja@example.invalid", "Demo Baja", StaffRole.DOCENTE, active = false)
    )

    private fun credential(
        id: String,
        email: String,
        fullName: String,
        role: StaffRole,
        active: Boolean = true
    ) = MockStaffCredential(
        profile = StaffProfile(id = id, email = email, fullName = fullName, role = role, active = active),
        password = "demo1234"
    )
}
