package com.example.data.enrollment

object MockEnrollmentData {
    val enrollments = listOf(
        // ENR-001: Mariana — Complete identity, ready for credential
        Enrollment(
            id = "ENR-2024-001",
            studentFullName = "Mariana Lopez Hernandez",
            curp = "LOHM100512MDFPRRA2",
            birthDate = "12/May/2010",
            gradeGroup = "2B",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.ReadyToSign,
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
                fullName = "Jose Lopez Ramirez",
                relationship = "Padre",
                phone = "55 3456 7890",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/jose_lopez.jpg",
                signatureUrl = "mock://firma/jose_lopez.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Hoy 09:36"
            ),
            authorizedPickups = listOf(
                AuthorizedPickup(
                    id = "AUTH-001",
                    studentId = "STU-001",
                    fullName = "Jose Lopez Ramirez",
                    relationship = "Padre",
                    phone = "55 3456 7890",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/jose_lopez.pdf",
                    photoUrl = "mock://photo/tutor/jose_lopez.jpg",
                    signatureUrl = "mock://firma/jose_lopez.png",
                    active = true,
                    notes = ""
                ),
                AuthorizedPickup(
                    id = "AUTH-002",
                    studentId = "STU-001",
                    fullName = "Ana Hernandez Soto",
                    relationship = "Madre",
                    phone = "55 8765 4321",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/ana_hernandez.pdf",
                    photoUrl = "mock://photo/tutor/ana_hernandez.jpg",
                    signatureUrl = "mock://firma/ana_hernandez.png",
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

        // ENR-002: Diego — Completed enrollment, credential already issued
        Enrollment(
            id = "ENR-2024-002",
            studentFullName = "Diego Morales Sanchez",
            curp = "MOSD110204MDFXNN05",
            birthDate = "04/Feb/2011",
            gradeGroup = "1A",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.Completed,
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
                fullName = "Laura Sanchez Diaz",
                relationship = "Madre",
                phone = "55 2345 6789",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/laura_sanchez.jpg",
                signatureUrl = "mock://firma/laura_sanchez.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Ayer 13:14"
            ),
            authorizedPickups = listOf(
                AuthorizedPickup(
                    id = "AUTH-003",
                    studentId = "STU-002",
                    fullName = "Laura Sanchez Diaz",
                    relationship = "Madre",
                    phone = "55 2345 6789",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/laura_sanchez.pdf",
                    photoUrl = "mock://photo/tutor/laura_sanchez.jpg",
                    signatureUrl = "mock://firma/laura_sanchez.png",
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

        // ENR-003: Valeria — Missing student photo, missing documents
        Enrollment(
            id = "ENR-2024-003",
            studentFullName = "Valeria Jimenez Torres",
            curp = "JITV090812MDFZRR01",
            birthDate = "12/Ago/2009",
            gradeGroup = "3C",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.MissingDocuments,
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
                fullName = "Carlos Jimenez Ruiz",
                relationship = "Padre",
                phone = "55 5678 9012",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/carlos_jimenez.jpg",
                signatureUrl = "mock://firma/carlos_jimenez.png",
                canPickupStudent = true,
                registeredBy = "Secretaria",
                createdAt = "Hoy 11:08"
            ),
            authorizedPickups = listOf(
                AuthorizedPickup(
                    id = "AUTH-004",
                    studentId = "STU-003",
                    fullName = "Carlos Jimenez Ruiz",
                    relationship = "Padre",
                    phone = "55 5678 9012",
                    ineVerified = true,
                    ineCopyUrl = "mock://ine/carlos_jimenez.pdf",
                    photoUrl = "mock://photo/tutor/carlos_jimenez.jpg",
                    signatureUrl = "mock://firma/carlos_jimenez.png",
                    active = true,
                    notes = ""
                ),
                AuthorizedPickup(
                    id = "AUTH-005",
                    studentId = "STU-003",
                    fullName = "Rosa Torres Mata",
                    relationship = "Tia",
                    phone = "55 1111 2200",
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

        // ENR-004: Sofia — Missing INE, no presenter registered
        Enrollment(
            id = "ENR-2024-004",
            studentFullName = "Sofia Ramirez Garcia",
            curp = "RAGS090815MDFXRN07",
            birthDate = "15/Ago/2009",
            gradeGroup = "3A",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.Submitted,
            submittedAt = "Hoy 14:20",
            address = Address(
                street = "Av. Insurgentes Sur 1500",
                neighborhood = "Del Valle",
                municipality = "Benito Juarez",
                state = "CDMX",
                zipCode = "03100"
            ),
            contacts = listOf(
                Contact("Pedro Ramirez Lopez", "Padre", "55 9999 8888", "pramirez@example.com", true)
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

        // ENR-005: Emiliano — Missing authorized pickups, otherwise ready
        Enrollment(
            id = "ENR-2024-005",
            studentFullName = "Emiliano Torres Cruz",
            curp = "TOCE100725MDFXRL04",
            birthDate = "25/Jul/2010",
            gradeGroup = "2A",
            schoolYear = "2024-2025",
            status = EnrollmentStatus.InReview,
            submittedAt = "Ayer 10:30",
            address = Address(
                street = "Cerro del Agua 45",
                neighborhood = "Coyoacan",
                municipality = "Coyoacan",
                state = "CDMX",
                zipCode = "04020"
            ),
            contacts = listOf(
                Contact("Gabriela Cruz Mendez", "Madre", "55 7777 6666", "gcruz@example.com", true),
                Contact("Luis Torres Hernandez", "Padre", "55 5555 4444", "ltorres@example.com", false)
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
                fullName = "Gabriela Cruz Mendez",
                relationship = "Madre",
                phone = "55 7777 6666",
                ineVerified = true,
                presentAtEnrollment = true,
                photoUrl = "mock://photo/tutor/gabriela_cruz.jpg",
                signatureUrl = "mock://firma/gabriela_cruz.png",
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
