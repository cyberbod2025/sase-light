package com.example.data.repository

import com.example.data.MockSaseData
import com.example.data.Student
import com.example.data.StudentAddResult
import kotlinx.coroutines.flow.StateFlow

class MockStudentRepositoryImpl : StudentRepository {
    override val students: StateFlow<List<Student>> = MockSaseData.students

    override fun updateStudent(student: Student) {
        MockSaseData.updateStudent(student)
    }

    override fun addStudent(student: Student): StudentAddResult {
        return MockSaseData.addStudent(student)
    }
}
