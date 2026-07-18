package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.ReadinessStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contrato de integración de [PreApplicationViewModel.correctPreApplicationCurp].
 *
 * La política pura es la única fuente de normalización, formato y resolución
 * de duplicados. El ViewModel localiza y protege el registro, construye
 * candidatos desde las tres fuentes institucionales, muta únicamente ante
 * éxito y reconcilia readiness después del cambio.
 */
class PreApplicationCurpCorrectionTest {

    private companion object {
        const val FOLIO_NEW = "PRE-NEW-001"
        const val FOLIO_CONFLICT = "PRE-CONFLICT-001"
        const val FOLIO_CONVERTED = "PRE-X1A2"
        const val FOLIO_REENROLLMENT = "PRE-REENROLL-001"
        const val CURP_UNIQUE_A = "NEWD020202HDFXYZ88"
        const val CURP_UNIQUE_B = "NEWE030303MDFQRS77"
    }

    @BeforeTest
    fun resetSharedState() {
        PreApplicationViewModel.resetSharedStateForTests()
    }

    private fun stored(folio: String): PreApplication =
        PreApplicationViewModel.sharedPreApplications.value.single { it.folio == folio }

    private fun duplicate(result: CurpCorrectionResult): CurpCorrectionResult.Duplicate =
        assertIs<CurpCorrectionResult.Duplicate>(result)

    private fun sources(result: CurpCorrectionResult): Set<CurpConflictSource> =
        duplicate(result).conflict.matches.map { it.source }.toSet()

    @Test
    fun syntheticCurpsAreValidAndDoNotCollideWithInitialFixtures() {
        val initialCurps = (
            PreApplicationViewModel.sharedPreApplications.value.map { it.alumnoCurp } +
                MockSaseData.students.value.map { it.curp } +
                PreApplicationViewModel.officialStudents.value.map { it.curp }
            )
            .map(CurpCorrectionPolicy::normalize)
            .toSet()

        listOf(CURP_UNIQUE_A, CURP_UNIQUE_B).forEach { curp ->
            assertIs<CurpValidationResult.Valid>(CurpCorrectionPolicy.validate(curp))
            assertFalse(curp in initialCurps, "$curp debe ser sintética y no colisionar con fixtures")
        }
    }

    @Test
    fun validCurpIsNormalizedPersistedAndReportedExplicitly() {
        val result = assertIs<CurpCorrectionResult.Updated>(
            PreApplicationViewModel.correctPreApplicationCurp(
                FOLIO_NEW,
                "  ${CURP_UNIQUE_A.lowercase()}  "
            )
        )

        assertEquals(CURP_UNIQUE_A, result.normalizedCurp)
        assertEquals(CURP_UNIQUE_A, stored(FOLIO_NEW).alumnoCurp)
        assertEquals(ReadinessStatus.READY, stored(FOLIO_NEW).readinessStatus)
    }

    @Test
    fun invalidFormatsReturnInvalidFormatWithoutAnyMutation() {
        val before = PreApplicationViewModel.sharedPreApplications.value
        val invalidCurps = listOf(
            CURP_UNIQUE_A.dropLast(1),
            CURP_UNIQUE_A + "9",
            CURP_UNIQUE_A.replaceRange(10, 11, "X"),
            "   "
        )

        invalidCurps.forEach { invalidCurp ->
            val result = assertIs<CurpCorrectionResult.InvalidFormat>(
                PreApplicationViewModel.correctPreApplicationCurp(FOLIO_NEW, invalidCurp)
            )
            assertTrue(result.message.isNotBlank())
            assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
        }
    }

    @Test
    fun familySubmissionRejectsAnEighteenCharacterNonCanonicalCurp() {
        val invalidCurp = "ABCDEFGHIJKLMNOPQR"
        val candidate = stored(FOLIO_NEW).copy(
            folio = "PRE-INVALID-CURP",
            alumnoCurp = invalidCurp
        )
        val beforePreApplications = PreApplicationViewModel.sharedPreApplications.value
        val beforeOfficials = PreApplicationViewModel.officialStudents.value
        val beforeAnnuals = MockSaseData.annualEnrollments.value
        val beforeMaster = MockSaseData.students.value

        val result = assertIs<FamilySubmissionResult.InsufficientData>(
            PreApplicationViewModel.submitFamilyPreApplication(candidate)
        )

        assertEquals(CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE, result.message)
        assertEquals(beforePreApplications, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(beforeOfficials, PreApplicationViewModel.officialStudents.value)
        assertEquals(beforeAnnuals, MockSaseData.annualEnrollments.value)
        assertEquals(beforeMaster, MockSaseData.students.value)
    }

    @Test
    fun familyResubmissionRejectsAnEighteenCharacterNonCanonicalCurp() {
        val correction = assertIs<CorrectionRequestResult.Success>(
            PreApplicationViewModel.requestCorrection(
                FOLIO_NEW,
                "Corregir datos familiares"
            )
        )
        val beforePreApplications = PreApplicationViewModel.sharedPreApplications.value
        val beforeOfficials = PreApplicationViewModel.officialStudents.value
        val beforeAnnuals = MockSaseData.annualEnrollments.value
        val beforeMaster = MockSaseData.students.value

        val result = assertIs<FamilyResubmissionResult.InvalidCurp>(
            PreApplicationViewModel.resubmitCorrectedPreApplication(
                correction.preApplication.copy(alumnoCurp = "ABCDEFGHIJKLMNOPQR")
            )
        )

        assertEquals(CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE, result.message)
        assertEquals(beforePreApplications, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(beforeOfficials, PreApplicationViewModel.officialStudents.value)
        assertEquals(beforeAnnuals, MockSaseData.annualEnrollments.value)
        assertEquals(beforeMaster, MockSaseData.students.value)
    }

    @Test
    fun unknownFolioReturnsNotFoundWithoutMutation() {
        val before = PreApplicationViewModel.sharedPreApplications.value

        assertIs<CurpCorrectionResult.NotFound>(
            PreApplicationViewModel.correctPreApplicationCurp("PRE-NO-EXISTE", CURP_UNIQUE_A)
        )

        assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun convertedPreApplicationReturnsAlreadyConvertedAndTouchesNoInstitutionalState() {
        val beforePreApplications = PreApplicationViewModel.sharedPreApplications.value
        val beforeOfficials = PreApplicationViewModel.officialStudents.value
        val beforeAnnuals = MockSaseData.annualEnrollments.value
        val beforeMaster = MockSaseData.students.value
        assertEquals(ReadinessStatus.CONVERTED, stored(FOLIO_CONVERTED).readinessStatus)

        assertIs<CurpCorrectionResult.AlreadyConverted>(
            PreApplicationViewModel.correctPreApplicationCurp(FOLIO_CONVERTED, CURP_UNIQUE_B)
        )

        assertEquals(beforePreApplications, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(beforeOfficials, PreApplicationViewModel.officialStudents.value)
        assertEquals(beforeAnnuals, MockSaseData.annualEnrollments.value)
        assertEquals(beforeMaster, MockSaseData.students.value)
    }

    @Test
    fun duplicateInAnotherPreApplicationIsRejectedWithoutMutation() {
        val before = PreApplicationViewModel.sharedPreApplications.value
        val curpFromAnotherPreApplication = stored(FOLIO_NEW).alumnoCurp

        val result = PreApplicationViewModel.correctPreApplicationCurp(
            FOLIO_CONFLICT,
            curpFromAnotherPreApplication
        )

        assertTrue(CurpConflictSource.PRE_APPLICATION in sources(result))
        assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun masterDuplicateWithNullRelatedFolioIsRejectedAndExposesStudentId() {
        val master = MockSaseData.students.value.single { it.id == "1" }
        MockSaseData.updateStudent(
            master.copy(
                curp = CURP_UNIQUE_B,
                preApplicationFolio = null
            )
        )
        val before = PreApplicationViewModel.sharedPreApplications.value

        val result = duplicate(
            PreApplicationViewModel.correctPreApplicationCurp(FOLIO_NEW, CURP_UNIQUE_B)
        )
        val masterCandidate = result.conflict.matches.single {
            it.source == CurpConflictSource.MASTER_STUDENT
        }

        assertEquals("1", masterCandidate.masterStudentId)
        assertNull(masterCandidate.relatedFolio)
        assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun duplicatePresentInOfficialStudentsIsRejectedWithoutMutation() {
        val before = PreApplicationViewModel.sharedPreApplications.value
        val officialCurp = PreApplicationViewModel.officialStudents.value.first().curp

        val result = PreApplicationViewModel.correctPreApplicationCurp(FOLIO_NEW, officialCurp)

        assertTrue(CurpConflictSource.OFFICIAL_STUDENT in sources(result))
        assertEquals(before, PreApplicationViewModel.sharedPreApplications.value)
    }

    @Test
    fun correctingPreConflictRemovesConflictPreservesReadyAndEnablesEnrollment() {
        val before = stored(FOLIO_CONFLICT)
        assertEquals(ReadinessStatus.READY, before.readinessStatus)
        assertNotNull(
            PreApplicationViewModel.curpDuplicateInfo(
                before.folio,
                before.alumnoCurp
            )
        )

        val result = assertIs<CurpCorrectionResult.Updated>(
            PreApplicationViewModel.correctPreApplicationCurp(
                FOLIO_CONFLICT,
                "  ${CURP_UNIQUE_B.lowercase()}  "
            )
        )

        val after = stored(FOLIO_CONFLICT)
        assertEquals(CURP_UNIQUE_B, result.normalizedCurp)
        assertEquals(CURP_UNIQUE_B, after.alumnoCurp)
        assertEquals(ReadinessStatus.READY, after.readinessStatus)
        assertNull(
            PreApplicationViewModel.curpDuplicateInfo(after.folio, after.alumnoCurp)
        )
        assertTrue(PreApplicationViewModel.officialEnrollmentPendingItems(after).isEmpty())
        assertTrue(PreApplicationViewModel.isReadyForOfficialEnrollment(after))
    }

    @Test
    fun blockedRecordDoesNotAutoPromoteAndMarkReadyRemainsAuthoritative() {
        val initial = stored(FOLIO_CONFLICT)
        assertEquals(ReadinessStatus.READY, initial.readinessStatus)
        assertNotNull(
            PreApplicationViewModel.curpDuplicateInfo(
                initial.folio,
                initial.alumnoCurp
            )
        )
        assertTrue(PreApplicationViewModel.reopenReview(FOLIO_CONFLICT))
        assertIs<ReadinessResult.NotReady>(
            PreApplicationViewModel.markReadyForOfficialEnrollment(FOLIO_CONFLICT)
        )
        assertEquals(ReadinessStatus.BLOCKED, stored(FOLIO_CONFLICT).readinessStatus)

        assertIs<CurpCorrectionResult.Updated>(
            PreApplicationViewModel.correctPreApplicationCurp(FOLIO_CONFLICT, CURP_UNIQUE_A)
        )
        val corrected = stored(FOLIO_CONFLICT)
        assertNull(
            PreApplicationViewModel.curpDuplicateInfo(
                corrected.folio,
                corrected.alumnoCurp
            )
        )
        assertTrue(PreApplicationViewModel.officialEnrollmentPendingItems(corrected).isEmpty())
        assertEquals(ReadinessStatus.BLOCKED, corrected.readinessStatus)
        assertTrue(corrected.readinessNotes.contains("requiere declaración institucional READY"))
        assertTrue(
            PreApplicationViewModel.isReadyForOfficialEnrollment(corrected),
            "Los requisitos ya están resueltos, pero la declaración READY sigue siendo explícita"
        )

        assertIs<ReadinessResult.Success>(
            PreApplicationViewModel.markReadyForOfficialEnrollment(FOLIO_CONFLICT)
        )
        val ready = stored(FOLIO_CONFLICT)
        assertEquals(ReadinessStatus.READY, ready.readinessStatus)
        assertTrue(PreApplicationViewModel.isReadyForOfficialEnrollment(ready))
    }

    @Test
    fun duplicateConflictPreservesMasterStudentDestinationForNominalFixture() {
        val current = stored(FOLIO_CONFLICT)

        val result = duplicate(
            PreApplicationViewModel.correctPreApplicationCurp(
                current.folio,
                current.alumnoCurp
            )
        )

        assertEquals("1", result.conflict.masterStudentId)
        assertTrue(result.conflict.isNavigable)
        assertEquals(current, stored(FOLIO_CONFLICT))
    }

    @Test
    fun reEnrollmentKeepsItsExistingInstitutionalCurpSemanticsAndRejectsDirectCorrection() {
        val reEnrollment = stored(FOLIO_REENROLLMENT)
        val beforePreApplications = PreApplicationViewModel.sharedPreApplications.value
        val beforeOfficials = PreApplicationViewModel.officialStudents.value
        val beforeAnnuals = MockSaseData.annualEnrollments.value
        val beforeMaster = MockSaseData.students.value

        assertNull(
            PreApplicationViewModel.curpDuplicateInfo(
                reEnrollment.folio,
                reEnrollment.alumnoCurp
            )
        )
        assertIs<CurpCorrectionResult.InstitutionalIdentityLocked>(
            PreApplicationViewModel.correctPreApplicationCurp(
                reEnrollment.folio,
                CURP_UNIQUE_A
            )
        )
        assertEquals(beforePreApplications, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(beforeOfficials, PreApplicationViewModel.officialStudents.value)
        assertEquals(beforeAnnuals, MockSaseData.annualEnrollments.value)
        assertEquals(beforeMaster, MockSaseData.students.value)

        val correction = assertIs<CorrectionRequestResult.Success>(
            PreApplicationViewModel.requestCorrection(
                reEnrollment.folio,
                "Actualizar datos familiares sin cambiar identidad"
            )
        )
        val beforeResubmission = PreApplicationViewModel.sharedPreApplications.value
        assertIs<FamilyResubmissionResult.InstitutionalIdentityLocked>(
            PreApplicationViewModel.resubmitCorrectedPreApplication(
                correction.preApplication.copy(alumnoCurp = CURP_UNIQUE_B)
            )
        )
        assertEquals(beforeResubmission, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(beforeOfficials, PreApplicationViewModel.officialStudents.value)
        assertEquals(beforeAnnuals, MockSaseData.annualEnrollments.value)
        assertEquals(beforeMaster, MockSaseData.students.value)
    }

    @Test
    fun resetRestoresExactInitialStateAndRemovesTestOnlyCurps() {
        val initialPreApplications = PreApplicationViewModel.sharedPreApplications.value
        val initialOfficials = PreApplicationViewModel.officialStudents.value
        val initialMaster = MockSaseData.students.value

        assertIs<CurpCorrectionResult.Updated>(
            PreApplicationViewModel.correctPreApplicationCurp(FOLIO_NEW, CURP_UNIQUE_A)
        )
        MockSaseData.updateStudent(
            MockSaseData.students.value.first().copy(
                curp = CURP_UNIQUE_B,
                preApplicationFolio = null
            )
        )

        PreApplicationViewModel.resetSharedStateForTests()

        assertEquals(initialPreApplications, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(initialOfficials, PreApplicationViewModel.officialStudents.value)
        assertEquals(initialMaster, MockSaseData.students.value)
        assertFalse(
            PreApplicationViewModel.sharedPreApplications.value.any {
                it.alumnoCurp == CURP_UNIQUE_A || it.alumnoCurp == CURP_UNIQUE_B
            }
        )
    }
}
