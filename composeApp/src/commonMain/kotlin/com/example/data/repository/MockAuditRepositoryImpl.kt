package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.data.MockSaseData
import com.example.data.SaseAudit
import kotlinx.coroutines.flow.StateFlow

class MockAuditRepositoryImpl : AuditRepository {
    override val audits: StateFlow<List<SaseAudit>> = MockSaseData.audits

    override fun logAudit(event: InstitutionalAuditEvent) {
        MockSaseData.logAudit(event)
    }
}
