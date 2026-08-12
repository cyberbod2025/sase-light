package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.data.SaseAudit
import kotlinx.coroutines.flow.StateFlow

interface AuditRepository {
    val audits: StateFlow<List<SaseAudit>>

    /** Recarga la bitacora de la institucion de la sesion activa. */
    suspend fun refresh(): Boolean

    /**
     * Registra solo un evento con identidad, membresia y rol tipados.
     *
     * Devuelve `false` cuando el evento no quedo asentado. Quien muta una
     * entidad institucional debe registrar la bitacora ANTES de confirmar el
     * cambio: sin evidencia no hay mutacion.
     */
    suspend fun logAudit(event: InstitutionalAuditEvent): Boolean

    /**
     * Vacia el estado en memoria (sin red). Se invoca en logout/expiracion de
     * sesion/antes de aceptar una sesion nueva para que el StateFlow nunca
     * exponga la bitacora de un usuario/institucion anterior.
     */
    fun clear()
}
