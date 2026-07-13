package com.example.data.enrollment

import com.example.data.MockSaseData

enum class EnrollmentFlowMode {
    LEGACY,
    ANNUAL_V2
}

data class AnnualEnrollmentFlowRequest(
    val declaredMovement: String,
    val normalizedCurp: String,
    val sourcePreApplicationFolio: String,
    val requestedGrade: Int,
    val previousGroup: String?,
    val schoolYear: String,
    val newStudentId: String?,
    val studentFullName: String,
    val actor: String,
    val occurredAt: String
)

sealed class AnnualEnrollmentFlowResult {
    data class Completed(
        val movement: AnnualEnrollmentMovement,
        val studentId: String,
        val enrollmentId: String,
        val schoolYear: String,
        val folio: String,
        val status: AnnualEnrollmentInitialStatus,
        val groupRequirement: GroupPlacementRequirement,
        val message: String
    ) : AnnualEnrollmentFlowResult()

    data class AlreadyCompleted(
        val enrollmentRecord: AnnualEnrollmentRecord,
        val message: String = "La inscripción anual ya fue aplicada."
    ) : AnnualEnrollmentFlowResult()

    data class NeedsDecision(
        val studentId: String,
        val enrollmentId: String,
        val schoolYear: String,
        val folio: String,
        val previousGroup: String?,
        val suggestedGroup: String?,
        val reason: String
    ) : AnnualEnrollmentFlowResult()

    data class Conflict(
        val cause: String,
        val message: String,
        val stage: String
    ) : AnnualEnrollmentFlowResult()
}

object AnnualEnrollmentFlowCoordinator {
    fun process(request: AnnualEnrollmentFlowRequest): AnnualEnrollmentFlowResult {
        val existingByFolio = MockSaseData.annualEnrollments.value.filter {
            it.sourcePreApplicationFolio.trim().uppercase() == request.sourcePreApplicationFolio.trim().uppercase()
        }
        if (existingByFolio.size > 1) {
            return AnnualEnrollmentFlowResult.Conflict(
                cause = "AMBIGUOUS_PRE_APPLICATION_FOLIO",
                message = "Existen varias anualidades asociadas al mismo folio.",
                stage = "IDEMPOTENCY"
            )
        }
        existingByFolio.singleOrNull()?.let { existing ->
            return if (matchesOriginalRequest(existing, request)) {
                AnnualEnrollmentFlowResult.AlreadyCompleted(
                    enrollmentRecord = existing
                )
            } else {
                AnnualEnrollmentFlowResult.Conflict(
                    cause = "PRE_APPLICATION_FOLIO_REUSED_WITH_DIFFERENT_DATA",
                    message = "El folio ya existe con datos incompatibles.",
                    stage = "IDEMPOTENCY"
                )
            }
        }
        val masterStudents = MockSaseData.students.value

        val classification = SchoolMovementClassifier.classify(
            declaredApplicationType = request.declaredMovement,
            curp = request.normalizedCurp,
            activeSchoolYear = request.schoolYear,
            masterStudents = masterStudents
        )
        if (classification is SchoolMovementClassification.Conflict) {
            return AnnualEnrollmentFlowResult.Conflict(
                cause = classification.reason.name,
                message = classification.message,
                stage = "CLASSIFICATION"
            )
        }

        val allocation = PermanentEnrollmentIdAllocator.allocatePermanentEnrollmentId(
            classification = classification,
            masterStudents = masterStudents,
            activeSchoolYear = request.schoolYear
        )
        when (allocation) {
            is PermanentEnrollmentIdAllocation.Conflict.Integrity -> return AnnualEnrollmentFlowResult.Conflict(
                cause = allocation.cause.name,
                message = allocation.message,
                stage = "ALLOCATION"
            )
            is PermanentEnrollmentIdAllocation.Conflict.ConflictResult -> return AnnualEnrollmentFlowResult.Conflict(
                cause = allocation.cause.name,
                message = allocation.message,
                stage = "ALLOCATION"
            )
            is PermanentEnrollmentIdAllocation.Conflict.Exhausted -> return AnnualEnrollmentFlowResult.Conflict(
                cause = "CONSECUTIVE_EXHAUSTED",
                message = allocation.message,
                stage = "ALLOCATION"
            )
            else -> {}
        }

        val existingAnnualEnrollments = buildSnapshots()
        val plan = AnnualEnrollmentPlanner.planAnnualEnrollment(
            classification = classification,
            enrollmentIdAllocation = allocation,
            schoolYear = request.schoolYear,
            sourcePreApplicationFolio = request.sourcePreApplicationFolio,
            requestedGrade = request.requestedGrade,
            previousGroup = request.previousGroup,
            existingAnnualEnrollments = existingAnnualEnrollments
        )
        if (plan is AnnualEnrollmentPlanningResult.Conflict) {
            return AnnualEnrollmentFlowResult.Conflict(
                cause = plan.cause.name,
                message = plan.message,
                stage = "PLANNING"
            )
        }

        val result = AnnualEnrollmentPersistenceAdapter.commit(
            planningResult = plan,
            newStudentId = request.newStudentId,
            studentFullName = request.studentFullName,
            actor = request.actor,
            occurredAt = request.occurredAt
        )

        return when (result) {
            is AnnualEnrollmentPersistenceResult.Applied -> {
                val draft = (plan as AnnualEnrollmentPlanningResult.Ready).draft
                when (draft.groupPlacementRequirement) {
                    is GroupPlacementRequirement.ContinuityDecisionRequired -> {
                        AnnualEnrollmentFlowResult.NeedsDecision(
                            studentId = result.annualEnrollment.studentId,
                            enrollmentId = result.annualEnrollment.permanentEnrollmentId,
                            schoolYear = result.annualEnrollment.schoolYear,
                            folio = result.annualEnrollment.sourcePreApplicationFolio,
                            previousGroup = draft.groupPlacementRequirement.previousGroup,
                            suggestedGroup = draft.groupPlacementRequirement.suggestedGroup,
                            reason = "Continuidad de grupo pendiente de decisión: ${draft.groupPlacementRequirement.previousGroup} → ${draft.groupPlacementRequirement.suggestedGroup}"
                        )
                    }
                    is GroupPlacementRequirement.AssignmentRequired -> {
                        AnnualEnrollmentFlowResult.Completed(
                            movement = draft.movement,
                            studentId = result.annualEnrollment.studentId,
                            enrollmentId = result.annualEnrollment.permanentEnrollmentId,
                            schoolYear = result.annualEnrollment.schoolYear,
                            folio = result.annualEnrollment.sourcePreApplicationFolio,
                            status = result.annualEnrollment.status,
                            groupRequirement = draft.groupPlacementRequirement,
                            message = "Inscripción anual completada. Pendiente de asignación de grupo."
                        )
                    }
                }
            }
            is AnnualEnrollmentPersistenceResult.AlreadyApplied -> {
                AnnualEnrollmentFlowResult.AlreadyCompleted(
                    enrollmentRecord = result.annualEnrollment
                )
            }
            is AnnualEnrollmentPersistenceResult.Conflict -> {
                AnnualEnrollmentFlowResult.Conflict(
                    cause = result.cause.name,
                    message = result.message,
                    stage = "PERSISTENCE"
                )
            }
        }
    }

    private fun buildSnapshots(): List<AnnualEnrollmentSnapshot> {
        return MockSaseData.annualEnrollments.value.map { record ->
            AnnualEnrollmentSnapshot(
                studentId = record.studentId,
                normalizedCurp = record.normalizedCurp,
                permanentEnrollmentId = record.permanentEnrollmentId,
                schoolYear = record.schoolYear,
                requestedGrade = record.requestedGrade,
                assignedGroup = record.assignedGroup,
                sourcePreApplicationFolio = record.sourcePreApplicationFolio
            )
        }
    }

    private fun matchesOriginalRequest(
        existing: AnnualEnrollmentRecord,
        request: AnnualEnrollmentFlowRequest
    ): Boolean =
        existing.sourcePreApplicationFolio.normalized() == request.sourcePreApplicationFolio.normalized() &&
            existing.normalizedCurp.normalized() == request.normalizedCurp.normalized() &&
            existing.schoolYear.trim() == request.schoolYear.trim() &&
            existing.requestedGrade == request.requestedGrade &&
            existing.movement.matchesDeclared(request.declaredMovement)

    private fun AnnualEnrollmentMovement.matchesDeclared(declaredMovement: String): Boolean =
        when (declaredMovement.normalized().replace('Ó', 'O')) {
            "NUEVO INGRESO" -> this == AnnualEnrollmentMovement.NEW_ENTRY
            "REINSCRIPCION" -> this == AnnualEnrollmentMovement.RE_ENROLLMENT ||
                this == AnnualEnrollmentMovement.INITIAL_MIGRATION
            else -> false
        }

    private fun String.normalized(): String = trim().uppercase()
}
