package com.example.data.enrollment

import com.example.data.Student

/**
 * Resultado de solo lectura para clasificar el movimiento escolar.
 *
 * Contrato futuro de matrícula (no implementado aquí): formato S310-000001-D,
 * permanente, consecutivo institucional, con dígito verificador y sin CURP,
 * año o grupo. Una matrícula institucional previa válida siempre se preserva.
 */
sealed class SchoolMovementClassification {
    data class NewEntry(
        val normalizedCurp: String,
        val activeSchoolYear: String
    ) : SchoolMovementClassification()

    data class ReEnrollment(
        val studentId: String,
        val normalizedCurp: String,
        val existingEnrollmentId: String,
        val activeSchoolYear: String
    ) : SchoolMovementClassification()

    data class InitialMigration(
        val studentId: String,
        val normalizedCurp: String,
        val activeSchoolYear: String
    ) : SchoolMovementClassification()

    data class Conflict(
        val reason: SchoolMovementConflictReason,
        val message: String
    ) : SchoolMovementClassification()
}

enum class SchoolMovementConflictReason {
    INVALID_CURP,
    UNKNOWN_APPLICATION_TYPE,
    NEW_ENTRY_ALREADY_EXISTS,
    RE_ENROLLMENT_NOT_FOUND,
    DUPLICATE_MASTER_STUDENTS,
    CONTRADICTORY_INSTITUTIONAL_DATA
}

object SchoolMovementClassifier {
    fun classify(
        declaredApplicationType: String,
        curp: String,
        activeSchoolYear: String,
        masterStudents: List<Student>
    ): SchoolMovementClassification {
        val normalizedCurp = normalizeCurp(curp)
        if (normalizedCurp.isBlank()) {
            return conflict(
                SchoolMovementConflictReason.INVALID_CURP,
                "La CURP es obligatoria para clasificar el movimiento escolar."
            )
        }

        val applicationType = parseApplicationType(declaredApplicationType)
            ?: return conflict(
                SchoolMovementConflictReason.UNKNOWN_APPLICATION_TYPE,
                "El tipo de trámite no corresponde a nuevo ingreso o reinscripción."
            )

        val matches = masterStudents.filter { normalizeCurp(it.curp) == normalizedCurp }
        if (matches.size > 1) {
            return conflict(
                SchoolMovementConflictReason.DUPLICATE_MASTER_STUDENTS,
                "Existen varios alumnos maestros con la misma CURP; se requiere revisión institucional."
            )
        }

        val existingStudent = matches.singleOrNull()
        return when (applicationType) {
            DeclaredApplicationType.NEW_ENTRY -> {
                if (existingStudent == null) {
                    SchoolMovementClassification.NewEntry(normalizedCurp, activeSchoolYear)
                } else {
                    conflict(
                        SchoolMovementConflictReason.NEW_ENTRY_ALREADY_EXISTS,
                        "La CURP declarada como nuevo ingreso ya existe en el padrón maestro."
                    )
                }
            }

            DeclaredApplicationType.RE_ENROLLMENT -> {
                if (existingStudent == null) {
                    conflict(
                        SchoolMovementConflictReason.RE_ENROLLMENT_NOT_FOUND,
                        "La CURP declarada para reinscripción no existe en el padrón maestro."
                    )
                } else if (existingStudent.id.isBlank()) {
                    conflict(
                        SchoolMovementConflictReason.CONTRADICTORY_INSTITUTIONAL_DATA,
                        "El registro institucional encontrado no tiene un identificador válido."
                    )
                } else if (existingStudent.enrollmentId.isBlank()) {
                    SchoolMovementClassification.InitialMigration(
                        studentId = existingStudent.id,
                        normalizedCurp = normalizedCurp,
                        activeSchoolYear = activeSchoolYear
                    )
                } else {
                    SchoolMovementClassification.ReEnrollment(
                        studentId = existingStudent.id,
                        normalizedCurp = normalizedCurp,
                        existingEnrollmentId = existingStudent.enrollmentId,
                        activeSchoolYear = activeSchoolYear
                    )
                }
            }
        }
    }

    private fun normalizeCurp(value: String): String = value.trim().uppercase()

    private fun parseApplicationType(value: String): DeclaredApplicationType? =
        when (value.trim().uppercase().replace('Ó', 'O')) {
            "NUEVO INGRESO" -> DeclaredApplicationType.NEW_ENTRY
            "REINSCRIPCION" -> DeclaredApplicationType.RE_ENROLLMENT
            else -> null
        }

    private fun conflict(
        reason: SchoolMovementConflictReason,
        message: String
    ): SchoolMovementClassification.Conflict =
        SchoolMovementClassification.Conflict(reason, message)

    private enum class DeclaredApplicationType {
        NEW_ENTRY,
        RE_ENROLLMENT
    }
}
