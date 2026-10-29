package com.example.data.enrollment

import com.example.data.Student
import com.example.data.repository.StudentRepository

/**
 * Contrato de repositorio para el registro y consulta de movimientos fechados de matrícula.
 */
interface SchoolMovementRepository {
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
                // Alumno trasladado con destino acreditado (no es abandono)
                SchoolMovementRecord(
                    id = "MOV-001",
                    studentId = "STU-003",
                    movementType = SchoolMovementType.TRASLADO_SALIENTE,
                    effectiveDate = "2026-09-30",
                    recordedAt = "2026-09-30T10:00:00Z",
                    recordedBy = "SECRETARIA_ESCOLAR",
                    fromGroup = "1-A",
                    destinationSchool = "Escuela Secundaria Técnica No. 14, CCT 09DST0014Z",
                    departureCategory = DepartureCategory.TRASLADO,
                    documentationSource = "Oficio de Traslado No. SE/09/2026-441",
                    classificationCriteria = "Cambio de domicilio familiar acreditado"
                ),
                // Alumno con cambio de grupo con vigencia formal
                SchoolMovementRecord(
                    id = "MOV-002",
                    studentId = "STU-001",
                    movementType = SchoolMovementType.CAMBIO_DE_GRUPO,
                    effectiveDate = "2026-10-01",
                    recordedAt = "2026-10-01T12:00:00Z",
                    recordedBy = "DIRECCION_ESCOLAR",
                    fromGroup = "1-A",
                    toGroup = "1-B",
                    documentationSource = "Acuerdo de Dirección por balance de cupo",
                    classificationCriteria = "Reubicación para nivelación de grupos"
                ),
                // Alumno con baja administrativa (no es abandono)
                SchoolMovementRecord(
                    id = "MOV-003",
                    studentId = "STU-006",
                    movementType = SchoolMovementType.BAJA_ADMINISTRATIVA,
                    effectiveDate = "2026-10-10",
                    recordedAt = "2026-10-10T09:30:00Z",
                    recordedBy = "SECRETARIA_ESCOLAR",
                    fromGroup = "2-A",
                    departureCategory = DepartureCategory.BAJA_ADMINISTRATIVA,
                    documentationSource = "Solicitud formal de revocación de matrícula por tutor",
                    classificationCriteria = "Trámite de cambio a sistema abierto"
                ),
                // Alumno con alta extemporánea posterior al inicio de ciclo
                SchoolMovementRecord(
                    id = "MOV-004",
                    studentId = "STU-010",
                    movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                    effectiveDate = "2026-10-15",
                    recordedAt = "2026-10-15T11:00:00Z",
                    recordedBy = "SECRETARIA_ESCOLAR",
                    toGroup = "3-A",
                    documentationSource = "Dictamen de Revalidación de Estudios SEP-2026",
                    classificationCriteria = "Incorporación por ingreso extemporáneo autorizado"
                )
            )
        )
    }

    override suspend fun recordMovement(movement: SchoolMovementRecord): Result<SchoolMovementRecord> = runCatching {
        SchoolMovementEvaluator.validateMovement(movement)
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
