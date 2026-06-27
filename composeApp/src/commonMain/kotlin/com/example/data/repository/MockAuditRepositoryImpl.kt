package com.example.data.repository

import com.example.data.MockSaseData
import com.example.data.SaseAudit
import kotlinx.coroutines.flow.StateFlow

class MockAuditRepositoryImpl : AuditRepository {
    override val audits: StateFlow<List<SaseAudit>> = MockSaseData.audits

    override fun logAudit(action: String, role: String, timestamp: String, detail: String) {
        MockSaseData.logAudit(action, role, timestamp, detail)
    }
}
