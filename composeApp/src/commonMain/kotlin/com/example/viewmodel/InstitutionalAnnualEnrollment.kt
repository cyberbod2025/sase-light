package com.example.viewmodel

import com.example.data.enrollment.AnnualEnrollmentFlowResult
import com.example.data.enrollment.AnnualEnrollmentRecord
import com.example.data.presolicitud.PreApplication
import com.example.data.presolicitud.PreApplicationStatus
import com.example.data.presolicitud.ReadinessStatus

enum class InstitutionalEnrollmentGuardCause {
    PRE_APPLICATION_NOT_FOUND,
    AMBIGUOUS_FOLIO,
    NOT_ACCEPTED,
    NOT_READY,
    PENDING_REQUIREMENTS,
    SOURCE_MISMATCH
}

enum class PreApplicationSynchronizationCause {
    PRE_APPLICATION_NOT_FOUND,
    AMBIGUOUS_FOLIO,
    IDENTITY_CHANGED,
    STATUS_CHANGED,
    READINESS_CHANGED,
    PREVIOUSLY_UNSYNCHRONIZED,
    CAS_FAILED
}

sealed interface InstitutionalAnnualEnrollmentResult {
    data class Completed(
        val annualResult: AnnualEnrollmentFlowResult.Completed
    ) : InstitutionalAnnualEnrollmentResult

    data class NeedsDecision(
        val annualResult: AnnualEnrollmentFlowResult.NeedsDecision
    ) : InstitutionalAnnualEnrollmentResult

    data class AlreadyCompleted(
        val annualResult: AnnualEnrollmentFlowResult.AlreadyCompleted
    ) : InstitutionalAnnualEnrollmentResult

    data class GuardRejected(
        val cause: InstitutionalEnrollmentGuardCause,
        val message: String
    ) : InstitutionalAnnualEnrollmentResult

    data class AnnualConflict(
        val annualResult: AnnualEnrollmentFlowResult.Conflict
    ) : InstitutionalAnnualEnrollmentResult

    data class SynchronizationIncomplete(
        val annualResult: AnnualEnrollmentFlowResult,
        val annualEnrollment: AnnualEnrollmentRecord?,
        val cause: PreApplicationSynchronizationCause,
        val message: String
    ) : InstitutionalAnnualEnrollmentResult
}

internal sealed interface PreApplicationConversionResult {
    data class Converted(val preApplication: PreApplication) : PreApplicationConversionResult
    data class AlreadyConverted(val preApplication: PreApplication) : PreApplicationConversionResult
    data class Incomplete(val cause: PreApplicationSynchronizationCause) : PreApplicationConversionResult
}

internal fun synchronizePreApplicationConversion(
    source: PreApplication,
    readState: () -> List<PreApplication>,
    compareAndSet: (List<PreApplication>, List<PreApplication>) -> Boolean
): PreApplicationConversionResult {
    repeat(2) {
        val currentState = readState()
        val matches = currentState.indices.filter { index ->
            currentState[index].folio.canonical() == source.folio.canonical()
        }
        if (matches.isEmpty()) {
            return PreApplicationConversionResult.Incomplete(
                PreApplicationSynchronizationCause.PRE_APPLICATION_NOT_FOUND
            )
        }
        if (matches.size > 1) {
            return PreApplicationConversionResult.Incomplete(
                PreApplicationSynchronizationCause.AMBIGUOUS_FOLIO
            )
        }

        val index = matches.single()
        val current = currentState[index]
        if (!current.hasSameInstitutionalIdentity(source)) {
            return PreApplicationConversionResult.Incomplete(
                PreApplicationSynchronizationCause.IDENTITY_CHANGED
            )
        }
        if (current.status != PreApplicationStatus.ACEPTADA) {
            return PreApplicationConversionResult.Incomplete(
                PreApplicationSynchronizationCause.STATUS_CHANGED
            )
        }
        if (current.readinessStatus == ReadinessStatus.CONVERTED) {
            return PreApplicationConversionResult.AlreadyConverted(current)
        }
        if (current.readinessStatus != ReadinessStatus.READY) {
            return PreApplicationConversionResult.Incomplete(
                PreApplicationSynchronizationCause.READINESS_CHANGED
            )
        }

        val updated = current.copy(readinessStatus = ReadinessStatus.CONVERTED)
        val updatedState = currentState.toMutableList().apply { this[index] = updated }
        if (compareAndSet(currentState, updatedState)) {
            return PreApplicationConversionResult.Converted(updated)
        }
    }

    return PreApplicationConversionResult.Incomplete(PreApplicationSynchronizationCause.CAS_FAILED)
}

private fun PreApplication.hasSameInstitutionalIdentity(other: PreApplication): Boolean =
    folio.canonical() == other.folio.canonical() &&
        alumnoCurp.identityCanonical() == other.alumnoCurp.identityCanonical() &&
        alumnoNombreCompleto.trim() == other.alumnoNombreCompleto.trim() &&
        cicloEscolar.trim() == other.cicloEscolar.trim() &&
        gradoSolicitado == other.gradoSolicitado &&
        tramite.movementCanonical() == other.tramite.movementCanonical()

private fun String.canonical(): String = trim().uppercase()

private fun String.identityCanonical(): String = filterNot(Char::isWhitespace).uppercase()

private fun String.movementCanonical(): String = trim().uppercase().replace('Ó', 'O')
