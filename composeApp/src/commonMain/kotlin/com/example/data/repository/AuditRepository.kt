package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.data.SaseAudit
import kotlinx.coroutines.flow.StateFlow

interface AuditRepository {
    val audits: StateFlow<List<SaseAudit>>

    /** Registra solo un evento con identidad, membresia y rol tipados. */
    fun logAudit(event: InstitutionalAuditEvent)

}
