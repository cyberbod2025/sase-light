package com.example.data.enrollment

object MockEnrollmentData {
    val enrollments = listOf(
        Enrollment(
            id = "ENR-2024-001",
            studentFullName = "Mariana Lopez Hernandez",
            curp = "LOHM100512MDFPRRA2",
            birthDate = "12/May/2010",
            gradeGroup = "2B",
            schoolYear = "2024-2025",
            status = "Listo para firma",
            submittedAt = "Hoy 09:35",
            address = Address(
                street = "Av. Siempre Viva 123",
                neighborhood = "Centro",
                municipality = "Cuauhtemoc",
                state = "CDMX",
                zipCode = "06000"
            ),
            contacts = listOf(
                Contact("Jose Lopez Ramirez", "Padre", "55 3456 7890", "jlopez@example.com", true),
                Contact("Ana Hernandez Soto", "Madre", "55 8765 4321", "ana.hdz@example.com", false)
            ),
            medicalRecord = MedicalRecord(
                bloodType = "O+",
                allergies = "Polvo y lacteos",
                chronicConditions = "Ninguna reportada",
                medication = "No aplica",
                emergencyNotes = "Llamar primero al tutor principal"
            ),
            socioeconomicRecord = SocioeconomicRecord(
                householdSize = 4,
                primaryIncome = "Empleado formal",
                internetAccess = true,
                transportation = "Transporte publico",
                notes = "Sin apoyos adicionales requeridos"
            ),
            documents = listOf(
                EnrollmentDocument("Acta de nacimiento", "Entregado", true, "Secretaria"),
                EnrollmentDocument("CURP", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Comprobante de domicilio", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Boleta anterior", "Entregado", true, "Control escolar"),
                EnrollmentDocument("INE tutor", "Entregado", true, "Secretaria")
            ),
            consents = listOf(
                Consent("Uso de datos personales", true, "Jose Lopez Ramirez", "Hoy 09:40"),
                Consent("Autorizacion medica", true, "Jose Lopez Ramirez", "Hoy 09:41")
            ),
            riskFlags = listOf(
                RiskFlag("Alergias", "Medio", "Registrar advertencia visible en expediente medico")
            ),
            readyForSignature = true
        ),
        Enrollment(
            id = "ENR-2024-002",
            studentFullName = "Diego Morales Sanchez",
            curp = "MOSD110204MDFXNN05",
            birthDate = "04/Feb/2011",
            gradeGroup = "1A",
            schoolYear = "2024-2025",
            status = "Completo",
            submittedAt = "Ayer 13:12",
            address = Address(
                street = "Paseo de la Reforma 456",
                neighborhood = "Juarez",
                municipality = "Cuauhtemoc",
                state = "CDMX",
                zipCode = "06500"
            ),
            contacts = listOf(
                Contact("Laura Sanchez Diaz", "Madre", "55 2345 6789", "lauras@example.com", true)
            ),
            medicalRecord = MedicalRecord(
                bloodType = "A+",
                allergies = "Ninguna",
                chronicConditions = "Ninguna",
                medication = "No aplica",
                emergencyNotes = "Sin indicaciones especiales"
            ),
            socioeconomicRecord = SocioeconomicRecord(
                householdSize = 3,
                primaryIncome = "Comercio independiente",
                internetAccess = true,
                transportation = "Caminando",
                notes = "Vive cerca del plantel"
            ),
            documents = listOf(
                EnrollmentDocument("Acta de nacimiento", "Entregado", true, "Secretaria"),
                EnrollmentDocument("CURP", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Comprobante de domicilio", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Boleta anterior", "Entregado", true, "Control escolar"),
                EnrollmentDocument("INE tutor", "Entregado", true, "Secretaria")
            ),
            consents = listOf(
                Consent("Uso de datos personales", true, "Laura Sanchez Diaz", "Ayer 13:18"),
                Consent("Autorizacion medica", true, "Laura Sanchez Diaz", "Ayer 13:18")
            ),
            riskFlags = emptyList(),
            readyForSignature = false
        ),
        Enrollment(
            id = "ENR-2024-003",
            studentFullName = "Valeria Jimenez Torres",
            curp = "JITV090812MDFZRR01",
            birthDate = "12/Ago/2009",
            gradeGroup = "3C",
            schoolYear = "2024-2025",
            status = "Incompleto",
            submittedAt = "Hoy 11:05",
            address = Address(
                street = "Calle de las Flores 789",
                neighborhood = "Portales",
                municipality = "Benito Juarez",
                state = "CDMX",
                zipCode = "04000"
            ),
            contacts = listOf(
                Contact("Carlos Jimenez Ruiz", "Padre", "55 5678 9012", "cjimenez@example.com", true),
                Contact("Rosa Torres Mata", "Tia", "55 1111 2200", "rosa.torres@example.com", false)
            ),
            medicalRecord = MedicalRecord(
                bloodType = "B+",
                allergies = "Ninguna",
                chronicConditions = "Asma",
                medication = "Salbutamol",
                emergencyNotes = "Mantener inhalador disponible"
            ),
            socioeconomicRecord = SocioeconomicRecord(
                householdSize = 5,
                primaryIncome = "Empleo eventual",
                internetAccess = false,
                transportation = "Transporte publico",
                notes = "Requiere seguimiento de trabajo social"
            ),
            documents = listOf(
                EnrollmentDocument("Acta de nacimiento", "Entregado", true, "Secretaria"),
                EnrollmentDocument("CURP", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Comprobante de domicilio", "Pendiente", true, "", "No subido por tutor"),
                EnrollmentDocument("Boleta anterior", "Pendiente", true, "", "Falta sello del plantel anterior"),
                EnrollmentDocument("INE tutor", "Entregado", true, "Secretaria")
            ),
            consents = listOf(
                Consent("Uso de datos personales", true, "Carlos Jimenez Ruiz", "Hoy 11:12"),
                Consent("Autorizacion medica", false, "", "")
            ),
            riskFlags = listOf(
                RiskFlag("Documento critico", "Alto", "Comprobante y boleta bloquean firma"),
                RiskFlag("Salud", "Medio", "Asma declarada por tutor")
            ),
            readyForSignature = false
        )
    )
}
