package com.example.viewmodel

import com.example.auth.AuthState
import com.example.auth.MockSessionRepository
import com.example.auth.SessionRepository
import com.example.data.SaseAudit
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.repository.AuditRepository
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.StudentRepository
import com.example.di.SaseDependencies
import com.example.formatTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Screen {
    data object SecretaryDashboard : Screen()
    data object StudentRecordsDashboard : Screen()
    data object EnrollmentDashboard : Screen()
    data class StudentRecord(
        val studentId: String,
        val institutionalKey: InstitutionalStudentRecordKey? = null,
        val returnTo: Screen = SecretaryDashboard
    ) : Screen() {
        init {
            require(institutionalKey == null || institutionalKey.studentId == studentId) {
                "La clave institucional debe conservar el mismo studentId de la ruta."
            }
        }
    }
    // Nuevas rutas FASE 1
    data object PreApplicationFamilyPortal : Screen()
    data object SecretariaPreApplicationDashboard : Screen()
    data object OfficialEnrollmentDashboard : Screen()
    data class CredentialPreview(val studentId: String) : Screen()
    data object StudentCredentialDashboard : Screen()
}

internal fun secretarySidebarDestination(item: String): Screen? = when (item) {
    "Inicio" -> Screen.SecretaryDashboard
    "Expedientes" -> Screen.StudentRecordsDashboard
    "Inscripciones" -> Screen.EnrollmentDashboard
    "Portal Familia" -> Screen.PreApplicationFamilyPortal
    "Pre-Solicitudes" -> Screen.SecretariaPreApplicationDashboard
    "Altas Oficiales" -> Screen.OfficialEnrollmentDashboard
    "Credenciales" -> Screen.StudentCredentialDashboard
    else -> null
}

internal fun enrollmentValidationDestination(): Screen =
    Screen.SecretariaPreApplicationDashboard

// Roles MOCK
enum class AppRole(val label: String) {
    FAMILIA("Familia"),
    SECRETARIA("Secretaría"),
    DIRECCION("Dirección"),
    MEDICO("Médico Escolar"),
    TRABAJO_SOCIAL("Trabajo Social"),
    UDEII("UDEII"),
    DOCENTE("Docente")
}

class LabViewModel(
    private val studentRepository: StudentRepository = MockStudentRepositoryImpl(),
    private val auditRepository: AuditRepository = MockAuditRepositoryImpl(),
    val sessionRepository: SessionRepository = MockSessionRepository()
) {
    constructor(dependencies: SaseDependencies) : this(
        studentRepository = dependencies.studentRepository,
        auditRepository = dependencies.auditRepository,
        sessionRepository = dependencies.sessionRepository
    )

    /** Estado de autenticación institucional (sin login visible en esta fase). */
    val authState: StateFlow<AuthState> = sessionRepository.authState

    private val _currentScreen = MutableStateFlow<Screen>(Screen.SecretaryDashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Mock global role selector para testing UI
    private val _userRole = MutableStateFlow(AppRole.SECRETARIA)
    val userRole: StateFlow<AppRole> = _userRole.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun navigateFromSecretarySidebar(item: String) {
        secretarySidebarDestination(item)?.let(::navigateTo)
    }

    fun navigateBack() {
        _currentScreen.value = Screen.SecretaryDashboard
    }

    fun setRole(role: AppRole) {
        _userRole.value = role
    }

    val saseStudents: StateFlow<List<Student>> = studentRepository.students
    val saseAudits: StateFlow<List<SaseAudit>> = auditRepository.audits

    fun updateStudent(student: Student) {
        studentRepository.updateStudent(student)
    }

    fun addStudent(student: Student): StudentAddResult {
        return studentRepository.addStudent(student)
    }

    fun logSaseAudit(action: String, role: String, detail: String) {
        val timestamp = "Hoy ${formatTimestamp("hh:mm a")}"
        auditRepository.logAudit(action, role, timestamp, detail)
    }
}
