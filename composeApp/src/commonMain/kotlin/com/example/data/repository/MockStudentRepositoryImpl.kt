package com.example.data.repository

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentUpdateResult
import kotlinx.coroutines.flow.StateFlow

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
}
