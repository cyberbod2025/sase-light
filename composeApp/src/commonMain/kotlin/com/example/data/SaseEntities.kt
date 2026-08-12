package com.example.data

import com.example.audit.InstitutionalAuditEvent
import com.example.data.auth.institutionalLabel

data class SaseDocument(
    val name: String,
    val date: String,
    val status: String
)

data class SaseObservation(
    val text: String,
    val author: String,
    val date: String,
    val category: String = "Acad\u00e9mica"
)

data class SaseIncident(
    val date: String,
    val type: String,
    val reporter: String,
    val status: String,
    val id: String = "",
    val reportedByStaffId: String? = null,
    val agreementNotes: String? = null,
    val followUps: List<String> = emptyList()
)

data class SaseAudit(
    val action: String,
    val userRole: String,
    val timestamp: String,
    val detail: String = "",
    val institutionalEvent: InstitutionalAuditEvent? = null
) {
    companion object {
        /**
         * Adapta el evento tipado a la proyeccion historica que consume la UI.
         * Ningun actor, rol o detalle libre se recibe por separado: todo se
         * deriva del evento institucional previamente validado.
         */
        fun fromInstitutionalEvent(event: InstitutionalAuditEvent): SaseAudit = SaseAudit(
            action = event.action,
            userRole = event.activeRole.institutionalLabel(),
            timestamp = event.timestamp,
            detail = "${event.entityType}:${event.entityId} [${event.result.name}]",
            institutionalEvent = event
        )
    }
}

data class Student(
    val id: String,
    val fullName: String,
    val group: String,
    val enrollmentId: String,
    val curp: String,
    val shift: String = "",
    val schoolYear: String = "",
    val status: String = "",
    // Los siguientes campos NUNCA deben tener un default con apariencia de
    // dato real: en SUPABASE_STAGING, un campo que el mapper no pudo
    // completar (sin tabla, sin permiso, o aun no capturado) se queda en
    // este valor neutro y la UI debe mostrarlo como "No registrado", nunca
    // como si fuera informacion real del alumno (P1 de Codex en PR #49,
    // "Stop fabricating unpersisted student details").
    val riskLevel: String = "",
    val bap: String = "",
    val schoolInsurance: String = "",
    val documentationStatus: String = "",
    val birthDate: String = "",
    val age: Int = 0,
    val birthPlace: String = "",
    val address: String = "",
    val zipCode: String = "",
    val tutorName: String = "",
    val tutorRelation: String = "",
    val tutorPhone: String = "",
    val tutorEmail: String = "",
    val emergencyContactName: String = "",
    val emergencyContactRelation: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactEmail: String = "",
    val attendancePercent: Int = 0,
    val attendances: Int = 0,
    val excusedAbsences: Int = 0,
    val unexcusedAbsences: Int = 0,
    val healthAlergies: String = "",
    val healthNotes: String = "",
    val healthMeds: String = "",
    val healthPasses: String = "",
    val orientationStatus: String = "",
    val orientationLastAppointment: String = "",
    val orientationInterventionPlan: String = "",
    val orientationResponsible: String = "",
    val documents: List<SaseDocument> = emptyList(),
    val observations: List<SaseObservation> = emptyList(),
    val schoolIncidents: List<SaseIncident> = emptyList(),
    val audits: List<SaseAudit> = emptyList(),
    val photoUrl: String? = null,
    val preApplicationFolio: String? = null
)

sealed class StudentAddResult {
    data class Added(val student: Student) : StudentAddResult()
    data class DuplicateCurp(val curp: String, val existing: Student) : StudentAddResult()
    data class DuplicateEnrollmentId(val enrollmentId: String, val existing: Student) : StudentAddResult()
    data class InvalidData(val message: String) : StudentAddResult()

    /**
     * El almacenamiento institucional no confirmo el alta (sin sesion, permiso
     * denegado por RLS o fallo de red). Nunca debe presentarse como "guardado":
     * es la unica forma de distinguir un rechazo del backend de un dato invalido.
     */
    data class Failed(val reason: StudentPersistenceFailure) : StudentAddResult()
}

/** Resultado de una actualizacion; el mock nunca falla, el backend real si. */
sealed class StudentUpdateResult {
    data class Updated(val student: Student) : StudentUpdateResult()
    data class Failed(val reason: StudentPersistenceFailure) : StudentUpdateResult()
}

/**
 * Causa por la que el almacenamiento institucional rechazo una operacion.
 * Se mantiene como enum para que ningun mensaje del servidor —que podria
 * arrastrar datos de la fila rechazada— llegue tal cual a la interfaz.
 */
enum class StudentPersistenceFailure {
    /** No hay sesion institucional activa: se escribe nada, se falla cerrado. */
    NO_SESSION,

    /** El servidor rechazo la operacion (RLS, permiso o validacion). */
    REJECTED,

    /** La operacion no llego a completarse (red o servidor no disponible). */
    NETWORK,

    /**
     * El expediente se escribio, pero la bitacora institucional no quedo asentada.
     * La mutacion no se revierte: se reporta como no confirmada para que quien
     * llama no la trate como un alta exitosa sin evidencia.
     */
    AUDIT_NOT_RECORDED,
}

/**
 * Mensaje institucional para un rechazo de persistencia. Nunca reproduce texto
 * devuelto por el servidor, que podria arrastrar datos de la fila rechazada.
 */
fun institutionalFailureMessage(reason: StudentPersistenceFailure): String = when (reason) {
    StudentPersistenceFailure.NO_SESSION ->
        "No hay sesión institucional activa. El cambio no se guardó."
    StudentPersistenceFailure.REJECTED ->
        "El sistema institucional rechazó el cambio. No se guardó."
    StudentPersistenceFailure.NETWORK ->
        "No fue posible contactar al sistema institucional. El cambio no se guardó."
    StudentPersistenceFailure.AUDIT_NOT_RECORDED ->
        "El expediente se guardó, pero la bitácora institucional no quedó registrada. Repórtalo antes de continuar."
}
