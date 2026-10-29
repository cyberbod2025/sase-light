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
        assertTrue(csvAgregado.contains("Nuevas Altas Extemporaneas,0,"), "El CSV agregado debe reportar 0 altas extemporáneas")
        assertTrue(csvAgregado.contains("Reincorporaciones al Plantel,0,"), "El CSV agregado debe reportar 0 reincorporaciones")
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
        val s4 = createTestStudent("STU-C4", "M-C4", "Rodrigo", "Navarro", "Gil", "1-A", "NAGR120515HMCRN04")
        val testStudents = listOf(s1, s2, s3, s4)

        val repo = MockSchoolMovementRepositoryImpl(studentRepo, mutableListOf(), testStudents)

        // Inicio ciclo: s1, s2 y s4 activos en la cohorte base
        repo.recordMovement(SchoolMovementRecord("M-01", "INST-001", "STU-C1", SchoolMovementType.INSCRIPCION_INICIAL, "2026-08-28"))
        repo.recordMovement(SchoolMovementRecord("M-02", "INST-001", "STU-C2", SchoolMovementType.INSCRIPCION_INICIAL, "2026-08-28"))
        repo.recordMovement(SchoolMovementRecord("M-03-INIT", "INST-001", "STU-C4", SchoolMovementType.INSCRIPCION_INICIAL, "2026-08-28"))

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

        // s3 es un alta extemporánea el 15 de septiembre (fuera de cohorte inicial)
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-04",
                studentId = "STU-C3",
                movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                effectiveDate = "2026-09-15"
            )
        )

        // s4 sufre baja administrativa el 5 de septiembre y reincorporación formal el 20 de septiembre
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-05",
                studentId = "STU-C4",
                movementType = SchoolMovementType.BAJA_ADMINISTRATIVA,
                effectiveDate = "2026-09-05",
                departureCategory = DepartureCategory.BAJA_ADMINISTRATIVA,
                documentationSource = "Suspensión administrativa temporal",
                classificationCriteria = "Revisión documental"
            )
        )
        repo.recordMovement(
            SchoolMovementRecord(
                id = "M-06",
                studentId = "STU-C4",
                movementType = SchoolMovementType.REINCORPORACION,
                effectiveDate = "2026-09-20",
                documentationSource = "Oficio de regularización de matrícula",
                classificationCriteria = "Reincorporación al mismo plantel"
            )
        )

        val snapshot = repo.calculateCutoff("2026-09-30", "2026-08-28")

        // Matrícula activa al 30 de septiembre: s1 (activo) + s3 (alta) + s4 (reincorporado) = 3
        assertEquals(3, snapshot.totalActiveStudents, "Matrícula activa al corte debe ser 3")

        // Cohorte base inicial (al 28 de agosto): s1, s2 y s4 = 3
        assertEquals(3, snapshot.initialCohortTotal, "Cohorte base inicial fue de 3 alumnos")

        // Alumnos de la cohorte base que continúan activos: s1 y s4 = 2
        assertEquals(2, snapshot.cohortRetainedTotal, "Retenidos de la cohorte base debe ser 2 (incluye al reincorporado s4)")

        // Base efectiva: 3 (cohorte) - 1 (traslado s2) = 2
        // Tasa de Retención de Cohorte: (2 / 2) * 100 = 100.0%
        assertEquals(100.0, snapshot.cohortRetentionRate, "La retención de la cohorte descontando el traslado acreditado debe ser 100%")

        // Variación neta de matrícula: 3 (activos) - 3 (cohorte) = 0
        assertEquals(0, snapshot.netEnrollmentVariation, "La variación neta debe ser 0 a pesar de haber retención del 100%")

        // Altas extemporáneas y reincorporaciones deben estar distinguidas
        assertEquals(1, snapshot.newEnrollmentsInPeriod, "Debe registrarse exactamente 1 alta extemporánea (s3)")
        assertEquals(1, snapshot.reincorporationsTotal, "Debe registrarse exactamente 1 reincorporación (s4)")
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

        // Caso 1: Estudiante con alta extemporánea el 4 de septiembre
        val movimientosAlta = listOf(
            SchoolMovementRecord(
                id = "MOV-ING-04",
                studentId = "STU-VIG-1",
                movementType = SchoolMovementType.ALTA_EXTEMPORANEA,
                effectiveDate = "2026-09-04"
            )
        )

        val diasAplicablesAlta = SchoolMovementEvaluator.calculateApplicableAttendanceDays(
            studentId = "STU-VIG-1",
            movements = movimientosAlta,
            periodStart = "2026-09-01",
            periodEnd = "2026-09-10",
            schoolDays = diasLectivos
        )

        assertEquals(5, diasAplicablesAlta.size, "Solo debe tener 5 oportunidades de asistencia en sus días vigentes")
        assertFalse(diasAplicablesAlta.contains("2026-09-01"))
        assertTrue(diasAplicablesAlta.contains("2026-09-04"))
        assertTrue(diasAplicablesAlta.contains("2026-09-10"))

        // Caso 2: Estudiante con baja y posterior reincorporación
        val movimientosReincorporacion = listOf(
            SchoolMovementRecord("M-A1", "INST-001", "STU-VIG-2", SchoolMovementType.INSCRIPCION_INICIAL, "2026-09-01"),
            SchoolMovementRecord("M-A2", "INST-001", "STU-VIG-2", SchoolMovementType.BAJA_ADMINISTRATIVA, "2026-09-04", departureCategory = DepartureCategory.BAJA_ADMINISTRATIVA),
            SchoolMovementRecord("M-A3", "INST-001", "STU-VIG-2", SchoolMovementType.REINCORPORACION, "2026-09-08")
        )

        val diasAplicablesReinc = SchoolMovementEvaluator.calculateApplicableAttendanceDays(
            studentId = "STU-VIG-2",
            movements = movimientosReincorporacion,
            periodStart = "2026-09-01",
            periodEnd = "2026-09-10",
            schoolDays = diasLectivos
        )

        // Días 09-01, 09-02, 09-03 (activo) -> 3 días
        // Días 09-04, 09-07 (en baja administrativa) -> 0 días
        // Días 09-08, 09-09, 09-10 (reincorporado) -> 3 días
        // Total: 6 días aplicables
        assertEquals(6, diasAplicablesReinc.size, "Debe tener 6 oportunidades de asistencia sumando periodos activos")
        assertTrue(diasAplicablesReinc.contains("2026-09-01"))
        assertTrue(diasAplicablesReinc.contains("2026-09-03"))
        assertFalse(diasAplicablesReinc.contains("2026-09-04"), "El día de la baja administrativa no es oportunidad aplicable")
        assertFalse(diasAplicablesReinc.contains("2026-09-07"), "El periodo entre baja y reincorporación no debe computar")
        assertTrue(diasAplicablesReinc.contains("2026-09-08"), "El día de reincorporación vuelve a computar")
        assertTrue(diasAplicablesReinc.contains("2026-09-10"))
    }

    @Test
    fun testSupabaseAdapterExigeSesionYNoSilenciaErrores() {
        runBlocking {
            val studentRepo = MockStudentRepositoryImpl()
            // Adaptador sin sesión activa
            val repoSinSesion = SupabaseSchoolMovementRepositoryImpl(
                baseUrl = "https://example.supabase.co",
                apiKey = "anon-key",
                sessionProvider = { null },
                studentRepository = studentRepo
            )

            // No debe retornar listas vacías silenciosamente simulando éxito
            assertFailsWith<IllegalStateException> {
                repoSinSesion.getAllMovements()
            }

            assertFailsWith<IllegalStateException> {
                repoSinSesion.getMovementsForStudent("STU-001")
            }

            // clear() debe lanzar UnsupportedOperationException en persistencia remota
            assertFailsWith<UnsupportedOperationException> {
                repoSinSesion.clear()
            }
        }
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
