package com.example.data.attendance

/**
 * Rebanada vertical "asistencia por clase y grupo".
 *
 * El modelo reutiliza la identidad institucional ya vigente
 * ([com.example.data.auth.AuthSession]): el docente, su membresia y su rol
 * activo NO se vuelven a modelar aqui. Estas entidades solo describen el
 * grupo, su lista y la captura de una fecha.
 */

/** Estado de asistencia de un alumno en una sesion de clase. */
enum class AttendanceStatus {
    PRESENTE,
    AUSENTE,
    RETARDO;

    companion object {
        fun fromCode(code: String): AttendanceStatus? =
            entries.firstOrNull { it.name == code.trim().uppercase() }
    }
}

fun AttendanceStatus.institutionalLabel(): String = when (this) {
    AttendanceStatus.PRESENTE -> "Presente"
    AttendanceStatus.AUSENTE -> "Ausente"
    AttendanceStatus.RETARDO -> "Retardo"
}

/** Grupo escolar que un docente tiene asignado. */
data class TeacherGroup(
    val id: String,
    val name: String,
    val gradeLabel: String,
    val schoolCycleId: String
) {
    init {
        require(id.isNotBlank()) { "El identificador del grupo no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre del grupo no puede estar vacío." }
    }
}

/** Alumno de la lista de un grupo. Datos ficticios de demo, nunca reales. */
data class GroupStudent(
    val id: String,
    val fullName: String,
    val listNumber: Int
) {
    init {
        require(id.isNotBlank()) { "El identificador del alumno no puede estar vacío." }
        require(listNumber > 0) { "El número de lista debe ser positivo." }
    }
}

/**
 * Una fila de captura: alumno + estado. [recorded] indica si ya existe un
 * registro persistido para ese alumno en esta sesion (falso = el estado
 * mostrado es el valor por omision "todos presentes", aun sin guardar).
 */
data class AttendanceEntry(
    val student: GroupStudent,
    val status: AttendanceStatus,
    val recorded: Boolean
)

/**
 * Sesion de clase de una fecha con el roster COMPLETO.
 *
 * Invariante del servidor y del mock: existe una sola sesion por
 * grupo + docente + fecha, y un solo registro por sesion + alumno.
 */
data class ClassAttendanceSnapshot(
    val sessionId: String,
    val groupId: String,
    val groupName: String,
    val gradeLabel: String,
    val date: String,
    val entries: List<AttendanceEntry>
) {
    init {
        require(sessionId.isNotBlank()) { "La sesión de clase debe tener identificador." }
        require(entries.map { it.student.id }.toSet().size == entries.size) {
            "El roster de una sesión no puede repetir alumnos."
        }
    }

    /** Verdadero cuando la fecha ya tiene una captura persistida. */
    val hasSavedCapture: Boolean get() = entries.any { it.recorded }

    fun statusOf(studentId: String): AttendanceStatus? =
        entries.firstOrNull { it.student.id == studentId }?.status
}

/** Motivo de fallo expresado en el vocabulario del dominio, nunca un error crudo. */
enum class AttendanceFailureReason {
    NOT_A_TEACHER,
    NOT_ASSIGNED_TO_GROUP,
    SESSION_NOT_FOUND,
    INCOMPLETE_ROSTER,
    INVALID_DATA,
    NETWORK,
    UNEXPECTED
}

fun AttendanceFailureReason.institutionalMessage(): String = when (this) {
    AttendanceFailureReason.NOT_A_TEACHER ->
        "La sesión activa no tiene rol de Docente."
    AttendanceFailureReason.NOT_ASSIGNED_TO_GROUP ->
        "Este grupo no está asignado a la sesión docente activa."
    AttendanceFailureReason.SESSION_NOT_FOUND ->
        "La sesión de clase ya no está disponible. Vuelve a abrir el grupo."
    AttendanceFailureReason.INCOMPLETE_ROSTER ->
        "El pase de lista debe enviarse completo. Vuelve a abrir el grupo e inténtalo de nuevo."
    AttendanceFailureReason.INVALID_DATA ->
        "Los datos de la captura no son válidos."
    AttendanceFailureReason.NETWORK ->
        "No fue posible contactar al servidor. Revisa tu conexión e inténtalo otra vez."
    AttendanceFailureReason.UNEXPECTED ->
        "Ocurrió un problema al procesar la asistencia. Inténtalo de nuevo."
}

/** Resultado de dominio: ningun consumidor recibe excepciones ni errores HTTP. */
sealed class AttendanceResult<out T> {
    data class Success<T>(val value: T) : AttendanceResult<T>()
    data class Failure(val reason: AttendanceFailureReason) : AttendanceResult<Nothing>()
}
