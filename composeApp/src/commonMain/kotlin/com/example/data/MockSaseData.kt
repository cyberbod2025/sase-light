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

    fun resetForTests() {
        _students.value = createInitialStudents()
        _audits.value = createInitialAudits()
    }

    private fun createInitialStudents(): List<Student> = listOf(
        Student(
            id = "1",
            fullName = "ALUMNO DEMO 01",
            group = "1°A",
            enrollmentId = "S310-DEMA100101-26",
            curp = "DEMA100101HDFABC01",
            shift = "Vespertino",
            schoolYear = "2026-2027",
            status = "Alta oficial con grupo",
            riskLevel = "Bajo",
            bap = "No",
            schoolInsurance = "Vigente",
            documentationStatus = "Completa",
            birthDate = "01/ENE/2010",
            age = 14,
            birthPlace = "CIUDAD DE MEXICO",
            address = "DOMICILIO DEMO 01",
            zipCode = "00001",
            tutorName = "RESPONSABLE DEMO 01",
            tutorRelation = "MADRE",
            tutorPhone = "5500000001",
            tutorEmail = "demo01@example.invalid",
            emergencyContactName = "RESPONSABLE DEMO ALTERNO 01",
            emergencyContactRelation = "TUTOR",
            emergencyContactPhone = "5500000011",
            emergencyContactEmail = "demo01-alt@example.invalid",
            attendancePercent = 96,
            attendances = 172,
            excusedAbsences = 4,
            unexcusedAbsences = 3,
            healthAlergies = "NINGUNA",
            healthNotes = "SIN OBSERVACIONES RELEVANTES",
            healthMeds = "NO APLICA",
            healthPasses = "NINGUNO",
            orientationStatus = "SIN SEGUIMIENTO",
            orientationLastAppointment = "N/A",
            orientationInterventionPlan = "NINGUNO",
            orientationResponsible = "N/A",
            documents = completeDocuments("HOY"),
            observations = listOf(SaseObservation("EXPEDIENTE DEMO COMPLETO PARA PRESENTACION INSTITUCIONAL.", "DOCENTE DEMO", "HOY", "Académica")),
            schoolIncidents = emptyList(),
            audits = listOf(SaseAudit("Alta oficial completada", "Secretaría", "HOY 11:25", "MATRICULA OFICIAL ASIGNADA")),
            preApplicationFolio = "PRE-X1A2"
        ),
        Student(
            id = "2",
            fullName = "ALUMNO DEMO 02",
            group = "Por asignar",
            enrollmentId = "",
            curp = "DEMB110202MDFABC02",
            schoolYear = "2026-2027",
            status = "Pre-solicitud recibida",
            riskLevel = "Bajo",
            documentationStatus = "En revisión",
            birthDate = "02/FEB/2011",
            age = 13,
            birthPlace = "CIUDAD DE MEXICO",
            address = "DOMICILIO DEMO 02",
            zipCode = "00002",
            tutorName = "RESPONSABLE DEMO 02",
            tutorRelation = "PADRE",
            tutorPhone = "5500000002",
            tutorEmail = "demo02@example.invalid",
            emergencyContactName = "RESPONSABLE DEMO ALTERNO 02",
            emergencyContactRelation = "TUTOR",
            emergencyContactPhone = "5500000012",
            emergencyContactEmail = "demo02-alt@example.invalid",
            documents = pendingDocuments(),
            observations = listOf(SaseObservation("PENDIENTE DE VALIDACION DE DATOS POR SECRETARIA.", "SECRETARIA DEMO", "HOY", "Académica")),
            audits = listOf(SaseAudit("Pre-solicitud recibida", "Secretaría", "HOY 09:00", "MATRICULA POR ASIGNAR")),
            preApplicationFolio = "PRE-B8Y2"
        ),
        Student(
            id = "3",
            fullName = "ALUMNO DEMO 03",
            group = "2°B",
            enrollmentId = "",
            curp = "DEMC120303HDFABC03",
            schoolYear = "2026-2027",
            status = "Documentos pendientes",
            riskLevel = "Medio",
            documentationStatus = "Incompleta",
            birthDate = "03/MAR/2012",
            age = 12,
            birthPlace = "CIUDAD DE MEXICO",
            address = "DOMICILIO DEMO 03",
            zipCode = "00003",
            tutorName = "RESPONSABLE DEMO 03",
            tutorRelation = "MADRE",
            tutorPhone = "5500000003",
            tutorEmail = "demo03@example.invalid",
            emergencyContactName = "RESPONSABLE DEMO ALTERNO 03",
            emergencyContactRelation = "TUTOR",
            emergencyContactPhone = "5500000013",
            emergencyContactEmail = "demo03-alt@example.invalid",
            healthAlergies = "NINGUNA",
            healthNotes = "SIN OBSERVACIONES RELEVANTES",
            healthMeds = "NO APLICA",
            healthPasses = "NINGUNO",
            orientationStatus = "EN REVISION",
            orientationLastAppointment = "N/A",
            orientationInterventionPlan = "DOCUMENTOS Y FOTOS PENDIENTES",
            orientationResponsible = "SECRETARIA DEMO",
            documents = pendingDocuments(),
            observations = listOf(SaseObservation("DATOS ACEPTADOS; FALTAN DOCUMENTOS Y FOTOS PARA ALTA OFICIAL.", "SECRETARIA DEMO", "AYER", "Académica")),
            schoolIncidents = listOf(SaseIncident("HOY", "Documentación pendiente", "SECRETARIA DEMO", "En seguimiento")),
            audits = listOf(SaseAudit("Pendientes detectados", "Sistema", "AYER 08:30", "DOCUMENTOS Y FOTOS")),
            preApplicationFolio = "PRE-C9Z3"
        ),
        Student(
            id = "4",
            fullName = "ALUMNO DEMO 04",
            group = "1°A",
            enrollmentId = "",
            curp = "DEMD130404MDFABC04",
            schoolYear = "2026-2027",
            status = "Lista para alta oficial",
            riskLevel = "Bajo",
            documentationStatus = "Completa",
            birthDate = "04/ABR/2013",
            age = 11,
            birthPlace = "CIUDAD DE MEXICO",
            address = "DOMICILIO DEMO 04",
            zipCode = "00004",
            tutorName = "RESPONSABLE DEMO 04",
            tutorRelation = "TUTOR",
            tutorPhone = "5500000004",
            tutorEmail = "demo04@example.invalid",
            emergencyContactName = "RESPONSABLE DEMO ALTERNO 04",
            emergencyContactRelation = "TUTOR",
            emergencyContactPhone = "5500000014",
            emergencyContactEmail = "demo04-alt@example.invalid",
            documents = completeDocuments("HOY"),
            observations = listOf(SaseObservation("CURP, DOCUMENTOS Y FOTOGRAFIAS LISTAS; PENDIENTE CONFIRMAR ALTA OFICIAL.", "SECRETARIA DEMO", "HOY", "Académica")),
            audits = listOf(SaseAudit("Lista para alta oficial", "Secretaría", "HOY 10:45", "MATRICULA PENDIENTE")),
            preApplicationFolio = "PRE-D4L4"
        ),
        Student(
            id = "5",
            fullName = "ALUMNO DEMO 05",
            group = "1°B",
            enrollmentId = "",
            curp = "DEME140505HDFABC05",
            schoolYear = "2026-2027",
            status = "Pendiente asignación manual de grupo",
            riskLevel = "Bajo",
            documentationStatus = "Completa",
            birthDate = "05/MAY/2014",
            age = 10,
            birthPlace = "CIUDAD DE MEXICO",
            address = "DOMICILIO DEMO 05",
            zipCode = "00005",
            tutorName = "RESPONSABLE DEMO 05",
            tutorRelation = "MADRE",
            tutorPhone = "5500000005",
            tutorEmail = "demo05@example.invalid",
            emergencyContactName = "RESPONSABLE DEMO ALTERNO 05",
            emergencyContactRelation = "TUTOR",
            emergencyContactPhone = "5500000015",
            emergencyContactEmail = "demo05-alt@example.invalid",
            documents = completeDocuments("HOY"),
            observations = listOf(SaseObservation("GRUPO SUGERIDO: 1B. ASIGNACION MANUAL DISPONIBLE PARA SECRETARIA/DIRECCION.", "SECRETARIA DEMO", "HOY", "Académica")),
            audits = listOf(SaseAudit("Grupo sugerido", "Secretaría", "HOY 11:25", "1B; MATRICULA PENDIENTE DE ALTA OFICIAL")),
            preApplicationFolio = "PRE-E5M5"
        )
    )

    private fun completeDocuments(date: String) = listOf(
        SaseDocument("ACTA DE NACIMIENTO", date, "Vigente"),
        SaseDocument("CURP", date, "Vigente"),
        SaseDocument("BOLETA / CERTIFICADO", date, "Vigente"),
        SaseDocument("COMPROBANTE DE DOMICILIO", date, "Vigente")
    )

    private fun pendingDocuments() = listOf(
        SaseDocument("ACTA DE NACIMIENTO", "HOY", "Vigente"),
        SaseDocument("CURP", "Pendiente de cotejo", "Pendiente"),
        SaseDocument("BOLETA / CERTIFICADO", "Pendiente", "Pendiente"),
        SaseDocument("COMPROBANTE DE DOMICILIO", "Pendiente de cotejo", "Pendiente")
    )

    private fun createInitialAudits(): List<SaseAudit> = listOf(
        SaseAudit("Alta oficial completada", "Secretaría", "HOY 11:25", "ALUMNO DEMO 01 (1°A)"),
        SaseAudit("Lista para alta oficial", "Secretaría", "HOY 10:45", "ALUMNO DEMO 04"),
        SaseAudit("Documentos pendientes", "Secretaría", "AYER 13:12", "ALUMNO DEMO 03"),
        SaseAudit("Solicitud recibida", "Secretaría", "HOY 09:00", "ALUMNO DEMO 02"),
        SaseAudit("Grupo sugerido", "Dirección", "HOY 11:25", "ALUMNO DEMO 05 -> 1B")
    )

    fun updateStudent(updated: Student) {
        val currentList = _students.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            currentList[index] = updated
            _students.value = currentList
        }
    }

    fun studentByCurp(curp: String): Student? {
        val cleanCurp = curp.trim().uppercase()
        return _students.value.firstOrNull { it.curp.trim().uppercase() == cleanCurp }
    }

    fun studentByEnrollmentId(enrollmentId: String): Student? {
        val cleanEnrollmentId = enrollmentId.trim().uppercase()
        return _students.value.firstOrNull { it.enrollmentId.trim().uppercase() == cleanEnrollmentId }
    }

    fun addStudent(student: Student): StudentAddResult {
        val cleanCurp = student.curp.trim().uppercase()
        val cleanEnrollmentId = student.enrollmentId.trim().uppercase()
        if (cleanCurp.isBlank() || cleanEnrollmentId.isBlank()) {
            return StudentAddResult.InvalidData("CURP y matrícula son obligatorias.")
        }

        val duplicateCurp = _students.value.firstOrNull { it.curp.trim().uppercase() == cleanCurp }
        if (duplicateCurp != null) return StudentAddResult.DuplicateCurp(cleanCurp, duplicateCurp)

        val duplicateEnrollmentId = _students.value.firstOrNull { it.enrollmentId.trim().uppercase() == cleanEnrollmentId }
        if (duplicateEnrollmentId != null) return StudentAddResult.DuplicateEnrollmentId(cleanEnrollmentId, duplicateEnrollmentId)

        val currentList = _students.value.toMutableList()
        val added = student.copy(curp = cleanCurp, enrollmentId = cleanEnrollmentId)
        currentList.add(added)
        _students.value = currentList
        logAudit("Alumno registrado", "Secretaría", "HOY " + getFormattedTime(), added.fullName)
        return StudentAddResult.Added(added)
    }

    fun logAudit(action: String, role: String, timestamp: String, detail: String) {
        val currentList = _audits.value.toMutableList()
        currentList.add(0, SaseAudit(action, role, timestamp, detail))
        _audits.value = currentList
    }

    private fun getFormattedTime(): String = formatTimestamp("hh:mm a")
}
