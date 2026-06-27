package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MockSaseData {

    private val _students = MutableStateFlow<List<Student>>(createInitialStudents())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _audits = MutableStateFlow<List<SaseAudit>>(createInitialAudits())
    val audits: StateFlow<List<SaseAudit>> = _audits.asStateFlow()

    private fun createInitialStudents(): List<Student> {
        return listOf(
            Student(
                id = "1",
                fullName = "Mariana López Hernández",
                group = "2°B",
                enrollmentId = "2023-00258",
                curp = "LOHM100512MDFPRRA2",
                shift = "Vespertino",
                schoolYear = "2023-2024",
                status = "Activo",
                riskLevel = "Medio",
                bap = "No",
                schoolInsurance = "Vigente",
                documentationStatus = "Completa",
                birthDate = "12/May/2010",
                age = 14,
                birthPlace = "Ciudad de México",
                address = "Av. Siempre Viva 123, Col. Centro, CDMX",
                zipCode = "06000",
                tutorName = "José López Ramírez",
                tutorRelation = "Padre",
                tutorPhone = "55 3456 7890",
                tutorEmail = "jlopez@example.com",
                attendancePercent = 92,
                attendances = 165,
                excusedAbsences = 8,
                unexcusedAbsences = 7,
                healthAlergies = "Polvo, lácteos",
                healthNotes = "Sin observaciones relevantes",
                healthMeds = "No aplica",
                healthPasses = "Ninguno registrado",
                orientationStatus = "Activo",
                orientationLastAppointment = "06/May/2024",
                orientationInterventionPlan = "Plan de organización de tareas",
                orientationResponsible = "Psic. Laura Méndez",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "15/Ago/2010", "Vigente"),
                    SaseDocument("CURP", "15/Ago/2010", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "10/Ene/2024", "Vigente"),
                    SaseDocument("Boletas 1° grado", "30/Jun/2023", "Vigente"),
                    SaseDocument("Carta compromiso", "05/Ago/2023", "Vigente")
                ),
                observations = listOf(
                    SaseObservation(
                        text = "Buena participación en clase, muestra interés y compromiso. Se sugiere continuar reforzando organización en tareas.",
                        author = "Prof. M. Aguilar",
                        date = "08/May/2024"
                    )
                ),
                schoolIncidents = listOf(
                    SaseIncident("08/May/2024", "Atraso", "Prof. M. Aguilar", "Atendida"),
                    SaseIncident("30/Abr/2024", "Falta de material", "Prof. L. Torres", "Atendida"),
                    SaseIncident("18/Abr/2024", "Llamada de atención", "Coord. Convivencia", "En seguimiento"),
                    SaseIncident("05/Abr/2024", "Atraso", "Prof. M. Aguilar", "Atendida")
                ),
                audits = listOf(
                    SaseAudit("Expediente validado", "Secretaría", "Hoy 11:25", "Verificación de CURP y Acta"),
                    SaseAudit("Observación registrada", "Prof. M. Aguilar", "08/May/2024", "Nota de conducta")
                )
            ),
            Student(
                id = "2",
                fullName = "Diego Morales Sánchez",
                group = "1°A",
                enrollmentId = "2023-00104",
                curp = "MOSD110204MDFXNN05",
                shift = "Matutino",
                schoolYear = "2023-2024",
                status = "Activo",
                riskLevel = "Bajo",
                bap = "No",
                schoolInsurance = "Vigente",
                documentationStatus = "Completa",
                birthDate = "04/Feb/2011",
                age = 13,
                birthPlace = "Estado de México",
                address = "Paseo de la Reforma 456, CDMX",
                zipCode = "06500",
                tutorName = "Laura Sánchez Díaz",
                tutorRelation = "Madre",
                tutorPhone = "55 2345 6789",
                tutorEmail = "lauras@example.com",
                attendancePercent = 98,
                attendances = 175,
                excusedAbsences = 2,
                unexcusedAbsences = 1,
                healthAlergies = "Ninguna",
                healthNotes = "Excelente salud general",
                healthMeds = "No aplica",
                healthPasses = "Ninguno",
                orientationStatus = "Sin seguimiento",
                orientationLastAppointment = "N/A",
                orientationInterventionPlan = "Ninguno",
                orientationResponsible = "N/A",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "10/Feb/2011", "Vigente"),
                    SaseDocument("CURP", "10/Feb/2011", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "15/Dic/2023", "Vigente"),
                    SaseDocument("Carta compromiso", "12/Ago/2023", "Vigente")
                ),
                observations = listOf(
                    SaseObservation(
                        text = "Alumno muy dedicado. Ha obtenido las calificaciones más altas de su grupo en Matemáticas.",
                        author = "Prof. R. Martínez",
                        date = "15/May/2024"
                    )
                ),
                schoolIncidents = emptyList(),
                audits = listOf(
                    SaseAudit("Expediente creado", "Secretaría", "12/Ago/2023", "Registro de matrícula")
                )
            ),
            Student(
                id = "3",
                fullName = "Valeria Jiménez Torres",
                group = "3°C",
                enrollmentId = "2022-00512",
                curp = "JITV090812MDFZRR01",
                shift = "Vespertino",
                schoolYear = "2023-2024",
                status = "En riesgo",
                riskLevel = "Alto",
                bap = "Sí",
                schoolInsurance = "Vigente",
                documentationStatus = "Incompleta",
                birthDate = "12/Ago/2009",
                age = 15,
                birthPlace = "Guadalajara, Jal.",
                address = "Calle de las Flores 789, CDMX",
                zipCode = "04000",
                tutorName = "Carlos Jiménez Ruiz",
                tutorRelation = "Padre",
                tutorPhone = "55 5678 9012",
                tutorEmail = "cjimenez@example.com",
                attendancePercent = 78,
                attendances = 140,
                excusedAbsences = 12,
                unexcusedAbsences = 22,
                healthAlergies = "Asma",
                healthNotes = "Requiere inhalador en caso de crisis",
                healthMeds = "Salbutamol",
                healthPasses = "2 pases de emergencia",
                orientationStatus = "En seguimiento",
                orientationLastAppointment = "10/May/2024",
                orientationInterventionPlan = "Plan de regularización académica",
                orientationResponsible = "Psic. Laura Méndez",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "20/Ago/2009", "Vigente"),
                    SaseDocument("CURP", "20/Ago/2009", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "Último vencido", "Pendiente"),
                    SaseDocument("Boleta 2° grado", "Falta firma", "Pendiente")
                ),
                observations = listOf(
                    SaseObservation(
                        text = "Muestra rezago debido a faltas frecuentes. Es urgente platicar con el tutor para definir plan de acción.",
                        author = "Prof. G. Olvera",
                        date = "12/May/2024"
                    )
                ),
                schoolIncidents = listOf(
                    SaseIncident("12/May/2024", "Inasistencia injustificada", "Prof. G. Olvera", "En seguimiento"),
                    SaseIncident("03/May/2024", "No entrega tareas", "Prof. G. Olvera", "En seguimiento")
                ),
                audits = listOf(
                    SaseAudit("Alerta de riesgo activada", "Sistema", "Ayer 08:30", "Faltas acumuladas")
                )
            ),
            Student(
                id = "4",
                fullName = "Emiliano Cruz Martínez",
                group = "2°A",
                enrollmentId = "2023-00215",
                curp = "CUME100115MDFKLL03",
                shift = "Matutino",
                schoolYear = "2023-2024",
                status = "Activo",
                riskLevel = "Bajo",
                bap = "No",
                schoolInsurance = "Vigente",
                documentationStatus = "Completa",
                birthDate = "15/Ene/2010",
                age = 14,
                birthPlace = "Puebla, Pue.",
                address = "Avenida Insurgentes Sur 987, CDMX",
                zipCode = "03100",
                tutorName = "Martha Martínez Vega",
                tutorRelation = "Madre",
                tutorPhone = "55 9876 5432",
                tutorEmail = "martha.martinez@example.com",
                attendancePercent = 95,
                attendances = 170,
                excusedAbsences = 4,
                unexcusedAbsences = 5,
                healthAlergies = "Ninguna",
                healthNotes = "Sin particularidades",
                healthMeds = "No aplica",
                healthPasses = "Ninguno",
                orientationStatus = "Sin seguimiento",
                orientationLastAppointment = "N/A",
                orientationInterventionPlan = "Ninguno",
                orientationResponsible = "N/A",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "18/Ene/2010", "Vigente"),
                    SaseDocument("CURP", "18/Ene/2010", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "22/Ene/2024", "Vigente")
                ),
                observations = listOf(
                    SaseObservation(
                        text = "Excelente conducta y participación en actividades cívicas de la escuela.",
                        author = "Profra. S. Juárez",
                        date = "22/Abr/2024"
                    )
                ),
                schoolIncidents = emptyList(),
                audits = listOf(
                    SaseAudit("Expediente validado", "Secretaría", "Ayer 10:17", "Revisión ordinaria")
                )
            ),
            Student(
                id = "5",
                fullName = "Sofía Ramírez Ortega",
                group = "1°B",
                enrollmentId = "2023-00098",
                curp = "RAOS110419MDFYSS08",
                shift = "Matutino",
                schoolYear = "2023-2024",
                status = "Nuevo ingreso",
                riskLevel = "Bajo",
                bap = "No",
                schoolInsurance = "Vigente",
                documentationStatus = "En revisión",
                birthDate = "19/Abr/2011",
                age = 13,
                birthPlace = "Querétaro, Qro.",
                address = "Eje Central Lázaro Cárdenas 12, CDMX",
                zipCode = "01000",
                tutorName = "Pedro Ramírez López",
                tutorRelation = "Padre",
                tutorPhone = "55 8765 4321",
                tutorEmail = "pramirez@example.com",
                attendancePercent = 90,
                attendances = 160,
                excusedAbsences = 10,
                unexcusedAbsences = 6,
                healthAlergies = "Nuez",
                healthNotes = "Evitar contacto con alérgenos alimentarios",
                healthMeds = "Antihistamínico",
                healthPasses = "Ninguno",
                orientationStatus = "Sin seguimiento",
                orientationLastAppointment = "N/A",
                orientationInterventionPlan = "Ninguno",
                orientationResponsible = "N/A",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "25/Abr/2011", "Vigente"),
                    SaseDocument("CURP", "25/Abr/2011", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "Pendiente de cotejo", "En revisión")
                ),
                observations = emptyList(),
                schoolIncidents = emptyList(),
                audits = listOf(
                    SaseAudit("Pre-registro recibido", "Secretaría", "Hoy 09:12", "Trámite de admisión")
                )
            )
        )
    }

    private fun createInitialAudits(): List<SaseAudit> {
        return listOf(
            SaseAudit("Expediente validado", "Secretaría", "Hoy 11:25", "Mariana López Hernández (2°B)"),
            SaseAudit("Alumno inscrito", "Secretaría", "Hoy 10:17", "Emiliano Cruz Martínez (2°A)"),
            SaseAudit("Documento emitido", "Secretaría", "Hoy 09:42", "Constancia de estudios para Mariana López"),
            SaseAudit("Cambio de grupo autorizado", "Dirección", "Ayer 16:30", "Diego Morales Sánchez (1°A)"),
            SaseAudit("Solicitud recibida", "Secretaría", "Ayer 13:12", "Carta compromiso de Valeria Jiménez")
        )
    }

    fun updateStudent(updated: Student) {
        val currentList = _students.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            currentList[index] = updated
            _students.value = currentList
        }
    }

    fun addStudent(student: Student) {
        val currentList = _students.value.toMutableList()
        currentList.add(student)
        _students.value = currentList
        logAudit("Alumno registrado", "Secretaría", "Hoy " + getFormattedTime(), student.fullName)
    }

    fun logAudit(action: String, role: String, timestamp: String, detail: String) {
        val currentList = _audits.value.toMutableList()
        currentList.add(0, SaseAudit(action, role, timestamp, detail))
        _audits.value = currentList
    }

    private fun getFormattedTime(): String {
        return java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    }
}
