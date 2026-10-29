package com.example.data.enrollment

import com.example.data.Student
import com.example.data.auth.StaffAction
import com.example.data.auth.StaffPermissions
import com.example.data.auth.StaffRole
import com.example.data.repository.MockStudentRepositoryImpl
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SchoolMovementCutoffTest {

    private fun createTestStudent(
        id: String,
        matricula: String,
        nombre: String,
        primerApellido: String,
        segundoApellido: String,
        grupo: String,
        curp: String
    ): Student = Student(
        id = id,
        fullName = "$nombre $primerApellido $segundoApellido",
        group = grupo,
        enrollmentId = matricula,
        curp = curp
    )

    @Test
    fun testFlujoCompletoPersistenciaReinicioYCoincidenciaCsv() = runBlocking {
        val studentRepo = MockStudentRepositoryImpl()
        val stu1 = createTestStudent("STU-101", "MAT-101", "Ana", "López", "Gómez", "1-A", "LOGA120515HMCRNA01")
        val stu2 = createTestStudent("STU-102", "MAT-102", "Carlos", "Pérez", "Ruiz", "1-A", "PERC120515HMCRNC02")
        val testStudents = listOf(stu1, stu2)

        // 1. Capturar movimientos iniciales
        val initialStore = mutableListOf<SchoolMovementRecord>()
        val repo1 = MockSchoolMovementRepositoryImpl(studentRepo, initialStore, testStudents)

        val alta1 = SchoolMovementRecord(
            id = "MOV-101",
            studentId = "STU-101",
            movementType = SchoolMovementType.INSCRIPCION_INICIAL,
            effectiveDate = "2026-08-28",
            recordedAt = "2026-08-28T08:00:00Z",
            recordedBy = "SECRETARIA_01",
            toGroup = "1-A",
            documentationSource = "Cédula de Inscripción 2026-2027"
        )
        val alta2 = SchoolMovementRecord(
            id = "MOV-102",
            studentId = "STU-102",
            movementType = SchoolMovementType.INSCRIPCION_INICIAL,
            effectiveDate = "2026-08-28",
            recordedAt = "2026-08-28T08:05:00Z",
            recordedBy = "SECRETARIA_01",
            toGroup = "1-A",
            documentationSource = "Cédula de Inscripción 2026-2027"
        )

        repo1.recordMovement(alta1)
        repo1.recordMovement(alta2)

        // Registrar traslado de Carlos
        val trasladoCarlos = SchoolMovementRecord(
            id = "MOV-103",
            studentId = "STU-102",
            movementType = SchoolMovementType.TRASLADO_SALIENTE,
            effectiveDate = "2026-09-25",
            recordedAt = "2026-09-25T10:00:00Z",
            recordedBy = "SECRETARIA_01",
            fromGroup = "1-A",
            destinationSchool = "Secundaria Técnica 15, CCT 09DST0015W",
            departureCategory = DepartureCategory.TRASLADO,
            documentationSource = "Oficio de Traslado SEP-2026-88"
        )
        repo1.recordMovement(trasladoCarlos)

        // 2. Simular "Cerrar y Reabrir / Reiniciar" instanciando un nuevo repositorio sobre el mismo almacén
        val repoReiniciado = MockSchoolMovementRepositoryImpl(studentRepo, initialStore, testStudents)

        // 3. Consultar historial
        val movesCarlos = repoReiniciado.getMovementsForStudent("STU-102")
        assertEquals(2, movesCarlos.size, "El historial de Carlos debe conservar sus 2 movimientos tras reiniciar")
        assertEquals(SchoolMovementType.TRASLADO_SALIENTE, movesCarlos.last().movementType)

        // 4. Generar corte al 30 de septiembre
        val snapshot = repoReiniciado.calculateCutoff("2026-09-30", "2026-08-28")
        assertEquals(1, snapshot.totalActiveStudents, "Solo Ana debe estar activa al corte")
        assertEquals(1, snapshot.transfersOutTotal, "Carlos debe computar como traslado saliente acreditado")
        assertEquals(0, snapshot.confirmedDropoutsTotal, "Carlos NO debe computar como abandono escolar")

        // 5. Descargar CSV y comprobar concordancia
        val csvAgregado = SchoolMovementExportHelper.generatePmcAggregatedCsv(snapshot)
        assertTrue(csvAgregado.contains("Matricula Activa al Corte,1,"), "El CSV agregado debe reportar exactamente 1 activo")
        assertTrue(csvAgregado.contains("Traslados Salientes Acreditados,1,"), "El CSV agregado debe reportar exactamente 1 traslado")
        assertTrue(csvAgregado.contains("Abandonos Escolares Confirmados,0,"), "El CSV agregado debe reportar 0 abandonos")
        assertFalse(csvAgregado.contains("Ana"), "El CSV agregado para PMC jamás debe contener nombres de alumnos")

        // 6. CSV Nominal para personal autorizado
        val csvNominal = SchoolMovementExportHelper.generateRestrictedNominalCsv(snapshot, requesterRole = StaffRole.SECRETARIA)
        assertTrue(csvNominal.contains("Ana"), "El padrón nominal reservado debe incluir el nombre de Ana")
        assertTrue(csvNominal.contains("Secundaria Técnica 15"), "El padrón nominal reservado debe documentar la escuela destino de Carlos")
    }

    @Test
    fun testSeparacionDeBajaAdministrativaVsAbandonoConfirmadoVsPendiente() = runBlocking {
        val studentRepo = MockStudentRepositoryImpl()
        val s1 = createTestStudent("STU-B1", "M-B1", "Luis", "Hernández", "Cruz", "2-A", "HECL120515HMCRN01")
        val s2 = createTestStudent("STU-B2", "M-B2", "María", "Torres", "Vela", "2-A", "TOVM120515MMCRN02")
        val s3 = createTestStudent("STU-B3", "M-B3", "Jorge", "Reyes", "Mora", "2-A", "REMJ120515HMCRN03")
        val testStudents = listOf(s1, s2, s3)

        val repo = MockSchoolMovementRepositoryImpl(studentRepo, mutableListOf(), testStudents)

        // s1: Baja administrativa por revocación de padres para sistema abierto
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-1",
                studentId = "STU-B1",
                movementType = SchoolMovementType.BAJA_ADMINISTRATIVA,
                effectiveDate = "2026-09-15",
                departureCategory = DepartureCategory.BAJA_ADMINISTRATIVA,
                documentationSource = "Solicitud firmada de baja administrativa",
                classificationCriteria = "Inscripción en modalidad no escolarizada"
            )
        )

        // s2: Abandono escolar confirmado tras agotar protocolo de visita domiciliaria
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-2",
                studentId = "STU-B2",
                movementType = SchoolMovementType.ABANDONO_CONFIRMADO,
                effectiveDate = "2026-09-20",
                departureCategory = DepartureCategory.ABANDONO_CONFIRMADO,
                documentationSource = "Acta de Visita Domiciliaria de Trabajo Social No. 12",
                classificationCriteria = "Deserción confirmada por cambio de residencia sin escolarización"
            )
        )

        // s3: Caso pendiente de aclaración (estudiante con inasistencias prolongadas en proceso de localización)
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-3",
                studentId = "STU-B3",
                movementType = SchoolMovementType.PENDIENTE_ACLARACION,
                effectiveDate = "2026-09-22",
                departureCategory = DepartureCategory.PENDIENTE_ACLARACION,
                documentationSource = "Reporte de alerta de prefectura",
                classificationCriteria = "En investigación por inasistencia recurrente"
            )
        )

        val snapshot = repo.calculateCutoff("2026-09-30", "2026-08-28")

        assertEquals(0, snapshot.totalActiveStudents)
        assertEquals(1, snapshot.administrativeDropoutsTotal, "Debe registrarse exactamente 1 baja administrativa")
        assertEquals(1, snapshot.confirmedDropoutsTotal, "Debe registrarse exactamente 1 abandono escolar confirmado")
        assertEquals(1, snapshot.pendingClarificationTotal, "Debe registrarse exactamente 1 caso pendiente de aclaración")
    }

    @Test
    fun testRetencionDeCohorteConAltasYReincorporaciones() = runBlocking {
        val studentRepo = MockStudentRepositoryImpl()
        val s1 = createTestStudent("STU-C1", "M-C1", "Diana", "Castro", "Luna", "1-A", "CALD120515MMCRN01")
        val s2 = createTestStudent("STU-C2", "M-C2", "Mateo", "Bravo", "Soto", "1-A", "BRSM120515HMCRN02")
        val s3 = createTestStudent("STU-C3", "M-C3", "Elena", "Vega", "Paz", "1-A", "VEPE120515MMCRN03")
        val testStudents = listOf(s1, s2, s3)

        val repo = MockSchoolMovementRepositoryImpl(studentRepo, mutableListOf(), testStudents)

        // Inicio ciclo: s1 y s2 activos
        repo.recordMovement(SchoolMovementRecord("M-01", "INST-001", "STU-C1", SchoolMovementType.INSCRIPCION_INICIAL, "2026-08-28"))
        repo.recordMovement(SchoolMovementRecord("M-02", "INST-001", "STU-C2", SchoolMovementType.INSCRIPCION_INICIAL, "2026-08-28"))

        // s2 se traslada formalmente el 10 de septiembre
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-03",
                studentId = "STU-C2",
                movementType = SchoolMovementType.TRASLADO_SALIENTE,
                effectiveDate = "2026-09-10",
                destinationSchool = "Escuela Secundaria 44",
                departureCategory = DepartureCategory.TRASLADO
            )
        )

        // s3 es un alta extemporánea el 15 de septiembre
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-04",
                studentId = "STU-C3",
                movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                effectiveDate = "2026-09-15"
            )
        )

        val snapshot = repo.calculateCutoff("2026-09-30", "2026-08-28")

        // Matrícula activa al 30 de septiembre: s1 (activo) + s3 (activo) = 2
        assertEquals(2, snapshot.totalActiveStudents, "Matrícula activa al corte debe ser 2")

        // Cohorte base inicial (al 28 de agosto): s1 y s2 = 2
        assertEquals(2, snapshot.initialCohortTotal, "Cohorte base inicial fue de 2 alumnos")

        // Alumnos de la cohorte base que continúan activos: solo s1 = 1
        assertEquals(1, snapshot.cohortRetainedTotal, "Retenidos de la cohorte base debe ser 1")

        // Retención de Cohorte: 1 / (2 - 1) * 100 = 100.0%
        assertEquals(100.0, snapshot.cohortRetentionRate, "La retención de la cohorte descontando el traslado acreditado debe ser 100%")

        // Variación neta de matrícula: 2 - 2 = 0
        assertEquals(0, snapshot.netEnrollmentVariation, "La variación neta debe ser 0 a pesar de haber retención del 100%")
    }

    @Test
    fun testOrdenDeterministaYCorreccionesHistoricasConSupersedes() = runBlocking {
        val studentRepo = MockStudentRepositoryImpl()
        val s1 = createTestStudent("STU-ORD", "M-ORD", "Gabriel", "Molina", "Sanz", "1-A", "MOSG120515HMCRN01")
        val testStudents = listOf(s1)

        val repo = MockSchoolMovementRepositoryImpl(studentRepo, mutableListOf(), testStudents)

        val movOriginal = SchoolMovementRecord(
            id = "MOV-ERR-01",
            studentId = "STU-ORD",
            movementType = SchoolMovementType.CAMBIO_DE_GRUPO,
            effectiveDate = "2026-10-01",
            recordedAt = "2026-10-01T09:00:00Z",
            fromGroup = "1-A",
            toGroup = "1-C"
        )
        repo.recordMovement(movOriginal)

        val movCorreccion = SchoolMovementRecord(
            id = "MOV-CORR-02",
            studentId = "STU-ORD",
            movementType = SchoolMovementType.CAMBIO_DE_GRUPO,
            effectiveDate = "2026-10-01",
            recordedAt = "2026-10-01T15:30:00Z",
            fromGroup = "1-A",
            toGroup = "1-B",
            isCorrection = true,
            supersedesMovementId = "MOV-ERR-01",
            correctionReason = "Error tipográfico en captura de cédula; correspondía 1-B y no 1-C"
        )
        repo.recordMovement(movCorreccion)

        val snapshot = repo.calculateCutoff("2026-10-15", "2026-08-28")
        val detalleGabriel = snapshot.studentDetails.first { it.studentId == "STU-ORD" }

        assertEquals("1-B", detalleGabriel.assignedGroup, "La corrección formal debe prevalecer sobre el movimiento sustituido")
    }

    @Test
    fun testCalculoDeOportunidadesDeAsistenciaSegunVigenciaReal() {
        val diasLectivos = listOf(
            "2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04",
            "2026-09-07", "2026-09-08", "2026-09-09", "2026-09-10"
        )

        val movimientos = listOf(
            SchoolMovementRecord(
                id = "MOV-ING-04",
                studentId = "STU-VIG-1",
                movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                effectiveDate = "2026-09-04"
            )
        )

        val diasAplicables = SchoolMovementEvaluator.calculateApplicableAttendanceDays(
            studentId = "STU-VIG-1",
            movements = movimientos,
            periodStart = "2026-09-01",
            periodEnd = "2026-09-10",
            schoolDays = diasLectivos
        )

        assertEquals(5, diasAplicables.size, "Solo debe tener 5 oportunidades de asistencia en sus días vigentes")
        assertFalse(diasAplicables.contains("2026-09-01"))
        assertTrue(diasAplicables.contains("2026-09-04"))
        assertTrue(diasAplicables.contains("2026-09-10"))
    }

    @Test
    fun testControlesNegativosDeIntegridad() {
        // 1. Rechazo de traslado sin escuela destino
        assertFailsWith<IllegalArgumentException> {
            SchoolMovementEvaluator.validateMovement(
                SchoolMovementRecord(
                    id = "NEG-1",
                    studentId = "STU-1",
                    movementType = SchoolMovementType.TRASLADO_SALIENTE,
                    effectiveDate = "2026-09-10",
                    destinationSchool = ""
                )
            )
        }

        // 2. Rechazo de abandono confirmado sin documentación probatoria o criterio
        assertFailsWith<IllegalArgumentException> {
            SchoolMovementEvaluator.validateMovement(
                SchoolMovementRecord(
                    id = "NEG-2",
                    studentId = "STU-1",
                    movementType = SchoolMovementType.ABANDONO_CONFIRMADO,
                    effectiveDate = "2026-09-10",
                    documentationSource = "",
                    classificationCriteria = "Motivo sin soporte"
                )
            )
        }

        // 3. Rechazo de cambio de grupo con mismo origen y destino
        assertFailsWith<IllegalArgumentException> {
            SchoolMovementEvaluator.validateMovement(
                SchoolMovementRecord(
                    id = "NEG-3",
                    studentId = "STU-1",
                    movementType = SchoolMovementType.CAMBIO_DE_GRUPO,
                    effectiveDate = "2026-09-10",
                    fromGroup = "1-A",
                    toGroup = "1-A"
                )
            )
        }

        // 4. Rechazo de corrección sin id superado
        assertFailsWith<IllegalArgumentException> {
            SchoolMovementEvaluator.validateMovement(
                SchoolMovementRecord(
                    id = "NEG-4",
                    studentId = "STU-1",
                    movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                    effectiveDate = "2026-09-10",
                    isCorrection = true,
                    supersedesMovementId = null,
                    correctionReason = "Motivo"
                )
            )
        }
    }

    @Test
    fun testPermisosDeAccesoAlPadronNominalRestringido() {
        assertTrue(StaffPermissions.canPerform(StaffRole.SECRETARIA, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertTrue(StaffPermissions.canPerform(StaffRole.DIRECCION, StaffAction.RECORD_SCHOOL_MOVEMENT))

        assertFalse(StaffPermissions.canPerform(StaffRole.DOCENTE, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.TRABAJO_SOCIAL, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.MEDICO_ESCOLAR, StaffAction.RECORD_SCHOOL_MOVEMENT))
        assertFalse(StaffPermissions.canPerform(StaffRole.UDEII, StaffAction.RECORD_SCHOOL_MOVEMENT))
    }
}
