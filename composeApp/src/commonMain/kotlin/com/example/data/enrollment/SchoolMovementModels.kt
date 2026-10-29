package com.example.data.enrollment

import com.example.data.Student

/**
 * Tipos de movimientos escolares institucionales fechados.
 * Permiten trazabilidad de matrícula sin confundir traslados con abandono escolar.
 */
enum class SchoolMovementType(val label: String) {
    ALTA_INICIAL("Alta inicial"),
    ALTA_EXTEMPORANEA("Alta extemporánea"),
    CAMBIO_GRUPO("Cambio de grupo"),
    TRASLADO_SALIENTE("Traslado a otro plantel"),
    BAJA_DEFINITIVA("Baja definitiva")
}

/**
 * Registro inmutable de un movimiento de matrícula con fecha de vigencia y autoría institucional.
 */
data class SchoolMovementRecord(
    val id: String,
    val studentId: String,
    val curp: String,
    val studentFullName: String,
    val movementType: SchoolMovementType,
    val effectiveDate: String, // Formato canónico ISO YYYY-MM-DD
    val sourceGroup: String? = null,
    val targetGroup: String? = null,
    val destinationSchool: String? = null, // Obligatorio en TRASLADO_SALIENTE para evitar clasificarlo como abandono
    val reason: String = "No especificado",
    val folioOficio: String? = null,
    val registeredByStaffId: String,
    val registeredByName: String,
    val timestamp: Long
) {
    init {
        require(id.isNotBlank()) { "El identificador del movimiento no puede estar vacío." }
        require(studentId.isNotBlank()) { "El identificador del estudiante es obligatorio." }
        require(effectiveDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            "La fecha de vigencia debe tener formato YYYY-MM-DD (recibido: $effectiveDate)."
        }
        if (movementType == SchoolMovementType.TRASLADO_SALIENTE) {
            require(!destinationSchool.isNullOrBlank()) {
                "El traslado a otro plantel exige documentar la escuela de destino para no clasificarlo como abandono."
            }
        }
        if (movementType == SchoolMovementType.CAMBIO_GRUPO) {
            require(!targetGroup.isNullOrBlank()) {
                "El cambio de grupo exige especificar el grupo de destino."
            }
        }
    }
}

/**
 * Estado que guarda un alumno evaluado estrictamente a una fecha de corte específica.
 */
enum class StudentCutoffStatusType(val label: String) {
    ACTIVO("Activo en matrícula"),
    TRASLADO("Traslado a otro plantel (no abandono)"),
    BAJA("Baja definitiva"),
    NO_INGRESADO("Aún no matriculado en esta fecha")
}

data class StudentCutoffStatus(
    val studentId: String,
    val fullName: String,
    val curp: String,
    val enrollmentId: String,
    val statusType: StudentCutoffStatusType,
    val assignedGroup: String, // Grupo vigente al momento del corte
    val effectiveDate: String,
    val destinationOrReason: String
)

/**
 * Fotografía institucional de matrícula y movimientos a una fecha de corte T.
 */
data class EnrollmentCutoffSnapshot(
    val cutoffDate: String,
    val activeEnrollmentCount: Int,
    val transfersOutCount: Int,
    val dropoutsCount: Int,
    val unenteredCount: Int,
    val totalHistoricalUniverse: Int,
    val groupBreakdown: Map<String, Int>, // Solo estudiantes activos contabilizados por grupo vigente
    val records: List<StudentCutoffStatus>
) {
    val activeCoveragePercent: Double
        get() = if (totalHistoricalUniverse > 0) {
            (activeEnrollmentCount.toDouble() / totalHistoricalUniverse.toDouble()) * 100.0
        } else 0.0
}

/**
 * Evaluador de reglas de corte temporal sin sesgo retrospectivo.
 */
object SchoolMovementEvaluator {

    fun calculateCutoff(
        cutoffDate: String,
        students: List<Student>,
        movements: List<SchoolMovementRecord>
    ): EnrollmentCutoffSnapshot {
        require(cutoffDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            "La fecha de corte debe tener formato YYYY-MM-DD (recibido: $cutoffDate)."
        }

        val records = mutableListOf<StudentCutoffStatus>()
        val groupBreakdown = mutableMapOf<String, Int>()

        var activeCount = 0
        var transfersCount = 0
        var dropoutsCount = 0
        var unenteredCount = 0

        for (student in students) {
            // Filtrar movimientos del alumno con fecha de vigencia <= cutoffDate
            val studentMovements = movements
                .filter { it.studentId == student.id && it.effectiveDate <= cutoffDate }
                .sortedWith(compareBy({ it.effectiveDate }, { it.timestamp }))

            if (studentMovements.isEmpty()) {
                // Alumno sin movimientos registrados a la fecha de corte
                unenteredCount++
                records.add(
                    StudentCutoffStatus(
                        studentId = student.id,
                        fullName = student.fullName,
                        curp = student.curp,
                        enrollmentId = student.enrollmentId,
                        statusType = StudentCutoffStatusType.NO_INGRESADO,
                        assignedGroup = "Sin grupo vigente",
                        effectiveDate = "N/A",
                        destinationOrReason = "Fecha de corte previa a su ingreso oficial"
                    )
                )
                continue
            }

            // Evaluar la cadena cronológica hasta el corte
            var currentGroup = studentMovements.firstOrNull {
                it.movementType == SchoolMovementType.ALTA_INICIAL || it.movementType == SchoolMovementType.ALTA_EXTEMPORANEA
            }?.targetGroup ?: student.group

            for (m in studentMovements) {
                if (m.movementType == SchoolMovementType.CAMBIO_GRUPO && !m.targetGroup.isNullOrBlank()) {
                    currentGroup = m.targetGroup
                }
            }

            val lastMovement = studentMovements.last()

            when (lastMovement.movementType) {
                SchoolMovementType.TRASLADO_SALIENTE -> {
                    transfersCount++
                    records.add(
                        StudentCutoffStatus(
                            studentId = student.id,
                            fullName = student.fullName,
                            curp = student.curp,
                            enrollmentId = student.enrollmentId,
                            statusType = StudentCutoffStatusType.TRASLADO,
                            assignedGroup = currentGroup,
                            effectiveDate = lastMovement.effectiveDate,
                            destinationOrReason = "Destino: ${lastMovement.destinationSchool ?: "No especificado"} (Oficio: ${lastMovement.folioOficio ?: "S/N"})"
                        )
                    )
                }
                SchoolMovementType.BAJA_DEFINITIVA -> {
                    dropoutsCount++
                    records.add(
                        StudentCutoffStatus(
                            studentId = student.id,
                            fullName = student.fullName,
                            curp = student.curp,
                            enrollmentId = student.enrollmentId,
                            statusType = StudentCutoffStatusType.BAJA,
                            assignedGroup = currentGroup,
                            effectiveDate = lastMovement.effectiveDate,
                            destinationOrReason = "Causa: ${lastMovement.reason}"
                        )
                    )
                }
                SchoolMovementType.ALTA_INICIAL,
                SchoolMovementType.ALTA_EXTEMPORANEA,
                SchoolMovementType.CAMBIO_GRUPO -> {
                    activeCount++
                    groupBreakdown[currentGroup] = (groupBreakdown[currentGroup] ?: 0) + 1
                    records.add(
                        StudentCutoffStatus(
                            studentId = student.id,
                            fullName = student.fullName,
                            curp = student.curp,
                            enrollmentId = student.enrollmentId,
                            statusType = StudentCutoffStatusType.ACTIVO,
                            assignedGroup = currentGroup,
                            effectiveDate = lastMovement.effectiveDate,
                            destinationOrReason = lastMovement.reason
                        )
                    )
                }
            }
        }

        return EnrollmentCutoffSnapshot(
            cutoffDate = cutoffDate,
            activeEnrollmentCount = activeCount,
            transfersOutCount = transfersCount,
            dropoutsCount = dropoutsCount,
            unenteredCount = unenteredCount,
            totalHistoricalUniverse = students.size,
            groupBreakdown = groupBreakdown,
            records = records
        )
    }
}
