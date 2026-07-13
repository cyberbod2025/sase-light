package com.example.ui.presolicitud

import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.enrollment.AnnualEnrollmentInitialStatus
import com.example.data.enrollment.AnnualEnrollmentMovement
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.EnrollmentFlowMode
import com.example.data.enrollment.GroupPlacementRequirement
import com.example.viewmodel.InstitutionalAnnualEnrollmentResult
import com.example.viewmodel.InstitutionalEnrollmentGuardCause
import com.example.viewmodel.PreApplicationSynchronizationCause
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnrollmentActionPresentationTest {
    @Test
    fun annualV2ModeShowsOnlyInstitutionalAnnualAction() {
        val presentation = enrollmentActionPresentation(EnrollmentFlowMode.ANNUAL_V2)

        assertFalse(presentation.showLegacyGroupControls)
        assertFalse(presentation.showLegacyStartAction)
        assertFalse(presentation.showLegacyConfirmationAction)
        assertTrue(presentation.showAnnualV2Action)
        assertTrue(presentation.annualV2ActionLabel == "Procesar inscripción anual")
        assertFalse(presentation.annualV2ActionLabel.contains("V2"))
    }

    @Test
    fun legacyModeHidesAnnualV2Action() {
        val presentation = enrollmentActionPresentation(EnrollmentFlowMode.LEGACY)

        assertTrue(presentation.showLegacyGroupControls)
        assertTrue(presentation.showLegacyStartAction)
        assertTrue(presentation.showLegacyConfirmationAction)
        assertFalse(presentation.showAnnualV2Action)
    }

    @Test
    fun needsDecisionMessageIsHonestAboutPendingGroup() {
        val result = InstitutionalAnnualEnrollmentResult.NeedsDecision(
            AnnualEnrollmentFlowResult.NeedsDecision(
                studentId = "MASTER-V2-PRE-TEST",
                enrollmentId = "S310-000001-1",
                schoolYear = "2026-2027",
                folio = "PRE-TEST",
                previousGroup = "1A",
                suggestedGroup = "2A",
                reason = "Pendiente"
            )
        )

        val message = institutionalEnrollmentMessage(result)

        assertTrue(message.contains("Inscripción anual registrada"))
        assertTrue(message.contains("asignación de grupo continúa pendiente"))
        assertFalse(message.contains("completada"))
    }

    @Test
    fun completedMessageKeepsGroupPending() {
        val result = InstitutionalAnnualEnrollmentResult.Completed(
            AnnualEnrollmentFlowResult.Completed(
                movement = AnnualEnrollmentMovement.NEW_ENTRY,
                studentId = "MASTER-V2-PRE-TEST",
                enrollmentId = "S310-000001-1",
                schoolYear = "2026-2027",
                folio = "PRE-TEST",
                status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT,
                groupRequirement = GroupPlacementRequirement.AssignmentRequired,
                message = "Registrada"
            )
        )

        val message = institutionalEnrollmentMessage(result)

        assertTrue(message.contains("Inscripción anual registrada"))
        assertTrue(message.contains("Grupo: pendiente de asignación"))
    }

    @Test
    fun alreadyCompletedMessageCommunicatesIdempotencyWithoutNewEnrollment() {
        val completedMessage = institutionalEnrollmentMessage(completedResult())
        val message = institutionalEnrollmentMessage(
            InstitutionalAnnualEnrollmentResult.AlreadyCompleted(
                AnnualEnrollmentFlowResult.AlreadyCompleted(annualEnrollmentRecord())
            )
        )

        assertTrue(message.contains("ya estaba registrada"))
        assertTrue(message.contains("Identidad y matrícula preservadas"))
        assertTrue(message.contains("no se generaron duplicados"))
        assertFalse(message.contains("V2"))
        assertNotEquals(completedMessage, message)
    }

    @Test
    fun guardRejectedMessageStatesOperationDidNotStartAndRepresentsTypedCause() {
        val result = InstitutionalAnnualEnrollmentResult.GuardRejected(
            cause = InstitutionalEnrollmentGuardCause.NOT_READY,
            message = "La pre-solicitud debe estar declarada READY."
        )
        val message = institutionalEnrollmentMessage(result)
        val conflictMessage = institutionalEnrollmentMessage(annualConflictResult())

        assertTrue(result.cause == InstitutionalEnrollmentGuardCause.NOT_READY)
        assertTrue(message.contains("No se inició"))
        assertTrue(message.contains("debe estar declarada READY"))
        assertFalse(message.contains("registrada"))
        assertFalse(message.contains("V2"))
        assertNotEquals(conflictMessage, message)
    }

    @Test
    fun annualConflictMessagePreservesStageAndCauseWithoutSuccess() {
        val result = annualConflictResult()
        val message = institutionalEnrollmentMessage(result)
        val synchronizationMessage = institutionalEnrollmentMessage(synchronizationIncompleteResult())

        assertTrue(result.annualResult.cause == "PRE_APPLICATION_FOLIO_REUSED_WITH_DIFFERENT_DATA")
        assertTrue(message.contains("No fue posible completar"))
        assertTrue(message.contains("Etapa: IDEMPOTENCY"))
        assertTrue(message.contains("El folio ya existe con datos incompatibles"))
        assertFalse(message.contains("Inscripción anual registrada"))
        assertFalse(message.contains("V2"))
        assertNotEquals(synchronizationMessage, message)
    }

    @Test
    fun synchronizationIncompleteMessageAcknowledgesPartialStateWithoutRollbackClaim() {
        val message = institutionalEnrollmentMessage(synchronizationIncompleteResult())

        assertTrue(message.contains("La anualidad existe"))
        assertTrue(message.contains("sincronización institucional está incompleta"))
        assertTrue(message.contains("requiere revisión"))
        assertFalse(message.contains("Inscripción anual registrada"))
        assertFalse(message.contains("completada", ignoreCase = true))
        assertFalse(message.contains("registrada correctamente"))
        assertFalse(message.contains("rollback", ignoreCase = true))
        assertFalse(message.contains("V2"))
    }

    @Test
    fun completedActionPreservesStudentIdentityAndFullRecordKey() {
        val action = assertNotNull(institutionalEnrollmentRecordAction(completedResult()))

        assertEquals("Abrir expediente", action.label)
        assertEquals("MASTER-V2-PRE-TEST", action.key.studentId)
        assertEquals("S310-000001-1", action.key.enrollmentId)
        assertEquals("2026-2027", action.key.schoolYear)
        assertEquals("PRE-TEST", action.key.sourcePreApplicationFolio)
    }

    @Test
    fun needsDecisionActionNavigatesWithStudentIdRatherThanVisibleIdentifiers() {
        val result = InstitutionalAnnualEnrollmentResult.NeedsDecision(
            AnnualEnrollmentFlowResult.NeedsDecision(
                studentId = "MASTER-V2-PRE-TEST",
                enrollmentId = "S310-000001-1",
                schoolYear = "2026-2027",
                folio = "PRE-TEST",
                previousGroup = "1A",
                suggestedGroup = "2A",
                reason = "Pendiente"
            )
        )

        val action = assertNotNull(institutionalEnrollmentRecordAction(result))

        assertEquals("MASTER-V2-PRE-TEST", action.key.studentId)
        assertEquals("S310-000001-1", action.key.enrollmentId)
        assertEquals("2026-2027", action.key.schoolYear)
        assertEquals("PRE-TEST", action.key.sourcePreApplicationFolio)
        assertNotEquals(action.key.enrollmentId, action.key.studentId)
        assertNotEquals(action.key.sourcePreApplicationFolio, action.key.studentId)
    }

    @Test
    fun alreadyCompletedActionUsesPersistedStudentIdentity() {
        val action = assertNotNull(
            institutionalEnrollmentRecordAction(
                InstitutionalAnnualEnrollmentResult.AlreadyCompleted(
                    AnnualEnrollmentFlowResult.AlreadyCompleted(annualEnrollmentRecord())
                )
            )
        )

        assertEquals(annualEnrollmentRecord().studentId, action.key.studentId)
        assertEquals(annualEnrollmentRecord().permanentEnrollmentId, action.key.enrollmentId)
        assertEquals(annualEnrollmentRecord().schoolYear, action.key.schoolYear)
        assertEquals(annualEnrollmentRecord().sourcePreApplicationFolio, action.key.sourcePreApplicationFolio)
    }

    @Test
    fun rejectedConflictAndIncompleteResultsDoNotExposeRecordAction() {
        val rejected = InstitutionalAnnualEnrollmentResult.GuardRejected(
            InstitutionalEnrollmentGuardCause.NOT_READY,
            "No disponible"
        )

        assertNull(institutionalEnrollmentRecordAction(rejected))
        assertNull(institutionalEnrollmentRecordAction(annualConflictResult()))
        assertNull(institutionalEnrollmentRecordAction(synchronizationIncompleteResult()))
    }

    @Test
    fun blankStudentIdDoesNotExposeRecordAction() {
        val malformed = completedResult().copy(
            annualResult = completedResult().annualResult.copy(studentId = "")
        )

        assertNull(institutionalEnrollmentRecordAction(malformed))
    }

    @Test
    fun incompleteResolutionContextDoesNotExposeRecordAction() {
        val annualResult = completedResult().annualResult

        assertNull(
            institutionalEnrollmentRecordAction(
                completedResult().copy(annualResult = annualResult.copy(schoolYear = ""))
            )
        )
        assertNull(
            institutionalEnrollmentRecordAction(
                completedResult().copy(annualResult = annualResult.copy(folio = ""))
            )
        )
        assertNull(
            institutionalEnrollmentRecordAction(
                completedResult().copy(annualResult = annualResult.copy(enrollmentId = ""))
            )
        )
    }

    private fun completedResult() = InstitutionalAnnualEnrollmentResult.Completed(
        AnnualEnrollmentFlowResult.Completed(
            movement = AnnualEnrollmentMovement.NEW_ENTRY,
            studentId = "MASTER-V2-PRE-TEST",
            enrollmentId = "S310-000001-1",
            schoolYear = "2026-2027",
            folio = "PRE-TEST",
            status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT,
            groupRequirement = GroupPlacementRequirement.AssignmentRequired,
            message = "Registrada"
        )
    )

    private fun annualEnrollmentRecord() = AnnualEnrollmentRecord(
        studentId = "MASTER-V2-PRE-TEST",
        normalizedCurp = "TEST100101HDFABC01",
        permanentEnrollmentId = "S310-000001-1",
        schoolYear = "2026-2027",
        sourcePreApplicationFolio = "PRE-TEST",
        movement = AnnualEnrollmentMovement.NEW_ENTRY,
        requestedGrade = 1,
        groupPlacementRequirement = GroupPlacementRequirement.AssignmentRequired,
        status = AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT
    )

    private fun annualConflictResult() = InstitutionalAnnualEnrollmentResult.AnnualConflict(
        AnnualEnrollmentFlowResult.Conflict(
            cause = "PRE_APPLICATION_FOLIO_REUSED_WITH_DIFFERENT_DATA",
            message = "El folio ya existe con datos incompatibles.",
            stage = "IDEMPOTENCY"
        )
    )

    private fun synchronizationIncompleteResult() =
        InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete(
            annualResult = AnnualEnrollmentFlowResult.AlreadyCompleted(annualEnrollmentRecord()),
            annualEnrollment = annualEnrollmentRecord(),
            cause = PreApplicationSynchronizationCause.PREVIOUSLY_UNSYNCHRONIZED,
            message = "La anualidad existe, pero la pre-solicitud no está sincronizada."
        )
}
