package com.example.data.enrollment

import com.example.data.MockSaseData
import com.example.data.Student
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnnualEnrollmentFlowCoordinatorTest {
    private val schoolYear = "2026-2027"
    private val actor = "Secretaría"
    private val occurredAt = "HOY 12:00"

    private fun request(
        movement: String = "NUEVO INGRESO",
        curp: String = "COOR100101HDFABC01",
        folio: String = "PRE-COOR-001",
        grade: Int = 1,
        previousGroup: String? = null,
        schoolYear: String = this.schoolYear,
        newStudentId: String? = null,
        studentFullName: String = "ALUMNO COORDINADOR"
    ) = AnnualEnrollmentFlowRequest(
        declaredMovement = movement,
        normalizedCurp = curp,
        sourcePreApplicationFolio = folio,
        requestedGrade = grade,
        previousGroup = previousGroup,
        schoolYear = schoolYear,
        newStudentId = newStudentId,
        studentFullName = studentFullName,
        actor = actor,
        occurredAt = occurredAt
    )

    private fun addMasterStudentWithValidS310Id(): Student {
        val student = Student(
            id = "TEST-RE",
            fullName = "ALUMNO RE",
            group = "1A",
            enrollmentId = "S310-000001-1",
            curp = "REAL100101HDFABC01",
            preApplicationFolio = "PRE-RE-ORIG"
        )
        MockSaseData.addStudent(student)
        return student
    }

    @Test
    fun newEntryProcessCompleted() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR100101HDFABC01", folio = "PRE-COOR-NE-001",
            newStudentId = "MASTER-NE")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertEquals(AnnualEnrollmentMovement.NEW_ENTRY, completed.movement)
        assertTrue(completed.enrollmentId.startsWith("S310-"))
        assertEquals(schoolYear, completed.schoolYear)
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT, completed.status)
        assertTrue(MockSaseData.annualEnrollments.value.any { it.studentId == "MASTER-NE" })
        assertTrue(MockSaseData.students.value.any { it.id == "MASTER-NE" })
    }

    @Test
    fun newEntryDoesNotAssignGroup() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR200101HDFABC02", folio = "PRE-COOR-NG-001",
            newStudentId = "MASTER-NG")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        val student = MockSaseData.students.value.firstOrNull { it.id == "MASTER-NG" }
        assertNotNull(student)
        assertEquals("", student.group)
    }

    @Test
    fun newEntryUsesV2EnrollmentIdFormat() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR300101HDFABC03", folio = "PRE-COOR-FMT-001",
            newStudentId = "MASTER-FMT")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertTrue(completed.enrollmentId.matches(Regex("S310-\\d{6}-\\d$")))
    }

    @Test
    fun reEnrollmentPreservesExistingEnrollmentId() {
        MockSaseData.resetForTests()
        addMasterStudentWithValidS310Id()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "REAL100101HDFABC01",
            folio = "PRE-COOR-RE-001",
            grade = 2,
            previousGroup = null
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertEquals("S310-000001-1", completed.enrollmentId)
    }

    @Test
    fun reEnrollmentReusesExistingStudent() {
        MockSaseData.resetForTests()
        addMasterStudentWithValidS310Id()
        val studentCount = MockSaseData.students.value.size
        val req = request(
            movement = "REINSCRIPCION",
            curp = "REAL100101HDFABC01",
            folio = "PRE-COOR-RE2-001",
            grade = 2,
            previousGroup = null
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertEquals(studentCount, MockSaseData.students.value.size)
    }

    @Test
    fun initialMigrationAddsEnrollmentId() {
        MockSaseData.resetForTests()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "DEMB110202MDFABC02",
            folio = "PRE-COOR-IM-001",
            grade = 2,
            previousGroup = null
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertTrue(completed.enrollmentId.startsWith("S310-"))
        assertTrue(MockSaseData.students.value.any { it.id == "2" && it.enrollmentId == completed.enrollmentId })
    }

    @Test
    fun initialMigrationDoesNotCreateNewStudent() {
        MockSaseData.resetForTests()
        val studentCount = MockSaseData.students.value.size
        val req = request(
            movement = "REINSCRIPCION",
            curp = "DEMB110202MDFABC02",
            folio = "PRE-COOR-IM2-001",
            grade = 2,
            previousGroup = null
        )
        AnnualEnrollmentFlowCoordinator.process(req)
        assertEquals(studentCount, MockSaseData.students.value.size)
    }

    @Test
    fun classificationConflictReturnsConflict() {
        MockSaseData.resetForTests()
        val req = request(curp = "")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val conflict = assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
        assertEquals("CLASSIFICATION", conflict.stage)
    }

    @Test
    fun planningConflictForGradeOutOfRange() {
        MockSaseData.resetForTests()
        val req = request(grade = 5)
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val conflict = assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
        assertEquals("PLANNING", conflict.stage)
    }

    @Test
    fun emptyActorReturnsConflict() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR400101HDFABC04", folio = "PRE-COOR-EA-001",
            newStudentId = "MASTER-EA").copy(actor = "")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
    }

    @Test
    fun secondIdenticalRequestReturnsAlreadyCompleted() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR500101HDFABC05", folio = "PRE-COOR-IDEM-001",
            newStudentId = "MASTER-IDEM")
        val first = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Completed>(first)
        val enrollmentsAfterFirst = MockSaseData.annualEnrollments.value.size
        val second = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.AlreadyCompleted>(second)
        assertEquals(enrollmentsAfterFirst, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun repeatedClickDoesNotDuplicate() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR600101HDFABC06", folio = "PRE-COOR-RPT-001",
            newStudentId = "MASTER-RPT")
        val first = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Completed>(first)
        val studentCount = MockSaseData.students.value.size
        val enrollmentCount = MockSaseData.annualEnrollments.value.size
        val second = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.AlreadyCompleted>(second)
        assertEquals(studentCount, MockSaseData.students.value.size)
        assertEquals(enrollmentCount, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun continuityDecisionForReEnrollment() {
        MockSaseData.resetForTests()
        addMasterStudentWithValidS310Id()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "REAL100101HDFABC01",
            folio = "PRE-COOR-CD-001",
            grade = 2,
            previousGroup = "1A"
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val needsDecision = assertIs<AnnualEnrollmentFlowResult.NeedsDecision>(result)
        assertEquals("1A", needsDecision.previousGroup)
        assertEquals("2A", needsDecision.suggestedGroup)
    }

    @Test
    fun newEntryWithoutPreviousGroupReturnsCompleted() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR700101HDFABC07", folio = "PRE-COOR-NWPG-001",
            newStudentId = "MASTER-NWPG")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Completed>(result)
    }

    @Test
    fun auditGeneratedOnce() {
        MockSaseData.resetForTests()
        val auditsBefore = MockSaseData.audits.value.size
        AnnualEnrollmentFlowCoordinator.process(
            request(curp = "COOR800101HDFABC08", folio = "PRE-COOR-AUD-001",
                newStudentId = "MASTER-AUD"))
        assertTrue(MockSaseData.audits.value.size > auditsBefore)
        val auditsBefore2 = MockSaseData.audits.value.size
        AnnualEnrollmentFlowCoordinator.process(
            request(curp = "COOR800101HDFABC08", folio = "PRE-COOR-AUD-001",
                newStudentId = "MASTER-AUD"))
        assertEquals(auditsBefore2, MockSaseData.audits.value.size)
    }

    @Test
    fun stateUnchangedOnConflict() {
        MockSaseData.resetForTests()
        val studentCount = MockSaseData.students.value.size
        val enrollmentCount = MockSaseData.annualEnrollments.value.size
        val req = request(grade = 5)
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
        assertEquals(studentCount, MockSaseData.students.value.size)
        assertEquals(enrollmentCount, MockSaseData.annualEnrollments.value.size)
    }

    @Test
    fun conflictResultIndicatesStage() {
        MockSaseData.resetForTests()
        val emptyCurp = request(curp = "")
        assertEquals("CLASSIFICATION",
            (AnnualEnrollmentFlowCoordinator.process(emptyCurp) as AnnualEnrollmentFlowResult.Conflict).stage)
        val badGrade = request(grade = 5)
        assertEquals("PLANNING",
            (AnnualEnrollmentFlowCoordinator.process(badGrade) as AnnualEnrollmentFlowResult.Conflict).stage)
        val neReq = request(curp = "COOR900101HDFABC09", folio = "PRE-COOR-CS-001",
            newStudentId = "MASTER-CS").copy(actor = "")
        assertEquals("PERSISTENCE",
            (AnnualEnrollmentFlowCoordinator.process(neReq) as AnnualEnrollmentFlowResult.Conflict).stage)
    }

    @Test
    fun invalidSchoolYearReturnsConflict() {
        MockSaseData.resetForTests()
        val req = request(schoolYear = "2026")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
    }

    @Test
    fun newEntryConflictWhenCurpAlreadyExists() {
        MockSaseData.resetForTests()
        val req = request(
            movement = "NUEVO INGRESO",
            curp = "DEMA100101HDFABC01",
            folio = "PRE-COOR-DUP-001"
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val conflict = assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
        assertEquals("CLASSIFICATION", conflict.stage)
    }

    @Test
    fun reEnrollmentWithoutExistingCurpReturnsConflict() {
        MockSaseData.resetForTests()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "NOEX100101HDFABC99",
            folio = "PRE-COOR-NOEX-001"
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Conflict>(result)
    }

    @Test
    fun initialMigrationOfStudentWithoutEnrollmentIdWorks() {
        MockSaseData.resetForTests()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "DEMC120303HDFABC03",
            folio = "PRE-COOR-IM3-001",
            grade = 3,
            previousGroup = null
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        assertIs<AnnualEnrollmentFlowResult.Completed>(result)
    }

    @Test
    fun needsDecisionForInitialMigrationWithContinuity() {
        MockSaseData.resetForTests()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "DEMB110202MDFABC02",
            folio = "PRE-COOR-ND-001",
            grade = 2,
            previousGroup = "1B"
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val needsDecision = assertIs<AnnualEnrollmentFlowResult.NeedsDecision>(result)
        assertEquals("1B", needsDecision.previousGroup)
        assertEquals("2B", needsDecision.suggestedGroup)
    }

    @Test
    fun invalidPreviousGroupGetsAssignmentRequired() {
        MockSaseData.resetForTests()
        addMasterStudentWithValidS310Id()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "REAL100101HDFABC01",
            folio = "PRE-COOR-IPG-001",
            grade = 2,
            previousGroup = "XX"
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertIs<GroupPlacementRequirement.AssignmentRequired>(completed.groupRequirement)
    }

    @Test
    fun needsDecisionCarriesStudentAndEnrollmentDetails() {
        MockSaseData.resetForTests()
        addMasterStudentWithValidS310Id()
        val req = request(
            movement = "REINSCRIPCION",
            curp = "REAL100101HDFABC01",
            folio = "PRE-COOR-ND2-001",
            grade = 2,
            previousGroup = "1A"
        )
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val nd = assertIs<AnnualEnrollmentFlowResult.NeedsDecision>(result)
        assertTrue(nd.studentId.isNotBlank())
        assertTrue(nd.enrollmentId.isNotBlank())
        assertEquals(schoolYear, nd.schoolYear)
    }

    @Test
    fun completedCarriesMovementInfo() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR910101HDFABC10", folio = "PRE-COOR-CMI-001",
            newStudentId = "MASTER-CMI")
        val result = AnnualEnrollmentFlowCoordinator.process(req)
        val completed = assertIs<AnnualEnrollmentFlowResult.Completed>(result)
        assertEquals(AnnualEnrollmentMovement.NEW_ENTRY, completed.movement)
        assertEquals(AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT, completed.status)
    }

    @Test
    fun alreadyCompletedCarriesExistingEnrollmentRecord() {
        MockSaseData.resetForTests()
        val req = request(curp = "COOR920101HDFABC11", folio = "PRE-COOR-ACE-001",
            newStudentId = "MASTER-ACE")
        assertIs<AnnualEnrollmentFlowResult.Completed>(AnnualEnrollmentFlowCoordinator.process(req))
        val second = AnnualEnrollmentFlowCoordinator.process(req)
        val already = assertIs<AnnualEnrollmentFlowResult.AlreadyCompleted>(second)
        assertNotNull(already.enrollmentRecord)
    }

    @Test
    fun newEntryProposedEnrollmentIdIsConsecutive() {
        MockSaseData.resetForTests()
        val r1 = assertIs<AnnualEnrollmentFlowResult.Completed>(
            AnnualEnrollmentFlowCoordinator.process(
                request(curp = "COOR930101HDFABC12", folio = "PRE-COOR-CN1-001",
                    newStudentId = "MASTER-CN1")))
        val r2 = assertIs<AnnualEnrollmentFlowResult.Completed>(
            AnnualEnrollmentFlowCoordinator.process(
                request(curp = "COOR940101HDFABC13", folio = "PRE-COOR-CN2-001",
                    newStudentId = "MASTER-CN2")))
        val c1 = r1.enrollmentId.substringAfter("S310-").substringBefore("-").toInt()
        val c2 = r2.enrollmentId.substringAfter("S310-").substringBefore("-").toInt()
        assertTrue(c2 > c1)
    }

    @Test
    fun enrollmentFlowModeEnumDefined() {
        assertEquals("LEGACY", EnrollmentFlowMode.LEGACY.name)
        assertEquals("ANNUAL_V2", EnrollmentFlowMode.ANNUAL_V2.name)
    }
}
