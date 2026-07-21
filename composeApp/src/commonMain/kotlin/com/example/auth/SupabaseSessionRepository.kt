package com.example.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio de sesión real del personal. Orquesta los gateways de
 * infraestructura y mapea todo a modelos de dominio: aquí no entra ningún
 * tipo del SDK, ningún token y ninguna contraseña almacenada.
 *
 * Cadena de validación del login: credenciales → perfil existente →
 * membresía activa (con institución, roles y scopes leídos del directorio
 * almacenado — nunca del cliente). Si cualquier paso posterior a la
 * autenticación falla, la sesión remota se revoca para no dejar estados a
 * medias. Los permisos efectivos siguen calculándose con AccessPolicy.
 */
class SupabaseSessionRepository(
    private val authGateway: StaffAuthGateway,
    private val directoryGateway: StaffDirectoryGateway
) : CredentialSessionRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.NoSession)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signInWithCredentials(email: String, password: String): StaffSignInResult {
        val authenticated = when (val result = authGateway.signIn(email, password)) {
            is StaffAuthGatewayResult.Failed -> {
                _authState.value = AuthState.NoSession
                return StaffSignInResult.Failure(result.failure)
            }
            is StaffAuthGatewayResult.Authenticated -> result.userId
        }
        return when (val resolution = resolveSession(authenticated)) {
            is StaffSignInResult.Success -> resolution
            is StaffSignInResult.Failure -> {
                // No dejar una sesión autenticada sin contexto institucional.
                authGateway.signOut()
                _authState.value = AuthState.NoSession
                resolution
            }
        }
    }

    override suspend fun signOutWithRevocation() {
        authGateway.signOut()
        _authState.value = AuthState.NoSession
    }

    /** Cierre local inmediato; la revocación remota usa [signOutWithRevocation]. */
    override fun signOut() {
        _authState.value = AuthState.NoSession
    }

    override suspend fun restoreSession(): AuthState {
        val userId = authGateway.restoreUserId() ?: run {
            _authState.value = AuthState.NoSession
            return AuthState.NoSession
        }
        return when (val resolution = resolveSession(userId)) {
            is StaffSignInResult.Success -> _authState.value
            is StaffSignInResult.Failure -> {
                authGateway.signOut()
                _authState.value = AuthState.NoSession
                AuthState.NoSession
            }
        }
    }

    override fun resetForTests() {
        _authState.value = AuthState.NoSession
    }

    private suspend fun resolveSession(userId: UserId): StaffSignInResult {
        val profile = try {
            directoryGateway.fetchProfile(userId)
        } catch (_: Throwable) {
            return StaffSignInResult.Failure(StaffAuthFailure.PROFILE_UNAVAILABLE)
        } ?: return StaffSignInResult.Failure(StaffAuthFailure.PROFILE_UNAVAILABLE)

        val membership = try {
            directoryGateway.fetchMembership(userId)
        } catch (_: Throwable) {
            return StaffSignInResult.Failure(StaffAuthFailure.MEMBERSHIP_NOT_FOUND)
        } ?: return StaffSignInResult.Failure(StaffAuthFailure.MEMBERSHIP_NOT_FOUND)

        if (membership.userId != userId) {
            return StaffSignInResult.Failure(StaffAuthFailure.MEMBERSHIP_NOT_FOUND)
        }
        if (!membership.active) {
            return StaffSignInResult.Failure(StaffAuthFailure.MEMBERSHIP_INACTIVE)
        }

        val session = SessionContext(
            userId = userId,
            displayName = profile.displayName,
            membership = membership
        )
        _authState.value = AuthState.Active(session)
        return StaffSignInResult.Success(session)
    }
}
