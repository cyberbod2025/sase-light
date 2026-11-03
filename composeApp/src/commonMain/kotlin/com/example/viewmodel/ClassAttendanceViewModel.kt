package com.example.viewmodel

import com.example.data.attendance.AttendanceExportArtifact
import com.example.data.attendance.AttendanceExportHelper
import com.example.data.attendance.AttendanceFailureReason
import com.example.data.attendance.AttendancePresence
import com.example.data.attendance.AttendanceRecordEntry
import com.example.data.attendance.AttendanceRepository
import com.example.data.attendance.AttendanceResult
import com.example.data.attendance.AttendanceStudentEntry
import com.example.data.attendance.AttendanceThresholds
import com.example.data.attendance.CaptureState
import com.example.data.attendance.ClassAttendanceReport
import com.example.data.attendance.ClassAttendanceSnapshot
import com.example.data.attendance.PunctualityStatus
import com.example.data.attendance.SessionStatus
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

data class ClassAttendanceUiState(
    val loadingGroups: Boolean = false,
    val groups: List<TeacherGroup> = emptyList(),
    val selectedGroup: TeacherGroup? = null,
    val selectedDate: String = "2026-10-15",
    val selectedModule: Int = 1,
    val sessionStatus: SessionStatus = SessionStatus.PROGRAMADA,
    val sessionNotes: String? = null,
    val openingSession: Boolean = false,
    val snapshot: ClassAttendanceSnapshot? = null,
    val draftEntries: Map<String, AttendanceRecordEntry> = emptyMap(),
    val saving: Boolean = false,
    val savedAtLeastOnce: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val loadingReport: Boolean = false,
    val report: ClassAttendanceReport? = null,
    val reportThresholds: AttendanceThresholds = AttendanceThresholds(),
    val exportedArtifact: AttendanceExportArtifact? = null
) {
    val displayRoster: List<AttendanceStudentEntry>
        get() {
            val base = snapshot?.entries.orEmpty()
            return base.map { item ->
                val draft = draftEntries[item.student.id]
                if (draft != null) {
                    item.copy(
                        presence = draft.presence,
                        punctuality = draft.punctuality,
                        minutesLate = draft.minutesLate,
                        justification = draft.justification,
                        isRecorded = draft.presence != null
                    )
                } else {
                    item
                }
            }
        }

    val captureState: CaptureState
        get() {
            val list = displayRoster
            if (list.isEmpty()) return CaptureState.PENDIENTE
            val recordedCount = list.count { it.isRecorded }
            return when {
                recordedCount == 0 -> CaptureState.PENDIENTE
                recordedCount == list.size -> CaptureState.COMPLETA
                else -> CaptureState.PARCIAL
            }
        }

    val presentCount: Int get() = displayRoster.count { it.isPresent }
    val absentCount: Int get() = displayRoster.count { it.isAbsent }
    val lateCount: Int get() = displayRoster.count { it.isLate }
    val pendingCount: Int get() = displayRoster.count { it.isPending }
    val hasUnsavedChanges: Boolean
        get() {
            if (snapshot == null) return false
            val persisted = snapshot.entries.associate {
                it.student.id to AttendanceRecordEntry(
                    studentId = it.student.id,
                    presence = it.presence,
                    punctuality = it.punctuality,
                    minutesLate = it.minutesLate,
                    justification = it.justification
                )
            }
            return draftEntries != persisted
        }
}

/**
 * ViewModel que orquesta la rebanada completa de Asistencia y Puntualidad (PMC).
 */
class ClassAttendanceViewModel(
    private val repository: AttendanceRepository,
    private val session: AuthSession,
    coroutineScope: CoroutineScope? = null,
    defaultDate: String = "2026-10-15"
) {
    private val scope: CoroutineScope by lazy { coroutineScope ?: MainScope() }

    private val _state = MutableStateFlow(ClassAttendanceUiState(selectedDate = defaultDate))
    val state: StateFlow<ClassAttendanceUiState> = _state.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        _state.value = _state.value.copy(loadingGroups = true, errorMessage = null)
        scope.launch {
            when (val result = repository.groupsForTeacher(session)) {
                is AttendanceResult.Success -> {
                    _state.value = _state.value.copy(
                        loadingGroups = false,
                        groups = result.value,
                        selectedGroup = _state.value.selectedGroup ?: result.value.firstOrNull(),
                        errorMessage = null
                    )
                }
                is AttendanceResult.Failure -> {
                    _state.value = _state.value.copy(
                        loadingGroups = false,
                        errorMessage = result.reason.institutionalMessage()
                    )
                }
            }
        }
    }

    fun selectGroup(group: TeacherGroup) {
        _state.value = _state.value.copy(
            selectedGroup = group,
            snapshot = null,
            draftEntries = emptyMap(),
            report = null,
            successMessage = null,
            errorMessage = null
        )
        openSession()
    }

    fun setDate(date: String) {
        if (date.isBlank() || date == _state.value.selectedDate) return
        _state.value = _state.value.copy(
            selectedDate = date,
            snapshot = null,
            draftEntries = emptyMap(),
            successMessage = null
        )
        if (_state.value.selectedGroup != null) {
            openSession()
        }
    }

    fun setModule(module: Int) {
        if (module !in 1..8 || module == _state.value.selectedModule) return
        _state.value = _state.value.copy(
            selectedModule = module,
            snapshot = null,
            draftEntries = emptyMap(),
            successMessage = null
        )
        if (_state.value.selectedGroup != null) {
            openSession()
        }
    }

    fun openSession() {
        val group = _state.value.selectedGroup ?: return
        val date = _state.value.selectedDate
        val module = _state.value.selectedModule
        val status = _state.value.sessionStatus

        _state.value = _state.value.copy(openingSession = true, errorMessage = null)
        scope.launch {
            when (val result = repository.scheduleOrOpenSession(session, group.id, date, module, status)) {
                is AttendanceResult.Success -> {
                    val snap = result.value
                    val initialDraft = snap.entries.associate {
                        it.student.id to AttendanceRecordEntry(
                            studentId = it.student.id,
                            presence = it.presence,
                            punctuality = it.punctuality,
                            minutesLate = it.minutesLate,
                            justification = it.justification
                        )
                    }
                    _state.value = _state.value.copy(
                        openingSession = false,
                        snapshot = snap,
                        sessionStatus = snap.sessionStatus,
                        sessionNotes = snap.notes,
                        draftEntries = initialDraft,
                        errorMessage = null
                    )
                }
                is AttendanceResult.Failure -> {
                    _state.value = _state.value.copy(
                        openingSession = false,
                        errorMessage = result.reason.institutionalMessage()
                    )
                }
            }
        }
    }

    fun setSessionStatus(newStatus: SessionStatus, notes: String? = null) {
        val snap = _state.value.snapshot ?: return
        scope.launch {
            when (val result = repository.updateSessionStatus(session, snap.sessionId, newStatus, notes)) {
                is AttendanceResult.Success -> {
                    _state.value = _state.value.copy(
                        snapshot = result.value,
                        sessionStatus = result.value.sessionStatus,
                        sessionNotes = result.value.notes,
                        successMessage = "Estatus de la sesión actualizado a: ${newStatus.label}"
                    )
                }
                is AttendanceResult.Failure -> {
                    _state.value = _state.value.copy(errorMessage = result.reason.institutionalMessage())
                }
            }
        }
    }

    fun setPresence(studentId: String, presence: AttendancePresence) {
        val current = _state.value.draftEntries[studentId]
        val newEntry = AttendanceRecordEntry(
            studentId = studentId,
            presence = presence,
            punctuality = if (presence == AttendancePresence.PRESENTE) {
                if (current?.punctuality == PunctualityStatus.RETARDO) PunctualityStatus.RETARDO else PunctualityStatus.A_TIEMPO
            } else {
                PunctualityStatus.NO_APLICA
            },
            minutesLate = if (presence == AttendancePresence.PRESENTE) current?.minutesLate ?: 0 else 0,
            justification = current?.justification
        )
        _state.value = _state.value.copy(
            draftEntries = _state.value.draftEntries + (studentId to newEntry),
            successMessage = null
        )
    }

    fun togglePunctuality(studentId: String) {
        val current = _state.value.draftEntries[studentId]
        if (current?.presence != AttendancePresence.PRESENTE) return

        val newPunctuality = if (current.punctuality == PunctualityStatus.A_TIEMPO) {
            PunctualityStatus.RETARDO
        } else {
            PunctualityStatus.A_TIEMPO
        }
        val newMinutes = if (newPunctuality == PunctualityStatus.RETARDO && current.minutesLate == 0) 10 else current.minutesLate

        val newEntry = current.copy(
            punctuality = newPunctuality,
            minutesLate = if (newPunctuality == PunctualityStatus.RETARDO) newMinutes else 0
        )
        _state.value = _state.value.copy(
            draftEntries = _state.value.draftEntries + (studentId to newEntry),
            successMessage = null
        )
    }

    fun setMinutesLate(studentId: String, minutes: Int) {
        val current = _state.value.draftEntries[studentId] ?: return
        if (current.presence != AttendancePresence.PRESENTE) return
        val clampedMinutes = minutes.coerceAtLeast(0)
        _state.value = _state.value.copy(
            draftEntries = _state.value.draftEntries + (
                studentId to current.copy(
                    punctuality = if (clampedMinutes > 0) PunctualityStatus.RETARDO else PunctualityStatus.A_TIEMPO,
                    minutesLate = clampedMinutes
                )
            ),
            successMessage = null
        )
    }

    fun markAllPresent() {
        val snap = _state.value.snapshot ?: return
        val updatedDraft = snap.entries.associate {
            it.student.id to AttendanceRecordEntry(
                studentId = it.student.id,
                presence = AttendancePresence.PRESENTE,
                punctuality = PunctualityStatus.A_TIEMPO,
                minutesLate = 0
            )
        }
        _state.value = _state.value.copy(draftEntries = updatedDraft, successMessage = null)
    }

    fun saveAttendance() {
        val snap = _state.value.snapshot ?: return
        if (_state.value.saving) return

        _state.value = _state.value.copy(saving = true, errorMessage = null, successMessage = null)
        val entriesToSave = _state.value.draftEntries.values.toList()

        scope.launch {
            when (val result = repository.saveAttendance(session, snap.sessionId, entriesToSave)) {
                is AttendanceResult.Success -> {
                    val updatedSnap = result.value
                    val syncedDraft = updatedSnap.entries.associate {
                        it.student.id to AttendanceRecordEntry(
                            studentId = it.student.id,
                            presence = it.presence,
                            punctuality = it.punctuality,
                            minutesLate = it.minutesLate,
                            justification = it.justification
                        )
                    }
                    _state.value = _state.value.copy(
                        saving = false,
                        snapshot = updatedSnap,
                        draftEntries = syncedDraft,
                        savedAtLeastOnce = true,
                        successMessage = "Asistencia guardada correctamente para ${updatedSnap.groupName} (${updatedSnap.date} - Módulo ${updatedSnap.moduleNumber})."
                    )
                }
                is AttendanceResult.Failure -> {
                    _state.value = _state.value.copy(
                        saving = false,
                        errorMessage = result.reason.institutionalMessage()
                    )
                }
            }
        }
    }

    fun loadReport(startDate: String = "2026-10-01", endDate: String = "2026-10-31") {
        val group = _state.value.selectedGroup ?: return
        _state.value = _state.value.copy(loadingReport = true, errorMessage = null)
        scope.launch {
            when (val result = repository.getGroupAttendanceReport(session, group.id, startDate, endDate, _state.value.reportThresholds)) {
                is AttendanceResult.Success -> {
                    _state.value = _state.value.copy(
                        loadingReport = false,
                        report = result.value,
                        errorMessage = null
                    )
                }
                is AttendanceResult.Failure -> {
                    _state.value = _state.value.copy(
                        loadingReport = false,
                        errorMessage = result.reason.institutionalMessage()
                    )
                }
            }
        }
    }

    fun exportAggregatedCsv(): AttendanceExportArtifact? {
        val rep = _state.value.report ?: return null
        val artifact = AttendanceExportHelper.exportAggregatedCsvToDisk(rep)
        _state.value = _state.value.copy(
            exportedArtifact = artifact,
            successMessage = "Reporte Agregado PMC descargado en: ${artifact.filePath}"
        )
        return artifact
    }

    fun exportNominalCsv(): AttendanceExportArtifact? {
        val rep = _state.value.report ?: return null
        val artifact = AttendanceExportHelper.exportRestrictedNominalCsvToDisk(rep)
        _state.value = _state.value.copy(
            exportedArtifact = artifact,
            successMessage = "Padrón Nominal Restringido descargado en: ${artifact.filePath}"
        )
        return artifact
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }

    fun closeSession() {
        _state.value = _state.value.copy(
            snapshot = null,
            draftEntries = emptyMap(),
            report = null,
            successMessage = null,
            errorMessage = null
        )
    }
}
