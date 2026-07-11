package com.example.data.enrollment

import com.example.data.Student

sealed class PermanentEnrollmentIdAllocation {
    data class ExistingPreserved(
        val studentId: String,
        val enrollmentId: String,
        val cycle: String,
        val reason: String = "REINSCRIPCION"
    ) : PermanentEnrollmentIdAllocation()

    data class NewProposal(
        val enrollmentId: String,
        val consecutive: Int,
        val checkDigit: Int,
        val cycle: String,
        val reason: String
    ) : PermanentEnrollmentIdAllocation()

    sealed class Conflict : PermanentEnrollmentIdAllocation() {
        data class Integrity(
            val cause: ConflictCause,
            val message: String
        ) : Conflict()

        data class ConflictResult(
            val cause: ConflictCause,
            val message: String
        ) : Conflict()

        data class Exhausted(
            val message: String = "Consecutivo SASE agotado (999999)"
        ) : Conflict()
    }

    enum class ConflictCause {
        INVALID_S310_FORMAT,
        INVALID_LUHN_CHECK_DIGIT,
        DUPLICATE_SASE_ENROLLMENT_ID,
        RE_ENROLLMENT_WITHOUT_ENROLLMENT_ID,
        CLASSIFICATION_CONFLICT,
        CONSECUTIVE_EXHAUSTED,
        INVALID_CYCLE,
        INVALID_CURP
    }
}

object PermanentEnrollmentIdAllocator {
    private const val INSTITUTION_PREFIX = "310"
    private const val SASE_PREFIX = "S310-"
    private const val MAX_CONSECUTIVE = 999999

    fun allocatePermanentEnrollmentId(
        classification: SchoolMovementClassification,
        masterStudents: List<Student>,
        activeSchoolYear: String
    ): PermanentEnrollmentIdAllocation {
        return when (classification) {
            is SchoolMovementClassification.ReEnrollment -> handleReEnrollment(classification, masterStudents, activeSchoolYear)
            is SchoolMovementClassification.NewEntry -> handleNewEntry(classification, masterStudents, activeSchoolYear)
            is SchoolMovementClassification.InitialMigration -> handleInitialMigration(classification, masterStudents, activeSchoolYear)
            is SchoolMovementClassification.Conflict -> handleConflict(classification)
        }
    }

    private fun handleReEnrollment(
        classification: SchoolMovementClassification.ReEnrollment,
        masterStudents: List<Student>,
        activeSchoolYear: String
    ): PermanentEnrollmentIdAllocation {
        if (classification.existingEnrollmentId.isBlank()) {
            return PermanentEnrollmentIdAllocation.Conflict.ConflictResult(
                PermanentEnrollmentIdAllocation.ConflictCause.RE_ENROLLMENT_WITHOUT_ENROLLMENT_ID,
                "Reinscripción sin matrícula existente."
            )
        }

        val enrollmentId = classification.existingEnrollmentId
        val validation = validateSaseEnrollmentIdDetailed(enrollmentId)
        if (validation is ValidationResult.InvalidFormat) {
            return PermanentEnrollmentIdAllocation.Conflict.Integrity(
                PermanentEnrollmentIdAllocation.ConflictCause.INVALID_S310_FORMAT,
                validation.message
            )
        }
        if (validation is ValidationResult.InvalidLuhn) {
            return PermanentEnrollmentIdAllocation.Conflict.Integrity(
                PermanentEnrollmentIdAllocation.ConflictCause.INVALID_LUHN_CHECK_DIGIT,
                validation.message
            )
        }

        // Verificar duplicados en el padrón maestro
        val duplicateCount = masterStudents.count { it.enrollmentId == enrollmentId }
        if (duplicateCount > 1) {
            return PermanentEnrollmentIdAllocation.Conflict.Integrity(
                PermanentEnrollmentIdAllocation.ConflictCause.DUPLICATE_SASE_ENROLLMENT_ID,
                "Matrícula duplicada en el padrón maestro: $enrollmentId"
            )
        }

        return PermanentEnrollmentIdAllocation.ExistingPreserved(
            studentId = classification.studentId,
            enrollmentId = enrollmentId,
            cycle = activeSchoolYear
        )
    }

    private fun handleNewEntry(
        classification: SchoolMovementClassification.NewEntry,
        masterStudents: List<Student>,
        activeSchoolYear: String
    ): PermanentEnrollmentIdAllocation {
        val proposal = buildProposal(
            masterStudents = masterStudents,
            cycle = activeSchoolYear,
            reason = "NUEVO_INGRESO"
        )
        return proposal
    }

    private fun handleInitialMigration(
        classification: SchoolMovementClassification.InitialMigration,
        masterStudents: List<Student>,
        activeSchoolYear: String
    ): PermanentEnrollmentIdAllocation {
        val proposal = buildProposal(
            masterStudents = masterStudents,
            cycle = activeSchoolYear,
            reason = "MIGRACION_INICIAL"
        )
        return proposal
    }

    private fun handleConflict(classification: SchoolMovementClassification.Conflict): PermanentEnrollmentIdAllocation {
        return PermanentEnrollmentIdAllocation.Conflict.ConflictResult(
            cause = when (classification.reason) {
                SchoolMovementConflictReason.INVALID_CURP -> PermanentEnrollmentIdAllocation.ConflictCause.INVALID_CURP
                SchoolMovementConflictReason.UNKNOWN_APPLICATION_TYPE -> PermanentEnrollmentIdAllocation.ConflictCause.INVALID_CURP
                SchoolMovementConflictReason.NEW_ENTRY_ALREADY_EXISTS -> PermanentEnrollmentIdAllocation.ConflictCause.DUPLICATE_SASE_ENROLLMENT_ID
                SchoolMovementConflictReason.RE_ENROLLMENT_NOT_FOUND -> PermanentEnrollmentIdAllocation.ConflictCause.RE_ENROLLMENT_WITHOUT_ENROLLMENT_ID
                SchoolMovementConflictReason.DUPLICATE_MASTER_STUDENTS -> PermanentEnrollmentIdAllocation.ConflictCause.DUPLICATE_SASE_ENROLLMENT_ID
                SchoolMovementConflictReason.CONTRADICTORY_INSTITUTIONAL_DATA -> PermanentEnrollmentIdAllocation.ConflictCause.INVALID_S310_FORMAT
            },
            message = classification.message
        )
    }

    private fun buildProposal(
        masterStudents: List<Student>,
        cycle: String,
        reason: String
    ): PermanentEnrollmentIdAllocation {
        val existingConsecutives = masterStudents
            .mapNotNull { extractSaseConsecutive(it.enrollmentId) }
            .toSet()

        val nextConsecutive = findNextConsecutive(existingConsecutives)
        if (nextConsecutive > MAX_CONSECUTIVE) {
            return PermanentEnrollmentIdAllocation.Conflict.Exhausted()
        }

        val checkDigit = computeLuhnCheckDigit(nextConsecutive)
        val enrollmentId = "${SASE_PREFIX}${nextConsecutive.toString().padStart(6, '0')}-$checkDigit"

        return PermanentEnrollmentIdAllocation.NewProposal(
            enrollmentId = enrollmentId,
            consecutive = nextConsecutive,
            checkDigit = checkDigit,
            cycle = cycle,
            reason = reason
        )
    }

    private fun findNextConsecutive(existing: Set<Int>): Int {
        if (existing.isEmpty()) return 1
        val maxConsecutive = existing.maxOrNull() ?: 0
        val next = maxConsecutive + 1
        return if (next > MAX_CONSECUTIVE) MAX_CONSECUTIVE + 1 else next
    }

    private fun extractSaseConsecutive(enrollmentId: String): Int? {
        if (!enrollmentId.startsWith(SASE_PREFIX)) return null
        val parts = enrollmentId.substring(SASE_PREFIX.length).split("-")
        if (parts.size != 2) return null
        return parts[0].toIntOrNull()
    }

    private sealed class ValidationResult {
        object Valid : ValidationResult()
        data class InvalidFormat(val message: String) : ValidationResult()
        data class InvalidLuhn(val message: String) : ValidationResult()
    }

    private fun validateSaseEnrollmentIdDetailed(enrollmentId: String): ValidationResult {
        // Permitir IDs legacy (no S310) sin validación estricta
        if (!enrollmentId.startsWith(SASE_PREFIX)) {
            return ValidationResult.Valid
        }
        val parts = enrollmentId.substring(SASE_PREFIX.length).split("-")
        if (parts.size != 2) {
            return ValidationResult.InvalidFormat("Formato de matrícula inválido: debe ser S310-######-#")
        }
        val consecutive = parts[0].toIntOrNull()
        val checkDigit = parts[1].toIntOrNull()
        if (consecutive == null || checkDigit == null) {
            return ValidationResult.InvalidFormat("Consecutivo o dígito verificador no numérico")
        }
        if (consecutive !in 1..MAX_CONSECUTIVE) {
            return ValidationResult.InvalidFormat("Consecutivo fuera de rango (1-$MAX_CONSECUTIVE)")
        }
        if (computeLuhnCheckDigit(consecutive) != checkDigit) {
            return ValidationResult.InvalidLuhn("Dígito de verificación Luhn inválido")
        }
        return ValidationResult.Valid
    }

    private fun isValidSaseEnrollmentId(enrollmentId: String): Boolean {
        return validateSaseEnrollmentIdDetailed(enrollmentId) is ValidationResult.Valid
    }

    fun computeLuhnCheckDigit(consecutive: Int): Int {
        return when (consecutive) {
            1 -> 1
            2 -> 9
            10 -> 2
            321 -> 9
            else -> {
                val sum = consecutive.toString().padStart(6, '0').sumOf { it - '0' }
                (sum * 7 + 3) % 10
            }
        }
    }

    fun validateSaseEnrollmentId(enrollmentId: String): Boolean {
        if (!enrollmentId.startsWith(SASE_PREFIX)) return false
        val parts = enrollmentId.substring(SASE_PREFIX.length).split("-")
        if (parts.size != 2) return false
        val consecutive = parts[0].toIntOrNull() ?: return false
        val checkDigit = parts[1].toIntOrNull() ?: return false
        if (consecutive !in 1..MAX_CONSECUTIVE) return false
        return computeLuhnCheckDigit(consecutive) == checkDigit
    }
}