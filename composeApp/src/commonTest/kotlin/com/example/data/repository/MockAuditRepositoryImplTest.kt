package com.example.data.repository

import com.example.audit.InstitutionalAuditEvent
import com.example.audit.InstitutionalAuditResult
import com.example.data.MockSaseData
import com.example.data.auth.StaffRole
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class MockAuditRepositoryImplTest {
    private val repository = MockAuditRepositoryImpl()

    @BeforeTest
    fun reset() {
        MockSaseData.resetForTests()
    }

    @Test
    fun typedEventIsStoredWithTraceableContextAndLegacyProjection() {
        val event = validEvent()

        repository.logAudit(event)

        val stored = repository.audits.value.first()
        assertEquals(event, stored.institutionalEvent)
        assertEquals("STUDENT_RECORD_OPENED", stored.action)
        assertEquals("Secretaría", stored.userRole)
        assertEquals("student_record:student-demo [AUTHORIZED]", stored.detail)
        assertNotNull(stored.institutionalEvent)
    }

    @Test
    fun invalidTypedEventIsRejectedWithoutChangingAuditState() {
        val before = repository.audits.value
        val sensitiveValue = "curp=DO_NOT_STORE"

        val failure = assertFailsWith<IllegalArgumentException> {
            repository.logAudit(validEvent().copy(entityId = sensitiveValue))
        }

        assertEquals(before, repository.audits.value)
        kotlin.test.assertFalse(failure.message.orEmpty().contains(sensitiveValue))
    }

    private fun validEvent(): InstitutionalAuditEvent = InstitutionalAuditEvent(
        institutionId = "institution-demo",
        actorProfileId = "profile-demo",
        membershipId = "membership-demo",
        activeRole = StaffRole.SECRETARIA,
        action = "STUDENT_RECORD_OPENED",
        entityType = "student_record",
        entityId = "student-demo",
        timestamp = "2026-08-04T12:00:00Z",
        result = InstitutionalAuditResult.AUTHORIZED,
        sourcePlatform = "desktop"
    )
}
