package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.Student
import com.example.data.SaseAudit
import com.example.data.MockSaseData

sealed class Screen {
    object SecretaryDashboard : Screen()
    data class StudentRecord(val studentId: String) : Screen()
}

class LabViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LabViewModel"

    // Navigation and Active Selection
    private val _currentScreen = MutableStateFlow<Screen>(Screen.SecretaryDashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- SASE-310 Student & Audit state management ---
    val saseStudents: StateFlow<List<Student>> = MockSaseData.students
    val saseAudits: StateFlow<List<SaseAudit>> = MockSaseData.audits

    fun updateStudent(student: Student) {
        MockSaseData.updateStudent(student)
    }

    fun addStudent(student: Student) {
        MockSaseData.addStudent(student)
    }

    fun logSaseAudit(action: String, role: String, detail: String) {
        val timestamp = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        MockSaseData.logAudit(action, role, "Hoy $timestamp", detail)
    }
}
