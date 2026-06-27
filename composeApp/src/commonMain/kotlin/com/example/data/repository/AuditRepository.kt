package com.example.data.repository

import com.example.data.SaseAudit
import kotlinx.coroutines.flow.StateFlow

interface AuditRepository {
    val audits: StateFlow<List<SaseAudit>>
    fun logAudit(action: String, role: String, timestamp: String, detail: String)
}
