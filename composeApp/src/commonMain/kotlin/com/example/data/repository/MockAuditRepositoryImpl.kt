package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.data.MockSaseData
import com.example.data.SaseAudit
import kotlinx.coroutines.flow.StateFlow

class MockAuditRepositoryImpl : AuditRepository {
    override val audits: StateFlow<List<SaseAudit>> = MockSaseData.audits

    override suspend fun refresh(): Boolean = true

    override suspend fun logAudit(event: InstitutionalAuditEvent): Boolean {
        MockSaseData.logAudit(event)
        return true
    }
}
