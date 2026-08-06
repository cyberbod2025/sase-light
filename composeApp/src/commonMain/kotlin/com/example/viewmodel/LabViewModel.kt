package com.example.viewmodel

import com.example.currentEpochMillis
import com.example.data.IncidentTransitionResult
import com.example.data.IncidentWorkflow
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.SaseAudit
import com.example.data.SaseObservation
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.auth.AuthFailureReason
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthSession
import com.example.data.auth.RoleSelectionContext
import com.example.data.auth.StaffAction
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole
import com.example.data.repository.AuditRepository
import com.example.data.repository.MockAuditRepositoryImpl
import com.example.data.repository.MockStudentRepositoryImpl
import com.example.data.repository.StudentRepository
import com.example.environment.AppEnvironment
import com.example.formatTimestamp
import com.example.getPlatformName
import com.example.audit.InstitutionalAuditEvent
import com.example.audit.InstitutionalAuditResult
import com.example.audit.InstitutionalAuditValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class Screen {
    data object SessionHome : Screen()
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
    data object PreApplicationFamilyPortal : Screen()
    data object SecretariaPreApplicationDashboard : Screen()
    data object OfficialEnrollmentDashboard : Screen()
    data class CredentialPreview(val studentId: String) : Screen()
    data object StudentCredentialDashboard : Screen()
}

internal fun secretarySidebarDestination(item: String): Screen? = when (item) {
    "Inicio" -> Screen.SessionHome
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

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class RoleSelectionRequired(val context: RoleSelectionContext) : LoginUiState
    data class Error(val reason: AuthFailureReason) : LoginUiState
}

/**
 * Estado institucional de la aplicación. El ambiente y el repositorio de
 * autenticación son obligatorios: el ViewModel nunca elige un mock ni degrada
 * silenciosamente una configuración conectada a demo.
 */
class LabViewModel(
    val appEnvironment: AppEnvironment,
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository = MockStudentRepositoryImpl(),
    private val auditRepository: AuditRepository = MockAuditRepositoryImpl(),
    coroutineScope: CoroutineScope? = null,
    private val nowMillis: () -> Long = ::currentEpochMillis
) {
    private val providedScope = coroutineScope
    private val authScope: CoroutineScope by lazy { providedScope ?: MainScope() }
    private var expirationJob: Job? = null

    private val _currentScreen = MutableStateFlow<Screen>(Screen.SessionHome)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val session: StateFlow<AuthSession?> = authRepository.session
    val demoAccessAvailable: Boolean =
        appEnvironment.demoUsersEnabled && authRepository.demoAccessAvailable

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _sessionTransitioning = MutableStateFlow(false)
    val sessionTransitioning: StateFlow<Boolean> = _sessionTransitioning.asStateFlow()

    val saseStudents: StateFlow<List<Student>> = studentRepository.students
    val saseAudits: StateFlow<List<SaseAudit>> = auditRepository.audits

    fun signIn(email: String, password: String) {
        authenticate { authRepository.signIn(email, password) }
    }

    fun signInDemo(role: StaffRole) {
        if (!demoAccessAvailable) {
            _loginState.value = LoginUiState.Error(AuthFailureReason.DEMO_UNAVAILABLE)
            return
        }
        authenticate { authRepository.signInDemo(role) }
    }

    fun selectRole(roleId: String) {
        authenticate { authRepository.selectRole(roleId) }
    }

    fun switchRole(roleId: String) {
        if (_sessionTransitioning.value) return
        _sessionTransitioning.value = true
        authScope.launch {
            try {
                when (val result = authRepository.switchRole(roleId)) {
                    is AuthResult.Success -> acceptSession(
                        active = result.session,
                        auditAction = "session.role.switched"
                    )
                    is AuthResult.RoleSelectionRequired -> {
                        _loginState.value = LoginUiState.RoleSelectionRequired(result.context)
                    }
                    is AuthResult.Failure -> _loginState.value = LoginUiState.Error(result.reason)
                }
            } finally {
                _sessionTransitioning.value = false
            }
        }
    }

    fun resetDemo() {
        if (!appEnvironment.isDemo) return
        _sessionTransitioning.value = true
        authScope.launch {
            try {
                expirationJob?.cancel()
                authRepository.resetDemo()
                PreApplicationViewModel.resetDemoData()
                _currentScreen.value = Screen.SessionHome
                _loginState.value = LoginUiState.Idle
            } finally {
                _sessionTransitioning.value = false
            }
        }
    }

    fun signOut() {
        if (_sessionTransitioning.value) return
        _sessionTransitioning.value = true
        authScope.launch {
            try {
                expirationJob?.cancel()
                session.value?.takeIf { it.profile.active && !it.isExpired(nowMillis()) }?.let { active ->
                    recordAudit(
                        session = active,
                        action = "session.ended",
                        entityType = "session",
                        entityId = active.membershipId,
                        result = InstitutionalAuditResult.AUTHORIZED
                    )
                }
                authRepository.signOut()
                _currentScreen.value = Screen.SessionHome
                _loginState.value = LoginUiState.Idle
            } finally {
                _sessionTransitioning.value = false
            }
        }
    }

    /** Devuelve false y cierra la sesión cuando el límite ya fue alcanzado. */
    fun revalidateSession(): Boolean {
        if (session.value == null) return false
        if (!sessionIsValid()) {
            expireSession()
            return false
        }
        return true
    }

    /** Consulta pura usada por la UI antes de renderizar contenido protegido. */
    fun sessionIsValid(): Boolean {
        val active = session.value ?: return false
        return active.profile.active && !active.isExpired(nowMillis())
    }

    private fun authenticate(block: suspend () -> AuthResult) {
        if (_loginState.value is LoginUiState.Loading) return
        _loginState.value = LoginUiState.Loading
        authScope.launch {
            when (val result = block()) {
                is AuthResult.Success -> acceptSession(
                    active = result.session,
                    auditAction = "session.started"
                )
                is AuthResult.RoleSelectionRequired -> {
                    _loginState.value = LoginUiState.RoleSelectionRequired(result.context)
                }
                is AuthResult.Failure -> _loginState.value = LoginUiState.Error(result.reason)
            }
        }
    }

    private suspend fun acceptSession(active: AuthSession, auditAction: String) {
        if (!active.profile.active || active.isExpired(nowMillis())) {
            authRepository.signOut()
            _loginState.value = LoginUiState.Error(AuthFailureReason.SESSION_EXPIRED)
            return
        }
        _currentScreen.value = Screen.SessionHome
        _loginState.value = LoginUiState.Idle
        recordAudit(
            session = active,
            action = auditAction,
            entityType = "session",
            entityId = active.membershipId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
        scheduleExpiration(active)
    }

    private fun scheduleExpiration(active: AuthSession) {
        expirationJob?.cancel()
        val expiration = active.expiresAt ?: return
        expirationJob = authScope.launch {
            delay((expiration - nowMillis()).coerceAtLeast(0L))
            val current = session.value
            if (current?.membershipId == active.membershipId && current.isExpired(nowMillis())) {
                expireSession()
            }
        }
    }

    private fun expireSession() {
        if (_sessionTransitioning.value) return
        _sessionTransitioning.value = true
        authScope.launch {
            try {
                expirationJob?.cancel()
                session.value?.let { expired ->
                    recordAudit(
                        session = expired,
                        action = "session.expired",
                        entityType = "session",
                        entityId = expired.membershipId,
                        result = InstitutionalAuditResult.FAILED
                    )
                }
                authRepository.signOut()
                _currentScreen.value = Screen.SessionHome
                _loginState.value = LoginUiState.Error(AuthFailureReason.SESSION_EXPIRED)
            } finally {
                _sessionTransitioning.value = false
            }
        }
    }

    fun navigateTo(screen: Screen) {
        if (!revalidateSession()) return
        if (canOpenScreen(session.value, screen)) {
            _currentScreen.value = screen
        }
    }

    fun navigateFromSecretarySidebar(item: String) {
        secretarySidebarDestination(item)?.let(::navigateTo)
    }

    fun navigateBack() {
        navigateTo(Screen.SessionHome)
    }

    fun updateStudent(student: Student): Boolean {
        val active = authorizedFor(
            action = StaffAction.UPDATE_STUDENT,
            entityType = "student",
            entityId = student.id
        ) ?: return false
        studentRepository.updateStudent(student)
        return recordAudit(
            session = active,
            action = "student.updated",
            entityType = "student",
            entityId = student.id,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    fun addStudent(student: Student): StudentAddResult {
        val active = authorizedFor(
            action = StaffAction.CREATE_STUDENT,
            entityType = "student",
            entityId = student.id.ifBlank { "new-student" }
        ) ?: return StudentAddResult.InvalidData("Acción no autorizada para la sesión activa.")

        val result = studentRepository.addStudent(student)
        if (result is StudentAddResult.Added) {
            recordAudit(
                session = active,
                action = "student.created",
                entityType = "student",
                entityId = result.student.id,
                result = InstitutionalAuditResult.AUTHORIZED
            )
        }
        return result
    }

    fun addObservation(studentId: String, text: String, category: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.ADD_OBSERVATION,
            entityType = "student_observation",
            entityId = studentId
        ) ?: return false
        val student = saseStudents.value.firstOrNull { it.id == studentId } ?: return false

        val observation = SaseObservation(
            text = text,
            author = active.profile.fullName,
            date = "Hoy",
            category = category
        )
        studentRepository.updateStudent(
            student.copy(observations = listOf(observation) + student.observations)
        )
        return recordAudit(
            session = active,
            action = "student.observation.created",
            entityType = "student_observation",
            entityId = studentId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    /**
     * Registra un evento sin aceptar actor, rol ni detalle libres. Esos campos
     * siempre provienen de la sesión institucional activa.
     */
    fun logSaseAudit(action: String, entityType: String, entityId: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.UPDATE_STUDENT,
            entityType = entityType,
            entityId = entityId
        ) ?: return false
        return recordAudit(
            session = active,
            action = action,
            entityType = entityType,
            entityId = entityId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    fun reportIncident(studentId: String, type: String, description: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.REPORT_INCIDENT,
            entityType = "school_incident",
            entityId = studentId
        ) ?: return false
        val student = saseStudents.value.firstOrNull { it.id == studentId } ?: return false

        val incident = IncidentWorkflow.report(
            type = type,
            description = description,
            date = "Hoy",
            reportedByStaffId = active.profile.id,
            reportedByName = active.profile.fullName,
            idGenerator = { "INC-${Random.nextInt(100000, 999999)}" }
        )
        studentRepository.updateStudent(
            student.copy(schoolIncidents = listOf(incident) + student.schoolIncidents)
        )
        return recordAudit(
            session = active,
            action = "school_incident.reported",
            entityType = "school_incident",
            entityId = incident.id,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    fun advanceIncident(studentId: String, incidentId: String, note: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.ADVANCE_INCIDENT,
            entityType = "school_incident",
            entityId = incidentId
        ) ?: return false
        val student = saseStudents.value.firstOrNull { it.id == studentId } ?: return false
        val incident = student.schoolIncidents.firstOrNull { it.id == incidentId } ?: return false

        val updated = when (val transition = IncidentWorkflow.advance(incident, note)) {
            is IncidentTransitionResult.IllegalTransition -> return false
            is IncidentTransitionResult.Success -> transition.incident
        }
        studentRepository.updateStudent(
            student.copy(
                schoolIncidents = student.schoolIncidents.map {
                    if (it.id == incidentId) updated else it
                }
            )
        )
        return recordAudit(
            session = active,
            action = "school_incident.advanced.${updated.status}",
            entityType = "school_incident",
            entityId = incidentId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    fun escalateCase(studentId: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.ESCALATE_CASE,
            entityType = "student_case",
            entityId = studentId
        ) ?: return false
        return recordAudit(
            session = active,
            action = "student_case.escalated",
            entityType = "student_case",
            entityId = studentId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    private fun activeSession(): AuthSession? {
        val active = session.value ?: return null
        if (!active.profile.active || active.isExpired(nowMillis())) {
            expireSession()
            return null
        }
        return active
    }

    private fun authorizedFor(
        action: StaffAction,
        entityType: String,
        entityId: String
    ): AuthSession? {
        val active = activeSession() ?: return null
        if (StaffPermissions.canPerform(active, action)) return active

        recordAudit(
            session = active,
            action = "authorization.denied.${action.name.lowercase()}",
            entityType = entityType,
            entityId = entityId,
            result = InstitutionalAuditResult.DENIED
        )
        return null
    }

    private fun recordAudit(
        session: AuthSession,
        action: String,
        entityType: String,
        entityId: String,
        result: InstitutionalAuditResult
    ): Boolean {
        val event = InstitutionalAuditEvent(
            institutionId = session.institutionId,
            actorProfileId = session.profileId,
            membershipId = session.membershipId,
            activeRole = session.activeRole,
            action = action,
            entityType = entityType,
            entityId = entityId,
            timestamp = formatTimestamp("yyyy-MM-dd HH:mm:ss"),
            result = result,
            sourcePlatform = getPlatformName()
        )
        if (!InstitutionalAuditValidator.validate(event).isValid) return false
        auditRepository.logAudit(event)
        return true
    }
}
