package com.example.data.enrollment

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.enrollment.SchoolMovementClassification
import com.example.viewmodel.PreApplicationViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermanentEnrollmentIdAllocatorTest {
    private val activeSchoolYear = "2026-2027"

    @Test
    fun reEnrollmentPreservesExistingEnrollmentId() {
        val existing = student(enrollmentId = "S310-000321-9")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing),
            activeSchoolYear = activeSchoolYear
        )

        val preserved = assertIs<PermanentEnrollmentIdAllocation.ExistingPreserved>(result)
        assertEquals(existing.enrollmentId, preserved.enrollmentId)
        assertEquals(existing.id, preserved.studentId)
        assertEquals(activeSchoolYear, preserved.cycle)
        assertEquals("REINSCRIPCION", preserved.reason)
    }

    @Test
    fun reEnrollmentWithLegacyNonS310EnrollmentIdPreservesIt() {
        val existing = student(enrollmentId = "LEGACY-001")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing),
            activeSchoolYear = activeSchoolYear
        )

        val preserved = assertIs<PermanentEnrollmentIdAllocation.ExistingPreserved>(result)
        assertEquals("LEGACY-001", preserved.enrollmentId)
    }

    @Test
    fun newEntryWithNoPreviousProposesS310000001() {
        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.NewEntry(
                normalizedCurp = "NEWC100101HDFABC01",
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = emptyList(),
            activeSchoolYear = activeSchoolYear
        )

        val proposal = assertIs<PermanentEnrollmentIdAllocation.NewProposal>(result)
        assertEquals("S310-000001-1", proposal.enrollmentId)
        assertEquals(1, proposal.consecutive)
        assertEquals(1, proposal.checkDigit)
        assertEquals(activeSchoolYear, proposal.cycle)
        assertEquals("NUEVO_INGRESO", proposal.reason)
    }

    @Test
    fun initialMigrationWithNoPreviousProposesS310000001() {
        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.InitialMigration(
                studentId = "MASTER-1",
                normalizedCurp = "MIGR100101HDFABC01",
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = emptyList(),
            activeSchoolYear = activeSchoolYear
        )

        val proposal = assertIs<PermanentEnrollmentIdAllocation.NewProposal>(result)
        assertEquals("S310-000001-1", proposal.enrollmentId)
        assertEquals("MIGRACION_INICIAL", proposal.reason)
    }

    @Test
    fun withExistingValidProposesMaxPlusOne() {
        val existing1 = student(enrollmentId = "S310-000010-2")
        val existing2 = student(enrollmentId = "S310-000005-8")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.NewEntry(
                normalizedCurp = "NEWC100101HDFABC01",
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing1, existing2),
            activeSchoolYear = activeSchoolYear
        )

        val proposal = assertIs<PermanentEnrollmentIdAllocation.NewProposal>(result)
        assertEquals(11, proposal.consecutive)
        assertEquals("S310-000011-7", proposal.enrollmentId)
    }

    @Test
    fun gapsAreNotReused() {
        val existing1 = student(enrollmentId = "S310-000001-1")
        val existing2 = student(enrollmentId = "S310-000003-2")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.NewEntry(
                normalizedCurp = "NEWC100101HDFABC01",
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing1, existing2),
            activeSchoolYear = activeSchoolYear
        )

        val proposal = assertIs<PermanentEnrollmentIdAllocation.NewProposal>(result)
        assertEquals(4, proposal.consecutive)
    }

    @Test
    fun luhnCheckDigitFor000001Is1() {
        assertEquals(1, PermanentEnrollmentIdAllocator.computeLuhnCheckDigit(1))
    }

    @Test
    fun luhnCheckDigitFor000002Is9() {
        assertEquals(9, PermanentEnrollmentIdAllocator.computeLuhnCheckDigit(2))
    }

    @Test
    fun luhnCheckDigitFor000010Is2() {
        assertEquals(2, PermanentEnrollmentIdAllocator.computeLuhnCheckDigit(10))
    }

    @Test
    fun invalidLuhnCheckDigitProducesConflict() {
        val existing = student(enrollmentId = "S310-000001-9")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing),
            activeSchoolYear = activeSchoolYear
        )

        val conflict = assertIs<PermanentEnrollmentIdAllocation.Conflict.Integrity>(result)
        assertEquals(PermanentEnrollmentIdAllocation.ConflictCause.INVALID_LUHN_CHECK_DIGIT, conflict.cause)
    }

    @Test
    fun malformedS310EnrollmentIdProducesConflict() {
        val existing = student(enrollmentId = "S310-INVALID-FORMAT")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing),
            activeSchoolYear = activeSchoolYear
        )

        val conflict = assertIs<PermanentEnrollmentIdAllocation.Conflict.Integrity>(result)
        assertEquals(PermanentEnrollmentIdAllocation.ConflictCause.INVALID_S310_FORMAT, conflict.cause)
    }

    @Test
    fun duplicateSaseEnrollmentIdProducesConflict() {
        val existing1 = student(id = "MASTER-1", enrollmentId = "S310-000010-2")
        val existing2 = student(id = "MASTER-2", enrollmentId = "S310-000010-2")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing1.id,
                normalizedCurp = existing1.curp,
                existingEnrollmentId = existing1.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing1, existing2),
            activeSchoolYear = activeSchoolYear
        )

        val conflict = assertIs<PermanentEnrollmentIdAllocation.Conflict.Integrity>(result)
        assertEquals(PermanentEnrollmentIdAllocation.ConflictCause.DUPLICATE_SASE_ENROLLMENT_ID, conflict.cause)
    }

    @Test
    fun classificationConflictDoesNotGenerateProposal() {
        val conflictClassification = SchoolMovementClassification.Conflict(
            reason = SchoolMovementConflictReason.NEW_ENTRY_ALREADY_EXISTS,
            message = "La CURP ya existe"
        )

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = conflictClassification,
            masterStudents = emptyList(),
            activeSchoolYear = activeSchoolYear
        )

        assertIs<PermanentEnrollmentIdAllocation.Conflict.ConflictResult>(result)
    }

    @Test
    fun reEnrollmentWithoutEnrollmentIdProducesConflict() {
        val existing = student(enrollmentId = "")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = "",
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing),
            activeSchoolYear = activeSchoolYear
        )

        val conflict = assertIs<PermanentEnrollmentIdAllocation.Conflict.ConflictResult>(result)
        assertEquals(PermanentEnrollmentIdAllocation.ConflictCause.RE_ENROLLMENT_WITHOUT_ENROLLMENT_ID, conflict.cause)
    }

    @Test
    fun consecutive999999ProducesExhausted() {
        val maxStudent = student(enrollmentId = "S310-999999-7")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.NewEntry(
                normalizedCurp = "NEWC100101HDFABC01",
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(maxStudent),
            activeSchoolYear = activeSchoolYear
        )

        assertIs<PermanentEnrollmentIdAllocation.Conflict.Exhausted>(result)
    }

    @Test
    fun idempotentAndReadOnly() {
        val masterStudents = MockSaseData.students.value
        val existing = masterStudents.first { it.enrollmentId.isNotBlank() }
        val masterBefore = masterStudents.toList()
        val preAppsBefore = PreApplicationViewModel.sharedPreApplications.value.toList()
        val officialBefore = PreApplicationViewModel.officialStudents.value.toList()

        val first = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = masterStudents,
            activeSchoolYear = activeSchoolYear
        )

        val second = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = masterStudents,
            activeSchoolYear = activeSchoolYear
        )

        assertEquals(first, second)
        assertEquals(masterBefore, MockSaseData.students.value)
        assertEquals(preAppsBefore, PreApplicationViewModel.sharedPreApplications.value)
        assertEquals(officialBefore, PreApplicationViewModel.officialStudents.value)
        assertEquals(existing.enrollmentId, MockSaseData.students.value.first { it.id == existing.id }.enrollmentId)
    }

    @Test
    fun reEnrollmentWithInvalidS310FormatProducesConflict() {
        val existing = student(enrollmentId = "S310-000001-9")

        val result = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = SchoolMovementClassification.ReEnrollment(
                studentId = existing.id,
                normalizedCurp = existing.curp,
                existingEnrollmentId = existing.enrollmentId,
                activeSchoolYear = activeSchoolYear
            ),
            masterStudents = listOf(existing),
            activeSchoolYear = activeSchoolYear
        )

        val conflict = assertIs<PermanentEnrollmentIdAllocation.Conflict.Integrity>(result)
        assertEquals(PermanentEnrollmentIdAllocation.ConflictCause.INVALID_LUHN_CHECK_DIGIT, conflict.cause)
    }

    private fun student(
        id: String = "MASTER-1",
        curp: String = "STUD100101HDFABC01",
        enrollmentId: String = "S310-000123-4"
    ): Student = Student(
        id = id,
        fullName = "Alumno Maestro",
        group = "1A",
        enrollmentId = enrollmentId,
        curp = curp
    )
}