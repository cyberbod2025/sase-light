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

    override fun clear() {
        // DEMO_LOCAL es un dataset compartido de demostracion, no datos de un
        // usuario real: no hay sesion anterior de la que aislarse. Ver el
        // mismo criterio en MockStudentRepositoryImpl.clear().
    }
}
