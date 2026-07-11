package com.example.data.enrollment

import com.example.data.MockSaseData
import com.example.data.SaseAudit
import com.example.data.Student
import com.example.data.StudentAddResult

sealed class AnnualEnrollmentPersistenceResult {
    data class Applied(
        val masterStudentMutation: MasterStudentMutation,
        val annualEnrollment: AnnualEnrollmentRecord,
        val addedAuditEntries: List<EnrollmentAuditEntry>
    ) : AnnualEnrollmentPersistenceResult()

    data class AlreadyApplied(
        val annualEnrollment: AnnualEnrollmentRecord
    ) : AnnualEnrollmentPersistenceResult()

    data class Conflict(
        val cause: PersistenceConflictCause,
        val message: String
    ) : AnnualEnrollmentPersistenceResult()
}

enum class PersistenceConflictCause {
    PLANNING_CONFLICT,
    EMPTY_ACTOR,
    EMPTY_OCCURRED_AT,
    MASTER_STUDENT_NOT_FOUND,
    MASTER_STUDENT_CREATE_FAILED,
    ADAPTER_ERROR
}

object AnnualEnrollmentPersistenceAdapter {
    fun commit(
        planningResult: AnnualEnrollmentPlanningResult,
        newStudentId: String?,
        studentFullName: String,
        actor: String,
        occurredAt: String
    ): AnnualEnrollmentPersistenceResult {
        if (planningResult is AnnualEnrollmentPlanningResult.Conflict) {
            return AnnualEnrollmentPersistenceResult.Conflict(
                PersistenceConflictCause.PLANNING_CONFLICT,
                planningResult.message
            )
        }
        val state = buildRegistryState()
        val commitResult = AnnualEnrollmentCommitter.applyAnnualEnrollmentPlan(
            state = state,
            planningResult = planningResult,
            newStudentId = newStudentId,
            actor = actor,
            occurredAt = occurredAt
        )
        return when (commitResult) {
            is AnnualEnrollmentCommitResult.Applied -> persistApplied(commitResult, studentFullName)
            is AnnualEnrollmentCommitResult.AlreadyApplied -> AnnualEnrollmentPersistenceResult.AlreadyApplied(
                annualEnrollment = commitResult.annualEnrollment
            )
            is AnnualEnrollmentCommitResult.Conflict -> AnnualEnrollmentPersistenceResult.Conflict(
                cause = PersistenceConflictCause.PLANNING_CONFLICT,
                message = commitResult.message
            )
        }
    }

    private fun buildRegistryState(): EnrollmentRegistryState {
        val masterStudents = MockSaseData.students.value.map { student ->
            MasterStudentIdentitySnapshot(
                studentId = student.id,
                normalizedCurp = student.curp.trim().uppercase(),
                permanentEnrollmentId = student.enrollmentId.trim(),
                sourcePreApplicationFolio = student.preApplicationFolio
            )
        }
        val annualEnrollments = MockSaseData.annualEnrollments.value.toList()
        val auditEntries = MockSaseData.audits.value.map { audit ->
            EnrollmentAuditEntry(
                action = EnrollmentAuditAction.ANNUAL_ENROLLMENT_CREATED,
                studentId = "",
                schoolYear = "",
                sourcePreApplicationFolio = "",
                actor = audit.userRole,
                occurredAt = audit.timestamp,
                detail = audit.detail
            )
        }
        return EnrollmentRegistryState(masterStudents, annualEnrollments, auditEntries)
    }

    private fun persistApplied(
        result: AnnualEnrollmentCommitResult.Applied,
        studentFullName: String
    ): AnnualEnrollmentPersistenceResult {
        when (result.masterStudentMutation) {
            MasterStudentMutation.CREATED -> {
                val student = Student(
                    id = result.masterStudent.studentId,
                    fullName = studentFullName,
                    group = "",
                    enrollmentId = result.masterStudent.permanentEnrollmentId,
                    curp = result.masterStudent.normalizedCurp,
                    preApplicationFolio = result.masterStudent.sourcePreApplicationFolio
                )
                val addResult = MockSaseData.addStudent(student)
                if (addResult !is StudentAddResult.Added) {
                    return AnnualEnrollmentPersistenceResult.Conflict(
                        PersistenceConflictCause.MASTER_STUDENT_CREATE_FAILED,
                        "No se pudo crear el alumno maestro: $addResult"
                    )
                }
            }
            MasterStudentMutation.MIGRATED -> {
                val existing = MockSaseData.students.value.firstOrNull { it.id == result.masterStudent.studentId }
                if (existing == null) {
                    return AnnualEnrollmentPersistenceResult.Conflict(
                        PersistenceConflictCause.MASTER_STUDENT_NOT_FOUND,
                        "No se encontró el alumno maestro para migración."
                    )
                }
                MockSaseData.updateStudent(existing.copy(
                    enrollmentId = result.masterStudent.permanentEnrollmentId
                ))
            }
            MasterStudentMutation.REUSED -> {
            }
        }
        MockSaseData.addAnnualEnrollment(result.annualEnrollment)
        for (auditEntry in result.addedAuditEntries) {
            MockSaseData.logAudit(
                action = auditEntry.action.name,
                role = auditEntry.actor,
                timestamp = auditEntry.occurredAt,
                detail = auditEntry.detail
            )
        }
        return AnnualEnrollmentPersistenceResult.Applied(
            masterStudentMutation = result.masterStudentMutation,
            annualEnrollment = result.annualEnrollment,
            addedAuditEntries = result.addedAuditEntries
        )
    }
}
