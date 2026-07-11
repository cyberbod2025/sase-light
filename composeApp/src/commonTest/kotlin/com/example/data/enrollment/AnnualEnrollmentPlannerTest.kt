package com.example.data.enrollment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AnnualEnrollmentPlannerTest {
    private val schoolYear = "2026-2027"
    private val curp = "STUD100101HDFABC01"
    private val folio = "PRE-2026-001"

    @Test
    fun newEntryProducesPlanWithoutStudentId() {
        val draft = ready(plan())

        assertNull(draft.studentId)
        assertEquals(AnnualEnrollmentMovement.NEW_ENTRY, draft.movement)
    }

    @Test
    fun newEntryUsesExactProposedEnrollmentId() {
        val draft = ready(plan(enrollmentIdAllocation = newProposal("S310-000777-4")))

        assertEquals("S310-000777-4", draft.permanentEnrollmentId)
    }

    @Test
    fun newEntryStartsPendingGroupAssignment() {
        val draft = ready(plan())

        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT, draft.initialStatus)
        assertEquals(GroupPlacementRequirement.AssignmentRequired, draft.groupPlacementRequirement)
    }

    @Test
    fun reEnrollmentPreservesStudentAndEnrollmentId() {
        val draft = ready(
            plan(
                classification = reEnrollment(),
                enrollmentIdAllocation = preserved()
            )
        )

        assertEquals("MASTER-1", draft.studentId)
        assertEquals("S310-000123-4", draft.permanentEnrollmentId)
        assertEquals(AnnualEnrollmentMovement.RE_ENROLLMENT, draft.movement)
    }

    @Test
    fun reEnrollmentSuggestsContinuityFrom1BTo2B() {
        val draft = ready(
            plan(
                classification = reEnrollment(),
                enrollmentIdAllocation = preserved(),
                requestedGrade = 2,
                previousGroup = "1B"
            )
        )

        assertEquals(
            GroupPlacementRequirement.ContinuityDecisionRequired("1B", "2B"),
            draft.groupPlacementRequirement
        )
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION, draft.initialStatus)
    }

    @Test
    fun reEnrollmentNormalizesPreviousGroupSpacesAndCase() {
        val draft = ready(
            plan(
                classification = reEnrollment(),
                enrollmentIdAllocation = preserved(),
                requestedGrade = 2,
                previousGroup = " 1 b "
            )
        )

        assertEquals(
            GroupPlacementRequirement.ContinuityDecisionRequired("1B", "2B"),
            draft.groupPlacementRequirement
        )
    }

    @Test
    fun invalidPreviousGroupRequiresNewAssignment() {
        val draft = ready(
            plan(
                classification = reEnrollment(),
                enrollmentIdAllocation = preserved(),
                previousGroup = "4Z"
            )
        )

        assertEquals(GroupPlacementRequirement.AssignmentRequired, draft.groupPlacementRequirement)
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT, draft.initialStatus)
    }

    @Test
    fun initialMigrationPreservesStudentAndUsesProposal() {
        val draft = ready(
            plan(
                classification = initialMigration(),
                enrollmentIdAllocation = migrationProposal()
            )
        )

        assertEquals("MASTER-1", draft.studentId)
        assertEquals("S310-000124-1", draft.permanentEnrollmentId)
        assertEquals(AnnualEnrollmentMovement.INITIAL_MIGRATION, draft.movement)
    }

    @Test
    fun initialMigrationSuggestsContinuityFrom2DTo3D() {
        val draft = ready(
            plan(
                classification = initialMigration(),
                enrollmentIdAllocation = migrationProposal(),
                requestedGrade = 3,
                previousGroup = "2D"
            )
        )

        assertEquals(
            GroupPlacementRequirement.ContinuityDecisionRequired("2D", "3D"),
            draft.groupPlacementRequirement
        )
    }

    @Test
    fun classificationConflictDoesNotProducePlan() {
        val result = plan(
            classification = SchoolMovementClassification.Conflict(
                SchoolMovementConflictReason.DUPLICATE_MASTER_STUDENTS,
                "CURP duplicada"
            )
        )

        assertConflict(AnnualEnrollmentConflictCause.CLASSIFICATION_CONFLICT, result)
    }

    @Test
    fun allocationConflictDoesNotProducePlan() {
        val result = plan(
            enrollmentIdAllocation = PermanentEnrollmentIdAllocation.Conflict.Integrity(
                PermanentEnrollmentIdAllocation.ConflictCause.INVALID_S310_FORMAT,
                "Matrícula inválida"
            )
        )

        assertConflict(AnnualEnrollmentConflictCause.ALLOCATION_CONFLICT, result)
    }

    @Test
    fun exhaustedAllocationDoesNotProducePlan() {
        val result = plan(
            enrollmentIdAllocation = PermanentEnrollmentIdAllocation.Conflict.Exhausted()
        )

        assertConflict(AnnualEnrollmentConflictCause.ALLOCATION_EXHAUSTED, result)
    }

    @Test
    fun incompatibleClassificationAndAllocationProduceConflict() {
        val result = plan(
            classification = reEnrollment(),
            enrollmentIdAllocation = newProposal()
        )

        assertConflict(AnnualEnrollmentConflictCause.INCOMPATIBLE_CLASSIFICATION_AND_ALLOCATION, result)
    }

    @Test
    fun existingStudentInSameSchoolYearProducesConflict() {
        val result = plan(
            classification = reEnrollment(),
            enrollmentIdAllocation = preserved(),
            existingAnnualEnrollments = listOf(snapshot())
        )

        assertConflict(AnnualEnrollmentConflictCause.DUPLICATE_STUDENT_IN_SCHOOL_YEAR, result)
    }

    @Test
    fun existingCurpInSameSchoolYearProducesConflict() {
        val result = plan(
            existingAnnualEnrollments = listOf(
                snapshot(
                    studentId = "MASTER-OTHER",
                    permanentEnrollmentId = "S310-000999-3"
                )
            )
        )

        assertConflict(AnnualEnrollmentConflictCause.DUPLICATE_CURP_IN_SCHOOL_YEAR, result)
    }

    @Test
    fun sameStudentInDifferentSchoolYearCanProducePlan() {
        val result = plan(
            classification = reEnrollment(),
            enrollmentIdAllocation = preserved(),
            existingAnnualEnrollments = listOf(
                snapshot(
                    schoolYear = "2025-2026",
                    sourcePreApplicationFolio = "PRE-2025-001"
                )
            )
        )

        assertIs<AnnualEnrollmentPlanningResult.Ready>(result)
    }

    @Test
    fun reusedFolioProducesConflictAcrossSchoolYears() {
        val result = plan(
            existingAnnualEnrollments = listOf(
                snapshot(
                    studentId = "MASTER-OTHER",
                    normalizedCurp = "OTHR100101HDFABC01",
                    permanentEnrollmentId = "S310-000999-3",
                    schoolYear = "2025-2026",
                    sourcePreApplicationFolio = folio
                )
            )
        )

        assertConflict(AnnualEnrollmentConflictCause.REUSED_PRE_APPLICATION_FOLIO, result)
    }

    @Test
    fun enrollmentIdUsedByDifferentIdentityInSameYearProducesConflict() {
        val result = plan(
            classification = reEnrollment(),
            enrollmentIdAllocation = preserved(),
            existingAnnualEnrollments = listOf(
                snapshot(
                    studentId = "MASTER-OTHER",
                    normalizedCurp = "OTHR100101HDFABC01",
                    sourcePreApplicationFolio = "PRE-2026-OTHER"
                )
            )
        )

        assertConflict(AnnualEnrollmentConflictCause.ENROLLMENT_ID_USED_BY_DIFFERENT_IDENTITY, result)
    }

    @Test
    fun invalidSchoolYearsProduceConflict() {
        listOf("", "2026", "2026-2028", "26-27").forEach { invalidSchoolYear ->
            assertConflict(
                AnnualEnrollmentConflictCause.INVALID_SCHOOL_YEAR,
                plan(schoolYear = invalidSchoolYear)
            )
        }
    }

    @Test
    fun gradesOutsideOneToThreeProduceConflict() {
        listOf(0, 4).forEach { invalidGrade ->
            assertConflict(
                AnnualEnrollmentConflictCause.INVALID_REQUESTED_GRADE,
                plan(requestedGrade = invalidGrade)
            )
        }
    }

    @Test
    fun planningIsIdempotent() {
        val first = plan(previousGroup = "unexpected")
        val second = plan(previousGroup = "unexpected")

        assertEquals(first, second)
    }

    @Test
    fun planningDoesNotModifyInputsOrCollections() {
        val classification = reEnrollment()
        val allocation = preserved()
        val snapshots = mutableListOf(
            snapshot(
                schoolYear = "2025-2026",
                sourcePreApplicationFolio = "PRE-2025-001"
            )
        )
        val snapshotsBefore = snapshots.toList()

        plan(
            classification = classification,
            enrollmentIdAllocation = allocation,
            existingAnnualEnrollments = snapshots
        )

        assertEquals(reEnrollment(), classification)
        assertEquals(preserved(), allocation)
        assertEquals(snapshotsBefore, snapshots)
    }

    @Test
    fun continuityRequirementNeverAssignsGroupAutomatically() {
        val draft = ready(
            plan(
                classification = reEnrollment(),
                enrollmentIdAllocation = preserved(),
                requestedGrade = 2,
                previousGroup = "1A"
            )
        )

        assertIs<GroupPlacementRequirement.ContinuityDecisionRequired>(draft.groupPlacementRequirement)
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION, draft.initialStatus)
    }

    @Test
    fun accidentalPreviousGroupIsIgnoredForNewEntry() {
        val draft = ready(plan(previousGroup = "1C", requestedGrade = 2))

        assertEquals(GroupPlacementRequirement.AssignmentRequired, draft.groupPlacementRequirement)
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT, draft.initialStatus)
    }

    @Test
    fun reEnrollmentWithDifferentStudentIdsProducesConflict() {
        val result = plan(
            classification = reEnrollment(),
            enrollmentIdAllocation = preserved(studentId = "MASTER-OTHER")
        )

        assertConflict(AnnualEnrollmentConflictCause.STUDENT_ID_MISMATCH, result)
    }

    @Test
    fun reEnrollmentWithDifferentEnrollmentIdsProducesConflict() {
        val result = plan(
            classification = reEnrollment(),
            enrollmentIdAllocation = preserved(enrollmentId = "S310-000999-3")
        )

        assertConflict(AnnualEnrollmentConflictCause.ENROLLMENT_ID_MISMATCH, result)
    }

    @Test
    fun newEntryWithExistingStudentAllocationProducesConflict() {
        val result = plan(enrollmentIdAllocation = preserved())

        assertConflict(AnnualEnrollmentConflictCause.NEW_ENTRY_WITH_EXISTING_STUDENT_ID, result)
    }

    @Test
    fun initialMigrationWithoutStudentIdProducesConflict() {
        val result = plan(
            classification = initialMigration(studentId = ""),
            enrollmentIdAllocation = migrationProposal()
        )

        assertConflict(AnnualEnrollmentConflictCause.INITIAL_MIGRATION_WITHOUT_STUDENT_ID, result)
    }

    @Test
    fun emptyCurpProducesConflict() {
        val result = plan(classification = newEntry(normalizedCurp = " "))

        assertConflict(AnnualEnrollmentConflictCause.EMPTY_CURP, result)
    }

    @Test
    fun emptyFolioProducesConflict() {
        val result = plan(sourcePreApplicationFolio = " ")

        assertConflict(AnnualEnrollmentConflictCause.EMPTY_PRE_APPLICATION_FOLIO, result)
    }

    @Test
    fun emptyEnrollmentIdProducesConflict() {
        val result = plan(enrollmentIdAllocation = newProposal(enrollmentId = " "))

        assertConflict(AnnualEnrollmentConflictCause.EMPTY_PERMANENT_ENROLLMENT_ID, result)
    }

    @Test
    fun mismatchedSchoolYearProducesConflict() {
        val result = plan(classification = newEntry(activeSchoolYear = "2025-2026"))

        assertConflict(AnnualEnrollmentConflictCause.INCOMPATIBLE_SCHOOL_YEAR, result)
    }

    @Test
    fun wrongProposalReasonProducesConflict() {
        val result = plan(enrollmentIdAllocation = newProposal(reason = "MIGRACION_INICIAL"))

        assertConflict(AnnualEnrollmentConflictCause.INCOMPATIBLE_CLASSIFICATION_AND_ALLOCATION, result)
    }

    private fun plan(
        classification: SchoolMovementClassification = newEntry(),
        enrollmentIdAllocation: PermanentEnrollmentIdAllocation = newProposal(),
        schoolYear: String = this.schoolYear,
        sourcePreApplicationFolio: String = folio,
        requestedGrade: Int = 1,
        previousGroup: String? = null,
        existingAnnualEnrollments: List<AnnualEnrollmentSnapshot> = emptyList()
    ): AnnualEnrollmentPlanningResult = AnnualEnrollmentPlanner.planAnnualEnrollment(
        classification = classification,
        enrollmentIdAllocation = enrollmentIdAllocation,
        schoolYear = schoolYear,
        sourcePreApplicationFolio = sourcePreApplicationFolio,
        requestedGrade = requestedGrade,
        previousGroup = previousGroup,
        existingAnnualEnrollments = existingAnnualEnrollments
    )

    private fun newEntry(
        normalizedCurp: String = curp,
        activeSchoolYear: String = schoolYear
    ) = SchoolMovementClassification.NewEntry(normalizedCurp, activeSchoolYear)

    private fun reEnrollment(
        studentId: String = "MASTER-1",
        normalizedCurp: String = curp,
        enrollmentId: String = "S310-000123-4",
        activeSchoolYear: String = schoolYear
    ) = SchoolMovementClassification.ReEnrollment(
        studentId,
        normalizedCurp,
        enrollmentId,
        activeSchoolYear
    )

    private fun initialMigration(
        studentId: String = "MASTER-1",
        normalizedCurp: String = curp,
        activeSchoolYear: String = schoolYear
    ) = SchoolMovementClassification.InitialMigration(studentId, normalizedCurp, activeSchoolYear)

    private fun newProposal(
        enrollmentId: String = "S310-000124-1",
        cycle: String = schoolYear,
        reason: String = "NUEVO_INGRESO"
    ) = PermanentEnrollmentIdAllocation.NewProposal(
        enrollmentId = enrollmentId,
        consecutive = 124,
        checkDigit = 1,
        cycle = cycle,
        reason = reason
    )

    private fun migrationProposal() = newProposal(reason = "MIGRACION_INICIAL")

    private fun preserved(
        studentId: String = "MASTER-1",
        enrollmentId: String = "S310-000123-4",
        cycle: String = schoolYear
    ) = PermanentEnrollmentIdAllocation.ExistingPreserved(
        studentId = studentId,
        enrollmentId = enrollmentId,
        cycle = cycle
    )

    private fun snapshot(
        studentId: String = "MASTER-1",
        normalizedCurp: String = curp,
        permanentEnrollmentId: String = "S310-000123-4",
        schoolYear: String = this.schoolYear,
        sourcePreApplicationFolio: String = "PRE-2026-OLD"
    ) = AnnualEnrollmentSnapshot(
        studentId = studentId,
        normalizedCurp = normalizedCurp,
        permanentEnrollmentId = permanentEnrollmentId,
        schoolYear = schoolYear,
        requestedGrade = 1,
        assignedGroup = null,
        sourcePreApplicationFolio = sourcePreApplicationFolio
    )

    private fun ready(result: AnnualEnrollmentPlanningResult): AnnualEnrollmentDraft =
        assertIs<AnnualEnrollmentPlanningResult.Ready>(result).draft

    private fun assertConflict(
        expectedCause: AnnualEnrollmentConflictCause,
        result: AnnualEnrollmentPlanningResult
    ) {
        assertEquals(expectedCause, assertIs<AnnualEnrollmentPlanningResult.Conflict>(result).cause)
    }
}
