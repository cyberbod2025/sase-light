package com.example.data.enrollment

import com.example.data.MockSaseData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AnnualEnrollmentPersistenceAdapterTest {
    private val schoolYear = "2026-2027"

    @Test
    fun newEntryCreatesIdentityAndEnrollment() {
        MockSaseData.resetForTests()
        val curp = "ENTR100101HDFABC99"
        val classification = SchoolMovementClassification.NewEntry(curp, schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000777-0", consecutive = 777, checkDigit = 0,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-NEW-001", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-ENTRY", "ALUMNO NUEVO", "Secretaría", "HOY 12:00"
        )
        val applied = assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertEquals(MasterStudentMutation.CREATED, applied.masterStudentMutation)
        assertTrue(MockSaseData.students.value.any { it.id == "MASTER-ENTRY" })
        assertTrue(MockSaseData.students.value.any { it.curp == curp })
        assertTrue(MockSaseData.students.value.any { it.enrollmentId == "S310-000777-0" })
        assertTrue(MockSaseData.annualEnrollments.value.any { it.studentId == "MASTER-ENTRY" })
    }

    @Test
    fun newEntryDoesNotAssignGroup() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.NewEntry("ENTR200101HDFABC98", schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000778-8", consecutive = 778, checkDigit = 8,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-NEW-002", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-ENTRY-2", "ALUMNO SIN GRUPO", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        val student = MockSaseData.students.value.first { it.id == "MASTER-ENTRY-2" }
        assertEquals("", student.group)
    }

    @Test
    fun newEntryUses6CEnrollmentIdFormat() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.NewEntry("ENTR300101HDFABC97", schoolYear)
        val enrollmentId = "S310-000779-5"
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = enrollmentId, consecutive = 779, checkDigit = 5,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-NEW-003", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-ENTRY-3", "ALUMNO 6C", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertTrue(MockSaseData.students.value.any { it.enrollmentId == enrollmentId })
    }

    @Test
    fun reEnrollmentReusesExistingStudent() {
        MockSaseData.resetForTests()
        val existingCount = MockSaseData.students.value.size
        val classification = SchoolMovementClassification.ReEnrollment(
            studentId = "1", normalizedCurp = "DEMA100101HDFABC01",
            existingEnrollmentId = "S310-DEMA100101-26", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.ExistingPreserved(
            studentId = "1", enrollmentId = "S310-DEMA100101-26", cycle = schoolYear
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-REN-001", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO DEMO 01", "Secretaría", "HOY 12:00"
        )
        val applied = assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertEquals(MasterStudentMutation.REUSED, applied.masterStudentMutation)
        assertEquals(existingCount, MockSaseData.students.value.size)
    }

    @Test
    fun reEnrollmentPreservesEnrollmentId() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.ReEnrollment(
            studentId = "1", normalizedCurp = "DEMA100101HDFABC01",
            existingEnrollmentId = "S310-DEMA100101-26", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.ExistingPreserved(
            studentId = "1", enrollmentId = "S310-DEMA100101-26", cycle = schoolYear
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-REN-002", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO DEMO 01", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        val student = MockSaseData.students.value.first { it.id == "1" }
        assertEquals("S310-DEMA100101-26", student.enrollmentId)
    }

    @Test
    fun reEnrollmentDoesNotDuplicateStudent() {
        MockSaseData.resetForTests()
        val countBefore = MockSaseData.students.value.size
        val classification = SchoolMovementClassification.ReEnrollment(
            studentId = "1", normalizedCurp = "DEMA100101HDFABC01",
            existingEnrollmentId = "S310-DEMA100101-26", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.ExistingPreserved(
            studentId = "1", enrollmentId = "S310-DEMA100101-26", cycle = schoolYear
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-REN-003", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO DEMO 01", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertEquals(countBefore, MockSaseData.students.value.size)
        assertEquals(1, MockSaseData.students.value.count { it.id == "1" })
    }

    @Test
    fun initialMigrationAddsEnrollmentId() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.InitialMigration(
            studentId = "2", normalizedCurp = "DEMB110202MDFABC02", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000780-1", consecutive = 780, checkDigit = 1,
            cycle = schoolYear, reason = "MIGRACION_INICIAL"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-MIG-001", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO DEMO 02", "Secretaría", "HOY 12:00"
        )
        val applied = assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertEquals(MasterStudentMutation.MIGRATED, applied.masterStudentMutation)
        val student = MockSaseData.students.value.first { it.id == "2" }
        assertEquals("S310-000780-1", student.enrollmentId)
    }

    @Test
    fun initialMigrationDoesNotCreateNewStudent() {
        MockSaseData.resetForTests()
        val countBefore = MockSaseData.students.value.size
        val classification = SchoolMovementClassification.InitialMigration(
            studentId = "3", normalizedCurp = "DEMC120303HDFABC03", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000781-9", consecutive = 781, checkDigit = 9,
            cycle = schoolYear, reason = "MIGRACION_INICIAL"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-MIG-002", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO DEMO 03", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertEquals(countBefore, MockSaseData.students.value.size)
    }

    @Test
    fun initialMigrationDoesNotAssignGroup() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.InitialMigration(
            studentId = "4", normalizedCurp = "DEMD130404MDFABC04", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000782-6", consecutive = 782, checkDigit = 6,
            cycle = schoolYear, reason = "MIGRACION_INICIAL"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-MIG-003", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO DEMO 04", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        val student = MockSaseData.students.value.first { it.id == "4" }
        assertEquals("1°A", student.group)
    }

    @Test
    fun appliedUpdatesAllRequiredStores() {
        MockSaseData.resetForTests()
        val countBefore = MockSaseData.students.value.size
        val auditsBefore = MockSaseData.audits.value.size
        val classification = SchoolMovementClassification.NewEntry("APPL100101HDFABC96", schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000783-3", consecutive = 783, checkDigit = 3,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-APPL-001", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-APPL", "ALUMNO APPLIED", "Secretaría", "HOY 12:00"
        )
        val applied = assertIs<AnnualEnrollmentPersistenceResult.Applied>(result)
        assertEquals("MASTER-APPL", applied.annualEnrollment.studentId)
        assertTrue(applied.addedAuditEntries.isNotEmpty())
        assertEquals(countBefore + 1, MockSaseData.students.value.size)
        assertEquals(1, MockSaseData.annualEnrollments.value.size)
        assertTrue(MockSaseData.audits.value.size > auditsBefore)
    }

    @Test
    fun alreadyAppliedDoesNotDuplicate() {
        MockSaseData.resetForTests()
        val curp = "ALRD100101HDFABC95"
        val classification = SchoolMovementClassification.NewEntry(curp, schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000784-0", consecutive = 784, checkDigit = 0,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-ALRD-001", 1, null, emptyList()
        )
        val first = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-ALRD", "ALUMNO ALREADY", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Applied>(first)
        val studentsAfterFirst = MockSaseData.students.value.size
        val enrollmentsAfterFirst = MockSaseData.annualEnrollments.value.size
        val second = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-ALRD", "ALUMNO ALREADY", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.AlreadyApplied>(second)
        assertEquals(studentsAfterFirst, MockSaseData.students.value.size)
        assertEquals(enrollmentsAfterFirst, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun conflictDoesNotModifyStores() {
        MockSaseData.resetForTests()
        val studentsBefore = MockSaseData.students.value.size
        val enrollmentsBefore = MockSaseData.annualEnrollments.value.size
        val auditsBefore = MockSaseData.audits.value.size
        val conflictPlan = AnnualEnrollmentPlanningResult.Conflict(
            AnnualEnrollmentConflictCause.INVALID_SCHOOL_YEAR,
            "Ciclo inválido"
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            conflictPlan, null, "NADIE", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
        assertEquals(studentsBefore, MockSaseData.students.value.size)
        assertEquals(enrollmentsBefore, MockSaseData.annualEnrollments.value.size)
        assertEquals(auditsBefore, MockSaseData.audits.value.size)
    }

    @Test
    fun duplicateCurpProducesConflict() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.NewEntry("DEMA100101HDFABC01", schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000785-8", consecutive = 785, checkDigit = 8,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-DUP-001", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-DUP", "ALUMNO DUPLICADO", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
    }

    @Test
    fun duplicateEnrollmentIdProducesConflict() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.NewEntry("DUPE100101HDFABC94", schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-DEMA100101-26", consecutive = 1, checkDigit = 0,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-DUP-002", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "MASTER-DUP-2", "ALUMNO DUPLICADO MAT", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
    }

    @Test
    fun failurePreventsAllMutations() {
        MockSaseData.resetForTests()
        val studentsBefore = MockSaseData.students.value.size
        val enrollmentsBefore = MockSaseData.annualEnrollments.value.size
        val classification = SchoolMovementClassification.NewEntry("FAIL100101HDFABC93", schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000786-5", consecutive = 786, checkDigit = 5,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-FAIL-001", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO FALLIDO", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
        assertEquals(studentsBefore, MockSaseData.students.value.size)
        assertEquals(enrollmentsBefore, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun reEnrollmentWithInvalidStudentIdShowsConflict() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.ReEnrollment(
            studentId = "NONEXISTENT", normalizedCurp = "NOEX100101HDFABC92",
            existingEnrollmentId = "S310-000787-2", activeSchoolYear = schoolYear
        )
        val allocation = PermanentEnrollmentIdAllocation.ExistingPreserved(
            studentId = "NONEXISTENT", enrollmentId = "S310-000787-2", cycle = schoolYear
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-NOEX-001", 1, null,
            existingAnnualEnrollments = emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "ALUMNO INEXISTENTE", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
    }

    @Test
    fun planningConflictBypassesCommitter() {
        MockSaseData.resetForTests()
        val studentsBefore = MockSaseData.students.value.size
        val plan = AnnualEnrollmentPlanningResult.Conflict(
            AnnualEnrollmentConflictCause.CLASSIFICATION_CONFLICT,
            "Conflicto de clasificación"
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, null, "", "", ""
        )
        val conflict = assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
        assertEquals(PersistenceConflictCause.PLANNING_CONFLICT, conflict.cause)
        assertEquals(studentsBefore, MockSaseData.students.value.size)
    }

    @Test
    fun newEntryWithExistingStudentIdShowsConflict() {
        MockSaseData.resetForTests()
        val classification = SchoolMovementClassification.NewEntry("DEMA100101HDFABC01", schoolYear)
        val allocation = PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = "S310-000788-0", consecutive = 788, checkDigit = 0,
            cycle = schoolYear, reason = "NUEVO_INGRESO"
        )
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification, allocation, schoolYear, "PRE-DUPID-001", 1, null, emptyList()
        )
        val result = AnnualEnrollmentPersistenceAdapter.commit(
            plan, "1", "ALUMNO DEMO 01", "Secretaría", "HOY 12:00"
        )
        assertIs<AnnualEnrollmentPersistenceResult.Conflict>(result)
    }
}
