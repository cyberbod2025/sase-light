package com.example.data.attendance

import com.example.data.auth.AuthSession

/**
 * Contrato de repositorio de asistencia y puntualidad.
 *
 * Sigue el mismo patrón arquitectónico que [com.example.data.enrollment.SchoolMovementRepository]:
 * la aplicación opera contra esta interfaz desacoplada de la implementación concreta
 * (en memoria para DEMO_LOCAL y pruebas, Supabase con fail-closed en staging).
 */
interface AttendanceRepository {

    /**
     * Indica si el repositorio está operativo en el ambiente actual.
     * En staging remoto, retorna `false` hasta que la migración 0030 sea autorizada y aplicada.
     */
    val isAvailableInCurrentEnvironment: Boolean get() = true

    /**
     * Grupos escolares asignados a la membresía docente de la [session] activa.
     */
    suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>>

    /**
     * Programa una nueva sesión o recupera la existente para [groupId], [date] y [moduleNumber].
     * Es idempotente para la misma combinación grupo + docente + fecha + módulo.
     */
    suspend fun scheduleOrOpenSession(
        session: AuthSession,
        groupId: String,
        date: String,
        moduleNumber: Int = 1,
        status: SessionStatus = SessionStatus.PROGRAMADA,
        notes: String? = null
    ): AttendanceResult<ClassAttendanceSnapshot>

    /**
     * Actualiza el estatus operativo de una sesión (PROGRAMADA, CANCELADA, NO_LECTIVA).
     */
    suspend fun updateSessionStatus(
        session: AuthSession,
        sessionId: String,
        status: SessionStatus,
        notes: String? = null
    ): AttendanceResult<ClassAttendanceSnapshot>

    /**
     * Guarda la captura de asistencia de la sesión (admite captura parcial o completa).
     */
    suspend fun saveAttendance(
        session: AuthSession,
        sessionId: String,
        entries: List<AttendanceRecordEntry>
    ): AttendanceResult<ClassAttendanceSnapshot>

    /**
     * Genera el reporte consolidado de asistencia y puntualidad por sesiones aplicables.
     */
    suspend fun getGroupAttendanceReport(
        session: AuthSession,
        groupId: String,
        startDate: String,
        endDate: String,
        thresholds: AttendanceThresholds = AttendanceThresholds()
    ): AttendanceResult<ClassAttendanceReport>

    /**
     * Limpia los datos locales del modo demostración o reinicio de pruebas.
     */
    suspend fun resetDemoData() = Unit
}
