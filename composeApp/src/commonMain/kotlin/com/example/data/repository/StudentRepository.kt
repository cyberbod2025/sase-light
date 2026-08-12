package com.example.data.repository

import com.example.data.SaseIncident
import com.example.data.SaseObservation
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

    /**
     * Agrega una observacion. Separado de [updateStudent] porque en
     * SUPABASE_STAGING vive en su propia tabla con su propia politica RLS
     * (misma que la identidad sensible, EDIT_STUDENT_IDENTITY) — no todo el
     * expediente central necesita reescribirse para anexar una nota.
     */
    suspend fun addObservation(studentId: String, observation: SaseObservation): StudentUpdateResult

    /**
     * Reporta una incidencia nueva. Separado de [updateStudent] porque su
     * politica RLS real (EDIT_INCIDENTS: Prefectura/Tutor) es DISTINTA de la
     * que protege la identidad central (EDIT_STUDENT_IDENTITY: Secretaria) —
     * enrutar esto por updateStudent() rechazaria la escritura para
     * cualquier rol que legitimamente puede reportar incidencias pero no
     * editar el expediente central.
     */
    suspend fun addIncident(
        studentId: String,
        type: String,
        description: String,
        date: String,
        reportedByStaffId: String,
        reportedByName: String
    ): StudentUpdateResult

    /** Persiste una incidencia ya avanzada por [com.example.data.IncidentWorkflow]. */
    suspend fun advanceIncident(studentId: String, updated: SaseIncident): StudentUpdateResult

    /**
     * Vacia el estado en memoria (sin red). Se invoca en logout/expiracion de
     * sesion/antes de aceptar una sesion nueva para que el StateFlow nunca
     * exponga expedientes de un usuario/institucion anterior.
     */
    fun clear()
}
