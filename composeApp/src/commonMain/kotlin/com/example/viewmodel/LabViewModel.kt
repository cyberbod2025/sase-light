package com.example.viewmodel

import com.example.currentEpochMillis
import com.example.data.IncidentTransitionResult
import com.example.data.IncidentWorkflow
import com.example.data.InstitutionalStudentRecordKey
import com.example.data.SaseAudit
import com.example.data.SaseObservation
import com.example.data.Student
import com.example.data.StudentAddResult
import com.example.data.StudentPersistenceFailure
import com.example.data.StudentUpdateResult
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
import com.example.data.repository.StudentSyncResult
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

/** Estado observable de la sincronizacion del expediente institucional. */
sealed interface StudentSyncUiState {
    data object Idle : StudentSyncUiState
    data object Loading : StudentSyncUiState
    data object Ready : StudentSyncUiState

    /**
     * El nucleo cargo pero algun subrecurso por area fallo por red/servidor;
     * [saseStudents] sigue siendo seguro de mostrar (conserva lo ultimo
     * conocido para esa seccion en vez de una lista vacia fabricada), pero
     * la sincronizacion no se completo — la UI debe poder distinguirlo de
     * [Ready] para, p.ej., ofrecer reintentar.
     */
    data class Incomplete(val reason: StudentPersistenceFailure) : StudentSyncUiState

    data class Error(val reason: StudentPersistenceFailure) : StudentSyncUiState
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

    private val _studentSync = MutableStateFlow<StudentSyncUiState>(StudentSyncUiState.Idle)

    /**
     * Estado real de la carga institucional. En DEMO_LOCAL se resuelve al
     * instante; conectado refleja la peticion de red, de modo que la interfaz
     * nunca presente una lista vacia por error de red como si fuera "sin datos".
     */
    val studentSync: StateFlow<StudentSyncUiState> = _studentSync.asStateFlow()

    /** Recarga manual del expediente institucional de la sesion activa. */
    suspend fun reloadInstitutionalData() {
        if (activeSession() == null) {
            _studentSync.value = StudentSyncUiState.Error(StudentPersistenceFailure.NO_SESSION)
            return
        }
        loadInstitutionalData()
    }

    private suspend fun loadInstitutionalData() {
        _studentSync.value = StudentSyncUiState.Loading
        val result = studentRepository.refresh()
        _studentSync.value = when (result) {
            is StudentSyncResult.Loaded -> StudentSyncUiState.Ready
            is StudentSyncResult.Partial -> StudentSyncUiState.Incomplete(result.reason)
            is StudentSyncResult.Failed -> {
                // Un refresh fallido no debe dejar expuesto lo que hubiera en
                // memoria de una sesion anterior (P1 de Codex en PR #49:
                // "Clear prior-session data before accepting a new session").
                studentRepository.clear()
                auditRepository.clear()
                StudentSyncUiState.Error(result.reason)
            }
        }
        if (!auditRepository.refresh()) {
            auditRepository.clear()
        }
    }

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
                studentRepository.clear()
                auditRepository.clear()
                _currentScreen.value = Screen.SessionHome
                _loginState.value = LoginUiState.Idle
                _studentSync.value = StudentSyncUiState.Idle
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
        // Un usuario nuevo (u otra institucion) nunca debe ver, ni siquiera
        // brevemente antes de que loadInstitutionalData() complete, los
        // expedientes/bitacora que hubieran quedado en memoria de la sesion
        // anterior (P1 de Codex en PR #49).
        studentRepository.clear()
        auditRepository.clear()
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
        loadInstitutionalData()
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
                studentRepository.clear()
                auditRepository.clear()
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

    suspend fun updateStudent(student: Student): Boolean {
        val active = authorizedFor(
            action = StaffAction.UPDATE_STUDENT,
            entityType = "student",
            entityId = student.id
        ) ?: return false

        // Sin bitácora asentable no se toca el expediente: el evento se valida
        // ANTES de mutar, y su resultado real se registra después.
        if (!canRecordAudit(active, "student.updated", "student", student.id)) return false

        val persisted = studentRepository.updateStudent(student) is StudentUpdateResult.Updated
        // Un guardado exitoso sin bitácora asentada no es un guardado confirmado:
        // la escritura de auditoría es una llamada de red independiente que puede
        // fallar aunque la del expediente haya tenido éxito.
        val audited = recordAudit(
            session = active,
            action = "student.updated",
            entityType = "student",
            entityId = student.id,
            result = if (persisted) InstitutionalAuditResult.AUTHORIZED
            else InstitutionalAuditResult.FAILED
        )
        return persisted && audited
    }

    suspend fun addStudent(student: Student): StudentAddResult {
        val active = authorizedFor(
            action = StaffAction.CREATE_STUDENT,
            entityType = "student",
            entityId = student.id.ifBlank { "new-student" }
        ) ?: return StudentAddResult.InvalidData("Acción no autorizada para la sesión activa.")

        // El identificador definitivo lo asigna el almacenamiento, así que aquí
        // se comprueba con el provisional que el evento será registrable.
        if (!canRecordAudit(active, "student.created", "student", student.id.ifBlank { "new-student" })) {
            return StudentAddResult.Failed(StudentPersistenceFailure.REJECTED)
        }

        val result = studentRepository.addStudent(student)
        if (result is StudentAddResult.Added) {
            val audited = recordAudit(
                session = active,
                action = "student.created",
                entityType = "student",
                entityId = result.student.id,
                result = InstitutionalAuditResult.AUTHORIZED
            )
            // El expediente ya se escribió y sigue committeado: reportarlo
            // como Failed induciría a un reintento que chocaría con la
            // CURP/matrícula ya creada. La falta de bitácora se distingue
            // como advertencia, no como "el alta no ocurrió".
            if (!audited) return StudentAddResult.CommittedWithoutAudit(result.student)
        }
        return result
    }

    suspend fun addObservation(studentId: String, text: String, category: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.ADD_OBSERVATION,
            entityType = "student_observation",
            entityId = studentId
        ) ?: return false
        if (saseStudents.value.none { it.id == studentId }) return false

        val observation = SaseObservation(
            text = text,
            author = active.profile.fullName,
            date = "Hoy",
            category = category
        )
        // Metodo dedicado, no updateStudent(): en SUPABASE_STAGING vive en su
        // propia tabla con su propio RLS, y el resultado nunca se reporta
        // como guardado si la fila no llego a insertarse (P1 de Codex en PR
        // #49, "Reject updates for fields the backend does not persist").
        val persisted = studentRepository.addObservation(studentId, observation) is StudentUpdateResult.Updated
        if (!persisted) return false
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
    suspend fun logSaseAudit(action: String, entityType: String, entityId: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.LOG_STUDENT_RECORD_EVENT,
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

    suspend fun reportIncident(studentId: String, type: String, description: String): Boolean {
        val active = authorizedFor(
            action = StaffAction.REPORT_INCIDENT,
            entityType = "school_incident",
            entityId = studentId
        ) ?: return false
        if (saseStudents.value.none { it.id == studentId }) return false

        // Metodo dedicado, no updateStudent(): en SUPABASE_STAGING su RLS
        // real es EDIT_INCIDENTS (Prefectura/Tutor), DISTINTO del que protege
        // el nucleo del expediente (EDIT_STUDENT_IDENTITY, Secretaria) —
        // enrutar por updateStudent() rechazaria el reporte para cualquier
        // rol que legitimamente puede reportar incidencias.
        val result = studentRepository.addIncident(
            studentId = studentId,
            type = type,
            description = description,
            date = "Hoy",
            reportedByStaffId = active.profile.id,
            reportedByName = active.profile.fullName
        )
        val updated = (result as? StudentUpdateResult.Updated)?.student ?: return false
        val incidentId = updated.schoolIncidents.firstOrNull()?.id ?: return false
        return recordAudit(
            session = active,
            action = "school_incident.reported",
            entityType = "school_incident",
            entityId = incidentId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    suspend fun advanceIncident(studentId: String, incidentId: String, note: String): Boolean {
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
        val persisted = studentRepository.advanceIncident(studentId, updated) is StudentUpdateResult.Updated
        if (!persisted) return false
        return recordAudit(
            session = active,
            action = "school_incident.advanced.${updated.status}",
            entityType = "school_incident",
            entityId = incidentId,
            result = InstitutionalAuditResult.AUTHORIZED
        )
    }

    suspend fun escalateCase(studentId: String): Boolean {
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

    private suspend fun authorizedFor(
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

    private fun buildAuditEvent(
        session: AuthSession,
        action: String,
        entityType: String,
        entityId: String,
        result: InstitutionalAuditResult
    ): InstitutionalAuditEvent = InstitutionalAuditEvent(
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

    /**
     * Comprobación previa a mutar: ¿este evento sería registrable? No asienta
     * nada, de modo que una mutación abortada no deja una bitácora de algo que
     * nunca ocurrió.
     */
    private fun canRecordAudit(
        session: AuthSession,
        action: String,
        entityType: String,
        entityId: String
    ): Boolean = InstitutionalAuditValidator.validate(
        buildAuditEvent(session, action, entityType, entityId, InstitutionalAuditResult.AUTHORIZED)
    ).isValid

    private suspend fun recordAudit(
        session: AuthSession,
        action: String,
        entityType: String,
        entityId: String,
        result: InstitutionalAuditResult
    ): Boolean {
        val event = buildAuditEvent(session, action, entityType, entityId, result)
        if (!InstitutionalAuditValidator.validate(event).isValid) return false
        return auditRepository.logAudit(event)
    }
}
