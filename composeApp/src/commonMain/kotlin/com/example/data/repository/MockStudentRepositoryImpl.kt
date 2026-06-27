package com.example.data.repository

import com.example.data.MockSaseData
import com.example.data.Student
import kotlinx.coroutines.flow.StateFlow

class MockStudentRepositoryImpl : StudentRepository {
    override val students: StateFlow<List<Student>> = MockSaseData.students

    override fun updateStudent(student: Student) {
        MockSaseData.updateStudent(student)
    }

    override fun addStudent(student: Student) {
        MockSaseData.addStudent(student)
    }
}
