package com.example.data

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
    val status: String
)

data class SaseAudit(
    val action: String,
    val userRole: String,
    val timestamp: String,
    val detail: String = ""
)

data class Student(
    val id: String,
    val fullName: String,
    val group: String,
    val enrollmentId: String,
    val curp: String,
    val shift: String = "Vespertino",
    val schoolYear: String = "2023-2024",
    val status: String = "Activo",
    val riskLevel: String = "Bajo",
    val bap: String = "No",
    val schoolInsurance: String = "Vigente",
    val documentationStatus: String = "Completa",
    val birthDate: String = "12/May/2010",
    val age: Int = 14,
    val birthPlace: String = "Ciudad de M\u00e9xico",
    val address: String = "Av. Siempre Viva 123, Col. Centro, CDMX",
    val zipCode: String = "06000",
    val tutorName: String = "",
    val tutorRelation: String = "",
    val tutorPhone: String = "",
    val tutorEmail: String = "",
    val emergencyContactName: String = "",
    val emergencyContactRelation: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactEmail: String = "",
    val attendancePercent: Int = 92,
    val attendances: Int = 165,
    val excusedAbsences: Int = 8,
    val unexcusedAbsences: Int = 7,
    val healthAlergies: String = "Polvo, l\u00e1cteos",
    val healthNotes: String = "Sin observaciones relevantes",
    val healthMeds: String = "No aplica",
    val healthPasses: String = "Ninguno registrado",
    val orientationStatus: String = "Activo",
    val orientationLastAppointment: String = "06/May/2024",
    val orientationInterventionPlan: String = "Plan de organizaci\u00f3n de tareas",
    val orientationResponsible: String = "Psic. Laura M\u00e9ndez",
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
}
