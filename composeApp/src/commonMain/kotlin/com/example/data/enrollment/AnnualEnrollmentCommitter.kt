package com.example.data.enrollment

data class MasterStudentIdentitySnapshot(
    val studentId: String,
    val normalizedCurp: String,
    val permanentEnrollmentId: String,
    val sourcePreApplicationFolio: String?
)

data class AnnualEnrollmentRecord(
    val studentId: String,
    val normalizedCurp: String,
    val permanentEnrollmentId: String,
    val schoolYear: String,
    val sourcePreApplicationFolio: String,
    val movement: AnnualEnrollmentMovement,
    val requestedGrade: Int,
    val groupPlacementRequirement: GroupPlacementRequirement,
    val status: AnnualEnrollmentInitialStatus,
    val assignedGroup: String? = null
)

enum class EnrollmentAuditAction {
    MASTER_STUDENT_CREATED,
    MASTER_STUDENT_REUSED,
    MASTER_STUDENT_INITIAL_MIGRATION,
    ANNUAL_ENROLLMENT_CREATED,
    ANNUAL_ENROLLMENT_ALREADY_APPLIED
}

data class EnrollmentAuditEntry(
    val action: EnrollmentAuditAction,
    val studentId: String,
    val schoolYear: String,
    val sourcePreApplicationFolio: String,
    val actor: String,
    val occurredAt: String,
    val detail: String
)

data class EnrollmentRegistryState(
    val masterStudents: List<MasterStudentIdentitySnapshot>,
    val annualEnrollments: List<AnnualEnrollmentRecord>,
    val auditEntries: List<EnrollmentAuditEntry>
)

enum class MasterStudentMutation {
    CREATED,
    REUSED,
    MIGRATED
}

sealed class AnnualEnrollmentCommitResult {
    data class Applied(
        val state: EnrollmentRegistryState,
        val masterStudent: MasterStudentIdentitySnapshot,
        val annualEnrollment: AnnualEnrollmentRecord,
        val masterStudentMutation: MasterStudentMutation,
        val addedAuditEntries: List<EnrollmentAuditEntry>
    ) : AnnualEnrollmentCommitResult()

    data class AlreadyApplied(
        val state: EnrollmentRegistryState,
        val annualEnrollment: AnnualEnrollmentRecord
    ) : AnnualEnrollmentCommitResult()

    data class Conflict(
        val cause: AnnualEnrollmentCommitConflictCause,
        val message: String
    ) : AnnualEnrollmentCommitResult()
}

enum class AnnualEnrollmentCommitConflictCause {
    PLANNING_CONFLICT,
    EMPTY_ACTOR,
    EMPTY_OCCURRED_AT,
    MISSING_NEW_STUDENT_ID,
    UNEXPECTED_NEW_STUDENT_ID,
    DUPLICATE_STUDENT_ID,
    DUPLICATE_MASTER_CURP,
    DUPLICATE_PERMANENT_ENROLLMENT_ID,
    MASTER_STUDENT_NOT_FOUND,
    MULTIPLE_MASTER_STUDENTS_FOUND,
    MASTER_CURP_MISMATCH,
    MASTER_ENROLLMENT_ID_MISMATCH,
    INITIAL_MIGRATION_ALREADY_HAS_ENROLLMENT_ID,
    ANNUAL_ENROLLMENT_ALREADY_EXISTS_WITH_DIFFERENT_DATA,
    PRE_APPLICATION_FOLIO_REUSED_WITH_DIFFERENT_DATA,
    ANNUAL_ENROLLMENT_IDENTITY_CONFLICT,
    INVALID_DRAFT_STATE,
    INCOMPATIBLE_MOVEMENT
}

object AnnualEnrollmentCommitter {
    fun applyAnnualEnrollmentPlan(
        state: EnrollmentRegistryState,
        planningResult: AnnualEnrollmentPlanningResult,
        newStudentId: String?,
        actor: String,
        occurredAt: String
    ): AnnualEnrollmentCommitResult {
        if (planningResult is AnnualEnrollmentPlanningResult.Conflict) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.PLANNING_CONFLICT,
                "No se puede aplicar un plan de inscripción en conflicto: ${planningResult.message}"
            )
        }
        planningResult as AnnualEnrollmentPlanningResult.Ready
        val draft = planningResult.draft

        val normalizedActor = actor.trim()
        if (normalizedActor.isBlank()) {
            return conflict(AnnualEnrollmentCommitConflictCause.EMPTY_ACTOR, "El actor de auditoría es obligatorio.")
        }
        val normalizedOccurredAt = occurredAt.trim()
        if (normalizedOccurredAt.isBlank()) {
            return conflict(AnnualEnrollmentCommitConflictCause.EMPTY_OCCURRED_AT, "La fecha de auditoría es obligatoria.")
        }

        validateDraft(draft)?.let { return it }
        val intendedIdResult = intendedStudentId(draft, newStudentId)
            ?: return conflict(
                AnnualEnrollmentCommitConflictCause.INVALID_DRAFT_STATE,
                "No fue posible determinar la identidad de la inscripción."
            )
        val intendedStudentId = when (intendedIdResult) {
            is IntendedStudentIdResult.Invalid -> return conflict(intendedIdResult.cause, intendedIdResult.message)
            is IntendedStudentIdResult.Valid -> intendedIdResult.studentId
        }

        val intendedRecord = draft.toRecord(intendedStudentId)
        val existingByFolio = state.annualEnrollments.filter {
            it.sourcePreApplicationFolio.trim().uppercase() == draft.sourcePreApplicationFolio.trim().uppercase()
        }
        if (existingByFolio.size > 1) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.ANNUAL_ENROLLMENT_IDENTITY_CONFLICT,
                "Existen varias inscripciones asociadas al mismo folio."
            )
        }
        existingByFolio.singleOrNull()?.let { existing ->
            if (existing == intendedRecord && masterIdentitySupports(state, intendedRecord)) {
                return AnnualEnrollmentCommitResult.AlreadyApplied(state, existing)
            }
            return conflict(
                AnnualEnrollmentCommitConflictCause.PRE_APPLICATION_FOLIO_REUSED_WITH_DIFFERENT_DATA,
                "El folio ya fue aplicado con datos diferentes."
            )
        }

        val sameStudentAndYear = state.annualEnrollments.filter {
            it.studentId == intendedStudentId && it.schoolYear == draft.schoolYear
        }
        if (sameStudentAndYear.isNotEmpty()) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.ANNUAL_ENROLLMENT_ALREADY_EXISTS_WITH_DIFFERENT_DATA,
                "El alumno ya tiene una inscripción distinta para el ciclo escolar."
            )
        }
        val identityConflict = state.annualEnrollments.any {
            it.schoolYear == draft.schoolYear &&
                (it.normalizedCurp.normalized() == draft.normalizedCurp.normalized() ||
                    it.permanentEnrollmentId.trim() == draft.permanentEnrollmentId.trim()) &&
                it.studentId != intendedStudentId
        }
        if (identityConflict) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.ANNUAL_ENROLLMENT_IDENTITY_CONFLICT,
                "La CURP o matrícula ya está vinculada a otra identidad en el ciclo escolar."
            )
        }

        val masterApplication = when (draft.movement) {
            AnnualEnrollmentMovement.NEW_ENTRY -> applyNewEntry(state, draft, intendedStudentId)
            AnnualEnrollmentMovement.RE_ENROLLMENT -> applyReEnrollment(state, draft, intendedStudentId)
            AnnualEnrollmentMovement.INITIAL_MIGRATION -> applyInitialMigration(state, draft, intendedStudentId)
        }
        if (masterApplication is MasterApplicationResult.Invalid) {
            return conflict(masterApplication.cause, masterApplication.message)
        }
        masterApplication as MasterApplicationResult.Valid

        val enrollmentAudit = EnrollmentAuditEntry(
            action = EnrollmentAuditAction.ANNUAL_ENROLLMENT_CREATED,
            studentId = intendedStudentId,
            schoolYear = draft.schoolYear,
            sourcePreApplicationFolio = draft.sourcePreApplicationFolio,
            actor = normalizedActor,
            occurredAt = normalizedOccurredAt,
            detail = "Inscripción anual ${draft.movement} creada para ${draft.schoolYear}."
        )
        val masterAudit = EnrollmentAuditEntry(
            action = masterApplication.auditAction,
            studentId = intendedStudentId,
            schoolYear = draft.schoolYear,
            sourcePreApplicationFolio = draft.sourcePreApplicationFolio,
            actor = normalizedActor,
            occurredAt = normalizedOccurredAt,
            detail = masterApplication.auditDetail
        )
        val addedAudits = listOf(masterAudit, enrollmentAudit)
        val newState = EnrollmentRegistryState(
            masterStudents = masterApplication.masterStudents.toList(),
            annualEnrollments = state.annualEnrollments + intendedRecord,
            auditEntries = state.auditEntries + addedAudits
        )
        return AnnualEnrollmentCommitResult.Applied(
            state = newState,
            masterStudent = masterApplication.masterStudent,
            annualEnrollment = intendedRecord,
            masterStudentMutation = masterApplication.mutation,
            addedAuditEntries = addedAudits
        )
    }

    private fun validateDraft(draft: AnnualEnrollmentDraft): AnnualEnrollmentCommitResult.Conflict? {
        if (draft.normalizedCurp.normalized().isBlank() || draft.permanentEnrollmentId.trim().isBlank() ||
            draft.schoolYear.trim().isBlank() || draft.sourcePreApplicationFolio.trim().isBlank() ||
            draft.requestedGrade !in 1..3
        ) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.INVALID_DRAFT_STATE,
                "El borrador de inscripción no contiene todos los datos mínimos válidos."
            )
        }
        if (draft.movement == AnnualEnrollmentMovement.NEW_ENTRY && draft.studentId != null) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.INCOMPATIBLE_MOVEMENT,
                "Nuevo ingreso no debe contener un alumno maestro existente."
            )
        }
        if (draft.movement != AnnualEnrollmentMovement.NEW_ENTRY && draft.studentId.isNullOrBlank()) {
            return conflict(
                AnnualEnrollmentCommitConflictCause.INCOMPATIBLE_MOVEMENT,
                "Reinscripción y migración requieren un alumno maestro existente."
            )
        }
        return null
    }

    private fun intendedStudentId(
        draft: AnnualEnrollmentDraft,
        newStudentId: String?
    ): IntendedStudentIdResult? = when (draft.movement) {
        AnnualEnrollmentMovement.NEW_ENTRY -> {
            val normalizedNewId = newStudentId?.trim().orEmpty()
            if (normalizedNewId.isBlank()) IntendedStudentIdResult.Invalid(
                AnnualEnrollmentCommitConflictCause.MISSING_NEW_STUDENT_ID,
                "Nuevo ingreso requiere un identificador de alumno maestro."
            ) else IntendedStudentIdResult.Valid(normalizedNewId)
        }
        AnnualEnrollmentMovement.RE_ENROLLMENT,
        AnnualEnrollmentMovement.INITIAL_MIGRATION -> {
            if (!newStudentId.isNullOrBlank()) IntendedStudentIdResult.Invalid(
                AnnualEnrollmentCommitConflictCause.UNEXPECTED_NEW_STUDENT_ID,
                "No debe sustituirse el identificador del alumno existente."
            ) else draft.studentId?.let(IntendedStudentIdResult::Valid)
        }
    }

    private fun applyNewEntry(
        state: EnrollmentRegistryState,
        draft: AnnualEnrollmentDraft,
        studentId: String
    ): MasterApplicationResult {
        if (state.masterStudents.any { it.studentId == studentId }) {
            return invalid(AnnualEnrollmentCommitConflictCause.DUPLICATE_STUDENT_ID, "El identificador ya está utilizado.")
        }
        if (state.masterStudents.any { it.normalizedCurp.normalized() == draft.normalizedCurp.normalized() }) {
            return invalid(AnnualEnrollmentCommitConflictCause.DUPLICATE_MASTER_CURP, "La CURP ya existe en el padrón maestro.")
        }
        if (state.masterStudents.any { it.permanentEnrollmentId.trim() == draft.permanentEnrollmentId.trim() }) {
            return invalid(AnnualEnrollmentCommitConflictCause.DUPLICATE_PERMANENT_ENROLLMENT_ID, "La matrícula ya pertenece a otro alumno.")
        }
        val created = MasterStudentIdentitySnapshot(
            studentId = studentId,
            normalizedCurp = draft.normalizedCurp.normalized(),
            permanentEnrollmentId = draft.permanentEnrollmentId.trim(),
            sourcePreApplicationFolio = draft.sourcePreApplicationFolio.trim()
        )
        return valid(
            state.masterStudents + created,
            created,
            MasterStudentMutation.CREATED,
            EnrollmentAuditAction.MASTER_STUDENT_CREATED,
            "Identidad maestra mínima creada desde pre-solicitud."
        )
    }

    private fun applyReEnrollment(
        state: EnrollmentRegistryState,
        draft: AnnualEnrollmentDraft,
        studentId: String
    ): MasterApplicationResult {
        val lookup = findSingleMaster(state, studentId, draft.normalizedCurp)
        if (lookup is MasterLookup.Invalid) return invalid(lookup.cause, lookup.message)
        lookup as MasterLookup.Valid
        val master = lookup.master
        if (master.normalizedCurp.normalized() != draft.normalizedCurp.normalized()) {
            return invalid(AnnualEnrollmentCommitConflictCause.MASTER_CURP_MISMATCH, "La CURP no coincide con el alumno maestro.")
        }
        if (master.permanentEnrollmentId.trim() != draft.permanentEnrollmentId.trim()) {
            return invalid(AnnualEnrollmentCommitConflictCause.MASTER_ENROLLMENT_ID_MISMATCH, "La matrícula no coincide con el alumno maestro.")
        }
        return valid(
            state.masterStudents,
            master,
            MasterStudentMutation.REUSED,
            EnrollmentAuditAction.MASTER_STUDENT_REUSED,
            "Identidad y matrícula permanentes reutilizadas para reinscripción."
        )
    }

    private fun applyInitialMigration(
        state: EnrollmentRegistryState,
        draft: AnnualEnrollmentDraft,
        studentId: String
    ): MasterApplicationResult {
        val lookup = findSingleMaster(state, studentId, draft.normalizedCurp)
        if (lookup is MasterLookup.Invalid) return invalid(lookup.cause, lookup.message)
        lookup as MasterLookup.Valid
        val master = lookup.master
        if (master.normalizedCurp.normalized() != draft.normalizedCurp.normalized()) {
            return invalid(AnnualEnrollmentCommitConflictCause.MASTER_CURP_MISMATCH, "La CURP no coincide con el alumno histórico.")
        }
        if (master.permanentEnrollmentId.isNotBlank()) {
            return invalid(
                AnnualEnrollmentCommitConflictCause.INITIAL_MIGRATION_ALREADY_HAS_ENROLLMENT_ID,
                "El alumno histórico ya tiene matrícula permanente."
            )
        }
        if (state.masterStudents.any {
                it.studentId != studentId && it.permanentEnrollmentId.trim() == draft.permanentEnrollmentId.trim()
            }
        ) {
            return invalid(
                AnnualEnrollmentCommitConflictCause.DUPLICATE_PERMANENT_ENROLLMENT_ID,
                "La matrícula propuesta ya pertenece a otra identidad."
            )
        }
        val migrated = master.copy(permanentEnrollmentId = draft.permanentEnrollmentId.trim())
        val updated = state.masterStudents.map { if (it.studentId == studentId) migrated else it }
        return valid(
            updated,
            migrated,
            MasterStudentMutation.MIGRATED,
            EnrollmentAuditAction.MASTER_STUDENT_INITIAL_MIGRATION,
            "Matrícula permanente incorporada durante migración inicial."
        )
    }

    private fun findSingleMaster(
        state: EnrollmentRegistryState,
        studentId: String,
        normalizedCurp: String
    ): MasterLookup {
        val idMatches = state.masterStudents.filter { it.studentId == studentId }
        if (idMatches.isEmpty()) {
            return MasterLookup.Invalid(
                AnnualEnrollmentCommitConflictCause.MASTER_STUDENT_NOT_FOUND,
                "No se encontró el alumno maestro."
            )
        }
        val curpMatches = state.masterStudents.filter { it.normalizedCurp.normalized() == normalizedCurp.normalized() }
        if (idMatches.size != 1 || curpMatches.size > 1) {
            return MasterLookup.Invalid(
                AnnualEnrollmentCommitConflictCause.MULTIPLE_MASTER_STUDENTS_FOUND,
                "Existen identidades maestras ambiguas para el alumno."
            )
        }
        return MasterLookup.Valid(idMatches.single())
    }

    private fun masterIdentitySupports(state: EnrollmentRegistryState, record: AnnualEnrollmentRecord): Boolean =
        state.masterStudents.count {
            it.studentId == record.studentId &&
                it.normalizedCurp.normalized() == record.normalizedCurp.normalized() &&
                it.permanentEnrollmentId.trim() == record.permanentEnrollmentId.trim()
        } == 1

    private fun AnnualEnrollmentDraft.toRecord(studentId: String) = AnnualEnrollmentRecord(
        studentId = studentId,
        normalizedCurp = normalizedCurp.normalized(),
        permanentEnrollmentId = permanentEnrollmentId.trim(),
        schoolYear = schoolYear.trim(),
        sourcePreApplicationFolio = sourcePreApplicationFolio.trim(),
        movement = movement,
        requestedGrade = requestedGrade,
        groupPlacementRequirement = groupPlacementRequirement,
        status = initialStatus,
        assignedGroup = null
    )

    private fun String.normalized(): String = trim().uppercase()

    private fun conflict(cause: AnnualEnrollmentCommitConflictCause, message: String) =
        AnnualEnrollmentCommitResult.Conflict(cause, message)

    private fun invalid(cause: AnnualEnrollmentCommitConflictCause, message: String) =
        MasterApplicationResult.Invalid(cause, message)

    private fun valid(
        masterStudents: List<MasterStudentIdentitySnapshot>,
        masterStudent: MasterStudentIdentitySnapshot,
        mutation: MasterStudentMutation,
        auditAction: EnrollmentAuditAction,
        auditDetail: String
    ) = MasterApplicationResult.Valid(masterStudents, masterStudent, mutation, auditAction, auditDetail)

    private sealed class IntendedStudentIdResult {
        data class Valid(val studentId: String) : IntendedStudentIdResult()
        data class Invalid(val cause: AnnualEnrollmentCommitConflictCause, val message: String) : IntendedStudentIdResult()
    }

    private sealed class MasterLookup {
        data class Valid(val master: MasterStudentIdentitySnapshot) : MasterLookup()
        data class Invalid(val cause: AnnualEnrollmentCommitConflictCause, val message: String) : MasterLookup()
    }

    private sealed class MasterApplicationResult {
        data class Valid(
            val masterStudents: List<MasterStudentIdentitySnapshot>,
            val masterStudent: MasterStudentIdentitySnapshot,
            val mutation: MasterStudentMutation,
            val auditAction: EnrollmentAuditAction,
            val auditDetail: String
        ) : MasterApplicationResult()

        data class Invalid(
            val cause: AnnualEnrollmentCommitConflictCause,
            val message: String
        ) : MasterApplicationResult()
    }
}
