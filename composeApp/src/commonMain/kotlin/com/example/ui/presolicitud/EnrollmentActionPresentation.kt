package com.example.ui.presolicitud

import com.example.data.enrollment.EnrollmentFlowMode
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
