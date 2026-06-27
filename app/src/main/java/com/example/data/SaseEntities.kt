package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

data class SaseDocument(
    val name: String,
    val date: String,
    val status: String // "Vigente", "Vencido", "Pendiente"
)

data class SaseObservation(
    val text: String,
    val author: String,
    val date: String,
    val category: String = "Académica"
)

data class SaseIncident(
    val date: String,
    val type: String,
    val reporter: String,
    val status: String // "Atendida", "En seguimiento", "Pendiente"
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
    val status: String = "Activo", // "Activo", "En riesgo", "Nuevo ingreso", "Baja"
    val riskLevel: String = "Bajo", // "Bajo", "Medio", "Alto"
    val bap: String = "No", // "Sí", "No" (Barreras para el Aprendizaje y la Participación)
    val schoolInsurance: String = "Vigente", // "Vigente", "Vencido"
    val documentationStatus: String = "Completa", // "Completa", "Incompleta", "En revisión"
    val birthDate: String = "12/May/2010",
    val age: Int = 14,
    val birthPlace: String = "Ciudad de México",
    val address: String = "Av. Siempre Viva 123, Col. Centro, CDMX",
    val zipCode: String = "06000",
    val tutorName: String = "",
    val tutorRelation: String = "",
    val tutorPhone: String = "",
    val tutorEmail: String = "",
    val attendancePercent: Int = 92,
    val attendances: Int = 165,
    val excusedAbsences: Int = 8,
    val unexcusedAbsences: Int = 7,
    val healthAlergies: String = "Polvo, lácteos",
    val healthNotes: String = "Sin observaciones relevantes",
    val healthMeds: String = "No aplica",
    val healthPasses: String = "Ninguno registrado",
    val orientationStatus: String = "Activo",
    val orientationLastAppointment: String = "06/May/2024",
    val orientationInterventionPlan: String = "Plan de organización de tareas",
    val orientationResponsible: String = "Psic. Laura Méndez",
    val documents: List<SaseDocument> = emptyList(),
    val observations: List<SaseObservation> = emptyList(),
    val schoolIncidents: List<SaseIncident> = emptyList(),
    val audits: List<SaseAudit> = emptyList(),
    val photoUrl: String? = null
)
