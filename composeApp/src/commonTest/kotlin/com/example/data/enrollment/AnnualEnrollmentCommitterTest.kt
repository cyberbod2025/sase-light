package com.example.data.enrollment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AnnualEnrollmentCommitterTest {
    @Test fun newEntryCreatesMasterIdentity() { assertEquals(1, applied().state.masterStudents.size) }
    @Test fun newEntryCreatesAnnualEnrollment() { assertEquals(1, applied().state.annualEnrollments.size) }
    @Test fun newEntryUsesExactDraftEnrollmentId() { assertEquals(matricula, applied().masterStudent.permanentEnrollmentId) }
    @Test fun newEntryRequiresNewStudentId() { assertConflict(AnnualEnrollmentCommitConflictCause.MISSING_NEW_STUDENT_ID, commit(newStudentId = null)) }
    @Test fun newEntryRejectsUsedStudentId() { assertConflict(AnnualEnrollmentCommitConflictCause.DUPLICATE_STUDENT_ID, commit(state = state(master()))) }
    @Test fun newEntryRejectsExistingCurp() { assertConflict(AnnualEnrollmentCommitConflictCause.DUPLICATE_MASTER_CURP, commit(state = state(master(id = "OTHER", enrollmentId = "OTHER")))) }
    @Test fun newEntryRejectsExistingEnrollmentId() { assertConflict(AnnualEnrollmentCommitConflictCause.DUPLICATE_PERMANENT_ENROLLMENT_ID, commit(state = state(master(id = "OTHER", curp = "OTHER", enrollmentId = matricula)))) }

    @Test fun reEnrollmentReusesIdentityWithoutChangingIt() {
        val existing = master()
        val result = applied(commit(state(existing), ready(reEnrollmentDraft()), null))
        assertEquals(listOf(existing), result.state.masterStudents)
        assertEquals(MasterStudentMutation.REUSED, result.masterStudentMutation)
    }
    @Test fun reEnrollmentPreservesExactEnrollmentId() { assertEquals(matricula, applied(commit(state(master()), ready(reEnrollmentDraft()), null)).annualEnrollment.permanentEnrollmentId) }
    @Test fun reEnrollmentCreatesOnlyAnnualEnrollment() { val result=applied(commit(state(master()), ready(reEnrollmentDraft()), null)); assertEquals(1,result.state.masterStudents.size); assertEquals(1,result.state.annualEnrollments.size) }
    @Test fun reEnrollmentFailsWhenMasterMissing() { assertConflict(AnnualEnrollmentCommitConflictCause.MASTER_STUDENT_NOT_FOUND, commit(state(), ready(reEnrollmentDraft()), null)) }
    @Test fun reEnrollmentFailsForDuplicateMasterId() { assertConflict(AnnualEnrollmentCommitConflictCause.MULTIPLE_MASTER_STUDENTS_FOUND, commit(state(master(), master(curp="OTHER")), ready(reEnrollmentDraft()), null)) }
    @Test fun reEnrollmentFailsForDuplicateMasterCurp() { assertConflict(AnnualEnrollmentCommitConflictCause.MULTIPLE_MASTER_STUDENTS_FOUND, commit(state(master(), master(id="OTHER")), ready(reEnrollmentDraft()), null)) }
    @Test fun reEnrollmentFailsForDifferentCurp() { assertConflict(AnnualEnrollmentCommitConflictCause.MASTER_CURP_MISMATCH, commit(state(master(curp="OTHER")), ready(reEnrollmentDraft()), null)) }
    @Test fun reEnrollmentFailsForDifferentEnrollmentId() { assertConflict(AnnualEnrollmentCommitConflictCause.MASTER_ENROLLMENT_ID_MISMATCH, commit(state(master(enrollmentId="OTHER")), ready(reEnrollmentDraft()), null)) }

    @Test fun migrationAddsEnrollmentIdToExistingMaster() { val result=applied(commit(state(master(enrollmentId="")), ready(migrationDraft()), null)); assertEquals(matricula,result.masterStudent.permanentEnrollmentId); assertEquals(MasterStudentMutation.MIGRATED,result.masterStudentMutation) }
    @Test fun migrationDoesNotCreateAnotherMaster() { assertEquals(1, applied(commit(state(master(enrollmentId="")), ready(migrationDraft()), null)).state.masterStudents.size) }
    @Test fun migrationRejectsMasterWithEnrollmentId() { assertConflict(AnnualEnrollmentCommitConflictCause.INITIAL_MIGRATION_ALREADY_HAS_ENROLLMENT_ID, commit(state(master()), ready(migrationDraft()), null)) }
    @Test fun migrationRejectsEnrollmentIdOwnedByOtherIdentity() { assertConflict(AnnualEnrollmentCommitConflictCause.DUPLICATE_PERMANENT_ENROLLMENT_ID, commit(state(master(enrollmentId=""),master(id="OTHER",curp="OTHER")), ready(migrationDraft()), null)) }

    @Test fun groupRemainsUnassignedForEveryMovement() {
        val newRecord=applied().annualEnrollment
        val reRecord=applied(commit(state(master()),ready(reEnrollmentDraft()),null)).annualEnrollment
        val migrationRecord=applied(commit(state(master(enrollmentId="")),ready(migrationDraft()),null)).annualEnrollment
        assertNull(newRecord.assignedGroup); assertNull(reRecord.assignedGroup); assertNull(migrationRecord.assignedGroup)
    }
    @Test fun placementRequirementIsPreservedExactly() { val requirement=GroupPlacementRequirement.ContinuityDecisionRequired("1B","2B"); val record=applied(commit(state(master()),ready(reEnrollmentDraft(requirement=requirement)),null)).annualEnrollment; assertEquals(requirement,record.groupPlacementRequirement) }
    @Test fun firstApplicationReturnsApplied() { assertIs<AnnualEnrollmentCommitResult.Applied>(commit()) }
    @Test fun identicalSecondApplicationReturnsAlreadyApplied() { val first=applied(); assertIs<AnnualEnrollmentCommitResult.AlreadyApplied>(commit(first.state)) }
    @Test fun secondApplicationDoesNotDuplicateAudits() { val first=applied(); val second=assertIs<AnnualEnrollmentCommitResult.AlreadyApplied>(commit(first.state)); assertEquals(first.state.auditEntries,second.state.auditEntries) }
    @Test fun sameFolioWithDifferentDataConflicts() { val first=applied(); assertConflict(AnnualEnrollmentCommitConflictCause.PRE_APPLICATION_FOLIO_REUSED_WITH_DIFFERENT_DATA,commit(first.state,ready(newEntryDraft(grade=2)))) }
    @Test fun sameStudentAndYearWithDifferentDataConflicts() { val first=applied(); val other=ready(newEntryDraft(folio="OTHER")); assertConflict(AnnualEnrollmentCommitConflictCause.ANNUAL_ENROLLMENT_ALREADY_EXISTS_WITH_DIFFERENT_DATA,commit(first.state,other)) }
    @Test fun planningConflictDoesNotChangeState() { val original=state(); val result=commit(original,AnnualEnrollmentPlanningResult.Conflict(AnnualEnrollmentConflictCause.INVALID_SCHOOL_YEAR,"bad")); assertConflict(AnnualEnrollmentCommitConflictCause.PLANNING_CONFLICT,result); assertEquals(state(),original) }
    @Test fun emptyActorConflictsWithoutMutation() { val original=state(); assertConflict(AnnualEnrollmentCommitConflictCause.EMPTY_ACTOR,commit(original,actor=" ")); assertEquals(state(),original) }
    @Test fun emptyDateConflictsWithoutMutation() { val original=state(); assertConflict(AnnualEnrollmentCommitConflictCause.EMPTY_OCCURRED_AT,commit(original,occurredAt=" ")); assertEquals(state(),original) }
    @Test fun enrollmentConflictDoesNotApplyMigrationEnrollmentId() { val historical=master(enrollmentId=""); val existing=record(studentId=id,folio="OLD",grade=1); val original=state(listOf(historical),listOf(existing)); assertConflict(AnnualEnrollmentCommitConflictCause.ANNUAL_ENROLLMENT_ALREADY_EXISTS_WITH_DIFFERENT_DATA,commit(original,ready(migrationDraft(folio="NEW",grade=2)),null)); assertEquals("",original.masterStudents.single().permanentEnrollmentId) }
    @Test fun inputsAndListsRemainUnchanged() { val masters=mutableListOf(master()); val enrollments=mutableListOf<AnnualEnrollmentRecord>(); val audits=mutableListOf<EnrollmentAuditEntry>(); val original=EnrollmentRegistryState(masters,enrollments,audits); commit(original,ready(reEnrollmentDraft()),null); assertEquals(listOf(master()),masters); assertEquals(emptyList(),enrollments); assertEquals(emptyList(),audits) }
    @Test fun sameInputsProduceSameResult() { assertEquals(commit(),commit()) }
    @Test fun operationAddsDeterministicAuditEntries() { val result=applied(); assertEquals(2,result.addedAuditEntries.size); assertEquals(actor,result.addedAuditEntries.first().actor); assertEquals(occurredAt,result.addedAuditEntries.first().occurredAt) }
    @Test fun unexpectedNewStudentIdForReEnrollmentConflicts() { assertConflict(AnnualEnrollmentCommitConflictCause.UNEXPECTED_NEW_STUDENT_ID,commit(state(master()),ready(reEnrollmentDraft()),"OTHER")) }
    @Test fun annualIdentityCollisionConflicts() { val existing=record(studentId="OTHER",curp=curp,enrollmentId="OTHER",folio="OLD"); assertConflict(AnnualEnrollmentCommitConflictCause.ANNUAL_ENROLLMENT_IDENTITY_CONFLICT,commit(state(annuals=listOf(existing)))) }

    private val id="MASTER-1"
    private val curp="STUD100101HDFABC01"
    private val matricula="S310-000123-4"
    private val cycle="2026-2027"
    private val folio="PRE-2026-001"
    private val actor="SECRETARIA-1"
    private val occurredAt="2026-07-11T10:00:00-06:00"

    private fun commit(state:EnrollmentRegistryState=state(), planning:AnnualEnrollmentPlanningResult=ready(newEntryDraft()), newStudentId:String?=id, actor:String=this.actor, occurredAt:String=this.occurredAt)=AnnualEnrollmentCommitter.applyAnnualEnrollmentPlan(state,planning,newStudentId,actor,occurredAt)
    private fun applied(result:AnnualEnrollmentCommitResult=commit())=assertIs<AnnualEnrollmentCommitResult.Applied>(result)
    private fun ready(draft:AnnualEnrollmentDraft)=AnnualEnrollmentPlanningResult.Ready(draft)
    private fun newEntryDraft(folio:String=this.folio,grade:Int=1)=draft(null,AnnualEnrollmentMovement.NEW_ENTRY,folio,grade,GroupPlacementRequirement.AssignmentRequired,AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT)
    private fun reEnrollmentDraft(folio:String=this.folio,grade:Int=2,requirement:GroupPlacementRequirement=GroupPlacementRequirement.ContinuityDecisionRequired("1B","2B"))=draft(id,AnnualEnrollmentMovement.RE_ENROLLMENT,folio,grade,requirement,AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION)
    private fun migrationDraft(folio:String=this.folio,grade:Int=2)=draft(id,AnnualEnrollmentMovement.INITIAL_MIGRATION,folio,grade,GroupPlacementRequirement.AssignmentRequired,AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT)
    private fun draft(studentId:String?,movement:AnnualEnrollmentMovement,folio:String,grade:Int,requirement:GroupPlacementRequirement,status:AnnualEnrollmentInitialStatus)=AnnualEnrollmentDraft(studentId,curp,matricula,cycle,folio,movement,grade,requirement,status)
    private fun master(id:String=this.id,curp:String=this.curp,enrollmentId:String=matricula)=MasterStudentIdentitySnapshot(id,curp,enrollmentId,null)
    private fun record(studentId:String=id,curp:String=this.curp,enrollmentId:String=matricula,folio:String=this.folio,grade:Int=1)=AnnualEnrollmentRecord(studentId,curp,enrollmentId,cycle,folio,AnnualEnrollmentMovement.NEW_ENTRY,grade,GroupPlacementRequirement.AssignmentRequired,AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT,null)
    private fun state(vararg masters:MasterStudentIdentitySnapshot)=state(masters.toList())
    private fun state(
        masters: List<MasterStudentIdentitySnapshot> = emptyList(),
        annuals: List<AnnualEnrollmentRecord> = emptyList(),
        audits: List<EnrollmentAuditEntry> = emptyList()
    ) = EnrollmentRegistryState(masters, annuals, audits)
    private fun assertConflict(cause:AnnualEnrollmentCommitConflictCause,result:AnnualEnrollmentCommitResult){assertEquals(cause,assertIs<AnnualEnrollmentCommitResult.Conflict>(result).cause)}
}
