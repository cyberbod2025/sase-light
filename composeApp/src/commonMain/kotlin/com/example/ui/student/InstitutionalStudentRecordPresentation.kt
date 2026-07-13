package com.example.ui.student

import com.example.data.InstitutionalRecordDataQuality
import com.example.data.InstitutionalRecordField
import com.example.data.InstitutionalRecordIdentityConflict
import com.example.data.InstitutionalRecordKeyError
import com.example.data.InstitutionalRecordWarning
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.InstitutionalStudentRecord
import com.example.data.InstitutionalStudentRecordResolution
import com.example.data.Student
import com.example.data.resolveInstitutionalStudentRecord
import com.example.data.enrollment.AnnualEnrollmentInitialStatus
import com.example.data.enrollment.AnnualEnrollmentMovement
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.ReadinessStatus

internal sealed interface InstitutionalStudentRecordPresentation {
    data class Content(
        val fullName: String,
        val curp: String,
        val fields: List<InstitutionalRecordPresentationField>,
        val warnings: List<String>
    ) : InstitutionalStudentRecordPresentation

    data class Terminal(
        val title: String,
        val message: String
    ) : InstitutionalStudentRecordPresentation
}

internal data class InstitutionalRecordPresentationField(
    val label: String,
    val value: String,
    val quality: InstitutionalRecordDataQuality
)

internal fun resolveInstitutionalStudentRecordForRoute(
    studentId: String,
    institutionalKey: InstitutionalStudentRecordKey?,
    students: List<Student>,
    annualEnrollments: List<AnnualEnrollmentRecord>,
    preApplications: List<PreApplication>
): InstitutionalStudentRecordResolution {
    if (institutionalKey != null) {
        return resolveInstitutionalStudentRecord(
            institutionalKey,
            students,
            annualEnrollments,
            preApplications
        )
    }

    val studentMatches = students.filter { it.id == studentId }
    if (studentMatches.isEmpty()) {
        return InstitutionalStudentRecordResolution.StudentNotFound(studentId)
    }
    if (studentMatches.size > 1) {
        return InstitutionalStudentRecordResolution.AmbiguousStudent(studentId, studentMatches.size)
    }

    val annualMatches = annualEnrollments.filter { it.studentId == studentId }
    val unresolvedKey = InstitutionalStudentRecordKey(studentId = studentId)
    if (annualMatches.isEmpty()) {
        return InstitutionalStudentRecordResolution.AnnualEnrollmentNotFound(unresolvedKey)
    }
    if (annualMatches.size > 1) {
        return InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment(
            unresolvedKey,
            annualMatches.size
        )
    }

    val annual = annualMatches.single()
    return resolveInstitutionalStudentRecord(
        key = InstitutionalStudentRecordKey(
            studentId = annual.studentId,
            schoolYear = annual.schoolYear,
            sourcePreApplicationFolio = annual.sourcePreApplicationFolio,
            enrollmentId = annual.permanentEnrollmentId
        ),
        students = students,
        annualEnrollments = annualEnrollments,
        preApplications = preApplications
    )
}

internal fun institutionalStudentRecordPresentation(
    resolution: InstitutionalStudentRecordResolution
): InstitutionalStudentRecordPresentation = when (resolution) {
    is InstitutionalStudentRecordResolution.Resolved -> resolution.record.toPresentation()
    is InstitutionalStudentRecordResolution.StudentNotFound -> terminal(
        "Estudiante no encontrado",
        "No existe una identidad institucional para ${resolution.studentId}."
    )
    is InstitutionalStudentRecordResolution.AmbiguousStudent -> terminal(
        "Identidad estudiantil ambigua",
        "Se encontraron ${resolution.matches} estudiantes con el mismo identificador."
    )
    is InstitutionalStudentRecordResolution.AnnualEnrollmentNotFound -> terminal(
        "Anualidad no encontrada",
        "No existe una inscripción anual para el ciclo y folio solicitados."
    )
    is InstitutionalStudentRecordResolution.AmbiguousAnnualEnrollment -> terminal(
        "Anualidad ambigua",
        if (resolution.key.schoolYear.isNullOrBlank() ||
            resolution.key.sourcePreApplicationFolio.isNullOrBlank()
        ) {
            "Se encontraron ${resolution.matches} anualidades para la identidad; selecciona ciclo y folio."
        } else {
            "Se encontraron ${resolution.matches} anualidades para el mismo contexto."
        }
    )
    is InstitutionalStudentRecordResolution.AmbiguousPreApplication -> terminal(
        "Pre-solicitud ambigua",
        "El folio ${resolution.folio} aparece ${resolution.matches} veces."
    )
    is InstitutionalStudentRecordResolution.IdentityConflict -> terminal(
        "Conflicto de identidad",
        resolution.conflicts.joinToString(", ") { it.label() }
    )
    is InstitutionalStudentRecordResolution.InvalidResolutionKey -> terminal(
        "Clave institucional inválida",
        resolution.errors.joinToString(", ") { it.label() }
    )
}

private fun InstitutionalStudentRecord.toPresentation(): InstitutionalStudentRecordPresentation.Content =
    InstitutionalStudentRecordPresentation.Content(
        fullName = fullName.ifBlank { "Nombre no disponible" },
        curp = curp.ifBlank { "No disponible" },
        fields = listOf(
            enrollmentId.presentation("Matrícula"),
            schoolYear.presentation("Ciclo escolar"),
            grade.presentation("Grado") { "$it°" },
            group.presentation("Grupo", pendingValue = "Pendiente de asignación"),
            annualStatus.presentation("Estado anual") { it.label() },
            movement.presentation("Movimiento") { it.label() },
            InstitutionalRecordPresentationField(
                label = "Folio de pre-solicitud",
                value = preApplicationFolio,
                quality = InstitutionalRecordDataQuality.CONFIRMED
            ),
            readinessStatus.presentation("Readiness") { it.label() },
            address.presentation("Domicilio"),
            householdPhone.presentation("Teléfono del hogar")
        ),
        warnings = warnings.map { it.label() }.sorted()
    )

private fun <T> InstitutionalRecordField<T>.presentation(
    label: String,
    pendingValue: String = "Pendiente",
    formatter: (T) -> String = { it.toString() }
): InstitutionalRecordPresentationField {
    val visibleValue = value?.let(formatter) ?: when (quality) {
        InstitutionalRecordDataQuality.PENDING -> pendingValue
        InstitutionalRecordDataQuality.UNAVAILABLE -> "No disponible"
        InstitutionalRecordDataQuality.INCONSISTENT -> "Dato inconsistente"
        InstitutionalRecordDataQuality.CONFIRMED -> "No disponible"
    }
    return InstitutionalRecordPresentationField(label, visibleValue, quality)
}

private fun AnnualEnrollmentInitialStatus.label(): String = when (this) {
    AnnualEnrollmentInitialStatus.PENDING_GROUP_ASSIGNMENT -> "Pendiente de asignación de grupo"
    AnnualEnrollmentInitialStatus.PENDING_GROUP_CONTINUITY_DECISION -> "Pendiente de decisión de continuidad"
}

private fun AnnualEnrollmentMovement.label(): String = when (this) {
    AnnualEnrollmentMovement.NEW_ENTRY -> "Nuevo ingreso"
    AnnualEnrollmentMovement.RE_ENROLLMENT -> "Reinscripción"
    AnnualEnrollmentMovement.INITIAL_MIGRATION -> "Migración inicial"
}

private fun ReadinessStatus.label(): String = when (this) {
    ReadinessStatus.PENDING -> "Pendiente"
    ReadinessStatus.BLOCKED -> "Bloqueada"
    ReadinessStatus.READY -> "Lista para alta"
    ReadinessStatus.CONVERTED -> "Convertida"
}

private fun InstitutionalRecordWarning.label(): String = when (this) {
    InstitutionalRecordWarning.PRE_APPLICATION_SYNCHRONIZATION_INCOMPLETE ->
        "La sincronización con la pre-solicitud está incompleta."
    InstitutionalRecordWarning.PRE_APPLICATION_NOT_FOUND ->
        "No se encontró la pre-solicitud vinculada."
    InstitutionalRecordWarning.MISSING_STUDENT_PRE_APPLICATION_TRACEABILITY ->
        "Falta trazabilidad del folio en la identidad estudiantil."
    InstitutionalRecordWarning.MISSING_CURP_TRACEABILITY ->
        "Falta evidencia de CURP en una fuente institucional."
    InstitutionalRecordWarning.MISSING_ENROLLMENT_ID_TRACEABILITY ->
        "Falta evidencia de matrícula en una fuente institucional."
    InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_INCOMPLETE ->
        "El contexto de la pre-solicitud está incompleto."
    InstitutionalRecordWarning.PRE_APPLICATION_CONTEXT_MISMATCH ->
        "El contexto de la pre-solicitud es contradictorio."
}

private fun InstitutionalRecordIdentityConflict.label(): String = when (this) {
    InstitutionalRecordIdentityConflict.STUDENT_ID_MISMATCH -> "identificador estudiantil contradictorio"
    InstitutionalRecordIdentityConflict.CURP_MISMATCH -> "CURP contradictoria"
    InstitutionalRecordIdentityConflict.ENROLLMENT_ID_MISMATCH -> "matrícula contradictoria"
    InstitutionalRecordIdentityConflict.PRE_APPLICATION_FOLIO_MISMATCH -> "folio contradictorio"
}

private fun InstitutionalRecordKeyError.label(): String = when (this) {
    InstitutionalRecordKeyError.MISSING_STUDENT_ID -> "falta studentId"
    InstitutionalRecordKeyError.MISSING_SCHOOL_YEAR -> "falta ciclo escolar"
    InstitutionalRecordKeyError.MISSING_PRE_APPLICATION_FOLIO -> "falta folio"
    InstitutionalRecordKeyError.MISSING_ENROLLMENT_ID -> "falta matrícula"
}

private fun terminal(title: String, message: String) =
    InstitutionalStudentRecordPresentation.Terminal(title, message)
