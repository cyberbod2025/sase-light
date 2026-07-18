package com.example.viewmodel

enum class CurpConflictSource {
    PRE_APPLICATION,
    MASTER_STUDENT,
    OFFICIAL_STUDENT,
}

data class CurpConflictCandidate(
    val source: CurpConflictSource,
    val curp: String,
    val relatedFolio: String? = null,
    val masterStudentId: String? = null,
)

data class CurpConflict(
    val normalizedCurp: String,
    val matches: List<CurpConflictCandidate>,
) {
    val masterStudentId: String?
        get() = matches.firstNotNullOfOrNull { candidate ->
            candidate.masterStudentId?.trim()?.takeIf { it.isNotEmpty() }
        }

    val isNavigable: Boolean
        get() = masterStudentId != null

    val institutionalMessage: String
        get() = when {
            matches.any { it.source == CurpConflictSource.MASTER_STUDENT } ->
                "CURP ya registrada en el padrón maestro."
            matches.any { it.source == CurpConflictSource.OFFICIAL_STUDENT } ->
                "CURP ya registrada en alta oficial."
            else ->
                "CURP ya registrada en otra pre-solicitud."
        }
}

sealed interface CurpValidationResult {
    data class Valid(val normalizedCurp: String) : CurpValidationResult

    data class Invalid(val message: String) : CurpValidationResult
}

sealed interface CurpCorrectionResult {
    data class Updated(val normalizedCurp: String) : CurpCorrectionResult

    data class InvalidFormat(val message: String) : CurpCorrectionResult

    data class Duplicate(val conflict: CurpConflict) : CurpCorrectionResult

    data object NotFound : CurpCorrectionResult

    data object AlreadyConverted : CurpCorrectionResult

    data object InstitutionalIdentityLocked : CurpCorrectionResult
}

object CurpCorrectionPolicy {
    const val INVALID_FORMAT_MESSAGE =
        "La CURP debe tener 18 caracteres y cumplir el formato institucional de la demo."

    private val curpPattern = Regex("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$")

    fun normalize(rawCurp: String): String = rawCurp.trim().uppercase()

    fun validate(rawCurp: String): CurpValidationResult {
        val normalizedCurp = normalize(rawCurp)
        return if (curpPattern.matches(normalizedCurp)) {
            CurpValidationResult.Valid(normalizedCurp)
        } else {
            CurpValidationResult.Invalid(INVALID_FORMAT_MESSAGE)
        }
    }

    fun resolveConflict(
        currentFolio: String,
        rawCurp: String,
        candidates: Iterable<CurpConflictCandidate>,
    ): CurpConflict? {
        val normalizedCurp = normalize(rawCurp)
        val normalizedCurrentFolio = normalizeIdentifier(currentFolio)
        val matches = candidates
            .filter { candidate -> normalize(candidate.curp) == normalizedCurp }
            .filterNot { candidate ->
                candidate.relatedFolio?.let(::normalizeIdentifier) == normalizedCurrentFolio
            }
            .toList()

        return matches
            .takeIf { it.isNotEmpty() }
            ?.let { CurpConflict(normalizedCurp = normalizedCurp, matches = it) }
    }

    private fun normalizeIdentifier(value: String): String = value.trim().uppercase()
}
