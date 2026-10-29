package com.example.data.enrollment

import com.example.data.Student

/**
 * Tipos de movimientos escolares fechados con vigencia temporal.
 */
enum class SchoolMovementType(val displayName: String) {
    INSCRIPCION_INICIAL("Inscripción Inicial"),
    ALTA_EXTEMPORANEA("Alta Extemporánea"),
    REINCORPORACION("Reincorporación"),
    CAMBIO_DE_GRUPO("Cambio de Grupo"),
    TRASLADO_SALIENTE("Traslado Saliente"),
    BAJA_ADMINISTRATIVA("Baja Administrativa"),
    ABANDONO_CONFIRMADO("Abandono Escolar Confirmado"),
    PENDIENTE_ACLARACION("Pendiente de Aclaración")
}

/**
 * Categorías formales de desvinculación escolar.
 * Evita catalogar automáticamente una baja o traslado como deserción escolar.
 */
enum class DepartureCategory(val label: String) {
    TRASLADO("Traslado a otra escuela"),
    BAJA_ADMINISTRATIVA("Baja por trámite administrativo"),
    ABANDONO_CONFIRMADO("Deserción / Abandono escolar confirmado"),
    PENDIENTE_ACLARACION("En proceso de verificación / No localizado")
}

/**
 * Estatus formal del alumno en una fecha de corte específica.
 */
enum class StudentCutoffStatus(val label: String, val isActive: Boolean) {
    ACTIVO("Activo en matrícula", true),
    NO_INGRESADO("No ingresado aún a la fecha", false),
    TRASLADADO("Traslado a otra institución", false),
    BAJA_ADMINISTRATIVA("Baja administrativa formal", false),
    ABANDONO_CONFIRMADO("Abandono escolar confirmado", false),
    PENDIENTE_ACLARACION("Pendiente de aclaración", false)
}

/**
 * Registro de un movimiento escolar fechado con auditoría de captura y corrección.
 */
data class SchoolMovementRecord(
    val id: String,
    val institutionId: String = "INST-001",
    val studentId: String,
    val movementType: SchoolMovementType,
    val effectiveDate: String, // Formato YYYY-MM-DD
    val recordedAt: String = "2026-08-28T08:00:00Z", // Timestamp ISO-8601
    val recordedBy: String = "CONTROL_ESCOLAR",
    val fromGroup: String? = null,
    val toGroup: String? = null,
    val destinationSchool: String? = null,
    val departureCategory: DepartureCategory? = null,
    val documentationSource: String? = null,
    val classificationCriteria: String? = null,
    val isCorrection: Boolean = false,
    val supersedesMovementId: String? = null,
    val correctionReason: String? = null,
    val notes: String? = null
)

/**
 * Detalle nominal de un estudiante al corte. Restringido a Secretaría y Dirección.
 */
data class StudentCutoffDetail(
    val studentId: String,
    val fullName: String,
    val curp: String,
    val officialId: String,
    val status: StudentCutoffStatus,
    val assignedGroup: String,
    val effectiveMovementDate: String,
    val movementType: SchoolMovementType,
    val destinationSchool: String? = null,
    val documentationSource: String? = null,
    val classificationCriteria: String? = null
)

/**
 * Resumen agregado por grupo escolar para el análisis de colectivo / PMC.
 * No contiene nombres ni identificadores individuales.
 */
data class GroupCutoffSummary(
    val groupName: String,
    val activeCount: Int,
    val initialCohortCount: Int,
    val cohortRetainedCount: Int,
    val transfersCount: Int,
    val administrativeDropoutsCount: Int,
    val confirmedDropoutsCount: Int,
    val pendingClarificationCount: Int,
    val cohortRetentionRate: Double
)

/**
 * Instantánea institucional de matrícula a una fecha de corte determinada.
 */
data class EnrollmentCutoffSnapshot(
    val cutoffDate: String,
    val cohortStartDate: String,
    val totalActiveStudents: Int,
    val initialCohortTotal: Int,
    val cohortRetainedTotal: Int,
    val cohortRetentionRate: Double,
    val netEnrollmentVariation: Int,
    val newEnrollmentsInPeriod: Int,
    val transfersOutTotal: Int,
    val administrativeDropoutsTotal: Int,
    val confirmedDropoutsTotal: Int,
    val pendingClarificationTotal: Int,
    val groupSummaries: List<GroupCutoffSummary>,
    val studentDetails: List<StudentCutoffDetail>
)

/**
 * Motor determinista de evaluación de movimientos de matrícula escolar.
 */
object SchoolMovementEvaluator {

    val DETERMINISTIC_ORDER: Comparator<SchoolMovementRecord> =
        compareBy<SchoolMovementRecord> { it.effectiveDate }
            .thenBy { it.recordedAt }
            .thenBy { it.id }

    /**
     * Valida reglas de integridad y consistencia antes de persistir un movimiento.
     */
    fun validateMovement(movement: SchoolMovementRecord) {
        if (movement.movementType == SchoolMovementType.TRASLADO_SALIENTE) {
            require(!movement.destinationSchool.isNullOrBlank()) {
                "El traslado saliente requiere especificar la escuela de destino."
            }
        }
        if (movement.movementType == SchoolMovementType.ABANDONO_CONFIRMADO) {
            require(!movement.documentationSource.isNullOrBlank() && !movement.classificationCriteria.isNullOrBlank()) {
                "El abandono confirmado requiere documentar la fuente probatoria y el criterio fáctico institucional."
            }
        }
        if (movement.movementType == SchoolMovementType.CAMBIO_DE_GRUPO) {
            require(!movement.fromGroup.isNullOrBlank() && !movement.toGroup.isNullOrBlank()) {
                "El cambio de grupo requiere definir grupo origen y grupo destino."
            }
            require(movement.fromGroup != movement.toGroup) {
                "El grupo de destino debe ser diferente al grupo de origen."
            }
        }
        if (movement.isCorrection) {
            require(!movement.supersedesMovementId.isNullOrBlank() && !movement.correctionReason.isNullOrBlank()) {
                "Una corrección formal requiere indicar el movimiento reemplazado y el motivo."
            }
        }
    }

    /**
     * Resuelve el corte institucional a la fecha indicada de forma pura y determinista.
     */
    fun calculateCutoff(
        students: List<Student>,
        movements: List<SchoolMovementRecord>,
        cutoffDate: String,
        cohortStartDate: String = "2026-08-28"
    ): EnrollmentCutoffSnapshot {
        // 1. Identificar movimientos superados por correcciones posteriores
        val supersededIds = movements
            .filter { it.isCorrection && !it.supersedesMovementId.isNullOrBlank() }
            .mapNotNull { it.supersedesMovementId }
            .toSet()

        // 2. Filtrar movimientos válidos hasta la fecha de corte y ordenarlos deterministamente
        val activeMovements = movements
            .filter { it.id !in supersededIds && it.effectiveDate <= cutoffDate }
            .sortedWith(DETERMINISTIC_ORDER)

        val movementsByStudent = activeMovements.groupBy { it.studentId }

        // Evaluar cohorte inicial a cohortStartDate
        val cohortMovements = movements
            .filter { it.id !in supersededIds && it.effectiveDate <= cohortStartDate }
            .sortedWith(DETERMINISTIC_ORDER)
            .groupBy { it.studentId }

        val initialCohortStudents = mutableSetOf<String>()
        val initialCohortGroupByStudent = mutableMapOf<String, String>()

        for (student in students) {
            val allStudentMoves = movementsByStudent[student.id].orEmpty()
            val firstMove = allStudentMoves.firstOrNull()
            if (firstMove != null && firstMove.effectiveDate > cohortStartDate) {
                // Llegó formalmente después de la fecha de cohorte base
                continue
            }

            val studentCohortMoves = cohortMovements[student.id].orEmpty()
            if (studentCohortMoves.isNotEmpty()) {
                val lastMove = studentCohortMoves.last()
                val isAct = when (lastMove.movementType) {
                    SchoolMovementType.INSCRIPCION_INICIAL,
                    SchoolMovementType.ALTA_EXTEMPORANEA,
                    SchoolMovementType.REINCORPORACION,
                    SchoolMovementType.CAMBIO_DE_GRUPO -> true
                    else -> false
                }
                if (isAct) {
                    initialCohortStudents.add(student.id)
                    initialCohortGroupByStudent[student.id] = lastMove.toGroup ?: student.group
                }
            } else if (allStudentMoves.isEmpty() && cohortStartDate >= "2026-08-28") {
                initialCohortStudents.add(student.id)
                initialCohortGroupByStudent[student.id] = student.group
            }
        }

        val studentDetails = mutableListOf<StudentCutoffDetail>()
        var newEnrollmentsCount = 0
        var transfersOutCount = 0
        var adminDropoutsCount = 0
        var confirmedDropoutsCount = 0
        var pendingClarificationCount = 0

        for (student in students) {
            val history = movementsByStudent[student.id].orEmpty()
            if (history.isEmpty()) {
                val status = if (cutoffDate >= cohortStartDate) StudentCutoffStatus.ACTIVO else StudentCutoffStatus.NO_INGRESADO
                studentDetails.add(
                    StudentCutoffDetail(
                        studentId = student.id,
                        fullName = student.fullName,
                        curp = student.curp,
                        officialId = student.enrollmentId.ifBlank { student.preApplicationFolio ?: student.id },
                        status = status,
                        assignedGroup = student.group,
                        effectiveMovementDate = cohortStartDate,
                        movementType = SchoolMovementType.INSCRIPCION_INICIAL
                    )
                )
            } else {
                val lastEvent = history.last()
                val currentGroup = when (lastEvent.movementType) {
                    SchoolMovementType.CAMBIO_DE_GRUPO -> lastEvent.toGroup ?: student.group
                    else -> lastEvent.toGroup ?: lastEvent.fromGroup ?: student.group
                }

                val status = when (lastEvent.movementType) {
                    SchoolMovementType.INSCRIPCION_INICIAL,
                    SchoolMovementType.ALTA_EXTEMPORANEA,
                    SchoolMovementType.REINCORPORACION,
                    SchoolMovementType.CAMBIO_DE_GRUPO -> StudentCutoffStatus.ACTIVO

                    SchoolMovementType.TRASLADO_SALIENTE -> StudentCutoffStatus.TRASLADADO
                    SchoolMovementType.BAJA_ADMINISTRATIVA -> StudentCutoffStatus.BAJA_ADMINISTRATIVA
                    SchoolMovementType.ABANDONO_CONFIRMADO -> StudentCutoffStatus.ABANDONO_CONFIRMADO
                    SchoolMovementType.PENDIENTE_ACLARACION -> StudentCutoffStatus.PENDIENTE_ACLARACION
                }

                if (lastEvent.effectiveDate in cohortStartDate..cutoffDate) {
                    when (lastEvent.movementType) {
                        SchoolMovementType.ALTA_EXTEMPORANEA,
                        SchoolMovementType.REINCORPORACION -> if (lastEvent.effectiveDate > cohortStartDate) newEnrollmentsCount++
                        SchoolMovementType.TRASLADO_SALIENTE -> transfersOutCount++
                        SchoolMovementType.BAJA_ADMINISTRATIVA -> adminDropoutsCount++
                        SchoolMovementType.ABANDONO_CONFIRMADO -> confirmedDropoutsCount++
                        SchoolMovementType.PENDIENTE_ACLARACION -> pendingClarificationCount++
                        else -> Unit
                    }
                }

                studentDetails.add(
                    StudentCutoffDetail(
                        studentId = student.id,
                        fullName = student.fullName,
                        curp = student.curp,
                        officialId = student.enrollmentId.ifBlank { student.preApplicationFolio ?: student.id },
                        status = status,
                        assignedGroup = currentGroup,
                        effectiveMovementDate = lastEvent.effectiveDate,
                        movementType = lastEvent.movementType,
                        destinationSchool = lastEvent.destinationSchool,
                        documentationSource = lastEvent.documentationSource,
                        classificationCriteria = lastEvent.classificationCriteria
                    )
                )
            }
        }

        val activeStudents = studentDetails.filter { it.status.isActive }
        val activeStudentIds = activeStudents.map { it.studentId }.toSet()

        val allGroups = (students.map { it.group } + studentDetails.map { it.assignedGroup }).distinct().sorted()
        val groupSummaries = allGroups.map { grp ->
            val inGroup = studentDetails.filter { it.assignedGroup == grp }
            val activeInGrp = inGroup.count { it.status.isActive }
            val cohortInGrp = initialCohortStudents.count { initialCohortGroupByStudent[it] == grp }
            val retainedCohortInGrp = initialCohortStudents.count {
                initialCohortGroupByStudent[it] == grp && it in activeStudentIds
            }
            val transfersInGrp = inGroup.count { it.status == StudentCutoffStatus.TRASLADADO }
            val adminDropInGrp = inGroup.count { it.status == StudentCutoffStatus.BAJA_ADMINISTRATIVA }
            val confirmedDropInGrp = inGroup.count { it.status == StudentCutoffStatus.ABANDONO_CONFIRMADO }
            val pendingInGrp = inGroup.count { it.status == StudentCutoffStatus.PENDIENTE_ACLARACION }

            val cohortBase = cohortInGrp - transfersInGrp
            val retentionRate = if (cohortBase > 0) {
                (retainedCohortInGrp.toDouble() / cohortBase.toDouble()) * 100.0
            } else if (cohortInGrp > 0) {
                100.0
            } else {
                0.0
            }

            GroupCutoffSummary(
                groupName = grp,
                activeCount = activeInGrp,
                initialCohortCount = cohortInGrp,
                cohortRetainedCount = retainedCohortInGrp,
                transfersCount = transfersInGrp,
                administrativeDropoutsCount = adminDropInGrp,
                confirmedDropoutsCount = confirmedDropInGrp,
                pendingClarificationCount = pendingInGrp,
                cohortRetentionRate = (retentionRate * 10.0).toInt() / 10.0
            )
        }

        val initialCohortTotal = initialCohortStudents.size
        val cohortRetainedTotal = initialCohortStudents.count { it in activeStudentIds }
        val cohortTransfersTotal = studentDetails.count {
            it.studentId in initialCohortStudents && it.status == StudentCutoffStatus.TRASLADADO
        }
        val effectiveCohortBase = initialCohortTotal - cohortTransfersTotal
        val overallRetentionRate = if (effectiveCohortBase > 0) {
            (cohortRetainedTotal.toDouble() / effectiveCohortBase.toDouble()) * 100.0
        } else if (initialCohortTotal > 0) {
            100.0
        } else {
            0.0
        }

        val netVariation = activeStudents.size - initialCohortTotal

        return EnrollmentCutoffSnapshot(
            cutoffDate = cutoffDate,
            cohortStartDate = cohortStartDate,
            totalActiveStudents = activeStudents.size,
            initialCohortTotal = initialCohortTotal,
            cohortRetainedTotal = cohortRetainedTotal,
            cohortRetentionRate = (overallRetentionRate * 10.0).toInt() / 10.0,
            netEnrollmentVariation = netVariation,
            newEnrollmentsInPeriod = newEnrollmentsCount,
            transfersOutTotal = transfersOutCount,
            administrativeDropoutsTotal = adminDropoutsCount,
            confirmedDropoutsTotal = confirmedDropoutsCount,
            pendingClarificationTotal = pendingClarificationCount,
            groupSummaries = groupSummaries,
            studentDetails = studentDetails.sortedBy { it.assignedGroup }
        )
    }

    /**
     * Calcula las oportunidades de asistencia aplicables a un estudiante según su vigencia real.
     */
    fun calculateApplicableAttendanceDays(
        studentId: String,
        movements: List<SchoolMovementRecord>,
        periodStart: String,
        periodEnd: String,
        schoolDays: List<String>
    ): List<String> {
        val studentMoves = movements
            .filter { it.studentId == studentId }
            .sortedWith(DETERMINISTIC_ORDER)

        if (studentMoves.isEmpty()) return schoolDays.filter { it in periodStart..periodEnd }

        return schoolDays.filter { day ->
            if (day !in periodStart..periodEnd) return@filter false
            val activeMovesUntilDay = studentMoves.filter { it.effectiveDate <= day }
            if (activeMovesUntilDay.isEmpty()) return@filter false
            val lastMove = activeMovesUntilDay.last()
            when (lastMove.movementType) {
                SchoolMovementType.INSCRIPCION_INICIAL,
                SchoolMovementType.ALTA_EXTEMPORANEA,
                SchoolMovementType.REINCORPORACION,
                SchoolMovementType.CAMBIO_DE_GRUPO -> true
                else -> false
            }
        }
    }
}
