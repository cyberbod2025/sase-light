package com.example.data.enrollment

data class AnnualEnrollmentSnapshot(
    val studentId: String,
    val normalizedCurp: String,
    val permanentEnrollmentId: String,
    val schoolYear: String,
    val requestedGrade: Int,
    val assignedGroup: String?,
    val sourcePreApplicationFolio: String
)

data class AnnualEnrollmentDraft(
    val studentId: String?,
    val normalizedCurp: String,
    val permanentEnrollmentId: String,
    val schoolYear: String,
    val sourcePreApplicationFolio: String,
    val movement: AnnualEnrollmentMovement,
    val requestedGrade: Int,
    val groupPlacementRequirement: GroupPlacementRequirement,
    val initialStatus: AnnualEnrollmentInitialStatus
)

enum class AnnualEnrollmentMovement {
    NEW_ENTRY,
    RE_ENROLLMENT,
    INITIAL_MIGRATION
}

enum class AnnualEnrollmentInitialStatus {
    PENDING_GROUP_ASSIGNMENT,
    PENDING_GROUP_CONTINUITY_DECISION
}

sealed class GroupPlacementRequirement {
    data object AssignmentRequired : GroupPlacementRequirement()

    data class ContinuityDecisionRequired(
        val previousGroup: String,
        val suggestedGroup: String
    ) : GroupPlacementRequirement()
}

sealed class AnnualEnrollmentPlanningResult {
    data class Ready(val draft: AnnualEnrollmentDraft) : AnnualEnrollmentPlanningResult()

    data class Conflict(
        val cause: AnnualEnrollmentConflictCause,
        val message: String
    ) : AnnualEnrollmentPlanningResult()
}

enum class AnnualEnrollmentConflictCause {
    CLASSIFICATION_CONFLICT,
    ALLOCATION_CONFLICT,
    ALLOCATION_EXHAUSTED,
    INCOMPATIBLE_CLASSIFICATION_AND_ALLOCATION,
    INCOMPATIBLE_SCHOOL_YEAR,
    STUDENT_ID_MISMATCH,
    ENROLLMENT_ID_MISMATCH,
    NEW_ENTRY_WITH_EXISTING_STUDENT_ID,
    INITIAL_MIGRATION_WITHOUT_STUDENT_ID,
    MISSING_STUDENT_ID,
    EMPTY_CURP,
    EMPTY_PRE_APPLICATION_FOLIO,
    EMPTY_PERMANENT_ENROLLMENT_ID,
    INVALID_SCHOOL_YEAR,
    INVALID_REQUESTED_GRADE,
    DUPLICATE_STUDENT_IN_SCHOOL_YEAR,
    DUPLICATE_CURP_IN_SCHOOL_YEAR,
    REUSED_PRE_APPLICATION_FOLIO,
    ENROLLMENT_ID_USED_BY_DIFFERENT_IDENTITY
}

object AnnualEnrollmentPlanner {
    private const val NEW_ENTRY_REASON = "NUEVO_INGRESO"
    private const val INITIAL_MIGRATION_REASON = "MIGRACION_INICIAL"
    private val schoolYearPattern = Regex("^(\\d{4})-(\\d{4})$")
    private val validPreviousGroupPattern = Regex("^[123][ABCD]$")

    fun planAnnualEnrollment(
        classification: SchoolMovementClassification,
        enrollmentIdAllocation: PermanentEnrollmentIdAllocation,
        schoolYear: String,
        sourcePreApplicationFolio: String,
        requestedGrade: Int,
        previousGroup: String?,
        existingAnnualEnrollments: List<AnnualEnrollmentSnapshot>
    ): AnnualEnrollmentPlanningResult {
        val normalizedSchoolYear = schoolYear.trim()
        if (!isValidSchoolYear(normalizedSchoolYear)) {
            return conflict(
                AnnualEnrollmentConflictCause.INVALID_SCHOOL_YEAR,
                "El ciclo escolar debe usar el formato YYYY-YYYY con años consecutivos."
            )
        }
        if (requestedGrade !in 1..3) {
            return conflict(
                AnnualEnrollmentConflictCause.INVALID_REQUESTED_GRADE,
                "El grado solicitado debe estar entre 1 y 3."
            )
        }

        val normalizedFolio = sourcePreApplicationFolio.trim()
        if (normalizedFolio.isBlank()) {
            return conflict(
                AnnualEnrollmentConflictCause.EMPTY_PRE_APPLICATION_FOLIO,
                "El folio de pre-solicitud es obligatorio."
            )
        }

        if (classification is SchoolMovementClassification.Conflict) {
            return conflict(
                AnnualEnrollmentConflictCause.CLASSIFICATION_CONFLICT,
                "No se puede planear la inscripción: ${classification.message}"
            )
        }
        if (enrollmentIdAllocation is PermanentEnrollmentIdAllocation.Conflict.Exhausted) {
            return conflict(
                AnnualEnrollmentConflictCause.ALLOCATION_EXHAUSTED,
                enrollmentIdAllocation.message
            )
        }
        if (enrollmentIdAllocation is PermanentEnrollmentIdAllocation.Conflict) {
            return conflict(
                AnnualEnrollmentConflictCause.ALLOCATION_CONFLICT,
                allocationConflictMessage(enrollmentIdAllocation)
            )
        }

        val candidate = buildCandidate(classification, enrollmentIdAllocation)
        if (candidate is CandidateResult.Invalid) {
            return conflict(candidate.cause, candidate.message)
        }
        candidate as CandidateResult.Valid

        if (candidate.classificationSchoolYear.trim() != normalizedSchoolYear ||
            candidate.allocationSchoolYear.trim() != normalizedSchoolYear
        ) {
            return conflict(
                AnnualEnrollmentConflictCause.INCOMPATIBLE_SCHOOL_YEAR,
                "La clasificación, la matrícula y el ciclo solicitado deben corresponder al mismo ciclo escolar."
            )
        }

        val normalizedCurp = candidate.normalizedCurp.trim().uppercase()
        if (normalizedCurp.isBlank()) {
            return conflict(
                AnnualEnrollmentConflictCause.EMPTY_CURP,
                "La CURP normalizada es obligatoria."
            )
        }

        val enrollmentId = candidate.permanentEnrollmentId.trim()
        if (enrollmentId.isBlank()) {
            return conflict(
                AnnualEnrollmentConflictCause.EMPTY_PERMANENT_ENROLLMENT_ID,
                "La matrícula permanente es obligatoria."
            )
        }

        findExistingEnrollmentConflict(
            studentId = candidate.studentId,
            normalizedCurp = normalizedCurp,
            permanentEnrollmentId = enrollmentId,
            schoolYear = normalizedSchoolYear,
            sourcePreApplicationFolio = normalizedFolio,
            existingAnnualEnrollments = existingAnnualEnrollments
        )?.let { return it }

        val placementRequirement = placementRequirement(
            movement = candidate.movement,
            previousGroup = previousGroup,
            requestedGrade = requestedGrade
        )
        val initialStatus = when (placementRequirement) {
            GroupPlacementRequirement.AssignmentRequired ->
                AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT
            is GroupPlacementRequirement.ContinuityDecisionRequired ->
                AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION
        }

        return AnnualEnrollmentPlanningResult.Ready(
            AnnualEnrollmentDraft(
                studentId = candidate.studentId,
                normalizedCurp = normalizedCurp,
                permanentEnrollmentId = enrollmentId,
                schoolYear = normalizedSchoolYear,
                sourcePreApplicationFolio = normalizedFolio,
                movement = candidate.movement,
                requestedGrade = requestedGrade,
                groupPlacementRequirement = placementRequirement,
                initialStatus = initialStatus
            )
        )
    }

    private fun buildCandidate(
        classification: SchoolMovementClassification,
        allocation: PermanentEnrollmentIdAllocation
    ): CandidateResult = when (classification) {
        is SchoolMovementClassification.NewEntry -> when (allocation) {
            is PermanentEnrollmentIdAllocation.NewProposal -> {
                if (allocation.reason != NEW_ENTRY_REASON) {
                    incompatible("Nuevo ingreso requiere una propuesta con motivo $NEW_ENTRY_REASON.")
                } else {
                    validCandidate(
                        studentId = null,
                        normalizedCurp = classification.normalizedCurp,
                        permanentEnrollmentId = allocation.enrollmentId,
                        movement = AnnualEnrollmentMovement.NEW_ENTRY,
                        classificationSchoolYear = classification.activeSchoolYear,
                        allocationSchoolYear = allocation.cycle
                    )
                }
            }
            is PermanentEnrollmentIdAllocation.ExistingPreserved -> CandidateResult.Invalid(
                AnnualEnrollmentConflictCause.NEW_ENTRY_WITH_EXISTING_STUDENT_ID,
                "Nuevo ingreso no puede usar la identidad de un alumno maestro existente."
            )
            is PermanentEnrollmentIdAllocation.Conflict -> incompatible("La asignación de matrícula es incompatible.")
        }

        is SchoolMovementClassification.ReEnrollment -> {
            if (classification.studentId.isBlank()) {
                CandidateResult.Invalid(
                    AnnualEnrollmentConflictCause.MISSING_STUDENT_ID,
                    "La reinscripción requiere un identificador de alumno maestro."
                )
            } else when (allocation) {
                is PermanentEnrollmentIdAllocation.ExistingPreserved -> when {
                    classification.studentId != allocation.studentId -> CandidateResult.Invalid(
                        AnnualEnrollmentConflictCause.STUDENT_ID_MISMATCH,
                        "La clasificación y la matrícula preservada corresponden a alumnos distintos."
                    )
                    classification.existingEnrollmentId != allocation.enrollmentId -> CandidateResult.Invalid(
                        AnnualEnrollmentConflictCause.ENROLLMENT_ID_MISMATCH,
                        "La reinscripción debe conservar exactamente la matrícula institucional existente."
                    )
                    else -> validCandidate(
                        studentId = classification.studentId,
                        normalizedCurp = classification.normalizedCurp,
                        permanentEnrollmentId = allocation.enrollmentId,
                        movement = AnnualEnrollmentMovement.RE_ENROLLMENT,
                        classificationSchoolYear = classification.activeSchoolYear,
                        allocationSchoolYear = allocation.cycle
                    )
                }
                is PermanentEnrollmentIdAllocation.NewProposal ->
                    incompatible("La reinscripción requiere una matrícula existente preservada.")
                is PermanentEnrollmentIdAllocation.Conflict ->
                    incompatible("La asignación de matrícula es incompatible.")
            }
        }

        is SchoolMovementClassification.InitialMigration -> {
            if (classification.studentId.isBlank()) {
                CandidateResult.Invalid(
                    AnnualEnrollmentConflictCause.INITIAL_MIGRATION_WITHOUT_STUDENT_ID,
                    "La migración inicial requiere un alumno maestro existente."
                )
            } else when (allocation) {
                is PermanentEnrollmentIdAllocation.NewProposal -> {
                    if (allocation.reason != INITIAL_MIGRATION_REASON) {
                        incompatible("Migración inicial requiere una propuesta con motivo $INITIAL_MIGRATION_REASON.")
                    } else {
                        validCandidate(
                            studentId = classification.studentId,
                            normalizedCurp = classification.normalizedCurp,
                            permanentEnrollmentId = allocation.enrollmentId,
                            movement = AnnualEnrollmentMovement.INITIAL_MIGRATION,
                            classificationSchoolYear = classification.activeSchoolYear,
                            allocationSchoolYear = allocation.cycle
                        )
                    }
                }
                is PermanentEnrollmentIdAllocation.ExistingPreserved ->
                    incompatible("La migración inicial requiere una propuesta de matrícula nueva.")
                is PermanentEnrollmentIdAllocation.Conflict ->
                    incompatible("La asignación de matrícula es incompatible.")
            }
        }

        is SchoolMovementClassification.Conflict ->
            incompatible("Una clasificación en conflicto no puede producir un plan.")
    }

    private fun findExistingEnrollmentConflict(
        studentId: String?,
        normalizedCurp: String,
        permanentEnrollmentId: String,
        schoolYear: String,
        sourcePreApplicationFolio: String,
        existingAnnualEnrollments: List<AnnualEnrollmentSnapshot>
    ): AnnualEnrollmentPlanningResult.Conflict? {
        val folioAlreadyUsed = existingAnnualEnrollments.any {
            it.sourcePreApplicationFolio.trim().uppercase() == sourcePreApplicationFolio.uppercase()
        }
        if (folioAlreadyUsed) {
            return conflict(
                AnnualEnrollmentConflictCause.REUSED_PRE_APPLICATION_FOLIO,
                "El folio de pre-solicitud ya fue utilizado para otra inscripción anual."
            )
        }

        val sameSchoolYear = existingAnnualEnrollments.filter { it.schoolYear.trim() == schoolYear }
        if (studentId != null && sameSchoolYear.any { it.studentId == studentId }) {
            return conflict(
                AnnualEnrollmentConflictCause.DUPLICATE_STUDENT_IN_SCHOOL_YEAR,
                "El alumno ya tiene una inscripción para el ciclo escolar solicitado."
            )
        }
        if (sameSchoolYear.any { it.normalizedCurp.trim().uppercase() == normalizedCurp }) {
            return conflict(
                AnnualEnrollmentConflictCause.DUPLICATE_CURP_IN_SCHOOL_YEAR,
                "La CURP ya tiene una inscripción para el ciclo escolar solicitado."
            )
        }

        val enrollmentUsedByDifferentIdentity = sameSchoolYear.any {
            it.permanentEnrollmentId.trim() == permanentEnrollmentId &&
                !representsSameIdentity(it, studentId, normalizedCurp)
        }
        if (enrollmentUsedByDifferentIdentity) {
            return conflict(
                AnnualEnrollmentConflictCause.ENROLLMENT_ID_USED_BY_DIFFERENT_IDENTITY,
                "La matrícula permanente está asociada a otra identidad en el mismo ciclo escolar."
            )
        }
        return null
    }

    private fun representsSameIdentity(
        snapshot: AnnualEnrollmentSnapshot,
        studentId: String?,
        normalizedCurp: String
    ): Boolean = if (studentId != null) {
        snapshot.studentId == studentId
    } else {
        snapshot.normalizedCurp.trim().uppercase() == normalizedCurp
    }

    private fun placementRequirement(
        movement: AnnualEnrollmentMovement,
        previousGroup: String?,
        requestedGrade: Int
    ): GroupPlacementRequirement {
        if (movement == AnnualEnrollmentMovement.NEW_ENTRY) {
            return GroupPlacementRequirement.AssignmentRequired
        }
        val normalizedPreviousGroup = previousGroup
            ?.filterNot { it.isWhitespace() }
            ?.uppercase()
            ?.takeIf(validPreviousGroupPattern::matches)
            ?: return GroupPlacementRequirement.AssignmentRequired
        return GroupPlacementRequirement.ContinuityDecisionRequired(
            previousGroup = normalizedPreviousGroup,
            suggestedGroup = "$requestedGrade${normalizedPreviousGroup.last()}"
        )
    }

    private fun isValidSchoolYear(schoolYear: String): Boolean {
        val match = schoolYearPattern.matchEntire(schoolYear) ?: return false
        val firstYear = match.groupValues[1].toIntOrNull() ?: return false
        val secondYear = match.groupValues[2].toIntOrNull() ?: return false
        return secondYear == firstYear + 1
    }

    private fun allocationConflictMessage(allocation: PermanentEnrollmentIdAllocation.Conflict): String =
        when (allocation) {
            is PermanentEnrollmentIdAllocation.Conflict.Integrity -> allocation.message
            is PermanentEnrollmentIdAllocation.Conflict.ConflictResult -> allocation.message
            is PermanentEnrollmentIdAllocation.Conflict.Exhausted -> allocation.message
        }

    private fun validCandidate(
        studentId: String?,
        normalizedCurp: String,
        permanentEnrollmentId: String,
        movement: AnnualEnrollmentMovement,
        classificationSchoolYear: String,
        allocationSchoolYear: String
    ): CandidateResult.Valid = CandidateResult.Valid(
        studentId = studentId,
        normalizedCurp = normalizedCurp,
        permanentEnrollmentId = permanentEnrollmentId,
        movement = movement,
        classificationSchoolYear = classificationSchoolYear,
        allocationSchoolYear = allocationSchoolYear
    )

    private fun incompatible(message: String): CandidateResult.Invalid = CandidateResult.Invalid(
        AnnualEnrollmentConflictCause.INCOMPATIBLE_CLASSIFICATION_AND_ALLOCATION,
        message
    )

    private fun conflict(
        cause: AnnualEnrollmentConflictCause,
        message: String
    ): AnnualEnrollmentPlanningResult.Conflict = AnnualEnrollmentPlanningResult.Conflict(cause, message)

    private sealed class CandidateResult {
        data class Valid(
            val studentId: String?,
            val normalizedCurp: String,
            val permanentEnrollmentId: String,
            val movement: AnnualEnrollmentMovement,
            val classificationSchoolYear: String,
            val allocationSchoolYear: String
        ) : CandidateResult()

        data class Invalid(
            val cause: AnnualEnrollmentConflictCause,
            val message: String
        ) : CandidateResult()
    }
}
