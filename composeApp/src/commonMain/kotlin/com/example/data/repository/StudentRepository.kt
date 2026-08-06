package com.example.data.repository

import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentPersistenceFailure
import com.example.data.StudentUpdateResult
import kotlinx.coroutines.flow.StateFlow

/** Resultado de sincronizar el listado con el almacenamiento institucional. */
sealed class StudentSyncResult {
    data class Loaded(val students: List<Student>) : StudentSyncResult()
    data class Failed(val reason: StudentPersistenceFailure) : StudentSyncResult()
}

/**
 * Costura de expedientes. Las operaciones son `suspend` porque una
 * implementacion conectada habla por red: ninguna puede resolverse en el hilo
 * de interfaz ni devolver un exito antes de que el backend lo confirme.
 */
interface StudentRepository {
    val students: StateFlow<List<Student>>

    /**
     * Recarga el listado desde el origen. El mock la resuelve en memoria; la
     * implementacion de Supabase consulta solo la institucion de la sesion.
     */
    suspend fun refresh(): StudentSyncResult

    suspend fun updateStudent(student: Student): StudentUpdateResult

    suspend fun addStudent(student: Student): StudentAddResult
}
