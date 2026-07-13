package com.example.ui.presolicitud

import com.example.data.InstitutionalStudentRecordKey
import com.example.data.toInstitutionalStudentRecordKey
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.enrollment.EnrollmentFlowMode
import com.example.data.presolicitud.ReadinessStatus
import com.example.viewmodel.InstitutionalAnnualEnrollmentResult

internal data class EnrollmentActionPresentation(
    val showLegacyGroupControls: Boolean,
    val showLegacyStartAction: Boolean,
    val showLegacyConfirmationAction: Boolean,
    val showAnnualV2Action: Boolean,
    val annualV2ActionLabel: String
)

internal fun enrollmentActionPresentation(mode: EnrollmentFlowMode): EnrollmentActionPresentation =
    when (mode) {
        EnrollmentFlowMode.ANNUAL_V2 -> EnrollmentActionPresentation(
            showLegacyGroupControls = false,
            showLegacyStartAction = false,
            showLegacyConfirmationAction = false,
            showAnnualV2Action = true,
            annualV2ActionLabel = "Procesar inscripción anual"
        )
        EnrollmentFlowMode.LEGACY -> EnrollmentActionPresentation(
            showLegacyGroupControls = true,
            showLegacyStartAction = true,
            showLegacyConfirmationAction = true,
            showAnnualV2Action = false,
            annualV2ActionLabel = "Procesar inscripción anual"
        )
    }

internal enum class InstitutionalEnrollmentPanelState {
    INITIAL,
    COMPLETED,
    NEEDS_DECISION,
    REJECTED,
    CONFLICT,
    SYNCHRONIZATION_INCOMPLETE
}

internal data class InstitutionalEnrollmentPanelPresentation(
    val state: InstitutionalEnrollmentPanelState,
    val title: String,
    val isCompleted: Boolean,
    val showInitialGuidance: Boolean,
    val showProcessAction: Boolean
)

internal fun institutionalEnrollmentPanelPresentation(
    readinessStatus: ReadinessStatus,
    result: InstitutionalAnnualEnrollmentResult?
): InstitutionalEnrollmentPanelPresentation {
    val state = when (result) {
        is InstitutionalAnnualEnrollmentResult.Completed,
        is InstitutionalAnnualEnrollmentResult.AlreadyCompleted -> InstitutionalEnrollmentPanelState.COMPLETED
        is InstitutionalAnnualEnrollmentResult.NeedsDecision -> InstitutionalEnrollmentPanelState.NEEDS_DECISION
        is InstitutionalAnnualEnrollmentResult.GuardRejected -> InstitutionalEnrollmentPanelState.REJECTED
        is InstitutionalAnnualEnrollmentResult.AnnualConflict -> InstitutionalEnrollmentPanelState.CONFLICT
        is InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete ->
            InstitutionalEnrollmentPanelState.SYNCHRONIZATION_INCOMPLETE
        null -> if (readinessStatus == ReadinessStatus.CONVERTED) {
            InstitutionalEnrollmentPanelState.COMPLETED
        } else {
            InstitutionalEnrollmentPanelState.INITIAL
        }
    }
    return InstitutionalEnrollmentPanelPresentation(
        state = state,
        title = when (state) {
            InstitutionalEnrollmentPanelState.COMPLETED -> "Alta oficial completada"
            InstitutionalEnrollmentPanelState.NEEDS_DECISION -> "Alta registrada · decisión pendiente"
            InstitutionalEnrollmentPanelState.REJECTED -> "Alta no iniciada"
            InstitutionalEnrollmentPanelState.CONFLICT -> "Alta requiere revisión"
            InstitutionalEnrollmentPanelState.SYNCHRONIZATION_INCOMPLETE -> "Sincronización pendiente"
            InstitutionalEnrollmentPanelState.INITIAL -> "Alta Oficial contextual"
        },
        isCompleted = state == InstitutionalEnrollmentPanelState.COMPLETED,
        showInitialGuidance = state in setOf(
            InstitutionalEnrollmentPanelState.INITIAL,
            InstitutionalEnrollmentPanelState.REJECTED,
            InstitutionalEnrollmentPanelState.CONFLICT
        ),
        showProcessAction = state in setOf(
            InstitutionalEnrollmentPanelState.INITIAL,
            InstitutionalEnrollmentPanelState.REJECTED,
            InstitutionalEnrollmentPanelState.CONFLICT
        )
    )
}

internal fun institutionalEnrollmentMessage(result: InstitutionalAnnualEnrollmentResult): String =
    when (result) {
        is InstitutionalAnnualEnrollmentResult.Completed -> buildString {
            appendLine("Inscripción anual registrada.")
            appendLine("Matrícula: ${result.annualResult.enrollmentId}")
            append("Grupo: pendiente de asignación.")
        }
        is InstitutionalAnnualEnrollmentResult.NeedsDecision -> buildString {
            appendLine("Inscripción anual registrada.")
            appendLine("Matrícula: ${result.annualResult.enrollmentId}")
            append("La asignación de grupo continúa pendiente.")
        }
        is InstitutionalAnnualEnrollmentResult.AlreadyCompleted -> buildString {
            appendLine("La inscripción anual ya estaba registrada.")
            appendLine("Matrícula: ${result.annualResult.enrollmentRecord.permanentEnrollmentId}")
            append("Identidad y matrícula preservadas; no se generaron duplicados.")
        }
        is InstitutionalAnnualEnrollmentResult.GuardRejected ->
            "No se inició la inscripción anual. ${result.message}"
        is InstitutionalAnnualEnrollmentResult.AnnualConflict -> buildString {
            appendLine("No fue posible completar la inscripción anual.")
            appendLine("Etapa: ${result.annualResult.stage}.")
            append("Motivo: ${result.annualResult.message}")
        }
        is InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete ->
            "La anualidad existe, pero la sincronización institucional está incompleta y requiere revisión."
    }

internal data class InstitutionalEnrollmentRecordAction(
    val label: String,
    val key: InstitutionalStudentRecordKey
)

internal fun institutionalEnrollmentRecordAction(
    result: InstitutionalAnnualEnrollmentResult
): InstitutionalEnrollmentRecordAction? {
    val key = when (result) {
        is InstitutionalAnnualEnrollmentResult.Completed ->
            result.annualResult.toInstitutionalStudentRecordKey()
        is InstitutionalAnnualEnrollmentResult.NeedsDecision ->
            result.annualResult.toInstitutionalStudentRecordKey()
        is InstitutionalAnnualEnrollmentResult.AlreadyCompleted ->
            result.annualResult.toInstitutionalStudentRecordKey()
        is InstitutionalAnnualEnrollmentResult.GuardRejected,
        is InstitutionalAnnualEnrollmentResult.AnnualConflict,
        is InstitutionalAnnualEnrollmentResult.SynchronizationIncomplete -> null
    }
    return key
        ?.takeIf {
            it.studentId.isNotBlank() &&
                !it.schoolYear.isNullOrBlank() &&
                !it.sourcePreApplicationFolio.isNullOrBlank() &&
                !it.enrollmentId.isNullOrBlank()
        }
        ?.let { InstitutionalEnrollmentRecordAction("Abrir expediente", it) }
}

internal fun institutionalEnrollmentRecordAction(
    folio: String,
    annualEnrollments: List<AnnualEnrollmentRecord>
): InstitutionalEnrollmentRecordAction? = annualEnrollments
    .singleOrNull { it.sourcePreApplicationFolio.trim().equals(folio.trim(), ignoreCase = true) }
    ?.let { annual ->
        InstitutionalStudentRecordKey(
            studentId = annual.studentId,
            schoolYear = annual.schoolYear,
            sourcePreApplicationFolio = annual.sourcePreApplicationFolio,
            enrollmentId = annual.permanentEnrollmentId
        )
    }
    ?.takeIf { key ->
        key.studentId.isNotBlank() &&
            !key.schoolYear.isNullOrBlank() &&
            !key.sourcePreApplicationFolio.isNullOrBlank() &&
            !key.enrollmentId.isNullOrBlank()
    }
    ?.let { InstitutionalEnrollmentRecordAction("Abrir expediente", it) }
