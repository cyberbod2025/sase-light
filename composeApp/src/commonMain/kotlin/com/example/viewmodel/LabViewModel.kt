package com.example.viewmodel

import com.example.data.MockSaseData
import com.example.data.SaseAudit
import com.example.data.Student
import com.example.formatTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Screen {
    data object SecretaryDashboard : Screen()
    data class StudentRecord(val studentId: String) : Screen()
}

class LabViewModel {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.SecretaryDashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    val saseStudents: StateFlow<List<Student>> = MockSaseData.students
    val saseAudits: StateFlow<List<SaseAudit>> = MockSaseData.audits

    fun updateStudent(student: Student) {
        MockSaseData.updateStudent(student)
    }

    fun addStudent(student: Student) {
        MockSaseData.addStudent(student)
    }

    fun logSaseAudit(action: String, role: String, detail: String) {
        val timestamp = "Hoy ${formatTimestamp("hh:mm a")}"
        MockSaseData.logAudit(action, role, timestamp, detail)
    }
}
