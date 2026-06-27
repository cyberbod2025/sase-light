package com.example.viewmodel

import com.example.data.SaseAudit
import com.example.data.Student
import com.example.data.repository.AuditRepository
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.StudentRepository
import com.example.formatTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Screen {
    data object SecretaryDashboard : Screen()
    data class StudentRecord(val studentId: String) : Screen()
}

class LabViewModel(
    private val studentRepository: StudentRepository = MockStudentRepositoryImpl(),
    private val auditRepository: AuditRepository = MockAuditRepositoryImpl()
) {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.SecretaryDashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    val saseStudents: StateFlow<List<Student>> = studentRepository.students
    val saseAudits: StateFlow<List<SaseAudit>> = auditRepository.audits

    fun updateStudent(student: Student) {
        studentRepository.updateStudent(student)
    }

    fun addStudent(student: Student) {
        studentRepository.addStudent(student)
    }

    fun logSaseAudit(action: String, role: String, detail: String) {
        val timestamp = "Hoy ${formatTimestamp("hh:mm a")}"
        auditRepository.logAudit(action, role, timestamp, detail)
    }
}
