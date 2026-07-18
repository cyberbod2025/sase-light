package com.example.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurpCorrectionPolicyTest {

    private companion object {
        const val CURRENT_FOLIO = "PRE-CURRENT-001"
        const val OTHER_FOLIO = "PRE-OTHER-001"
        const val CURP_UNIQUE_A = "NEWD020202HDFXYZ88"
        const val CURP_UNIQUE_B = "NEWE030303MDFQRS77"
        const val MASTER_STUDENT_ID = "student-fixture-1"
    }

    @Test
    fun normalizeTrimsAndUppercases() {
        assertEquals(
            CURP_UNIQUE_A,
            CurpCorrectionPolicy.normalize("  newd020202hdfxyz88  "),
        )
    }

    @Test
    fun seventeenCharactersAreRejected() {
        assertTrue(CurpCorrectionPolicy.validate(CURP_UNIQUE_A.dropLast(1)) is CurpValidationResult.Invalid)
    }

    @Test
    fun nineteenCharactersAreRejected() {
        assertTrue(CurpCorrectionPolicy.validate("${CURP_UNIQUE_A}9") is CurpValidationResult.Invalid)
    }

    @Test
    fun blankValueIsRejected() {
        assertTrue(CurpCorrectionPolicy.validate("   ") is CurpValidationResult.Invalid)
    }

    @Test
    fun invalidEighteenCharacterStructureIsRejected() {
        assertEquals(18, "NEWD020202XDFXYZ88".length)
        assertEquals(
            CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE,
            (CurpCorrectionPolicy.validate("NEWD020202XDFXYZ88") as CurpValidationResult.Invalid).message,
        )
    }

    @Test
    fun demoRegexAcceptsCompatibleSyntheticValues() {
        assertEquals(
            CurpValidationResult.Valid(CURP_UNIQUE_A),
            CurpCorrectionPolicy.validate(CURP_UNIQUE_A),
        )
        assertEquals(
            CurpValidationResult.Valid(CURP_UNIQUE_B),
            CurpCorrectionPolicy.validate("  newe030303mdfqrs77  "),
        )
    }

    @Test
    fun duplicateFromAnotherPreApplicationIsDetected() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.PRE_APPLICATION,
                curp = CURP_UNIQUE_A,
                relatedFolio = OTHER_FOLIO,
            ),
        )

        assertEquals(CurpConflictSource.PRE_APPLICATION, conflict.matches.single().source)
    }

    @Test
    fun duplicateFromMasterStudentIsDetected() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.MASTER_STUDENT,
                curp = CURP_UNIQUE_A,
                relatedFolio = OTHER_FOLIO,
                masterStudentId = MASTER_STUDENT_ID,
            ),
        )

        assertEquals(CurpConflictSource.MASTER_STUDENT, conflict.matches.single().source)
    }

    @Test
    fun duplicateFromOfficialStudentIsDetected() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.OFFICIAL_STUDENT,
                curp = CURP_UNIQUE_A,
                relatedFolio = OTHER_FOLIO,
            ),
        )

        assertEquals(CurpConflictSource.OFFICIAL_STUDENT, conflict.matches.single().source)
    }

    @Test
    fun normalizedCurrentFolioIsTheOnlyRelatedRecordExcluded() {
        val conflict = CurpCorrectionPolicy.resolveConflict(
            currentFolio = "  pre-current-001  ",
            rawCurp = CURP_UNIQUE_A,
            candidates = listOf(
                CurpConflictCandidate(
                    source = CurpConflictSource.PRE_APPLICATION,
                    curp = CURP_UNIQUE_A,
                    relatedFolio = " pre-current-001 ",
                ),
            ),
        )

        assertNull(conflict)
    }

    @Test
    fun nullRelatedFolioRemainsConflictive() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.MASTER_STUDENT,
                curp = CURP_UNIQUE_A,
                relatedFolio = null,
                masterStudentId = MASTER_STUDENT_ID,
            ),
        )

        assertEquals(1, conflict.matches.size)
    }

    @Test
    fun masterStudentConflictIsNavigable() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.MASTER_STUDENT,
                curp = CURP_UNIQUE_A,
                relatedFolio = OTHER_FOLIO,
                masterStudentId = "  $MASTER_STUDENT_ID  ",
            ),
        )

        assertEquals(MASTER_STUDENT_ID, conflict.masterStudentId)
        assertTrue(conflict.isNavigable)
        assertFalse(conflict.institutionalMessage.contains(CURP_UNIQUE_A))
        assertFalse(conflict.institutionalMessage.contains(MASTER_STUDENT_ID))
        assertTrue(conflict.institutionalMessage.contains("padrón maestro"))
    }

    @Test
    fun conflictWithoutMasterStudentIdIsNotNavigable() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.OFFICIAL_STUDENT,
                curp = CURP_UNIQUE_A,
                relatedFolio = OTHER_FOLIO,
            ),
        )

        assertNull(conflict.masterStudentId)
        assertFalse(conflict.isNavigable)
        assertTrue(conflict.institutionalMessage.contains("alta oficial"))
    }

    @Test
    fun allSourcesAreAggregatedAndMasterIdIsPreservedRegardlessOfOrder() {
        val conflict = assertNotNull(
            CurpCorrectionPolicy.resolveConflict(
                currentFolio = CURRENT_FOLIO,
                rawCurp = "  newd020202hdfxyz88  ",
                candidates = listOf(
                    CurpConflictCandidate(
                        source = CurpConflictSource.OFFICIAL_STUDENT,
                        curp = CURP_UNIQUE_A,
                        relatedFolio = "PRE-OFFICIAL-001",
                    ),
                    CurpConflictCandidate(
                        source = CurpConflictSource.PRE_APPLICATION,
                        curp = CURP_UNIQUE_A,
                        relatedFolio = OTHER_FOLIO,
                    ),
                    CurpConflictCandidate(
                        source = CurpConflictSource.MASTER_STUDENT,
                        curp = CURP_UNIQUE_A,
                        relatedFolio = "PRE-MASTER-001",
                        masterStudentId = MASTER_STUDENT_ID,
                    ),
                    CurpConflictCandidate(
                        source = CurpConflictSource.PRE_APPLICATION,
                        curp = CURP_UNIQUE_B,
                        relatedFolio = "PRE-UNRELATED-001",
                    ),
                ),
            ),
        )

        assertEquals(CURP_UNIQUE_A, conflict.normalizedCurp)
        assertEquals(
            listOf(
                CurpConflictSource.OFFICIAL_STUDENT,
                CurpConflictSource.PRE_APPLICATION,
                CurpConflictSource.MASTER_STUDENT,
            ),
            conflict.matches.map { it.source },
        )
        assertEquals(MASTER_STUDENT_ID, conflict.masterStudentId)
        assertTrue(conflict.isNavigable)
    }

    @Test
    fun correctionResultsExposeOnlyTheirTypedPayloads() {
        val conflict = resolveSingle(
            CurpConflictCandidate(
                source = CurpConflictSource.PRE_APPLICATION,
                curp = CURP_UNIQUE_A,
                relatedFolio = OTHER_FOLIO,
            ),
        )
        val updated = CurpCorrectionResult.Updated(CURP_UNIQUE_A)
        val invalid = CurpCorrectionResult.InvalidFormat(CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE)
        val duplicate = CurpCorrectionResult.Duplicate(conflict)
        val notFound: CurpCorrectionResult = CurpCorrectionResult.NotFound
        val alreadyConverted: CurpCorrectionResult = CurpCorrectionResult.AlreadyConverted
        val identityLocked: CurpCorrectionResult = CurpCorrectionResult.InstitutionalIdentityLocked

        assertEquals(CURP_UNIQUE_A, updated.normalizedCurp)
        assertEquals(CurpCorrectionPolicy.INVALID_FORMAT_MESSAGE, invalid.message)
        assertEquals(conflict, duplicate.conflict)
        assertEquals(CurpCorrectionResult.NotFound, notFound)
        assertEquals(CurpCorrectionResult.AlreadyConverted, alreadyConverted)
        assertEquals(CurpCorrectionResult.InstitutionalIdentityLocked, identityLocked)
    }

    private fun resolveSingle(candidate: CurpConflictCandidate): CurpConflict =
        assertNotNull(
            CurpCorrectionPolicy.resolveConflict(
                currentFolio = CURRENT_FOLIO,
                rawCurp = CURP_UNIQUE_A,
                candidates = listOf(candidate),
            ),
        )
}
