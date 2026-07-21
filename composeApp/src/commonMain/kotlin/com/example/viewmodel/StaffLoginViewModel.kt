package com.example.viewmodel

import com.example.auth.AuthState
import com.example.auth.BackendUnavailableReason
import com.example.auth.CredentialSessionRepository
import com.example.auth.SessionRepository
import com.example.auth.SessionRepositoryProvision
import com.example.auth.StaffAuthFailure
import com.example.auth.StaffSignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Motivo de rechazo que SÍ puede verse en pantalla. Se traduce a texto en la
 * UI; nunca transporta mensajes internos del SDK, tokens ni credenciales.
 */
enum class LoginErrorReason {
    INVALID_CREDENTIALS,
    INACTIVE_MEMBERSHIP,
    NO_INSTITUTIONAL_ACCESS,
    NETWORK,
    UNEXPECTED
}

/** Estado observable de la pantalla de acceso. */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Submitting : LoginUiState
    data class Error(val reason: LoginErrorReason) : LoginUiState

    /**
     * El backend elegido no puede proveerse (configuración ausente o inválida).
     * No hay fallback silencioso a mock: la pantalla queda deshabilitada.
     */
    data class BackendUnavailable(val reason: BackendUnavailableReason) : LoginUiState
}

/**
 * ViewModel del acceso del personal. No guarda contraseñas: las recibe como
 * parámetro y las entrega al repositorio sin retenerlas en el estado.
 * La sesión y los roles SIEMPRE provienen del repositorio, nunca de la UI.
 */
class StaffLoginViewModel(
    private val provision: SessionRepositoryProvision
) {

    private val _uiState = MutableStateFlow<LoginUiState>(initialState(provision))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Repositorio activo, o null si el backend no pudo proveerse. */
    val sessionRepository: SessionRepository? =
        (provision as? SessionRepositoryProvision.Ready)?.repository

    val authState: StateFlow<AuthState>? = sessionRepository?.authState

    private val credentialRepository: CredentialSessionRepository?
        get() = sessionRepository as? CredentialSessionRepository

    /** Limpia un error previo al volver a escribir en el formulario. */
    fun onInputChanged() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    suspend fun signIn(email: String, password: String) {
        if (_uiState.value is LoginUiState.BackendUnavailable) return
        if (email.isBlank() || password.isEmpty()) {
            _uiState.value = LoginUiState.Error(LoginErrorReason.INVALID_CREDENTIALS)
            return
        }
        val repository = credentialRepository ?: run {
            _uiState.value = LoginUiState.Error(LoginErrorReason.UNEXPECTED)
            return
        }

        _uiState.value = LoginUiState.Submitting
        when (val result = repository.signInWithCredentials(email, password)) {
            is StaffSignInResult.Success -> _uiState.value = LoginUiState.Idle
            is StaffSignInResult.Failure ->
                _uiState.value = LoginUiState.Error(result.failure.toReason())
        }
    }

    /** Intenta reanudar una sesión previa; el resultado lo publica el repositorio. */
    suspend fun restoreSession() {
        credentialRepository?.restoreSession()
    }

    suspend fun signOut() {
        val repository = sessionRepository ?: return
        when (repository) {
            is CredentialSessionRepository -> repository.signOutWithRevocation()
            else -> repository.signOut()
        }
        _uiState.value = LoginUiState.Idle
    }

    private companion object {
        fun initialState(provision: SessionRepositoryProvision): LoginUiState =
            when (provision) {
                is SessionRepositoryProvision.Ready -> LoginUiState.Idle
                is SessionRepositoryProvision.Unavailable ->
                    LoginUiState.BackendUnavailable(provision.reason)
            }
    }
}

/**
 * Traducción de fallos de infraestructura a motivos presentables. Los detalles
 * que distinguirían "correo inexistente" de "contraseña incorrecta" se funden
 * a propósito en INVALID_CREDENTIALS.
 */
internal fun StaffAuthFailure.toReason(): LoginErrorReason = when (this) {
    StaffAuthFailure.INVALID_CREDENTIALS -> LoginErrorReason.INVALID_CREDENTIALS
    StaffAuthFailure.MEMBERSHIP_INACTIVE -> LoginErrorReason.INACTIVE_MEMBERSHIP
    StaffAuthFailure.MEMBERSHIP_NOT_FOUND,
    StaffAuthFailure.PROFILE_UNAVAILABLE -> LoginErrorReason.NO_INSTITUTIONAL_ACCESS
    StaffAuthFailure.NETWORK_UNAVAILABLE -> LoginErrorReason.NETWORK
    StaffAuthFailure.CONFIGURATION_MISSING,
    StaffAuthFailure.UNEXPECTED_FAILURE -> LoginErrorReason.UNEXPECTED
}
