package com.example.data.enrollment

import com.example.data.MockSaseData
import com.example.data.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SchoolMovementResult {
    data class Success(val record: SchoolMovementRecord) : SchoolMovementResult()
    data class InvalidMovement(val message: String) : SchoolMovementResult()
    data class PermissionDenied(val message: String) : SchoolMovementResult()
}

interface SchoolMovementRepository {
    val movements: StateFlow<List<SchoolMovementRecord>>
    suspend fun getMovementsForStudent(studentId: String): List<SchoolMovementRecord>
    suspend fun recordMovement(record: SchoolMovementRecord): SchoolMovementResult
    suspend fun calculateCutoff(cutoffDate: String, studentFilter: List<Student>? = null): EnrollmentCutoffSnapshot
}

/**
 * Implementación en memoria del repositorio de movimientos para DEMO_LOCAL y pruebas unitarias.
 */
class MockSchoolMovementRepositoryImpl(
    initialMovements: List<SchoolMovementRecord> = emptyList()
) : SchoolMovementRepository {

    private val _movements = MutableStateFlow<List<SchoolMovementRecord>>(initialMovements)
    override val movements: StateFlow<List<SchoolMovementRecord>> = _movements.asStateFlow()

    override suspend fun getMovementsForStudent(studentId: String): List<SchoolMovementRecord> {
        return _movements.value.filter { it.studentId == studentId }
    }

    override suspend fun recordMovement(record: SchoolMovementRecord): SchoolMovementResult {
        // Validar coherencia básica de grupo
        if (record.movementType == SchoolMovementType.CAMBIO_GRUPO && record.sourceGroup == record.targetGroup) {
            return SchoolMovementResult.InvalidMovement("El grupo destino no puede ser igual al grupo origen.")
        }
        _movements.value = _movements.value + record
        return SchoolMovementResult.Success(record)
    }

    override suspend fun calculateCutoff(
        cutoffDate: String,
        studentFilter: List<Student>?
    ): EnrollmentCutoffSnapshot {
        val masterStudents = studentFilter ?: MockSaseData.students.value
        return SchoolMovementEvaluator.calculateCutoff(
            cutoffDate = cutoffDate,
            students = masterStudents,
            movements = _movements.value
        )
    }

    fun setMovements(movementsList: List<SchoolMovementRecord>) {
        _movements.value = movementsList
    }

    fun clear() {
        _movements.value = emptyList()
    }
}
