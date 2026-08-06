package com.example.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Costura de autenticacion. La app solo conoce esta interfaz, de modo que la
 * implementacion mock y la de Supabase sean intercambiables.
 */
interface AuthRepository {
    val session: StateFlow<AuthSession?>
    val demoAccessAvailable: Boolean get() = false
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signInDemo(role: StaffRole): AuthResult =
        AuthResult.Failure(AuthFailureReason.DEMO_UNAVAILABLE)
    suspend fun selectRole(roleId: String): AuthResult =
        AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
    suspend fun switchRole(roleId: String): AuthResult =
        AuthResult.Failure(AuthFailureReason.ROLE_NOT_ASSIGNED)
    suspend fun resetDemo() = Unit
    suspend fun signOut()
}
