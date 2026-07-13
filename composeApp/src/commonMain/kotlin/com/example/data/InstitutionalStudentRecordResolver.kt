package com.example.data

import com.example.data.enrollment.AnnualEnrollmentInitialStatus
import com.example.data.enrollment.AnnualEnrollmentMovement
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.enrollment.GroupPlacementRequirement
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.ReadinessStatus

data class InstitutionalStudentRecordKey(
    val studentId: String,
    val schoolYear: String? = null,
    val sourcePreApplicationFolio: String? = null,
    val enrollmentId: String? = null
)

enum class InstitutionalRecordDataQuality {
    CONFIRMED,
    PENDING,
    UNAVAILABLE,
    INCONSISTENT
}

data class InstitutionalRecordField<T>(
    val value: T?,
    val quality: InstitutionalRecordDataQuality
)

enum class InstitutionalRecordWarning {
    PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE,
    PRE_APPLICATION_NOT_FOUND,
    MISSING_STUDENT_PRE_APPLICATION_TRACEABILITY,
    MISSING_CURP_TRACEABILITY,
    MISSING_ENROLLMENT_ID_TRACEABILITY,
    PRE_APPLICATION_CONTEXT_INCOMPLETE,
    PRE_APPLICATION_CONTEXT_MISMATCH
}

enum class InstitutionalRecordIdentityConflict {
    STUDENT_ID_MISMATCH,
    CURP_MISMATCH,
    ENROLLMENT_ID_MISMATCH,
    PRE_APPLICATION_FOLIO_MISMATCH
}

enum class InstitutionalRecordKeyError {
    MISSING_STUDENT_ID,
    MISSING_SCHOOL_YEAR,
    MISSING_PRE_APPLICATION_FOLIO,
    MISSING_ENROLLMENT_ID
}

data class InstitutionalStudentRecord(
    val studentId: String,
    val fullName: String,
    val curp: String,
    val enrollmentId: InstitutionalRecordField<String>,
    val schoolYear: InstitutionalRecordField<String>,
    val grade: InstitutionalRecordField<Int>,
    val group: InstitutionalRecordField<String>,
    val annualStatus: InstitutionalRecordField<AnnualEnrollmentInitialStatus>,
    val movement: InstitutionalRecordField<AnnualEnrollmentMovement>,
    val preApplicationFolio: String,
    val readinessStatus: InstitutionalRecordField<ReadinessStatus>,
    val address: InstitutionalRecordField<String>,
    val householdPhone: InstitutionalRecordField<String>,
    val warnings: Set<InstitutionalRecordWarning>
)

sealed interface InstitutionalStudentRecordResolution {
    data class Resolved(
        val record: InstitutionalStudentRecord
    ) : InstitutionalStudentRecordResolution

    data class StudentNotFound(
        val studentId: String
    ) : InstitutionalStudentRecordResolution

    data class AmbiguousStudent(
        val studentId: String,
        val matches: Int
    ) : InstitutionalStudentRecordResolution

    data class AnnualEnrollmentNotFound(
        val key: InstitutionalStudentRecordKey
    ) : InstitutionalStudentRecordResolution

    data class AmbiguousAnnualEnrollment(
        val key: InstitutionalStudentRecordKey,
        val matches: Int
    ) : InstitutionalStudentRecordResolution

    data class AmbiguousPreApplication(
        val folio: String,
        val matches: Int
    ) : InstitutionalStudentRecordResolution

    data class IdentityConflict(
        val conflicts: Set<InstitutionalRecordIdentityConflict>
    ) : InstitutionalStudentRecordResolution

    data class InvalidResolutionKey(
        val errors: Set<InstitutionalRecordKeyError>
    ) : InstitutionalStudentRecordResolution
}

fun AnnualEnrollmentFlowResult.toInstitutionalStudentRecordKey(): InstitutionalStudentRecordKey? =
    when (this) {
        is AnnualEnrollmentFlowResult.Completed -> InstitutionalStudentRecordKey(
            studentId, schoolYear, folio, enrollmentId
        )
        is AnnualEnrollmentFlowResult.NeedsDecision -> InstitutionalStudentRecordKey(
            studentId, schoolYear, folio, enrollmentId
        )
        is AnnualEnrollmentFlowResult.AlreadyCompleted -> InstitutionalStudentRecordKey(
            enrollmentRecord.studentId,
            enrollmentRecord.schoolYear,
            enrollmentRecord.sourcePreApplicationFolio,
            enrollmentRecord.permanentEnrollmentId
        )
        is AnnualEnrollmentFlowResult.Conflict -> null
    }

fun resolveInstitutionalStudentRecord(
    key: InstitutionalStudentRecordKey,
    students: List<Student>,
    annualEnrollments: List<AnnualEnrollmentRecord>,
    preApplications: List<PreApplication>
): InstitutionalStudentRecordResolution {
    val keyErrors = buildSet {
        if (key.studentId.isBlank()) add(InstitutionalRecordKeyError.MISSING_STUDENT_ID)
        if (key.schoolYear.isNullOrBlank()) add(InstitutionalRecordKeyError.MISSING_SCHOOL_YEAR)
        if (key.sourcePreApplicationFolio.isNullOrBlank()) {
            add(InstitutionalRecordKeyError.MISSING_PRE_APPLICATION_FOLIO)
        }
        if (key.enrollmentId.isNullOrBlank()) add(InstitutionalRecordKeyError.MISSING_ENROLLMENT_ID)
    }
    if (keyErrors.isNotEmpty()) {
        return InstitutionalStudentRecordResolution.InvalidResolutionKey(keyErrors)
    }

    val studentMatches = students.filter { it.id == key.studentId }
    if (studentMatches.isEmpty()) {
        return InstitutionalStudentRecordResolution.StudentNotFound(key.studentId)
    }
    if (studentMatches.size > 1) {
        return InstitutionalStudentRecordResolution.AmbiguousStudent(key.studentId, studentMatches.size)
    }
    val student = studentMatches.single()

    val requestedSchoolYear = key.schoolYear?.trim()?.takeIf { it.isNotEmpty() }
    val requestedFolio = key.sourcePreApplicationFolio?.canonical()?.takeIf { it.isNotEmpty() }
    val requestedEnrollmentId = key.enrollmentId?.canonical()?.takeIf { it.isNotEmpty() }
    val contextualAnnualEnrollments = annualEnrollments.filter { annual ->
        (requestedSchoolYear == null || annual.schoolYear.trim() == requestedSchoolYear) &&
            (requestedFolio == null || annual.sourcePreApplicationFolio.canonical() == requestedFolio)
    }
    val annualMatches = contextualAnnualEnrollments.filter { it.studentId == key.studentId }
    if (annualMatches.isEmpty()) {
        if (requestedSchoolYear != null && requestedFolio != null &&
            contextualAnnualEnrollments.any { it.studentId != key.studentId }
        ) {
            return InstitutionalStudentRecordResolution.IdentityConflict(
                setOf(InstitutionalRecordIdentityConflict.STUDENT_ID_MISMATCH)
            )
        }
        return InstitutionalStudentRecordResolution.AnnualEnrollmentNotFound(key)
    }
    if (annualMatches.size > 1) {
        return InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment(key, annualMatches.size)
    }
    val annual = annualMatches.single()

    val conflicts = buildSet {
        if (annual.studentId != student.id) {
            add(InstitutionalRecordIdentityConflict.STUDENT_ID_MISMATCH)
        }
        val annualCurp = annual.normalizedCurp.identityCanonical()
        val studentCurp = student.curp.identityCanonical()
        if (annualCurp.isNotEmpty() && studentCurp.isNotEmpty() && annualCurp != studentCurp) {
            add(InstitutionalRecordIdentityConflict.CURP_MISMATCH)
        }
        if (requestedEnrollmentId != null && annual.permanentEnrollmentId.isNotBlank() &&
            annual.permanentEnrollmentId.canonical() != requestedEnrollmentId
        ) {
            add(InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH)
        }
        if (student.enrollmentId.isNotBlank() && annual.permanentEnrollmentId.isNotBlank() &&
            annual.permanentEnrollmentId.canonical() != student.enrollmentId.canonical()
        ) {
            add(InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH)
        }
        if (requestedEnrollmentId != null && student.enrollmentId.isNotBlank() &&
            requestedEnrollmentId != student.enrollmentId.canonical()
        ) {
            add(InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH)
        }
        val studentFolio = student.preApplicationFolio?.canonical()?.takeIf { it.isNotEmpty() }
        if (studentFolio != null &&
            studentFolio != annual.sourcePreApplicationFolio.canonical()
        ) {
            add(InstitutionalRecordIdentityConflict.PRE_APPLICATION_FOLIO_MISMATCH)
        }
    }.toMutableSet()

    val preApplicationMatches = preApplications.filter {
        it.folio.canonical() == annual.sourcePreApplicationFolio.canonical()
    }
    if (preApplicationMatches.size > 1) {
        return InstitutionalStudentRecordResolution.AmbiguousPreApplication(
            folio = annual.sourcePreApplicationFolio,
            matches = preApplicationMatches.size
        )
    }
    val preApplication = preApplicationMatches.singleOrNull()
    if (preApplication != null) {
        val preApplicationCurp = preApplication.alumnoCurp.identityCanonical()
        val studentCurp = student.curp.identityCanonical()
        val annualCurp = annual.normalizedCurp.identityCanonical()
        if (preApplicationCurp.isNotEmpty() && studentCurp.isNotEmpty() && preApplicationCurp != studentCurp) {
            conflicts += InstitutionalRecordIdentityConflict.CURP_MISMATCH
        }
        if (preApplicationCurp.isNotEmpty() && annualCurp.isNotEmpty() && preApplicationCurp != annualCurp) {
            conflicts += InstitutionalRecordIdentityConflict.CURP_MISMATCH
        }
    }
    if (conflicts.isNotEmpty()) {
        return InstitutionalStudentRecordResolution.IdentityConflict(conflicts)
    }

    val preApplicationContextIncomplete = preApplication != null && (
        preApplication.cicloEscolar.isBlank() ||
            preApplication.gradoSolicitado !in 1..3 ||
            preApplication.alumnoNombreCompleto.isBlank() ||
            preApplication.tramite.isBlank()
        )
    val preApplicationContextMismatch = preApplication != null && (
        (preApplication.cicloEscolar.isNotBlank() &&
            preApplication.cicloEscolar.trim() != annual.schoolYear.trim()) ||
            (preApplication.gradoSolicitado in 1..3 &&
                preApplication.gradoSolicitado != annual.requestedGrade) ||
            (preApplication.alumnoNombreCompleto.isNotBlank() &&
                preApplication.alumnoNombreCompleto.identityCanonical() != student.fullName.identityCanonical()) ||
            (preApplication.tramite.isNotBlank() &&
                !annual.movement.matchesDeclared(preApplication.tramite))
        )

    val warnings = buildSet {
        if (student.preApplicationFolio?.trim().isNullOrEmpty()) {
            add(InstitutionalRecordWarning.MISSING_STUDENT_PRE_APPLICATION_TRACEABILITY)
        }
        if (student.curp.identityCanonical().isEmpty() ||
            annual.normalizedCurp.identityCanonical().isEmpty() ||
            (preApplication != null && preApplication.alumnoCurp.identityCanonical().isEmpty())
        ) {
            add(InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY)
        }
        if (student.enrollmentId.isBlank() || annual.permanentEnrollmentId.isBlank()) {
            add(InstitutionalRecordWarning.MISSING_ENROLLMENT_ID_TRACEABILITY)
        }
        if (preApplication == null) {
            add(InstitutionalRecordWarning.PRE_APPLICATION_NOT_FOUND)
        } else if (preApplication.readinessStatus != ReadinessStatus.CONVERTED) {
            add(InstitutionalRecordWarning.PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE)
        }
        if (preApplicationContextMismatch) {
            add(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH)
        }
        if (preApplicationContextIncomplete) {
            add(InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE)
        }
    }

    return InstitutionalStudentRecordResolution.Resolved(
        InstitutionalStudentRecord(
            studentId = student.id,
            fullName = student.fullName,
            curp = student.curp,
            enrollmentId = annual.permanentEnrollmentId.toAvailableField(),
            schoolYear = annual.schoolYear.toAvailableField(),
            grade = annual.requestedGrade.toGradeField(),
            group = annual.groupField(),
            annualStatus = annual.status.confirmedField(),
            movement = annual.movement.confirmedField(),
            preApplicationFolio = annual.sourcePreApplicationFolio,
            readinessStatus = preApplication.toReadinessField(
                contextMismatch = preApplicationContextMismatch,
                contextIncomplete = preApplicationContextIncomplete
            ),
            address = preApplication?.alumnoDomicilio.toContactField(
                preApplication,
                preApplicationContextMismatch,
                preApplicationContextIncomplete
            ),
            householdPhone = preApplication?.alumnoTelefonoCasa.toContactField(
                preApplication,
                preApplicationContextMismatch,
                preApplicationContextIncomplete
            ),
            warnings = warnings
        )
    )
}

private fun AnnualEnrollmentRecord.groupField(): InstitutionalRecordField<String> {
    val assigned = assignedGroup?.trim()?.takeIf { it.isNotEmpty() }
    if (assigned != null) {
        // The annual model only exposes pending placement states; confirmed/unavailable
        // group quality requires a future explicit expansion of that model.
        return InstitutionalRecordField(assigned, InstitutionalRecordDataQuality.INCONSISTENT)
    }

    val pending = when (groupPlacementRequirement) {
        GroupPlacementRequirement.AssignmentRequired ->
            status == AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT
        is GroupPlacementRequirement.ContinuityDecisionRequired ->
            status == AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION
    }
    return if (pending) {
        InstitutionalRecordField(null, InstitutionalRecordDataQuality.PENDING)
    } else {
        InstitutionalRecordField(null, InstitutionalRecordDataQuality.INCONSISTENT)
    }
}

private fun PreApplication?.toReadinessField(
    contextMismatch: Boolean,
    contextIncomplete: Boolean
): InstitutionalRecordField<ReadinessStatus> = when {
    this == null -> unavailableField()
    contextMismatch -> InstitutionalRecordField(readinessStatus, InstitutionalRecordDataQuality.INCONSISTENT)
    contextIncomplete -> InstitutionalRecordField(readinessStatus, InstitutionalRecordDataQuality.PENDING)
    readinessStatus == ReadinessStatus.CONVERTED -> readinessStatus.confirmedField()
    else -> InstitutionalRecordField(readinessStatus, InstitutionalRecordDataQuality.PENDING)
}

private fun Int.toGradeField(): InstitutionalRecordField<Int> =
    if (this in 1..3) confirmedField()
    else InstitutionalRecordField(this, InstitutionalRecordDataQuality.INCONSISTENT)

private fun String?.toAvailableField(
    inconsistent: Boolean = false
): InstitutionalRecordField<String> {
    val cleanValue = this?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        cleanValue == null -> unavailableField()
        inconsistent -> InstitutionalRecordField(cleanValue, InstitutionalRecordDataQuality.INCONSISTENT)
        else -> cleanValue.confirmedField()
    }
}

private fun String?.toContactField(
    preApplication: PreApplication?,
    contextMismatch: Boolean,
    contextIncomplete: Boolean
): InstitutionalRecordField<String> {
    val cleanValue = this?.trim()?.takeIf { it.isNotEmpty() }
    val quality = when {
        contextMismatch -> InstitutionalRecordDataQuality.INCONSISTENT
        cleanValue == null -> InstitutionalRecordDataQuality.UNAVAILABLE
        contextIncomplete || preApplication?.readinessStatus != ReadinessStatus.CONVERTED ->
            InstitutionalRecordDataQuality.PENDING
        else -> InstitutionalRecordDataQuality.CONFIRMED
    }
    return InstitutionalRecordField(cleanValue, quality)
}

private fun <T> T.confirmedField(): InstitutionalRecordField<T> =
    InstitutionalRecordField(this, InstitutionalRecordDataQuality.CONFIRMED)

private fun <T> unavailableField(): InstitutionalRecordField<T> =
    InstitutionalRecordField(null, InstitutionalRecordDataQuality.UNAVAILABLE)

private fun String.canonical(): String = trim().uppercase()

private fun String.identityCanonical(): String = filterNot(Char::isWhitespace).uppercase()

private fun AnnualEnrollmentMovement.matchesDeclared(declaredMovement: String): Boolean =
    when (declaredMovement.normalizedWords().replace('Ó', 'O')) {
        "NUEVO INGRESO" -> this == AnnualEnrollmentMovement.NEW_ENTRY
        "REINSCRIPCION" -> this == AnnualEnrollmentMovement.RE_ENROLLMENT ||
            this == AnnualEnrollmentMovement.INITIAL_MIGRATION
        else -> false
    }

private fun String.normalizedWords(): String =
    trim().replace(Regex("\\s+"), " ").uppercase()
