package com.example.data.enrollment

object MockEnrollmentData {
    val enrollments = listOf(
        // ENR-001: ALUMNO DEMO 01 — Complete identity, ready for credential
        Enrollment(
            id = "ENR-2024-001",
            studentFullName = "ALUMNO DEMO 01",
            curp = "CURP-DEMO-01",
            birthDate = "12/May/2010",
            gradeGroup = "2B",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.ReadyToSign,
            submittedAt = "Hoy 09:35",
            address = Address(
                street = "DOMICILIO DEMO",
                neighborhood = "Centro",
                municipality = "Cuauhtemoc",
                state = "CDMX",
                zipCode = "06000"
            ),
            contacts = listOf(
                Contact("TUTOR DEMO 01", "Padre", "TEL-DEMO-01", "demo01@example.invalid", true),
                Contact("TUTOR DEMO 01B", "Madre", "TEL-DEMO-01B", "demo01b@example.invalid", false)
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
            readyForSignature = true,
            // v1.1A Identity
            studentPhotoUrl = "mock://photo/student/mariana_lopez.jpg",
            photoTakenAt = "Hoy 09:38",
            photoTakenBy = "Secretaria",
            photoForCredential = true,
            presenter = EnrollmentPresenter(
                id = "PRES-001",
                enrollmentId = "ENR-2024-001",
                studentId = "STU-001",
                fullName = "TUTOR DEMO 01",
                relationship = "Padre",
                phone = "TEL-DEMO-01",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/demo01.jpg",
                signatureUrl = "mock://firma/demo01.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Hoy 09:36"
            ),
            authorizedPickups = listOf(
                AuthorizedPickup(
                    id = "AUTH-001",
                    studentId = "STU-001",
                    fullName = "TUTOR DEMO 01",
                    relationship = "Padre",
                    phone = "TEL-DEMO-01",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/demo01.pdf",
                    photoUrl = "mock://photo/tutor/demo01.jpg",
                    signatureUrl = "mock://firma/demo01.png",
                    active = true,
                    notes = ""
                ),
                AuthorizedPickup(
                    id = "AUTH-002",
                    studentId = "STU-001",
                    fullName = "TUTOR DEMO 01B",
                    relationship = "Madre",
                    phone = "TEL-DEMO-01B",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/demo01b.pdf",
                    photoUrl = "mock://photo/tutor/demo01b.jpg",
                    signatureUrl = "mock://firma/demo01b.png",
                    active = true,
                    notes = ""
                )
            ),
            identityChecklist = IdentityChecklist(
                studentPhotographed = true,
                tutorPhotographed = true,
                tutorIdentified = true,
                ineVerified = true,
                authorizedPickupsRegistered = true,
                documentsComplete = true
            )
        ),

        // ENR-002: ALUMNO DEMO 02 — Completed enrollment, credential already issued
        Enrollment(
            id = "ENR-2024-002",
            studentFullName = "ALUMNO DEMO 02",
            curp = "CURP-DEMO-02",
            birthDate = "04/Feb/2011",
            gradeGroup = "1A",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.Completed,
            submittedAt = "Ayer 13:12",
            address = Address(
                street = "DOMICILIO DEMO",
                neighborhood = "Juarez",
                municipality = "Cuauhtemoc",
                state = "CDMX",
                zipCode = "06500"
            ),
            contacts = listOf(
                Contact("TUTOR DEMO 02", "Madre", "TEL-DEMO-02", "demo02@example.invalid", true)
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
            readyForSignature = false,
            // v1.1A Identity
            studentPhotoUrl = "mock://photo/student/diego_morales.jpg",
            photoTakenAt = "Ayer 13:15",
            photoTakenBy = "Secretaria",
            photoForCredential = true,
            presenter = EnrollmentPresenter(
                id = "PRES-002",
                enrollmentId = "ENR-2024-002",
                studentId = "STU-002",
                fullName = "TUTOR DEMO 02",
                relationship = "Madre",
                phone = "TEL-DEMO-02",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/demo02.jpg",
                signatureUrl = "mock://firma/demo02.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Ayer 13:14"
            ),
            authorizedPickups = listOf(
                AuthorizedPickup(
                    id = "AUTH-003",
                    studentId = "STU-002",
                    fullName = "TUTOR DEMO 02",
                    relationship = "Madre",
                    phone = "TEL-DEMO-02",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/demo02.pdf",
                    photoUrl = "mock://photo/tutor/demo02.jpg",
                    signatureUrl = "mock://firma/demo02.png",
                    active = true,
                    notes = ""
                )
            ),
            identityChecklist = IdentityChecklist(
                studentPhotographed = true,
                tutorPhotographed = true,
                tutorIdentified = true,
                ineVerified = true,
                authorizedPickupsRegistered = true,
                documentsComplete = true
            )
        ),

        // ENR-003: ALUMNO DEMO 03 — Missing student photo, missing documents
        Enrollment(
            id = "ENR-2024-003",
            studentFullName = "ALUMNO DEMO 03",
            curp = "CURP-DEMO-03",
            birthDate = "12/Ago/2009",
            gradeGroup = "3C",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.MissingDocuments,
            submittedAt = "Hoy 11:05",
            address = Address(
                street = "DOMICILIO DEMO",
                neighborhood = "Portales",
                municipality = "Benito Juarez",
                state = "CDMX",
                zipCode = "04000"
            ),
            contacts = listOf(
                Contact("TUTOR DEMO 03", "Padre", "TEL-DEMO-03", "demo03@example.invalid", true),
                Contact("AUTORIZADO DEMO 01", "Tia", "TEL-DEMO-03B", "demo03b@example.invalid", false)
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
            readyForSignature = false,
            // v1.1A Identity — missing student photo
            studentPhotoUrl = null,
            photoTakenAt = null,
            photoTakenBy = null,
            photoForCredential = false,
            presenter = EnrollmentPresenter(
                id = "PRES-003",
                enrollmentId = "ENR-2024-003",
                studentId = "STU-003",
                fullName = "TUTOR DEMO 03",
                relationship = "Padre",
                phone = "TEL-DEMO-03",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/demo03.jpg",
                signatureUrl = "mock://firma/demo03.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Hoy 11:08"
            ),
            authorizedPickups = listOf(
                AuthorizedPickup(
                    id = "AUTH-004",
                    studentId = "STU-003",
                    fullName = "TUTOR DEMO 03",
                    relationship = "Padre",
                    phone = "TEL-DEMO-03",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/demo03.pdf",
                    photoUrl = "mock://photo/tutor/demo03.jpg",
                    signatureUrl = "mock://firma/demo03.png",
                    active = true,
                    notes = ""
                ),
                AuthorizedPickup(
                    id = "AUTH-005",
                    studentId = "STU-003",
                    fullName = "AUTORIZADO DEMO 01",
                    relationship = "Tia",
                    phone = "TEL-DEMO-03B",
                    ineVerified = false,
                    ineCopyUrl = null,
                    photoUrl = null,
                    signatureUrl = null,
                    active = true,
                    notes = "Pendiente verificar INE"
                )
            ),
            identityChecklist = IdentityChecklist(
                studentPhotographed = false,
                tutorPhotographed = true,
                tutorIdentified = true,
                ineVerified = true,
                authorizedPickupsRegistered = true,
                documentsComplete = false
            )
        ),

        // ENR-004: ALUMNO DEMO 04 — Missing INE, no presenter registered
        Enrollment(
            id = "ENR-2024-004",
            studentFullName = "ALUMNO DEMO 04",
            curp = "CURP-DEMO-04",
            birthDate = "15/Ago/2009",
            gradeGroup = "3A",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.Submitted,
            submittedAt = "Hoy 14:20",
            address = Address(
                street = "DOMICILIO DEMO",
                neighborhood = "Del Valle",
                municipality = "Benito Juarez",
                state = "CDMX",
                zipCode = "03100"
            ),
            contacts = listOf(
                Contact("TUTOR DEMO 04", "Padre", "TEL-DEMO-04", "demo04@example.invalid", true)
            ),
            medicalRecord = MedicalRecord(
                bloodType = "AB+",
                allergies = "Ninguna reportada",
                chronicConditions = "Ninguna",
                medication = "No aplica",
                emergencyNotes = "Sin indicaciones especiales"
            ),
            socioeconomicRecord = SocioeconomicRecord(
                householdSize = 3,
                primaryIncome = "Profesionista independiente",
                internetAccess = true,
                transportation = "Automovil particular",
                notes = "Sin necesidad de apoyo"
            ),
            documents = listOf(
                EnrollmentDocument("Acta de nacimiento", "Entregado", true, "Secretaria"),
                EnrollmentDocument("CURP", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Comprobante de domicilio", "Pendiente", true, "", "Por validar con tutor"),
                EnrollmentDocument("Boleta anterior", "Entregado", true, "Control escolar"),
                EnrollmentDocument("INE tutor", "Pendiente", true, "", "Tutor no presento INE")
            ),
            consents = listOf(
                Consent("Uso de datos personales", false, "", ""),
                Consent("Autorizacion medica", false, "", "")
            ),
            riskFlags = emptyList(),
            readyForSignature = false,
            // v1.1A Identity — missing INE, no presenter
            studentPhotoUrl = "mock://photo/student/sofia_ramirez.jpg",
            photoTakenAt = "Hoy 14:25",
            photoTakenBy = "Secretaria",
            photoForCredential = false,
            presenter = null,
            authorizedPickups = emptyList(),
            identityChecklist = IdentityChecklist(
                studentPhotographed = true,
                tutorPhotographed = false,
                tutorIdentified = false,
                ineVerified = false,
                authorizedPickupsRegistered = false,
                documentsComplete = false
            )
        ),

        // ENR-005: ALUMNO DEMO 05 — Missing authorized pickups, otherwise ready
        Enrollment(
            id = "ENR-2024-005",
            studentFullName = "ALUMNO DEMO 05",
            curp = "CURP-DEMO-05",
            birthDate = "25/Jul/2010",
            gradeGroup = "2A",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.InReview,
            submittedAt = "Ayer 10:30",
            address = Address(
                street = "DOMICILIO DEMO",
                neighborhood = "Coyoacan",
                municipality = "Coyoacan",
                state = "CDMX",
                zipCode = "04020"
            ),
            contacts = listOf(
                Contact("TUTOR DEMO 05", "Madre", "TEL-DEMO-05", "demo05@example.invalid", true),
                Contact("TUTOR DEMO 05B", "Padre", "TEL-DEMO-05B", "demo05b@example.invalid", false)
            ),
            medicalRecord = MedicalRecord(
                bloodType = "O-",
                allergies = "Penicilina",
                chronicConditions = "Ninguna",
                medication = "No aplica",
                emergencyNotes = "Alergia a penicilina documentada"
            ),
            socioeconomicRecord = SocioeconomicRecord(
                householdSize = 4,
                primaryIncome = "Empleado formal",
                internetAccess = true,
                transportation = "Transporte publico",
                notes = "Madre solicita beca de transporte"
            ),
            documents = listOf(
                EnrollmentDocument("Acta de nacimiento", "Entregado", true, "Secretaria"),
                EnrollmentDocument("CURP", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Comprobante de domicilio", "Entregado", true, "Secretaria"),
                EnrollmentDocument("Boleta anterior", "Entregado", true, "Control escolar"),
                EnrollmentDocument("INE tutor", "Entregado", true, "Secretaria")
            ),
            consents = listOf(
                Consent("Uso de datos personales", true, "Gabriela Cruz Mendez", "Ayer 10:35"),
                Consent("Autorizacion medica", true, "Gabriela Cruz Mendez", "Ayer 10:36")
            ),
            riskFlags = listOf(
                RiskFlag("Alergia medica", "Alto", "Penicilina - alertar en cada ingreso")
            ),
            readyForSignature = false,
            // v1.1A Identity — missing authorized pickups
            studentPhotoUrl = "mock://photo/student/emiliano_torres.jpg",
            photoTakenAt = "Ayer 10:32",
            photoTakenBy = "Secretaria",
            photoForCredential = true,
            presenter = EnrollmentPresenter(
                id = "PRES-005",
                enrollmentId = "ENR-2024-005",
                studentId = "STU-005",
                fullName = "TUTOR DEMO 05",
                relationship = "Madre",
                phone = "TEL-DEMO-05",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/demo05.jpg",
                signatureUrl = "mock://firma/demo05.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Ayer 10:31"
            ),
            authorizedPickups = emptyList(),
            identityChecklist = IdentityChecklist(
                studentPhotographed = true,
                tutorPhotographed = true,
                tutorIdentified = true,
                ineVerified = true,
                authorizedPickupsRegistered = false,
                documentsComplete = true
            )
        )
    )
}
