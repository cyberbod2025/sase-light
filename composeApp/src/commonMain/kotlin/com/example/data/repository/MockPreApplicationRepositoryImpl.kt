package com.example.data.repository

import com.example.data.presolicitud.MockPreApplicationData
import com.example.data.presolicitud.PreApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Implementacion en memoria de DEMO_LOCAL. Es `suspend` solo para compartir la
 * costura con la implementacion conectada: aqui nada viaja por red, asi que
 * ninguna operacion puede fallar por transporte y el comportamiento
 * observable es el mismo que tenia el companion object de
 * `PreApplicationViewModel` antes de este repositorio.
 */
class MockPreApplicationRepositoryImpl : PreApplicationRepository {
    private val _preApplications = MutableStateFlow(MockPreApplicationData.preApplications)
    override val preApplications: StateFlow<List<PreApplication>> = _preApplications.asStateFlow()

    override suspend fun refresh(): PreApplicationSyncResult =
        PreApplicationSyncResult.Loaded(_preApplications.value)

    override suspend fun submit(preApplication: PreApplication): PreApplicationSubmitResult {
        val current = _preApplications.value
        if (preApplication.folio.isNotBlank() && current.any { it.folio == preApplication.folio }) {
            return PreApplicationSubmitResult.DuplicateFolio(preApplication.folio)
        }
        val normalizedCurp = preApplication.alumnoCurp.trim().uppercase()
        val curpConflict = current.firstOrNull { it.alumnoCurp.trim().uppercase() == normalizedCurp }
        if (curpConflict != null) {
            return PreApplicationSubmitResult.DuplicateCurp(normalizedCurp, curpConflict)
        }
        _preApplications.value = current + preApplication
        return PreApplicationSubmitResult.Submitted(preApplication, accessToken = randomAccessToken())
    }

    override suspend fun update(preApplication: PreApplication): PreApplicationUpdateResult {
        val current = _preApplications.value
        if (current.none { it.folio == preApplication.folio }) {
            return PreApplicationUpdateResult.Failed(PreApplicationPersistenceFailure.REJECTED)
        }
        _preApplications.value = current.map { if (it.folio == preApplication.folio) preApplication else it }
        return PreApplicationUpdateResult.Updated(preApplication)
    }

    override fun clear() {
        // DEMO_LOCAL es un dataset compartido de demostracion, no datos de un
        // usuario real: no hay sesion anterior de la que aislarse (mismo
        // criterio que MockStudentRepositoryImpl.clear()).
    }

    /** Solo para pruebas: reinicia el dataset a los fixtures originales. */
    fun resetToFixtures() {
        _preApplications.value = MockPreApplicationData.preApplications
    }

    /**
     * DEMO_LOCAL no valida este token contra nada (mismo criterio que el
     * resto del dataset compartido: sin aislamiento por actor) -- solo se
     * genera para que el ViewModel tenga un [com.example.data.auth.FamilySession]
     * con el mismo aspecto que en SUPABASE_STAGING.
     */
    private fun randomAccessToken(): String {
        val hex = "0123456789abcdef"
        return buildString { repeat(32) { append(hex[Random.nextInt(hex.length)]) } }
    }
}
