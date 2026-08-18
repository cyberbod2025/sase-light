package com.example.viewmodel

import com.example.data.attendance.AttendanceEntry
import com.example.data.attendance.AttendanceFailureReason
import com.example.data.attendance.AttendanceRepository
import com.example.data.attendance.AttendanceResult
import com.example.data.attendance.AttendanceStatus
import com.example.data.attendance.ClassAttendanceSnapshot
import com.example.data.attendance.TeacherGroup
import com.example.data.attendance.institutionalMessage
import com.example.data.auth.AuthSession
import com.example.formatTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la captura de asistencia de un grupo.
 *
 * [draft] SIEMPRE contiene el roster completo (el servidor exige el grupo
 * entero en cada guardado), de modo que la UI no puede construir un envio
 * parcial ni por accidente.
 */
data class ClassAttendanceUiState(
    val loadingGroups: Boolean = false,
    val groupsLoaded: Boolean = false,
    val groups: List<TeacherGroup> = emptyList(),
    val openingGroup: Boolean = false,
    val snapshot: ClassAttendanceSnapshot? = null,
    val draft: Map<String, AttendanceStatus> = emptyMap(),
    val saving: Boolean = false,
    val savedAtLeastOnce: Boolean = false,
    val successMessage: String? = null,
    val error: AttendanceFailureReason? = null,
    /**
     * Grupo+fecha del ultimo intento de apertura, exitoso o no. Permite que
     * "Reintentar" tras un fallo de apertura reintente ESE grupo en vez de
     * solo recargar la lista (que exigiria al docente volver a tocar la
     * misma tarjeta para lograr algo).
     */
    val lastOpenAttempt: Pair<String, String>? = null
) {
    /** Lista mostrada: roster del snapshot con el estado del borrador aplicado. */
    val roster: List<AttendanceEntry>
        get() = snapshot?.entries.orEmpty().map { entry ->
            entry.copy(status = draft[entry.student.id] ?: entry.status)
        }

    private val persistedStatuses: Map<String, AttendanceStatus>?
        get() = snapshot?.takeIf { it.hasSavedCapture }
            ?.entries
            ?.associate { it.student.id to it.status }

    /**
     * Una sesion recien abierta parte de "todos presentes", pero esa captura
     * todavia NO existe en la base: cuenta como cambio sin guardar aunque el
     * docente no toque nada.
     */
    val hasUnsavedChanges: Boolean
        get() {
            if (snapshot == null) return false
            val persisted = persistedStatuses ?: return true
            return draft != persisted
        }

    val canSave: Boolean
        get() = snapshot != null && !saving && hasUnsavedChanges

    val presentCount: Int get() = roster.count { it.status == AttendanceStatus.PRESENTE }
    val absentCount: Int get() = roster.count { it.status == AttendanceStatus.AUSENTE }
    val lateCount: Int get() = roster.count { it.status == AttendanceStatus.RETARDO }

    val showsEmptyGroupsState: Boolean
        get() = groupsLoaded && !loadingGroups && groups.isEmpty() && error == null
}

/**
 * Orquesta la rebanada de asistencia sobre la sesion institucional YA
 * autenticada (PR #47/#48). No autentica, no elige rol y no consulta un
 * selector de rol paralelo: recibe la [AuthSession] activa y la pasa al
 * repositorio, que es quien la usa para autorizar.
 */
class ClassAttendanceViewModel(
    private val repository: AttendanceRepository,
    private val session: AuthSession,
    coroutineScope: CoroutineScope? = null,
    private val today: () -> String = { formatTimestamp("yyyy-MM-dd") }
) {
    private val scope: CoroutineScope by lazy { coroutineScope ?: MainScope() }

    private val _state = MutableStateFlow(ClassAttendanceUiState())
    val state: StateFlow<ClassAttendanceUiState> = _state.asStateFlow()

    // Se incrementa cada vez que cambia "de que grupo se habla" (abrir uno
    // nuevo o cerrar el detalle). openGroup()/save() capturan el valor
    // vigente al lanzar la corrutina; si al resolver ya no coincide, el
    // resultado es de un grupo abandonado y se descarta sin tocar _state —
    // evita que un guardado o una apertura tardia (p. ej. tras closeGroup()
    // + abrir otro grupo) sobreescriba el estado del grupo actual.
    private var groupContextToken: Int = 0

    fun loadGroups() {
        if (_state.value.loadingGroups) return
        // Limpia el ultimo intento de apertura: un error que surja de ESTA
        // llamada es del listado, no de abrir un grupo puntual, y no debe
        // hacer que "Reintentar" reabra un grupo viejo por error.
        _state.value = _state.value.copy(
            loadingGroups = true,
            error = null,
            successMessage = null,
            lastOpenAttempt = null
        )
        scope.launch {
            when (val result = repository.groupsForTeacher(session)) {
                is AttendanceResult.Success -> _state.value = _state.value.copy(
                    loadingGroups = false,
                    groupsLoaded = true,
                    groups = result.value,
                    error = null
                )
                is AttendanceResult.Failure -> _state.value = _state.value.copy(
                    loadingGroups = false,
                    groupsLoaded = true,
                    groups = emptyList(),
                    error = result.reason
                )
            }
        }
    }

    /** Inicia o recupera la sesion de clase de hoy para [groupId]. */
    fun openGroup(groupId: String, date: String = today()) {
        if (_state.value.openingGroup) return
        val token = ++groupContextToken
        _state.value = _state.value.copy(
            openingGroup = true,
            error = null,
            successMessage = null,
            snapshot = null,
            draft = emptyMap(),
            lastOpenAttempt = groupId to date
        )
        scope.launch {
            val result = repository.openClassSession(session, groupId, date)
            if (token != groupContextToken) return@launch
            when (result) {
                is AttendanceResult.Success -> _state.value = _state.value.copy(
                    openingGroup = false,
                    snapshot = result.value,
                    draft = result.value.entries.associate { it.student.id to it.status },
                    savedAtLeastOnce = result.value.hasSavedCapture,
                    error = null
                )
                is AttendanceResult.Failure -> _state.value = _state.value.copy(
                    openingGroup = false,
                    snapshot = null,
                    draft = emptyMap(),
                    error = result.reason
                )
            }
        }
    }

    /** Marca la excepcion de un alumno. Solo altera el borrador local. */
    fun setStatus(studentId: String, status: AttendanceStatus) {
        val current = _state.value
        if (current.snapshot == null || current.saving) return
        if (current.snapshot.entries.none { it.student.id == studentId }) return
        _state.value = current.copy(
            draft = current.draft + (studentId to status),
            successMessage = null
        )
    }

    /** Devuelve a todo el grupo al estado inicial "todos presentes". */
    fun markAllPresent() {
        val current = _state.value
        val snapshot = current.snapshot ?: return
        if (current.saving) return
        _state.value = current.copy(
            draft = snapshot.entries.associate { it.student.id to AttendanceStatus.PRESENTE },
            successMessage = null
        )
    }

    /**
     * Guarda en UNA operacion el roster COMPLETO, no solo lo modificado.
     * Un segundo toque mientras guarda no reenvia nada.
     */
    fun save() {
        val current = _state.value
        val snapshot = current.snapshot ?: return
        if (current.saving) return
        val token = groupContextToken
        _state.value = current.copy(saving = true, error = null, successMessage = null)
        val payload = snapshot.entries.map { entry ->
            entry.student.id to (current.draft[entry.student.id] ?: entry.status)
        }
        scope.launch {
            val result = repository.saveClassAttendance(session, snapshot.sessionId, payload)
            // El docente ya salio de este grupo (closeGroup()/otro openGroup())
            // mientras el guardado seguia en vuelo: aplicar el resultado ahora
            // pisaria el estado del grupo que esta viendo. Se descarta.
            if (token != groupContextToken) return@launch
            when (result) {
                is AttendanceResult.Success -> _state.value = _state.value.copy(
                    saving = false,
                    snapshot = result.value,
                    draft = result.value.entries.associate { it.student.id to it.status },
                    savedAtLeastOnce = true,
                    successMessage = "Asistencia guardada para ${result.value.date}.",
                    error = null
                )
                // El borrador NO se descarta al fallar: el docente puede
                // reintentar sin volver a capturar.
                is AttendanceResult.Failure -> _state.value = _state.value.copy(
                    saving = false,
                    error = result.reason
                )
            }
        }
    }

    /** Vuelve a la lista de grupos descartando el detalle abierto. */
    fun closeGroup() {
        groupContextToken++
        _state.value = _state.value.copy(
            openingGroup = false,
            saving = false,
            snapshot = null,
            draft = emptyMap(),
            successMessage = null,
            error = null
        )
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }

    /** Recarga la sesion abierta desde el origen de datos. */
    fun reloadOpenGroup() {
        val snapshot = _state.value.snapshot ?: return
        openGroup(snapshot.groupId, snapshot.date)
    }

    /**
     * Reintenta el ultimo grupo que se intento abrir y fallo. Es la accion de
     * "Reintentar" que ve el docente en la lista de grupos tras un fallo de
     * apertura (p. ej. NETWORK/SESSION_NOT_FOUND transitorio) — sin esto,
     * "Reintentar" solo recargaba la lista de grupos y el docente tenia que
     * volver a tocar la misma tarjeta para lograr algo.
     */
    fun retryLastOpenAttempt() {
        val (groupId, date) = _state.value.lastOpenAttempt ?: return
        openGroup(groupId, date)
    }
}

/** Texto de encabezado del detalle del grupo. */
fun classAttendanceHeadline(state: ClassAttendanceUiState): String {
    val snapshot = state.snapshot ?: return "Selecciona un grupo"
    return "${snapshot.groupName} · ${snapshot.date}"
}

/** Resumen visible mientras se captura. */
fun classAttendanceSummary(state: ClassAttendanceUiState): String =
    "Presentes ${state.presentCount} · Ausentes ${state.absentCount} · Retardos ${state.lateCount}"

/** Etiqueta del boton de guardado, incluyendo el estado en curso. */
fun classAttendanceSaveLabel(state: ClassAttendanceUiState): String = when {
    state.saving -> "Guardando…"
    state.hasUnsavedChanges -> "Guardar asistencia"
    else -> "Asistencia guardada"
}

fun unsavedChangesLabel(state: ClassAttendanceUiState): String? =
    if (state.hasUnsavedChanges) "Cambios sin guardar" else null

fun attendanceErrorMessage(reason: AttendanceFailureReason?): String? =
    reason?.institutionalMessage()
