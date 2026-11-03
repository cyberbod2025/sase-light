package com.example.data.enrollment

import com.example.data.Student
import com.example.data.repository.StudentRepository

/**
 * Contrato de repositorio para el registro y consulta de movimientos fechados de matrícula.
 */
interface SchoolMovementRepository {
    val isAvailableInCurrentEnvironment: Boolean
        get() = true

    val unavailabilityReason: String?
        get() = null

    suspend fun recordMovement(movement: SchoolMovementRecord): Result<SchoolMovementRecord>
    suspend fun getMovementsForStudent(studentId: String): List<SchoolMovementRecord>
    suspend fun getAllMovements(): List<SchoolMovementRecord>
    suspend fun calculateCutoff(
        cutoffDate: String,
        cohortStartDate: String = "2026-08-28"
    ): EnrollmentCutoffSnapshot
    fun clear()
}

/**
 * Implementación en memoria para modo DEMO_LOCAL y pruebas unitarias aisladas.
 * No debe utilizarse en ambientes conectados a Supabase Staging.
 */
class MockSchoolMovementRepositoryImpl(
    private val studentRepository: StudentRepository,
    initialMovements: MutableList<SchoolMovementRecord>? = null,
    private val studentsOverride: List<Student>? = null
) : SchoolMovementRepository {

    private val movementsStore: MutableList<SchoolMovementRecord> =
        initialMovements ?: mutableListOf()

    init {
        if (initialMovements == null) {
            seedSyntheticMovements()
        }
    }

    private fun seedSyntheticMovements() {
        movementsStore.addAll(
            listOf(
                // Alumno 3 trasladado con destino acreditado (no es abandono)
                SchoolMovementRecord(
                    id = "MOV-001",
                    studentId = "3",
                    movementType = SchoolMovementType.TRASLADO_SALIENTE,
                    effectiveDate = "2026-09-30",
                    recordedAt = "2026-09-30T10:00:00Z",
                    recordedBy = "SECRETARIA_ESCOLAR",
                    fromGroup = "2°B",
                    destinationSchool = "Escuela Secundaria Técnica No. 14, CCT 09DST0014Z",
                    departureCategory = DepartureCategory.TRASLADO,
                    documentationSource = "Oficio de Traslado No. SE/09/2026-441",
                    classificationCriteria = "Cambio de domicilio familiar acreditado"
                ),
                // Alumno 1 con cambio de grupo con vigencia formal
                SchoolMovementRecord(
                    id = "MOV-002",
                    studentId = "1",
                    movementType = SchoolMovementType.CAMBIO_DE_GRUPO,
                    effectiveDate = "2026-10-01",
                    recordedAt = "2026-10-01T12:00:00Z",
                    recordedBy = "DIRECCION_ESCOLAR",
                    fromGroup = "1°A",
                    toGroup = "1°B",
                    documentationSource = "Acuerdo de Dirección por balance de cupo",
                    classificationCriteria = "Reubicación para nivelación de grupos"
                ),
                // Alumno 5 con baja administrativa (no es abandono)
                SchoolMovementRecord(
                    id = "MOV-003",
                    studentId = "5",
                    movementType = SchoolMovementType.BAJA_ADMINISTRATIVA,
                    effectiveDate = "2026-10-10",
                    recordedAt = "2026-10-10T09:30:00Z",
                    recordedBy = "SECRETARIA_ESCOLAR",
                    fromGroup = "1°B",
                    departureCategory = DepartureCategory.BAJA_ADMINISTRATIVA,
                    documentationSource = "Solicitud formal de revocación de matrícula por tutor",
                    classificationCriteria = "Trámite de cambio a sistema abierto"
                ),
                // Alumno 2 con alta extemporánea posterior al inicio de ciclo
                SchoolMovementRecord(
                    id = "MOV-004",
                    studentId = "2",
                    movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                    effectiveDate = "2026-10-15",
                    recordedAt = "2026-10-15T11:00:00Z",
                    recordedBy = "SECRETARIA_ESCOLAR",
                    toGroup = "1°A",
                    documentationSource = "Dictamen de Revalidación de Estudios SEP-2026",
                    classificationCriteria = "Incorporación por ingreso extemporáneo autorizado"
                )
            )
        )
    }

    override suspend fun recordMovement(movement: SchoolMovementRecord): Result<SchoolMovementRecord> = runCatching {
        SchoolMovementEvaluator.validateMovement(movement)
        if (!movement.isCorrection) {
            val duplicateExists = movementsStore.any {
                it.id != movement.id &&
                    !it.isCorrection &&
                    it.studentId == movement.studentId &&
                    it.movementType == movement.movementType &&
                    it.effectiveDate == movement.effectiveDate
            }
            require(!duplicateExists) {
                "Movimiento duplicado: ya existe un registro de ${movement.movementType.displayName} para el estudiante ${movement.studentId} en la fecha ${movement.effectiveDate}."
            }
        } else {
            val target = movementsStore.firstOrNull { it.id == movement.supersedesMovementId }
                ?: throw IllegalArgumentException("El movimiento a corregir (${movement.supersedesMovementId}) no existe.")
            require(target.studentId == movement.studentId && target.institutionId == movement.institutionId) {
                "La corrección debe corresponder al mismo estudiante e institución del movimiento original."
            }
            val alreadySuperseded = movementsStore.any {
                it.id != movement.id && it.supersedesMovementId == movement.supersedesMovementId
            }
            require(!alreadySuperseded) {
                "El movimiento ${movement.supersedesMovementId} ya fue sustituido por otra corrección previa."
            }
        }
        movementsStore.removeAll { it.id == movement.id }
        movementsStore.add(movement)
        movement
    }

    override suspend fun getMovementsForStudent(studentId: String): List<SchoolMovementRecord> {
        return movementsStore
            .filter { it.studentId == studentId }
            .sortedWith(SchoolMovementEvaluator.DETERMINISTIC_ORDER)
    }

    override suspend fun getAllMovements(): List<SchoolMovementRecord> {
        return movementsStore.toList().sortedWith(SchoolMovementEvaluator.DETERMINISTIC_ORDER)
    }

    override suspend fun calculateCutoff(
        cutoffDate: String,
        cohortStartDate: String
    ): EnrollmentCutoffSnapshot {
        val students = studentsOverride ?: studentRepository.students.value
        return SchoolMovementEvaluator.calculateCutoff(
            students = students,
            movements = movementsStore,
            cutoffDate = cutoffDate,
            cohortStartDate = cohortStartDate
        )
    }

    override fun clear() {
        movementsStore.clear()
    }
}
