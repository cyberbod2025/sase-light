package com.example.viewmodel

import com.example.data.SaseAudit
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.auth.AuthFailureReason
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthSession
import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.repository.AuditRepository
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.StudentRepository
import com.example.formatTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

/**
 * Estado de la compuerta de acceso. `Success` no se representa aqui: cuando el
 * login funciona, la sesion pasa a no-nula y el estado vuelve a [Idle]; la
 * sesion (no este estado) es la que decide que se muestra.
 */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val reason: AuthFailureReason) : LoginUiState
}

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
    private val authRepository: AuthRepository = MockAuthRepositoryImpl(),
    coroutineScope: CoroutineScope? = null
) {
    // Perezoso a proposito: construir MainScope() tocaria Dispatchers.Main, que
    // no existe en desktopTest. Solo se materializa si se usa signIn/signOut, de
    // modo que los tests que no autentican pueden construir LabViewModel() libre.
    private val providedScope = coroutineScope
    private val authScope: CoroutineScope by lazy { providedScope ?: MainScope() }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.SecretaryDashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Autenticacion (mock) ---
    val session: StateFlow<AuthSession?> = authRepository.session

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_loginState.value is LoginUiState.Loading) return
        _loginState.value = LoginUiState.Loading
        authScope.launch {
            _loginState.value = when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> LoginUiState.Idle
                is AuthResult.Failure -> LoginUiState.Error(result.reason)
            }
        }
    }

    fun signOut() {
        authScope.launch { authRepository.signOut() }
        _loginState.value = LoginUiState.Idle
    }

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
