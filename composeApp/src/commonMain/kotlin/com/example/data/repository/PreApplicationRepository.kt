package com.example.data.repository

import com.example.data.presolicitud.PreApplication
import kotlinx.coroutines.flow.StateFlow

/** Motivo por el que una operacion de pre-solicitud no se pudo completar. */
enum class PreApplicationPersistenceFailure {
    NO_SESSION,
    NETWORK,
    REJECTED
}

sealed class PreApplicationSyncResult {
    data class Loaded(val preApplications: List<PreApplication>) : PreApplicationSyncResult()
    data class Failed(val reason: PreApplicationPersistenceFailure) : PreApplicationSyncResult()
}

sealed class PreApplicationSubmitResult {
    /**
     * [accessToken] es el token de acceso opaco (uuid) que el servidor
     * genera para esta pre-solicitud (migracion 0015) -- unica identidad de
     * la familia frente al portal, sin cuenta/login. Se devuelve una sola
     * vez; el llamador (ViewModel) es responsable de conservarlo en un
     * [com.example.data.auth.FamilySession] y de mostrarlo a la familia.
     */
    data class Submitted(val preApplication: PreApplication, val accessToken: String) : PreApplicationSubmitResult()
    data class DuplicateCurp(val curp: String, val existing: PreApplication) : PreApplicationSubmitResult()
    data class DuplicateFolio(val folio: String) : PreApplicationSubmitResult()
    data class Failed(val reason: PreApplicationPersistenceFailure) : PreApplicationSubmitResult()
}

sealed class PreApplicationUpdateResult {
    data class Updated(val preApplication: PreApplication) : PreApplicationUpdateResult()
    data class Failed(val reason: PreApplicationPersistenceFailure) : PreApplicationUpdateResult()
}

/**
 * Costura de pre-solicitudes. Mismo criterio que [StudentRepository]: toda
 * operacion es `suspend` porque una implementacion conectada habla por red, y
 * ninguna puede resolverse en el hilo de interfaz ni devolver un exito antes
 * de que el backend lo confirme.
 *
 * A diferencia de [StudentRepository] (que separa observaciones/incidencias
 * en sus propias operaciones porque tienen RLS distinto al nucleo), el
 * diseno de la migracion 0015 revisa TODOS los bloques 1:1 de una
 * pre-solicitud como una sola unidad por la misma persona (SECRETARIA, via
 * REVIEW_PRE_APPLICATION) -- una sola operacion [update] de fila completa
 * basta para todas las mutaciones (aprobar, cotejar documento, declarar
 * readiness, etc.); no hay necesidad de operaciones separadas por campo.
 */
interface PreApplicationRepository {
    val preApplications: StateFlow<List<PreApplication>>

    /**
     * Recarga el listado desde el origen. El mock la resuelve en memoria; la
     * implementacion de Supabase consulta solo la institucion de la sesion
     * (familia: solo su propia pre-solicitud: SECRETARIA vía REVIEW_PRE_APPLICATION: todas).
     */
    suspend fun refresh(): PreApplicationSyncResult

    /** Alta de una pre-solicitud nueva (envio familiar). */
    suspend fun submit(preApplication: PreApplication): PreApplicationSubmitResult

    /**
     * Reemplaza la fila completa (y sus 3 colecciones hijas) de una
     * pre-solicitud ya existente, identificada por [PreApplication.folio].
     */
    suspend fun update(preApplication: PreApplication): PreApplicationUpdateResult

    /**
     * Vacia el estado en memoria (sin red). Se invoca en logout/expiracion de
     * sesion/antes de aceptar una sesion nueva para que el StateFlow nunca
     * exponga pre-solicitudes de un usuario/institucion anterior.
     */
    fun clear()
}
