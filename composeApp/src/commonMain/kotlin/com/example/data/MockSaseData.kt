package com.example.data

import com.example.formatTimestamp
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
                fullName = "Mariana L\u00f3pez Hern\u00e1ndez",
                group = "2\u00b0B",
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
                birthPlace = "Ciudad de M\u00e9xico",
                address = "Av. Siempre Viva 123, Col. Centro, CDMX",
                zipCode = "06000",
                tutorName = "Jos\u00e9 L\u00f3pez Ram\u00edrez",
                tutorRelation = "Padre",
                tutorPhone = "55 3456 7890",
                tutorEmail = "jlopez@example.com",
                attendancePercent = 92,
                attendances = 165,
                excusedAbsences = 8,
                unexcusedAbsences = 7,
                healthAlergies = "Polvo, l\u00e1cteos",
                healthNotes = "Sin observaciones relevantes",
                healthMeds = "No aplica",
                healthPasses = "Ninguno registrado",
                orientationStatus = "Activo",
                orientationLastAppointment = "06/May/2024",
                orientationInterventionPlan = "Plan de organizaci\u00f3n de tareas",
                orientationResponsible = "Psic. Laura M\u00e9ndez",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "15/Ago/2010", "Vigente"),
                    SaseDocument("CURP", "15/Ago/2010", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "10/Ene/2024", "Vigente"),
                    SaseDocument("Boletas 1\u00b0 grado", "30/Jun/2023", "Vigente"),
                    SaseDocument("Carta compromiso", "05/Ago/2023", "Vigente")
                ),
                observations = listOf(
                    SaseObservation(
                        text = "Buena participaci\u00f3n en clase, muestra inter\u00e9s y compromiso.",
                        author = "Prof. M. Aguilar",
                        date = "08/May/2024"
                    )
                ),
                schoolIncidents = listOf(
                    SaseIncident("08/May/2024", "Atraso", "Prof. M. Aguilar", "Atendida"),
                    SaseIncident("30/Abr/2024", "Falta de material", "Prof. L. Torres", "Atendida"),
                    SaseIncident("18/Abr/2024", "Llamada de atenci\u00f3n", "Coord. Convivencia", "En seguimiento"),
                    SaseIncident("05/Abr/2024", "Atraso", "Prof. M. Aguilar", "Atendida")
                ),
                audits = listOf(
                    SaseAudit("Expediente validado", "Secretar\u00eda", "Hoy 11:25", "Verificaci\u00f3n de CURP y Acta"),
                    SaseAudit("Observaci\u00f3n registrada", "Prof. M. Aguilar", "08/May/2024", "Nota de conducta")
                )
            ),
            Student(
                id = "2",
                fullName = "Diego Morales S\u00e1nchez",
                group = "1\u00b0A",
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
                birthPlace = "Estado de M\u00e9xico",
                address = "Paseo de la Reforma 456, CDMX",
                zipCode = "06500",
                tutorName = "Laura S\u00e1nchez D\u00edaz",
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
                        text = "Alumno muy dedicado. Ha obtenido las calificaciones m\u00e1s altas de su grupo en Matem\u00e1ticas.",
                        author = "Prof. R. Mart\u00ednez",
                        date = "15/May/2024"
                    )
                ),
                schoolIncidents = emptyList(),
                audits = listOf(
                    SaseAudit("Expediente creado", "Secretar\u00eda", "12/Ago/2023", "Registro de matr\u00edcula")
                )
            ),
            Student(
                id = "3",
                fullName = "Valeria Jim\u00e9nez Torres",
                group = "3\u00b0C",
                enrollmentId = "2022-00512",
                curp = "JITV090812MDFZRR01",
                shift = "Vespertino",
                schoolYear = "2023-2024",
                status = "En riesgo",
                riskLevel = "Alto",
                bap = "S\u00ed",
                schoolInsurance = "Vigente",
                documentationStatus = "Incompleta",
                birthDate = "12/Ago/2009",
                age = 15,
                birthPlace = "Guadalajara, Jal.",
                address = "Calle de las Flores 789, CDMX",
                zipCode = "04000",
                tutorName = "Carlos Jim\u00e9nez Ruiz",
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
                orientationInterventionPlan = "Plan de regularizaci\u00f3n acad\u00e9mica",
                orientationResponsible = "Psic. Laura M\u00e9ndez",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "20/Ago/2009", "Vigente"),
                    SaseDocument("CURP", "20/Ago/2009", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "\u00daltimo vencido", "Pendiente"),
                    SaseDocument("Boleta 2\u00b0 grado", "Falta firma", "Pendiente")
                ),
                observations = listOf(
                    SaseObservation(
                        text = "Muestra rezago debido a faltas frecuentes.",
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
                fullName = "Emiliano Cruz Mart\u00ednez",
                group = "2\u00b0A",
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
                tutorName = "Martha Mart\u00ednez Vega",
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
                        text = "Excelente conducta y participaci\u00f3n en actividades c\u00edvicas de la escuela.",
                        author = "Profra. S. Ju\u00e1rez",
                        date = "22/Abr/2024"
                    )
                ),
                schoolIncidents = emptyList(),
                audits = listOf(
                    SaseAudit("Expediente validado", "Secretar\u00eda", "Ayer 10:17", "Revisi\u00f3n ordinaria")
                )
            ),
            Student(
                id = "5",
                fullName = "Sof\u00eda Ram\u00edrez Ortega",
                group = "1\u00b0B",
                enrollmentId = "2023-00098",
                curp = "RAOS110419MDFYSS08",
                shift = "Matutino",
                schoolYear = "2023-2024",
                status = "Nuevo ingreso",
                riskLevel = "Bajo",
                bap = "No",
                schoolInsurance = "Vigente",
                documentationStatus = "En revisi\u00f3n",
                birthDate = "19/Abr/2011",
                age = 13,
                birthPlace = "Quer\u00e9taro, Qro.",
                address = "Eje Central L\u00e1zaro C\u00e1rdenas 12, CDMX",
                zipCode = "01000",
                tutorName = "Pedro Ram\u00edrez L\u00f3pez",
                tutorRelation = "Padre",
                tutorPhone = "55 8765 4321",
                tutorEmail = "pramirez@example.com",
                attendancePercent = 90,
                attendances = 160,
                excusedAbsences = 10,
                unexcusedAbsences = 6,
                healthAlergies = "Nuez",
                healthNotes = "Evitar contacto con al\u00e9rgenos alimentarios",
                healthMeds = "Antihistam\u00ednico",
                healthPasses = "Ninguno",
                orientationStatus = "Sin seguimiento",
                orientationLastAppointment = "N/A",
                orientationInterventionPlan = "Ninguno",
                orientationResponsible = "N/A",
                documents = listOf(
                    SaseDocument("Acta de nacimiento", "25/Abr/2011", "Vigente"),
                    SaseDocument("CURP", "25/Abr/2011", "Vigente"),
                    SaseDocument("Comprobante de domicilio", "Pendiente de cotejo", "En revisi\u00f3n")
                ),
                observations = emptyList(),
                schoolIncidents = emptyList(),
                audits = listOf(
                    SaseAudit("Pre-registro recibido", "Secretar\u00eda", "Hoy 09:12", "Tr\u00e1mite de admisi\u00f3n")
                )
            )
        )
    }

    private fun createInitialAudits(): List<SaseAudit> {
        return listOf(
            SaseAudit("Expediente validado", "Secretar\u00eda", "Hoy 11:25", "Mariana L\u00f3pez Hern\u00e1ndez (2\u00b0B)"),
            SaseAudit("Alumno inscrito", "Secretar\u00eda", "Hoy 10:17", "Emiliano Cruz Mart\u00ednez (2\u00b0A)"),
            SaseAudit("Documento emitido", "Secretar\u00eda", "Hoy 09:42", "Constancia de estudios para Mariana L\u00f3pez"),
            SaseAudit("Cambio de grupo autorizado", "Direcci\u00f3n", "Ayer 16:30", "Diego Morales S\u00e1nchez (1\u00b0A)"),
            SaseAudit("Solicitud recibida", "Secretar\u00eda", "Ayer 13:12", "Carta compromiso de Valeria Jim\u00e9nez")
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

    fun addStudent(student: Student): StudentAddResult {
        val cleanCurp = student.curp.trim().uppercase()
        val cleanEnrollmentId = student.enrollmentId.trim().uppercase()
        if (cleanCurp.isBlank() || cleanEnrollmentId.isBlank()) {
            return StudentAddResult.InvalidData("CURP y matrícula son obligatorias.")
        }

        val duplicateCurp = _students.value.firstOrNull { it.curp.trim().uppercase() == cleanCurp }
        if (duplicateCurp != null) {
            return StudentAddResult.DuplicateCurp(cleanCurp, duplicateCurp)
        }

        val duplicateEnrollmentId = _students.value.firstOrNull { it.enrollmentId.trim().uppercase() == cleanEnrollmentId }
        if (duplicateEnrollmentId != null) {
            return StudentAddResult.DuplicateEnrollmentId(cleanEnrollmentId, duplicateEnrollmentId)
        }

        val currentList = _students.value.toMutableList()
        currentList.add(student.copy(curp = cleanCurp, enrollmentId = cleanEnrollmentId))
        _students.value = currentList
        logAudit("Alumno registrado", "Secretar\u00eda", "Hoy " + getFormattedTime(), student.fullName)
        return StudentAddResult.Added(student.copy(curp = cleanCurp, enrollmentId = cleanEnrollmentId))
    }

    fun logAudit(action: String, role: String, timestamp: String, detail: String) {
        val currentList = _audits.value.toMutableList()
        currentList.add(0, SaseAudit(action, role, timestamp, detail))
        _audits.value = currentList
    }

    private fun getFormattedTime(): String {
        return formatTimestamp("hh:mm a")
    }
}
