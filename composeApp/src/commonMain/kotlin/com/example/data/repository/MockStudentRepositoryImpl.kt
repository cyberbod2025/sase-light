package com.example.data.repository

import com.example.data.IncidentWorkflow
import com.example.data.MockSaseData
import com.example.data.SaseIncident
import com.example.data.SaseObservation
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentPersistenceFailure
import com.example.data.StudentUpdateResult
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * Implementacion en memoria de DEMO_LOCAL. Es `suspend` solo para compartir la
 * costura con la implementacion conectada: aqui nada viaja por red, asi que
 * ninguna operacion puede fallar y el comportamiento observable es el mismo de
 * antes de M1-B.
 */
class MockStudentRepositoryImpl : StudentRepository {
    override val students: StateFlow<List<Student>> = MockSaseData.students

    /** El origen ya es el StateFlow en memoria; recargar es leerlo. */
    override suspend fun refresh(): StudentSyncResult =
        StudentSyncResult.Loaded(MockSaseData.students.value)

    override suspend fun updateStudent(student: Student): StudentUpdateResult {
        MockSaseData.updateStudent(student)
        return StudentUpdateResult.Updated(student)
    }

    override suspend fun addStudent(student: Student): StudentAddResult {
        return MockSaseData.addStudent(student)
    }

    override suspend fun addObservation(studentId: String, observation: SaseObservation): StudentUpdateResult {
        val student = MockSaseData.students.value.firstOrNull { it.id == studentId }
            ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)
        val updated = student.copy(observations = listOf(observation) + student.observations)
        MockSaseData.updateStudent(updated)
        return StudentUpdateResult.Updated(updated)
    }

    override suspend fun addIncident(
        studentId: String,
        type: String,
        description: String,
        date: String,
        reportedByStaffId: String,
        reportedByName: String
    ): StudentUpdateResult {
        val student = MockSaseData.students.value.firstOrNull { it.id == studentId }
            ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)
        val incident = IncidentWorkflow.report(
            type = type,
            description = description,
            date = date,
            reportedByStaffId = reportedByStaffId,
            reportedByName = reportedByName,
            idGenerator = { "INC-${Random.nextInt(100000, 999999)}" }
        )
        val updated = student.copy(schoolIncidents = listOf(incident) + student.schoolIncidents)
        MockSaseData.updateStudent(updated)
        return StudentUpdateResult.Updated(updated)
    }

    override suspend fun advanceIncident(studentId: String, updated: SaseIncident): StudentUpdateResult {
        val student = MockSaseData.students.value.firstOrNull { it.id == studentId }
            ?: return StudentUpdateResult.Failed(StudentPersistenceFailure.REJECTED)
        val next = student.copy(
            schoolIncidents = student.schoolIncidents.map { if (it.id == updated.id) updated else it }
        )
        MockSaseData.updateStudent(next)
        return StudentUpdateResult.Updated(next)
    }

    override fun clear() {
        // DEMO_LOCAL es un dataset compartido de demostracion, no datos de un
        // usuario real: no hay sesion anterior de la que aislarse. El
        // aislamiento real (P1 de Codex en PR #49) aplica al repositorio de
        // Supabase, donde si hay usuarios/instituciones distintos.
    }
}
