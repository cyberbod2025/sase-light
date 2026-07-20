package com.example.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Costura de autenticacion. La app solo conoce esta interfaz, de modo que la
 * implementacion mock y la de Supabase sean intercambiables.
 */
interface AuthRepository {
    val session: StateFlow<AuthSession?>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signOut()
}
