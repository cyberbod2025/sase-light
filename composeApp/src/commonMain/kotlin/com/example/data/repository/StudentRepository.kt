package com.example.data.repository

import com.example.data.Student
import com.example.data.StudentAddResult
import kotlinx.coroutines.flow.StateFlow

interface StudentRepository {
    val students: StateFlow<List<Student>>
    fun updateStudent(student: Student)
    fun addStudent(student: Student): StudentAddResult
}
