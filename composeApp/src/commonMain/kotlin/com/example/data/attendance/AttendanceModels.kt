package com.example.data.attendance

import kotlinx.serialization.Serializable

/**
 * Rebanada vertical de Asistencia y Puntualidad (PMC) para SASE-Light.
 *
 * Reutiliza la identidad institucional ya vigente ([com.example.data.auth.AuthSession]):
 * el docente, su membresía institucional y sus roles activos no se duplican.
 *
 * Distingue explícitamente:
 * 1. Estatus de sesión: PROGRAMADA, CANCELADA, NO_LECTIVA.
 * 2. Estatus de captura: PENDIENTE, PARCIAL, COMPLETA.
 * 3. Presencia física: PRESENTE vs AUSENTE.
 * 4. Puntualidad (asociada a una persona presente): A_TIEMPO vs RETARDO (con minutos).
 * 5. Reporte de retardos: eventos totales de retardo vs alumnos distintos con retardo.
 * 6. Umbrales configurables y pendientes de validación colegiada (CTE).
 */

/** Estatus operativo de la sesión de clase. */
@Serializable
enum class SessionStatus(val label: String, val isTeachingSession: Boolean) {
    PROGRAMADA("Sesión Programada", true),
    CANCELADA("Sesión Cancelada / Suspendida", false),
    NO_LECTIVA("Día No Lectivo / Actividad Cívica / CTE", false);

    companion object {
        fun fromCode(code: String): SessionStatus =
            entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) } ?: PROGRAMADA
    }
}

/** Estatus de completitud de la captura de asistencia en una sesión. */
@Serializable
enum class CaptureState(val label: String) {
    PENDIENTE("Pendiente de captura"),
    PARCIAL("Captura parcial"),
    COMPLETA("Captura completa")
}

/** Estatus fundamental de presencia en clase. */
@Serializable
enum class AttendancePresence(val label: String) {
    PRESENTE("Presente"),
    AUSENTE("Ausente");

    companion object {
        fun fromCode(code: String?): AttendancePresence? =
            if (code.isNullOrBlank()) null
            else entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) }
    }
}

/** Estatus de puntualidad (asociado a presencia). Si el alumno está ausente, no aplica. */
@Serializable
enum class PunctualityStatus(val label: String) {
    A_TIEMPO("A tiempo"),
    RETARDO("Retardo"),
    NO_APLICA("No aplica (Ausente)");

    companion object {
        fun fromCode(code: String?): PunctualityStatus =
            if (code.isNullOrBlank()) NO_APLICA
            else entries.firstOrNull { it.name.equals(code.trim(), ignoreCase = true) } ?: NO_APLICA
    }
}

/** Grupo escolar asignado a un docente. */
@Serializable
data class TeacherGroup(
    val id: String,
    val name: String,
    val gradeLabel: String,
    val schoolCycleId: String,
    val institutionId: String = "INST-001"
) {
    init {
        require(id.isNotBlank()) { "El identificador del grupo no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre del grupo no puede estar vacío." }
    }
}

/** Alumno vigente en la lista de un grupo. */
@Serializable
data class GroupStudent(
    val id: String,
    val fullName: String,
    val listNumber: Int,
    val active: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "El identificador del alumno no puede estar vacío." }
        require(listNumber > 0) { "El número de lista debe ser positivo." }
    }
}

/**
 * Fila enviada o persistida en la captura de asistencia.
 * [presence] nulo indica que la asistencia para este alumno aún está pendiente en la sesión.
 */
@Serializable
data class AttendanceRecordEntry(
    val studentId: String,
    val presence: AttendancePresence?,
    val punctuality: PunctualityStatus = if (presence == AttendancePresence.PRESENTE) PunctualityStatus.A_TIEMPO else PunctualityStatus.NO_APLICA,
    val minutesLate: Int = 0,
    val justification: String? = null,
    val recordedAt: String? = null,
    val recordedBy: String? = null
) {
    init {
        require(studentId.isNotBlank()) { "El identificador del alumno no puede estar vacío." }
        if (presence == AttendancePresence.AUSENTE) {
            require(punctuality == PunctualityStatus.NO_APLICA) {
                "Un alumno ausente no puede tener retardo ni estatus de puntualidad a tiempo."
            }
        }
        if (punctuality == PunctualityStatus.RETARDO) {
            require(presence == AttendancePresence.PRESENTE) {
                "El retardo debe asociarse a una persona presente."
            }
        }
    }
}

/**
 * Fila en la vista del pase de lista combinando los datos del alumno y su registro de asistencia.
 */
data class AttendanceStudentEntry(
    val student: GroupStudent,
    val presence: AttendancePresence?,
    val punctuality: PunctualityStatus,
    val minutesLate: Int = 0,
    val justification: String? = null,
    val isRecorded: Boolean = false
) {
    val isPending: Boolean get() = presence == null
    val isPresent: Boolean get() = presence == AttendancePresence.PRESENTE
    val isAbsent: Boolean get() = presence == AttendancePresence.AUSENTE
    val isLate: Boolean get() = isPresent && punctuality == PunctualityStatus.RETARDO
}

/**
 * Fotografía completa de una sesión de clase y su captura.
 *
 * Soporta módulos dobles mediante [moduleNumber] (ej. Módulo 1 y Módulo 2 en la misma fecha).
 */
data class ClassAttendanceSnapshot(
    val sessionId: String,
    val groupId: String,
    val groupName: String,
    val gradeLabel: String,
    val date: String,
    val moduleNumber: Int = 1,
    val sessionStatus: SessionStatus = SessionStatus.PROGRAMADA,
    val notes: String? = null,
    val entries: List<AttendanceStudentEntry> = emptyList()
) {
    init {
        require(sessionId.isNotBlank()) { "La sesión de clase debe tener identificador." }
        require(moduleNumber in 1..8) { "El número de módulo debe estar entre 1 y 8." }
        require(entries.map { it.student.id }.toSet().size == entries.size) {
            "El pase de lista no puede repetir alumnos."
        }
    }

    val captureState: CaptureState
        get() {
            if (entries.isEmpty()) return CaptureState.PENDIENTE
            val recordedCount = entries.count { it.isRecorded }
            return when {
                recordedCount == 0 -> CaptureState.PENDIENTE
                recordedCount == entries.size -> CaptureState.COMPLETA
                else -> CaptureState.PARCIAL
            }
        }

    val presentCount: Int get() = entries.count { it.isPresent }
    val absentCount: Int get() = entries.count { it.isAbsent }
    val lateCount: Int get() = entries.count { it.isLate }
    val pendingCount: Int get() = entries.count { it.isPending }
}

/**
 * Parámetros configurables de alerta para asistencia y puntualidad.
 * Declarados explícitamente como referenciales y pendientes de validación colegiada del CTE.
 */
@Serializable
data class AttendanceThresholds(
    val minAttendanceRatePct: Double = 85.0,
    val maxTardinessEvents: Int = 3,
    val isFormalAgreementApproved: Boolean = false,
    val approvalReference: String = "PENDIENTE_APROBACION_CTE",
    val statusNotice: String = "Umbral referencial configurable. Pendiente de aprobación formal por Consejo Técnico Escolar."
)

/**
 * Métrica individual de asistencia calculada por sesiones aplicables al estudiante.
 */
data class StudentAttendanceMetric(
    val studentId: String,
    val fullName: String,
    val assignedGroup: String,
    val listNumber: Int,
    val applicableSessions: Int,
    val attendedSessions: Int,
    val absentSessions: Int,
    val pendingSessions: Int,
    val tardinessEvents: Int,
    val attendanceRatePct: Double,
    val hasAttendanceAlert: Boolean,
    val hasTardinessAlert: Boolean
) {
    val alertSummary: String
        get() = when {
            hasAttendanceAlert && hasTardinessAlert -> "Alerta Crítica: Inasistencia y Retardos"
            hasAttendanceAlert -> "Alerta: Baja Asistencia (< 85%)"
            hasTardinessAlert -> "Alerta: Retardos Reiterados (≥ 3)"
            else -> "Regular"
        }
}

/**
 * Reporte consolidado de asistencia por grupo para el Programa de Mejora Continua (PMC).
 */
data class ClassAttendanceReport(
    val groupId: String,
    val groupName: String,
    val periodStart: String,
    val periodEnd: String,
    val totalSessionsScheduled: Int,
    val sessionsHeld: Int,
    val sessionsCancelled: Int,
    val sessionsNonTeaching: Int,
    val totalAttendanceEvents: Int,
    val totalAbsenceEvents: Int,
    val totalTardinessEvents: Int,
    val distinctStudentsWithTardiness: Int,
    val overallAttendanceRatePct: Double,
    val thresholds: AttendanceThresholds,
    val studentMetrics: List<StudentAttendanceMetric>
)

/** Motivo de fallo expresado en vocabulario del dominio institucional. */
enum class AttendanceFailureReason {
    NOT_A_TEACHER,
    NOT_ASSIGNED_TO_GROUP,
    SESSION_NOT_FOUND,
    INVALID_DATA,
    DUPLICATE_SESSION,
    NETWORK,
    UNEXPECTED,
    REMOTE_PERSISTENCE_NOT_READY
}

fun AttendanceFailureReason.institutionalMessage(): String = when (this) {
    AttendanceFailureReason.NOT_A_TEACHER ->
        "La sesión activa no cuenta con rol de Docente."
    AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP ->
        "El grupo escolar seleccionado no está asignado al docente activo."
    AttendanceFailureReason.SESSION_NOT_FOUND ->
        "La sesión de clase solicitada no fue encontrada."
    AttendanceFailureReason.INVALID_DATA ->
        "Los datos de captura o de la sesión no cumplen con los requisitos de validación."
    AttendanceFailureReason.DUPLICATE_SESSION ->
        "Ya existe una sesión programada para este grupo, docente, fecha y módulo."
    AttendanceFailureReason.NETWORK ->
        "No se pudo establecer comunicación con el servidor. Revisa tu conexión de red."
    AttendanceFailureReason.REMOTE_PERSISTENCE_NOT_READY ->
        "La persistencia remota de asistencia no está disponible en este ambiente hasta aplicar y validar la migración."
    AttendanceFailureReason.UNEXPECTED ->
        "Ocurrió un error inesperado al procesar la asistencia escolar."
}

/** Contenedor de resultado del dominio sin filtrar excepciones técnicas. */
sealed class AttendanceResult<out T> {
    data class Success<T>(val value: T) : AttendanceResult<T>()
    data class Failure(val reason: AttendanceFailureReason) : AttendanceResult<Nothing>()
}
