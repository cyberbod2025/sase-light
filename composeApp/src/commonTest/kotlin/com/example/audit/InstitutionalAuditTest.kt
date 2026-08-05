package com.example.audit

import com.example.data.auth.StaffRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstitutionalAuditTest {

    @Test
    fun validEventCarriesTheRequiredInstitutionalContext() {
        val event = validEvent()

        assertEquals("institution-demo", event.institutionId)
        assertEquals("profile-demo", event.actorProfileId)
        assertEquals("membership-demo", event.membershipId)
        assertEquals(StaffRole.SECRETARIA, event.activeRole)
        assertEquals("STUDENT_RECORD_OPENED", event.action)
        assertEquals("student_record", event.entityType)
        assertEquals("student-demo", event.entityId)
        assertEquals("2026-08-04T12:00:00Z", event.timestamp)
        assertEquals(InstitutionalAuditResult.AUTHORIZED, event.result)
        assertEquals("desktop", event.sourcePlatform)
        assertTrue(InstitutionalAuditValidator.validate(event).isValid)
        assertEquals(
            setOf(
                InstitutionalAuditResult.AUTHORIZED,
                InstitutionalAuditResult.DENIED,
                InstitutionalAuditResult.FAILED
            ),
            InstitutionalAuditResult.entries.toSet()
        )
    }

    @Test
    fun everyRequiredTextFieldRejectsBlankContent() {
        val event = validEvent()
        val blankVariants = listOf(
            InstitutionalAuditField.INSTITUTION_ID to event.copy(institutionId = " "),
            InstitutionalAuditField.ACTOR_PROFILE_ID to event.copy(actorProfileId = " "),
            InstitutionalAuditField.MEMBERSHIP_ID to event.copy(membershipId = " "),
            InstitutionalAuditField.ACTION to event.copy(action = " "),
            InstitutionalAuditField.ENTITY_TYPE to event.copy(entityType = " "),
            InstitutionalAuditField.ENTITY_ID to event.copy(entityId = " "),
            InstitutionalAuditField.TIMESTAMP to event.copy(timestamp = " "),
            InstitutionalAuditField.SOURCE_PLATFORM to event.copy(sourcePlatform = " ")
        )

        blankVariants.forEach { (field, variant) ->
            val validation = InstitutionalAuditValidator.validate(variant)

            assertFalse(validation.isValid, "$field debe ser obligatorio")
            assertTrue(
                InstitutionalAuditViolation(
                    field = field,
                    code = InstitutionalAuditViolationCode.REQUIRED
                ) in validation.violations,
                "$field debe reportarse sin incluir su valor"
            )
        }
    }

    @Test
    fun sensitiveContentIsRejectedAndNeverEchoedByDiagnostics() {
        val sensitiveSamples = listOf(
            "password=DO_NOT_STORE",
            "token=DO_NOT_STORE",
            "curp=DO_NOT_STORE",
            "diagnosis=DO_NOT_STORE",
            "Bearer DO_NOT_STORE",
            "eyJdemo.payload.signature",
            "AAAA000000HAAAAAA0"
        )

        sensitiveSamples.forEach { sample ->
            val event = validEvent().copy(entityId = sample)
            val validation = InstitutionalAuditValidator.validate(event)
            val rendered = event.toString()

            assertFalse(validation.isValid, "el contenido sensible debe rechazarse")
            assertTrue(
                InstitutionalAuditViolation(
                    field = InstitutionalAuditField.ENTITY_ID,
                    code = InstitutionalAuditViolationCode.SENSITIVE_CONTENT
                ) in validation.violations
            )
            assertFalse(validation.toString().contains(sample, ignoreCase = true))
            assertFalse(rendered.contains(sample, ignoreCase = true))
            assertTrue(rendered.contains(InstitutionalAuditSanitizer.REDACTED))
        }
    }

    @Test
    fun safeAuditRepresentationHasNoSensitiveFieldsOrDetailBag() {
        val rendered = validEvent().toString().lowercase()

        listOf("password=", "token=", "curp=", "diagnosis=", "detail=").forEach { forbidden ->
            assertFalse(rendered.contains(forbidden), "no debe aparecer $forbidden")
        }
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
