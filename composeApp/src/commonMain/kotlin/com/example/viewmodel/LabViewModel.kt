package com.example.viewmodel

import com.example.api.GeminiImageGenerator
import com.example.data.MockSaseData
import com.example.data.SaseAudit
import com.example.data.Student
import com.example.data.repository.AuditRepository
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.StudentRepository
import com.example.formatTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    data object SecretaryDashboard : Screen()
    data class StudentRecord(val studentId: String) : Screen()
}

sealed class GeminiState {
    data object Idle : GeminiState()
    data object Loading : GeminiState()
    data class Success(val base64Image: String) : GeminiState()
    data class Error(val message: String) : GeminiState()
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

    private val _geminiState = MutableStateFlow<GeminiState>(GeminiState.Idle)
    val geminiState: StateFlow<GeminiState> = _geminiState.asStateFlow()

    fun generateGeminiImage(prompt: String, scope: CoroutineScope, size: String = "1024x1024") {
        if (prompt.isBlank()) {
            _geminiState.value = GeminiState.Error("El prompt no puede estar vacío")
            return
        }
        _geminiState.value = GeminiState.Loading
        scope.launch {
            val result = GeminiImageGenerator.generateImage(prompt, size)
            _geminiState.value = if (result != null) {
                GeminiState.Success(result)
            } else {
                GeminiState.Error("No se pudo generar la imagen. Verifica tu API key y conexión.")
            }
        }
    }

    fun resetGeminiState() {
        _geminiState.value = GeminiState.Idle
    }

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
