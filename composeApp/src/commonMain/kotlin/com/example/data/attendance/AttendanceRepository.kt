package com.example.data.attendance

import com.example.data.auth.AuthSession

/**
 * Costura de datos de asistencia, con la misma forma que
 * [com.example.data.auth.AuthRepository]: la app solo conoce esta interfaz y
 * el mock local y la implementacion Supabase son intercambiables desde
 * [com.example.SaseCompositionRoot].
 *
 * Toda operacion recibe la [AuthSession] activa: la autorizacion nunca se
 * deriva de un parametro suelto que la UI pueda falsificar.
 */
interface AttendanceRepository {

    /** Grupos que la membresia docente de [session] tiene asignados. */
    suspend fun groupsForTeacher(session: AuthSession): AttendanceResult<List<TeacherGroup>>

    /**
     * Inicia o recupera la sesion de clase de [date] para [groupId].
     * Idempotente: dos llamadas devuelven la misma sesion, nunca una duplicada.
     */
    suspend fun openClassSession(
        session: AuthSession,
        groupId: String,
        date: String
    ): AttendanceResult<ClassAttendanceSnapshot>

    /**
     * Guarda la captura en UNA sola operacion logica.
     *
     * [entries] debe cubrir el roster COMPLETO del grupo, no solo los alumnos
     * modificados: si el docente deja a todos en PRESENTE sin tocar nada, esa
     * captura tiene que persistir igual. Un envio parcial se rechaza entero con
     * [AttendanceFailureReason.INCOMPLETE_ROSTER].
     */
    suspend fun saveClassAttendance(
        session: AuthSession,
        classSessionId: String,
        entries: List<Pair<String, AttendanceStatus>>
    ): AttendanceResult<ClassAttendanceSnapshot>

    /**
     * Restaura el estado sintetico del modo demo. Solo la implementacion local
     * hace algo: un backend real nunca se "reinicia" desde el cliente.
     */
    suspend fun resetDemoData() = Unit
}
